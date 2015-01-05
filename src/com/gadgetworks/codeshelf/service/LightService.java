package com.gadgetworks.codeshelf.service;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import lombok.EqualsAndHashCode;
import lombok.Getter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gadgetworks.codeshelf.device.LedCmdGroup;
import com.gadgetworks.codeshelf.device.LedSample;
import com.gadgetworks.codeshelf.model.LedRange;
import com.gadgetworks.codeshelf.model.domain.Bay;
import com.gadgetworks.codeshelf.model.domain.DomainObjectProperty;
import com.gadgetworks.codeshelf.model.domain.Facility;
import com.gadgetworks.codeshelf.model.domain.Item;
import com.gadgetworks.codeshelf.model.domain.LedController;
import com.gadgetworks.codeshelf.model.domain.Location;
import com.gadgetworks.codeshelf.model.domain.User;
import com.gadgetworks.codeshelf.ws.jetty.protocol.message.LightLedsMessage;
import com.gadgetworks.codeshelf.ws.jetty.server.SessionManager;
import com.gadgetworks.flyweight.command.ColorEnum;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.ForwardingFuture;
import com.google.inject.Inject;

public class LightService implements IApiService {

	private static final Logger				LOGGER	= LoggerFactory.getLogger(LightService.class);

	private final PropertyService 			propertyService;
	private final SessionManager			sessionManager;
	private final ScheduledExecutorService	mExecutorService;
	private Future<Void>					mLastChaserFuture;

	// may need to fix for multi-tenancy. Cached value as these are referenced extremely frequently. Is there a separate LightService per facility now?
	private ColorEnum						mDiagnosticColor = ColorEnum.RED;
	private int								mLightDurationSeconds = 20;

	@Inject
	public LightService(PropertyService propertyService) {
		this(propertyService, SessionManager.getInstance(), Executors.newSingleThreadScheduledExecutor());
		
		// Cannot do this. LightService is injected. This makes the applciation not start. May have to keep these values elsewhere.
		// setValuesFromConfigs();
	}

	LightService(PropertyService propertyService, SessionManager sessionManager, ScheduledExecutorService executorService) {
		this.propertyService = propertyService;
		this.sessionManager = sessionManager;
		this.mExecutorService = executorService;
	}

	private ColorEnum getDiagnosticColor() {
		return mDiagnosticColor;
	}

	private int getLightDurationSeconds() {
		return mLightDurationSeconds;
	}

	private Facility getMyFacility() {
		// fix for multi-tenancy. LightService should know its facility.
		List<Facility> facilityList = Facility.DAO.getAll();
		int theSize = facilityList.size();
		if (theSize == 0)
			return null;
		if (theSize > 1) {
			LOGGER.error("fix HousekeepingInjector for multi-tenancy");
		}
		return facilityList.get(0);
	}

	public void setValuesFromConfigs() {
		// warning: currently setting the static member variables.
		Facility facility = getMyFacility();
		String lightSecondsValue = PropertyService.getPropertyFromConfig(facility, DomainObjectProperty.LIGHTSEC);
		String lightColorValue = PropertyService.getPropertyFromConfig(facility, DomainObjectProperty.LIGHTCLR);
		if (lightSecondsValue == null || lightColorValue == null) {
			LOGGER.error("problem in LightService.setValuesFromConfigs");
			return;
		}
		mDiagnosticColor = ColorEnum.valueOf(lightColorValue);
		mLightDurationSeconds = Integer.parseInt(lightSecondsValue);

	}

	// --------------------------------------------------------------------------
	/**
	 * Light one item. Any subsequent activity on the aisle controller will wipe this away.
	 */
	public void lightItem(final String facilityPersistentId, final String inItemPersistentId) {
		// checkFacility calls checkNotNull, which throws NPE. ok. Should always have facility.
		Facility facility = checkFacility(facilityPersistentId);

		// should we throw if item not found? No. We can error and move on. This is called directly by the UI message processing.
		Item theItem = Item.DAO.findByPersistentId(inItemPersistentId);
		if (theItem == null) {
			LOGGER.error("persistented id for item not found: " + inItemPersistentId);
			return;
		}

		// IMPORTANT. When DEV-411 resumes, change to 4.  For now, we want only 3 LED lit at GoodEggs.
		if (theItem.isLightable()) {
			sendToAllSiteControllers(facility.getSiteControllerUsers(), toLedsMessage(3, getDiagnosticColor(), theItem));
		} else {
			LOGGER.warn("The item is not lightable: " + theItem);
		}
	}

