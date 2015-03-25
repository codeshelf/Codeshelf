package com.codeshelf.service;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import lombok.EqualsAndHashCode;
import lombok.Getter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.device.LedCmdGroup;
import com.codeshelf.device.LedInstrListMessage;
import com.codeshelf.device.LedSample;
import com.codeshelf.device.PosControllerInstr;
import com.codeshelf.device.PosControllerInstrList;
import com.codeshelf.flyweight.command.ColorEnum;
import com.codeshelf.manager.User;
import com.codeshelf.model.LedRange;
import com.codeshelf.model.domain.DomainObjectProperty;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.Item;
import com.codeshelf.model.domain.LedController;
import com.codeshelf.model.domain.Location;
import com.codeshelf.model.domain.WorkInstruction;
import com.codeshelf.ws.jetty.protocol.message.LightLedsMessage;
import com.codeshelf.ws.jetty.protocol.message.MessageABC;
import com.codeshelf.ws.jetty.server.WebSocketManagerService;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.ForwardingFuture;
import com.google.inject.Inject;

public class LightService implements IApiService {

	private static final Logger				LOGGER						= LoggerFactory.getLogger(LightService.class);

	private final WebSocketManagerService			webSocketManagerService;
	private final ScheduledExecutorService	mExecutorService;
	private Future<Void>					mLastChaserFuture;

	private final static ColorEnum			defaultColor				= ColorEnum.RED;
	private final static int				defaultLightDurationSeconds	= 20;

	// Originally 4 leds. The aisle file read and UI indicates 4 leds.  Was changed to 3 leds before aisle controller message splitting to allow more simultaneous lighting.
	// Should not longer be necessary. Between v6 and v10 there was some inconsistency between 3 and 4. Now consistent.  Ideally no configuration parameter for this because if set
	// otherwise, the UI still show 4. See DEV-411
	private final static int				defaultLedsToLight			= 4; 	// IMPORTANT. This should be synched with WIFactory.maxLedsToLight

	@Inject
	public LightService(WebSocketManagerService webSocketManagerService) {
		this(webSocketManagerService, Executors.newSingleThreadScheduledExecutor());
	}

	LightService(WebSocketManagerService webSocketManagerService, ScheduledExecutorService executorService) {
		this.webSocketManagerService = webSocketManagerService;
		this.mExecutorService = executorService;
	}

	// --------------------------------------------------------------------------
	/**
	 * Light one item. Any subsequent activity on the aisle controller will wipe this away.
	 */
	public void lightItem(final String facilityPersistentId, final String inItemPersistentId) {
		// checkFacility calls checkNotNull, which throws NPE. ok. Should always have facility.
		Facility facility = checkFacility(facilityPersistentId);
		ColorEnum color = PropertyService.getInstance().getPropertyAsColor(facility, DomainObjectProperty.LIGHTCLR, defaultColor);

		lightItemSpecificColor(facilityPersistentId, inItemPersistentId, color);
	}
	
	// --------------------------------------------------------------------------
	/**
	 * Light one item. Any subsequent activity on the aisle controller will wipe this away.
	 */
	public void lightItemSpecificColor(final String facilityPersistentId, final String inItemPersistentId, ColorEnum color) {
		// checkFacility calls checkNotNull, which throws NPE. ok. Should always have facility.
		Facility facility = checkFacility(facilityPersistentId);

		// should we throw if item not found? No. We can error and move on. This is called directly by the UI message processing.
		Item theItem = Item.staticGetDao().findByPersistentId(inItemPersistentId);
		if (theItem == null) {
			LOGGER.error("persistented id for item not found: " + inItemPersistentId);
			return;
		}

		if (theItem.isLightable()) {
			sendMessage(facility.getSiteControllerUsers(), toLedsMessage(facility, defaultLedsToLight, color, theItem));
		} else {
			LOGGER.warn("The item is not lightable: " + theItem);
		}
	}

