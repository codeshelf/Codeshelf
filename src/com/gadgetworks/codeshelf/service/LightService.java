package com.gadgetworks.codeshelf.service;

import static com.google.common.base.Preconditions.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gadgetworks.codeshelf.device.LedCmdGroup;
import com.gadgetworks.codeshelf.device.LedCmdGroupSerializer;
import com.gadgetworks.codeshelf.device.LedSample;
import com.gadgetworks.codeshelf.model.LedRange;
import com.gadgetworks.codeshelf.model.domain.Aisle;
import com.gadgetworks.codeshelf.model.domain.Facility;
import com.gadgetworks.codeshelf.model.domain.ILocation;
import com.gadgetworks.codeshelf.model.domain.ISubLocation;
import com.gadgetworks.codeshelf.model.domain.Item;
import com.gadgetworks.codeshelf.model.domain.LedController;
import com.gadgetworks.codeshelf.model.domain.LocationABC;
import com.gadgetworks.codeshelf.model.domain.Tier;
import com.gadgetworks.codeshelf.model.domain.User;
import com.gadgetworks.codeshelf.ws.jetty.protocol.message.LightLedsMessage;
import com.gadgetworks.codeshelf.ws.jetty.server.SessionManager;
import com.gadgetworks.flyweight.command.ColorEnum;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ForwardingFuture;

public class LightService implements IApiService {

	private static final int				LIGHT_LOCATION_DURATION_SECS	= 20;
	private static final Logger				LOGGER							= LoggerFactory.getLogger(LightService.class);

	private final SessionManager			sessionManager;
	private final ScheduledExecutorService	mExecutorService;
	private Future<Void>					mLastChaserFuture;

	public LightService() {
		this(SessionManager.getInstance(), Executors.newSingleThreadScheduledExecutor());

	}

	public LightService(SessionManager sessionManager, ScheduledExecutorService executorService) {
		this.sessionManager = sessionManager;
		this.mExecutorService = executorService;
	}

	// --------------------------------------------------------------------------
	/**
	 * Light one item. Any subsequent activity on the aisle controller will wipe this away.
	 * May be called with BLACK to clear whatever you just sent.
	 */
	public void lightItem(final String facilityPersistentId, final String inItemPersistentId) {

		Facility facility = checkFacility(facilityPersistentId);

		Item theItem = checkNotNull(Item.DAO.findByPersistentId(inItemPersistentId),
			"persistented id for item not found: %s",
			inItemPersistentId);

		// IMPORTANT. When DEV-411 resumes, change to 4.  For now, we want only 3 LED lit at GoodEggs.
		sendToAllSiteControllers(facility, toLedsMessage(3, facility.getDiagnosticColor(), theItem));
	}

	public void lightLocation(final String facilityPersistentId, final String inLocationNominalId) {

		Facility facility = checkFacility(facilityPersistentId);

		ISubLocation<?> theLocation = checkLocation(facility, inLocationNominalId);
		if (theLocation.getActiveChildren().isEmpty()) {
			lightOneLocation(facility, theLocation);
		} else {
			lightChildLocations(facility, theLocation);
		}
	}

	public Future<Void> lightInventory(final String facilityPersistentId, final String inLocationNominalId) {
		Facility facility = checkFacility(facilityPersistentId);

		ISubLocation<?> theLocation = checkLocation(facility, inLocationNominalId);

		List<LightLedsMessage> messages = Lists.newArrayList();
		for (Item item : theLocation.getInventoryInWorkingOrder()) {
			messages.add(toLedsMessage(3, facility.getDiagnosticColor(), item));
		}
		return chaserLight(facility, messages);
	}

	// --------------------------------------------------------------------------
	/**
	 * Light one location transiently. Any subsequent activity on the aisle controller will wipe this away.
	 * May be called with BLACK to clear whatever you just sent. 
	 */
	private void lightOneLocation(final Facility facility, final ISubLocation<?> theLocation) {

		// IMPORTANT. When DEV-411 resumes, change to 4.  For now, we want only 3 LED lit at GoodEggs.
		sendToAllSiteControllers(facility, toLedsMessage(3, facility.getDiagnosticColor(), theLocation));
	}

	// --------------------------------------------------------------------------
	/**
	 * Light one location transiently. Any subsequent activity on the aisle controller will wipe this away.
	 * May be called with BLACK to clear whatever you just sent. 
	 */
	Future<Void> lightChildLocations(final Facility facility, final ISubLocation<?> theLocation) {

		List<LightLedsMessage> ledMessages = Lists.newArrayList();
		if (theLocation instanceof Aisle) { //TODO lost the OO here
			List<ISubLocation<?>> children = theLocation.getActiveChildrenAtLevel(Tier.class);
			Collections.sort(children, new LocationABC.LocationWorkingOrderComparator());
			for (@SuppressWarnings("rawtypes")
			ISubLocation child : children) {
				ledMessages.add(toLedsMessage(4, facility.getDiagnosticColor(), child));
			}
			return chaserLight(facility, ledMessages);
		} else {
			List<ISubLocation> children = theLocation.getChildrenInWorkingOrder();
			for (@SuppressWarnings("rawtypes")
			ISubLocation child : children) {
				ledMessages.add(toLedsMessage(4, facility.getDiagnosticColor(), child));
			}
			return chaserLight(facility, ledMessages);
		}

	}