	public void lightLocation(final String facilityPersistentId, final String inLocationNominalId) {

		Facility facility = checkFacility(facilityPersistentId);

		Location theLocation = checkLocation(facility, inLocationNominalId);
		if (theLocation.getActiveChildren().isEmpty()) {
			lightOneLocation(facility, theLocation);
		} else {
			lightChildLocations(facility, theLocation);
		}
	}

	public Future<Void> lightInventory(final String facilityPersistentId, final String inLocationNominalId) {
		Facility facility = checkFacility(facilityPersistentId);

		Location theLocation = checkLocation(facility, inLocationNominalId);

		List<Set<LightLedsMessage>> messages = Lists.newArrayList();
		for (Item item : theLocation.getInventoryInWorkingOrder()) {
			try {
				if (item.isLightable()) {
					LightLedsMessage message = toLedsMessage(4, getDiagnosticColor(), item);
					messages.add(ImmutableSet.of(message));
				} else {
					LOGGER.warn("unable to light item: " + item);
				}
			} catch (Exception e) {
				LOGGER.warn("unable to light item: " + item, e);

			}
		}
		return chaserLight(facility.getSiteControllerUsers(), messages);
	}

	// --------------------------------------------------------------------------
	/**
	 * Light one location transiently. Any subsequent activity on the aisle controller will wipe this away.
	 * May be called with BLACK to clear whatever you just sent. 
	 */
	private void lightOneLocation(final Facility facility, final Location theLocation) {
		// IMPORTANT. When DEV-411 resumes, change to 4.  For now, we want only 3 LED lit at GoodEggs.
		if (theLocation.isLightable()) {
			LightLedsMessage message = toLedsMessage(4, getDiagnosticColor(), theLocation);
			sendToAllSiteControllers(facility.getSiteControllerUsers(), message);
		} else {
			LOGGER.warn("Unable to light location: " + theLocation);
		}

	}

	// --------------------------------------------------------------------------
	/**
	 * Light one location transiently. Any subsequent activity on the aisle controller will wipe this away.
	 * May be called with BLACK to clear whatever you just sent. 
	 */
	Future<Void> lightChildLocations(final Facility facility, final Location theLocation) {

		List<Set<LightLedsMessage>> ledMessages = Lists.newArrayList();
		if (theLocation instanceof Bay) { //light whole bay at once, consistent across controller configurations
			ledMessages.add(lightAllAtOnce(4, getDiagnosticColor(), theLocation.getChildrenInWorkingOrder()));
		} else {
			List<Location> children = theLocation.getChildrenInWorkingOrder();
			for (Location child : children) {
				try {
					if (child instanceof Bay) {
						//when the child we are lighting is a bay, light all of the tiers at once
						// this will light each controller that may be spanning a bay (e.g. Accu Logistics)
						ledMessages.add(lightAllAtOnce(4, getDiagnosticColor(), child.getChildrenInWorkingOrder()));
					} else {
						if (child.isLightable()) {
							ledMessages.add(ImmutableSet.of(toLedsMessage(4, getDiagnosticColor(), child)));
						}
					}
				} catch (Exception e) {
					LOGGER.warn("Unable to light child: " + child, e);
				}

			}
		}
		return chaserLight(facility.getSiteControllerUsers(), ledMessages);
	}

	Future<Void> chaserLight(final Set<User> siteControllerUsers, final List<Set<LightLedsMessage>> messageSequence) {
		long millisToSleep = 2250;
		final TerminatingScheduledRunnable lightLocationRunnable = new TerminatingScheduledRunnable() {

			private LinkedList<Set<LightLedsMessage>>	chaseListToFire	= Lists.newLinkedList(messageSequence);

			@Override
			public void run() {
				Set<LightLedsMessage> messageSet = chaseListToFire.poll();
				if (messageSet == null) {
					terminate();
				} else {
					for (LightLedsMessage message : messageSet) { //send "all at once" -> quick succession for now
						sessionManager.sendMessage(siteControllerUsers, message);
					}
				}
			}
		};
		return scheduleChaserRunnable(lightLocationRunnable, millisToSleep, TimeUnit.MILLISECONDS);
	}

	private int sendToAllSiteControllers(Set<User> users, LightLedsMessage message) {
		return this.sessionManager.sendMessage(users, message);
	}