	public void lightLocation(final String facilityPersistentId, final String inLocationNominalId) {

		final Facility facility = checkFacility(facilityPersistentId);
		ColorEnum color = PropertyService.getInstance().getPropertyAsColor(facility, DomainObjectProperty.LIGHTCLR, defaultColor);

		Location theLocation = checkLocation(facility, inLocationNominalId);
		//if (theLocation.getActiveChildren().isEmpty()) {
		//	lightOneLocation(facility, theLocation);
		//} else {
		lightChildLocationsNew(facility, theLocation, color);
		//}
		
		//Light the POS range
		List<PosControllerInstr> instructions = new ArrayList<PosControllerInstr>();
		getInstructionsForPosConRange(facility, null, theLocation, instructions);
		final PosControllerInstrList message = new PosControllerInstrList(instructions);
		sendMessage(facility.getSiteControllerUsers(), message);
		//Modify all POS commands to clear their POSs instead.
		new Timer().schedule(new TimerTask() {
			@Override
			public void run() {
				LOGGER.info("AisleDeviceLogic expire timer fired.");
				for (PosControllerInstr instructions : message.getInstructions()){
					instructions.getRemovePos().add(instructions.getPosition());
				}
				sendMessage(facility.getSiteControllerUsers(), message);
			}
		}, 20000);
	}

	public void lightInventory(final String facilityPersistentId, final String inLocationNominalId) {
		Facility facility = checkFacility(facilityPersistentId);
		ColorEnum color = PropertyService.getInstance().getPropertyAsColor(facility, DomainObjectProperty.LIGHTCLR, defaultColor);

		Location theLocation = checkLocation(facility, inLocationNominalId);

		List<LightLedsMessage> instructions = Lists.newArrayList();
		for (Item item : theLocation.getInventoryInWorkingOrder()) {
			try {
				if (item.isLightable()) {
					LightLedsMessage instruction = toLedsMessage(facility, defaultLedsToLight, color, item);
					instructions.add(instruction);
				} else {
					LOGGER.warn("unable to light item: " + item);
				}
			} catch (Exception e) {
				LOGGER.warn("unable to light item: " + item, e);

			}
		}
		LedInstrListMessage message = new LedInstrListMessage(instructions);
		sendMessage(facility.getSiteControllerUsers(), message);
	}
	/*
	public Future<Void> lightItemsSpecificColor(final String facilityPersistentId, final List<Item> items, ColorEnum color) {
		Facility facility = checkFacility(facilityPersistentId);

		List<Set<LightLedsMessage>> messages = Lists.newArrayList();
		for (Item item : items) {
			try {
				if (item.isLightable()) {
					LightLedsMessage message = toLedsMessage(facility, defaultLedsToLight, color, item);
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
	*/
	public void lightItemsSpecificColor(final String facilityPersistentId, final List<Item> items, ColorEnum color) {
		Facility facility = checkFacility(facilityPersistentId);

		List<LightLedsMessage> messages = Lists.newArrayList();
		for (Item item : items) {
			try {
				if (item.isLightable()) {
					LightLedsMessage message = toLedsMessage(facility, defaultLedsToLight, color, item);
					messages.add(message);
				} else {
					LOGGER.warn("unable to light item: " + item);
				}
			} catch (Exception e) {
				LOGGER.warn("unable to light item: " + item, e);

			}
		}
		LedInstrListMessage message = new LedInstrListMessage(messages);
		sendMessage(facility.getSiteControllerUsers(), message);
	}