	Future<Void> chaserLight(final Facility facility, final List<LightLedsMessage> messageSequence) {
		long millisToSleep = 2250;
		final TerminatingScheduledRunnable lightLocationRunnable = new TerminatingScheduledRunnable() {

			private LinkedList<LightLedsMessage>	chaseListToFire	= Lists.newLinkedList(messageSequence);

			@Override
			public void run() {
				LightLedsMessage message = chaseListToFire.poll();
				if (message == null) {
					terminate();
				}
				sendToAllSiteControllers(facility, message);
			}
		};
		return scheduleChaserRunnable(lightLocationRunnable, millisToSleep, TimeUnit.MILLISECONDS);
	}

	private int sendToAllSiteControllers(Facility facility, LightLedsMessage message) {
		Set<User> users = facility.getSiteControllerUsers();
		return this.sessionManager.sendMessage(users, message);
	}

	private LightLedsMessage toLedsMessage(int maxNumLeds, final ColorEnum inColor, final Item inItem) {
		// Use our utility function to get the leds for the item
		LedRange theRange = inItem.getFirstLastLedsForItem().capLeds(maxNumLeds);
		LightLedsMessage message = getLedCmdGroupListForRange(inColor, inItem.getStoredLocation(), theRange);
		if (message == null) {
			throw new IllegalArgumentException("inItem with incomplete LED configuration: " + inItem);
		} else {
			return message;
		}
	}

	private LightLedsMessage toLedsMessage(int maxNumLeds, final ColorEnum inColor, final ILocation<?> inLocation) {
		LedRange theRange = ((LocationABC<?>) inLocation).getFirstLastLedsForLocation().capLeds(maxNumLeds);
		LightLedsMessage message = getLedCmdGroupListForRange(inColor, inLocation, theRange);
		if (message == null) {
			throw new IllegalArgumentException("location with incomplete LED configuration: " + inLocation);
		} else {
			return message;
		}
	}

	/**
	 * Utility function to create LED command group. Will return a list, which may be empty if there is nothing to send. Caller should check for empty list.
	 * Called now for setting WI LED pattern for inventory pick.
	 * Also called for directly lighting inventory item or location
	 * @param inNetGuidStr
	 * @param inItem
	 * @param inColor
	 */
	private LightLedsMessage getLedCmdGroupListForRange(final ColorEnum inColor, ILocation<?> inLocation, final LedRange ledRange) {
		LedController controller = inLocation.getEffectiveLedController();
		Short controllerChannel = inLocation.getEffectiveLedChannel();
		// This should never happen as an ledRange comes in and it had to have controller available to make it.
		if (controller == null || controllerChannel == null)
			throw new IllegalArgumentException("location with incomplete LED configuration: " + inLocation);

		short firstLedPosNum = ledRange.getFirstLedToLight();
		short lastLedPosNum = ledRange.getLastLedToLight();
		if (firstLedPosNum == 0)
			return null;

		// This is how we send LED data to the remote controller. In this case, only one led sample range.
		List<LedSample> ledSamples = new ArrayList<LedSample>();
		for (short ledPos = firstLedPosNum; ledPos <= lastLedPosNum; ledPos++) {
			LedSample ledSample = new LedSample(ledPos, inColor);
			ledSamples.add(ledSample);
		}
		LedCmdGroup ledCmdGroup = new LedCmdGroup(controller.getDeviceGuidStr(), controllerChannel, firstLedPosNum, ledSamples);
		String theLedCommands = LedCmdGroupSerializer.serializeLedCmdString(ImmutableList.of(ledCmdGroup));
		return new LightLedsMessage(controller.getDeviceGuidStr(), LIGHT_LOCATION_DURATION_SECS, theLedCommands);
	}

	private Facility checkFacility(final String facilityPersistentId) {
		return checkNotNull(Facility.DAO.findByPersistentId(facilityPersistentId), "Unknown facility: %s", facilityPersistentId);
	}

	private ISubLocation<?> checkLocation(Facility facility, final String inLocationNominalId) {
		ISubLocation<?> theLocation = facility.findSubLocationById(inLocationNominalId);
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

			
			ISubLocation<?> theLocation = facility.findSubLocationById(inLocationNominalId);
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
				ISubLocation<?> lastLocation = lastLocationWithinBay.get(key);

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
				LightLedsMessage theMessage = new LightLedsMessage(theGuidStr, LIGHT_LOCATION_DURATION_SECS, theLedCommands);
				ledMessages.add(theMessage);
			}
			return ledMessages;
		}
		
		@EqualsAndHashCode
		private class ControllerChannelBayKey  {
			
			@Getter
			private String controllerGuid;
			
			@Getter
			private short channel;
			private String bayId;
			
			public ControllerChannelBayKey(LedController controller, short channel, Bay bay) {
				this.controllerGuid = controller.getDeviceGuidStr();
				this.channel = channel;
				this.bayId = bay.getLocationId();
			}
		}
	*/
}