	private LightLedsMessage toLedsMessage(int maxNumLeds, final ColorEnum inColor, final Item inItem) {
		// Use our utility function to get the leds for the item
		LedRange theRange = inItem.getFirstLastLedsForItem().capLeds(maxNumLeds);
		Location itemLocation = inItem.getStoredLocation();
		if (itemLocation.isLightable()) {
			LightLedsMessage message = getLedCmdGroupListForRange(inColor, itemLocation, theRange);
			return message;
		} else {
			return null;
		}
	}

	private LightLedsMessage toLedsMessage(int maxNumLeds, final ColorEnum inColor, final Location inLocation) {
		LedRange theRange = inLocation.getFirstLastLedsForLocation().capLeds(maxNumLeds);
		LightLedsMessage message = getLedCmdGroupListForRange(inColor, inLocation, theRange);
		return message;
	}

	private Set<LightLedsMessage> lightAllAtOnce(int numLeds, ColorEnum diagnosticColor, List<Location> children) {
		Map<ControllerChannelKey, LightLedsMessage> byControllerChannel = Maps.newHashMap();
		for (Location child : children) {
			try {
				if (child.isLightable()) {
					LightLedsMessage ledMessage = toLedsMessage(numLeds, diagnosticColor, child);
					ControllerChannelKey key = new ControllerChannelKey(ledMessage.getNetGuidStr(), ledMessage.getChannel());

					//merge messages per controller and key
					LightLedsMessage messageForKey = byControllerChannel.get(key);
					if (messageForKey != null) {
						ledMessage = messageForKey.merge(ledMessage);

					}

					byControllerChannel.put(key, ledMessage);
				} else {
					LOGGER.warn("Unable to light location: " + child);
				}
			} catch (Exception e) {
				LOGGER.warn("Unable to light location: " + child, e);
			}
		}

		return Sets.newHashSet(byControllerChannel.values());
	}

	/**
	 * Utility function to create LED command group. Will return a list, which may be empty if there is nothing to send. Caller should check for empty list.
	 * Called now for setting WI LED pattern for inventory pick.
	 * Also called for directly lighting inventory item or location
	 * @param inNetGuidStr
	 * @param inItem
	 * @param inColor
	 */
	private LightLedsMessage getLedCmdGroupListForRange(final ColorEnum inColor, Location inLocation, final LedRange ledRange) {
		LedController controller = inLocation.getEffectiveLedController();
		Short controllerChannel = inLocation.getEffectiveLedChannel();
		// This should never happen as an ledRange comes in and it had to have controller available to make it.
		if (controller == null || controllerChannel == null) {
			throw new IllegalArgumentException("location with no controller or channel: " + inLocation);
		}
		short firstLedPosNum = ledRange.getFirstLedToLight();
		short lastLedPosNum = ledRange.getLastLedToLight();
		if (firstLedPosNum == 0)
			throw new IllegalArgumentException("location with zero for first Led Position: " + inLocation);

		// This is how we send LED data to the remote controller. In this case, only one led sample range.
		List<LedSample> ledSamples = new ArrayList<LedSample>();
		for (short ledPos = firstLedPosNum; ledPos <= lastLedPosNum; ledPos++) {
			LedSample ledSample = new LedSample(ledPos, inColor);
			ledSamples.add(ledSample);
		}
		LedCmdGroup ledCmdGroup = new LedCmdGroup(controller.getDeviceGuidStr(), controllerChannel, firstLedPosNum, ledSamples);
		return new LightLedsMessage(controller.getDeviceGuidStr(),
			controllerChannel,
			getLightDurationSeconds(),
			ImmutableList.of(ledCmdGroup));
	}

	private Facility checkFacility(final String facilityPersistentId) {
		return checkNotNull(Facility.DAO.findByPersistentId(facilityPersistentId), "Unknown facility: %s", facilityPersistentId);
	}

	private Location checkLocation(Facility facility, final String inLocationNominalId) {
		Location theLocation = facility.findSubLocationById(inLocationNominalId);
		checkArgument(theLocation != null && !(theLocation instanceof Facility),
			"Location nominalId unknown: %s",
			inLocationNominalId);
		return theLocation;
	}