	// --------------------------------------------------------------------------
	/**
	 * Light one location transiently. Any subsequent activity on the aisle controller will wipe this away.
	 * May be called with BLACK to clear whatever you just sent. 
	 */
	/*
	private void lightOneLocation(final Facility facility, final Location theLocation) {
		ColorEnum color = PropertyService.getInstance().getPropertyAsColor(facility, DomainObjectProperty.LIGHTCLR, defaultColor);

		if (theLocation.isLightableAisleController()) {
			LightLedsMessage message = toLedsMessage(facility, defaultLedsToLight, color, theLocation);
			sendMessage(facility.getSiteControllerUsers(), message);
		} else {
			LOGGER.warn("Unable to light location: " + theLocation);
		}
		
	}
	*/
	public static void getInstructionsForPosConRange(final Facility facility, final WorkInstruction wi, final Location theLocation, List<PosControllerInstr> instructions){
		if (theLocation == null) {return;}
		if (theLocation.isLightablePoscon()) {
			LedController controller = theLocation.getEffectiveLedController();
			String posConController = controller == null ? "" : controller.getDeviceGuidStr();
			int posConIndex = theLocation.getPosconIndex();
			PosControllerInstr message = null;
			if (wi == null) {
				 message = new PosControllerInstr(
					posConController,
					(byte) posConIndex,
					PosControllerInstr.BITENCODED_SEGMENTS_CODE,
					PosControllerInstr.BITENCODED_TRIPLE_DASH,
					PosControllerInstr.BITENCODED_TRIPLE_DASH,
					PosControllerInstr.BLINK_FREQ,
					PosControllerInstr.BRIGHT_DUTYCYCLE);
			} else {
				message = new PosControllerInstr(
					posConController,
					(byte) posConIndex,
					wi.getPlanQuantity().byteValue(),
					wi.getPlanMinQuantity().byteValue(),
					wi.getPlanMaxQuantity().byteValue(),
					PosControllerInstr.SOLID_FREQ,
					PosControllerInstr.BRIGHT_DUTYCYCLE);
 
			}
			instructions.add(message);
		}
		List<Location> children = theLocation.getActiveChildren();
		if (!children.isEmpty()){
			for (Location child : children) {
				getInstructionsForPosConRange(facility, wi, child, instructions);
			}
		}
	}

	// --------------------------------------------------------------------------
	
	void lightChildLocationsNew(final Facility facility, final Location theLocation, ColorEnum color) {
		List<Location> leaves = Lists.newArrayList(); 
		getAllLedLightableLeaves(theLocation, leaves);
		List<LightLedsMessage> instructions = lightAllAtOnceNew(facility, defaultLedsToLight, color, leaves);
		LedInstrListMessage message = new LedInstrListMessage(instructions);
		sendMessage(facility.getSiteControllerUsers(), message);
	}
	
	private void getAllLedLightableLeaves(final Location theLocation, List<Location> leaves) {
		List<Location> children = theLocation.getActiveChildren();
		if (children.isEmpty()) {
			if (theLocation.isLightableAisleController()) {
				leaves.add(theLocation);
			}
		} else {
			for (Location child : children) {
				getAllLedLightableLeaves(child, leaves);
			}
		}
	}

	/**
	 * Light one location transiently. Any subsequent activity on the aisle controller will wipe this away.
	 * May be called with BLACK to clear whatever you just sent. 
	 */
	/*
	Future<Void> lightChildLocations(final Facility facility, final Location theLocation, ColorEnum color) {

		List<Set<LightLedsMessage>> ledMessages = Lists.newArrayList();
		if (theLocation.isBay()) { //light whole bay at once, consistent across controller configurations
			ledMessages.add(lightAllAtOnce(facility, defaultLedsToLight, color, theLocation.getChildrenInWorkingOrder()));
		} else {
			List<Location> children = theLocation.getChildrenInWorkingOrder();
			for (Location child : children) {
				try {
					if (child.isBay()) {
						//when the child we are lighting is a bay, light all of the tiers at once
						// this will light each controller that may be spanning a bay (e.g. Accu Logistics)
						ledMessages.add(lightAllAtOnce(facility, defaultLedsToLight, color, child.getChildrenInWorkingOrder()));
					} else {
						if (child.isLightableAisleController()) {
							ledMessages.add(ImmutableSet.of(toLedsMessage(facility, defaultLedsToLight, color, child)));
						}
					}
				} catch (Exception e) {
					LOGGER.warn("Unable to light child: " + child, e);
				}

			}
		}
		return chaserLight(facility.getSiteControllerUsers(), ledMessages);
	}
	*/
	/*
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
						sendMessage(siteControllerUsers, message);
					}
				}
			}
		};
		return scheduleChaserRunnable(lightLocationRunnable, millisToSleep, TimeUnit.MILLISECONDS);
	}
	*/
	private int sendMessage(Set<User> users, MessageABC message) {
		return webSocketManagerService.sendMessage(users, message);
	}