	private Future<Void> scheduleChaserRunnable(final TerminatingScheduledRunnable runPerPeriod,
		long intervalToSleep,
		TimeUnit intervalUnit) {
		if (mLastChaserFuture != null) {
			mLastChaserFuture.cancel(true);
		}

		//Future that ends on exception when pop returns nothing
		@SuppressWarnings({ "unchecked" })
		Future<Void> scheduledFuture = (Future<Void>) mExecutorService.scheduleWithFixedDelay(runPerPeriod,
			0,
			intervalToSleep,
			intervalUnit);

		//Wrap in a future that hides the NoSuchElementException semantics
		Future<Void> chaserFuture = new ForwardingFuture.SimpleForwardingFuture<Void>(scheduledFuture) {
			public Void get() throws ExecutionException, InterruptedException {
				try {
					return delegate().get();
				} catch (ExecutionException e) {
					if (!runPerPeriod.isTerminatingException(e.getCause())) {
						throw e;
					}
				}
				return null;
			}
		};
		mLastChaserFuture = chaserFuture;
		return mLastChaserFuture;
	}

	private static abstract class TerminatingScheduledRunnable implements Runnable {

		private RuntimeException	terminatingException;

		public TerminatingScheduledRunnable() {
			this.terminatingException = new RuntimeException("normal termination");
		}

		public boolean isTerminatingException(Throwable e) {
			return this.terminatingException.equals(e);
		}

		protected void terminate() {
			throw this.terminatingException;
		}
	}

	/*	
		/*
		public void lightAllControllers(final String inColorStr, final String facilityPersistentId, final String inLocationNominalId) {
			
			ColorEnum theColor = ColorEnum.valueOf(inColorStr);
			if (theColor == ColorEnum.INVALID) {
				LOGGER.error("lightOneLocation called with unknown color");
				return;
			}

			Facility facility = Facility.DAO.findByPersistentId(facilityPersistentId);
			if (facility == null) {
				LOGGER.error("lightAllControllers called with unknown facility");
				return;
			}

			
			LocationABC theLocation = facility.findSubLocationById(inLocationNominalId);
			if (theLocation == null || theLocation instanceof Facility) {
				LOGGER.error("lightAllControllers called with unknown location");
				return;
			}

			HashMap<ControllerChannelBayKey, Tier> lastLocationWithinBay = new HashMap<ControllerChannelBayKey, Tier>();
			List<Tier> tiers = theLocation.getActiveChildrenAtLevel(Tier.class);
			if (tiers.isEmpty()) {
				LOGGER.error("lightAllControllers called with location with no tiers");
				return;
			}
			
			//Last tier within bay per controller
			for (Tier tier : tiers) {
				ControllerChannelBayKey key = new ControllerChannelBayKey(tier.getEffectiveLedController(), tier.getEffectiveLedChannel(), (Bay) tier.getParent());
				LocationABC lastLocation = lastLocationWithinBay.get(key);

				if (lastLocation == null || lastLocation.getLastLedNumAlongPath() < tier.getLastLedNumAlongPath()) {
					lastLocationWithinBay.put(key, tier);
				}
			}
			
			List<Tier> sortedTiers = Lists.newArrayList(lastLocationWithinBay.values());
			Collections.sort(sortedTiers, new LocationABC.LocationWorkingOrderComparator());
		
			lightLocations(theColor, facility.getPersistentId(), sortedTiers);
		}
		
		private final List<LightLedsMessage> toLedMessages(List<LedCmdGroup> ledCmdGroupList) {
			List<LightLedsMessage> ledMessages = Lists.newArrayList();
			
			ArrayListMultimap<String, LedCmdGroup> byLedController = ArrayListMultimap.<String, LedCmdGroup>create();
			for (LedCmdGroup ledCmdGroup : ledCmdGroupList) {
				byLedController.put(ledCmdGroup.getControllerId(), ledCmdGroup);
			}
			
			for (String theGuidStr : byLedController.keys()) {
				List<LedCmdGroup> ledCmdGroups = byLedController.get(theGuidStr);
				
				String theLedCommands = LedCmdGroupSerializer.serializeLedCmdString(ledCmdGroups);
				LightLedsMessage theMessage = new LightLedsMessage(theGuidStr, getLightDurationSeconds(), theLedCommands);
				ledMessages.add(theMessage);
			}
			return ledMessages;
		}
		
		
	*/
	@EqualsAndHashCode
	private static class ControllerChannelKey {

		@Getter
		private String	controllerGuid;

		@Getter
		private short	channel;

		public ControllerChannelKey(String controllerGuid, short channel) {
			this.controllerGuid = controllerGuid;
			this.channel = channel;
		}
	}
}