	private LightLedsMessage toLedsMessage(Facility facility, int maxNumLeds, final ColorEnum inColor, final Item inItem) {
		// Use our utility function to get the leds for the item
		LedRange theRange = inItem.getFirstLastLedsForItem().capLeds(maxNumLeds);
		Location itemLocation = inItem.getStoredLocation();
		if (itemLocation.isLightableAisleController()) {
			LightLedsMessage message = getLedCmdGroupListForRange(facility, inColor, itemLocation, theRange);
			return message;
		} else {
			return null;
		}
	}

	private LightLedsMessage toLedsMessage(Facility facility, int maxNumLeds, final ColorEnum inColor, final Location inLocation) {
		LedRange theRange = inLocation.getFirstLastLedsForLocation().capLeds(maxNumLeds);
		LightLedsMessage message = getLedCmdGroupListForRange(facility, inColor, inLocation, theRange);
		return message;
	}
	
	/*
	private Set<LightLedsMessage> lightAllAtOnce(Facility facility, int numLeds, ColorEnum diagnosticColor, List<Location> children) {
		Map<ControllerChannelKey, LightLedsMessage> byControllerChannel = Maps.newHashMap();
		for (Location child : children) {
			try {
				if (child.isLightableAisleController()) {
					LightLedsMessage ledMessage = toLedsMessage(facility, numLeds, diagnosticColor, child);
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
	*/
	
	private List<LightLedsMessage> lightAllAtOnceNew(Facility facility, int numLeds, ColorEnum diagnosticColor, List<Location> children) {
		Map<ControllerChannelKey, LightLedsMessage> byControllerChannel = Maps.newHashMap();
		for (Location child : children) {
			try {
				if (child.isLightableAisleController()) {
					LightLedsMessage ledMessage = toLedsMessage(facility, numLeds, diagnosticColor, child);
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

		return new ArrayList<LightLedsMessage>(byControllerChannel.values());
	}


	/**
	 * Utility function to create LED command group. Will return a list, which may be empty if there is nothing to send. Caller should check for empty list.
	 * Called now for setting WI LED pattern for inventory pick.
	 * Also called for directly lighting inventory item or location
	 * @param inNetGuidStr
	 * @param inItem
	 * @param inColor
	 */
	private LightLedsMessage getLedCmdGroupListForRange(Facility facility,
		final ColorEnum inColor,
		Location inLocation,
		final LedRange ledRange) {
		int lightDuration = PropertyService.getInstance().getPropertyAsInt(facility,
			DomainObjectProperty.LIGHTSEC,
			defaultLightDurationSeconds);

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
		return new LightLedsMessage(controller.getDeviceGuidStr(), controllerChannel, lightDuration, ImmutableList.of(ledCmdGroup));
	}

	private Facility checkFacility(final String facilityPersistentId) {
		return checkNotNull(Facility.staticGetDao().findByPersistentId(facilityPersistentId), "Unknown facility: %s", facilityPersistentId);
	}

	private Location checkLocation(Facility facility, final String inLocationNominalId) {
		Location theLocation = facility.findSubLocationById(inLocationNominalId);
		checkArgument(theLocation != null && !(theLocation.isFacility()),
			"Location nominalId unknown: %s",
			inLocationNominalId);
		return theLocation;
	}
	/*
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
	*/
	
	/*	WAS COMMENTED OUT BEFORE CURRENT REFACTORING 
		/*
		public void lightAllControllers(final String inColorStr, final String facilityPersistentId, final String inLocationNominalId) {
			
			ColorEnum theColor = ColorEnum.valueOf(inColorStr);
			if (theColor == ColorEnum.INVALID) {
				LOGGER.error("lightOneLocation called with unknown color");
				return;
			}

			Facility facility = Facility.staticGetDao().findByPersistentId(facilityPersistentId);
			if (facility == null) {
				LOGGER.error("lightAllControllers called with unknown facility");
				return;
			}

			
			LocationABC theLocation = facility.findSubLocationById(inLocationNominalId);
			if (theLocation == null || theLocation.isFacility()) {
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
