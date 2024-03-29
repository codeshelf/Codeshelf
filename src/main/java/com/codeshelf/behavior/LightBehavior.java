package com.codeshelf.behavior;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

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
import com.codeshelf.model.FacilityPropertyType;
import com.codeshelf.model.LedRange;
import com.codeshelf.model.domain.Bay;
import com.codeshelf.model.domain.Che;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.Item;
import com.codeshelf.model.domain.LedController;
import com.codeshelf.model.domain.Location;
import com.codeshelf.model.domain.WorkInstruction;
import com.codeshelf.ws.protocol.message.LightLedsInstruction;
import com.codeshelf.ws.protocol.message.MessageABC;
import com.codeshelf.ws.server.WebSocketManagerService;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Inject;

public class LightBehavior implements IApiBehavior {

	private static final Logger		LOGGER						= LoggerFactory.getLogger(LightBehavior.class);

	private final static ColorEnum	defaultColor				= ColorEnum.RED;
	private final static int		defaultLightDurationSeconds	= 20;

	// Originally 4 leds.
	private final static int		defaultLedsToLight			= 4;

	// IMPORTANT. maxNormalLedsToLight should be synched with LightBehavior.defaultLedsToLight
	private static final int		maxNormalLedsToLight		= 4;
	private static final int		maxMaxLedsToLight			= 20;											// Let hard configuration go up to 20.

	// There may be other business cases similar to palletizer that want even more.

	@Inject
	public LightBehavior() {
	}

	// --------------------------------------------------------------------------
	/**
	 * This function is called by UI to illuminate a location by name
	 */
	public void lightLocation(final String facilityPersistentId, final String inLocationNominalId) {
		final Facility facility = checkFacility(facilityPersistentId);
		Location theLocation = checkLocation(facility, inLocationNominalId);
		ColorEnum color = PropertyBehavior.getPropertyAsColor(facility, FacilityPropertyType.LIGHTCLR, defaultColor);
		lightLocationServerCall(theLocation, color);
	}

	/**
	 * This fucntion is called by the server (which already has access to Facility and Locaiton objects)
	 */
	public void lightLocationServerCall(final Location theLocation, ColorEnum color) {
		List<Location> locations = Lists.newArrayList();
		locations.add(theLocation);
		lightLocationServerCall(locations, color);
	}

	/**
	 * This fucntion is called by the server (which already has access to Facility and Locaiton objects)
	 */
	public void lightLocationServerCall(final List<Location> locations, ColorEnum color) {
		if (locations == null || locations.isEmpty()) {
			return;
		}

		final Facility facility = locations.get(0).getFacility();

		//Light LEDs
		lightChildLocations(facility, locations, color);

		//Light the POS range
		List<PosControllerInstr> instructions = new ArrayList<PosControllerInstr>();
		//The following collection contains a list of all Controllers that contain affected (lit) PosCons
		//When the time comes to extinguish illuminated PosCons, a clear-all instruction will be sent to each of those device
		final HashSet<String> affectedControllers = new HashSet<String>();
		getInstructionsForPosConRange(facility, null, locations, instructions, affectedControllers);
		PosControllerInstrList posMessage = new PosControllerInstrList(instructions);
		sendMessage(facility.getSiteControllerUsers(), posMessage);
		int lightDuration = PropertyBehavior.getPropertyAsInt(facility, FacilityPropertyType.LIGHTSEC, defaultLightDurationSeconds);
		//Extinguish all PosCons in affected controllers after some time. This will not affect displayed instructions send from other devices (such as CHEs)
		new Timer().schedule(new TimerTask() {
			@Override
			public void run() {
				LOGGER.info("AisleDeviceLogic expire timer fired.");
				//Generate a list of clear-all instruction
				List<PosControllerInstr> instructions = Lists.newArrayList();
				for (String controller : affectedControllers) {
					PosControllerInstr instructionClear = new PosControllerInstr();
					instructionClear.setControllerId(controller);
					instructionClear.setRemove("all");
					instructionClear.processRemoveField();
					instructions.add(instructionClear);
				}
				//Send the generated list
				PosControllerInstrList clearListMessage = new PosControllerInstrList(instructions);
				sendMessage(facility.getSiteControllerUsers(), clearListMessage);
			}
		}, lightDuration * 1000);
	}

	/**
	 * The primary use of this is after a scan of tape or location in INVENTORY mode. We want to light where user scanned.
	 * Initially, only LEDs, but we could add poscon later.
	 * This calls getMetersFromAnchorGivenCmFromLeft() which may throw InputValidationException
	 * @param color 
	 */
	public void lightLocationCmFromLeft(Location inLocation, int inCmFromLeft, ColorEnum color) {
		if (inLocation == null) {
			LOGGER.error("null location in lightLocationOffset");
			return;
		}
		Facility facility = inLocation.getFacility();

		if (inLocation.isLightableAisleController()) {
			List<LightLedsInstruction> instructions = Lists.newArrayList();

			LedRange theRange = getLedRangeForLocationCmFromLeft(inLocation, inCmFromLeft);
			LightLedsInstruction instruction = getLedCmdGroupListForRange(facility, color, inLocation, theRange);
			instructions.add(instruction);

			LedInstrListMessage message = new LedInstrListMessage(instructions);
			sendMessage(facility.getSiteControllerUsers(), message);
		}

	}

	public void lightInventory(final String facilityPersistentId, final String inLocationNominalId) {
		Facility facility = checkFacility(facilityPersistentId);
		ColorEnum color = PropertyBehavior.getPropertyAsColor(facility, FacilityPropertyType.LIGHTCLR, defaultColor);
		Location theLocation = checkLocation(facility, inLocationNominalId);
		List<Item> items = theLocation.getInventoryInWorkingOrder();
		lightItemsSpecificColor(facilityPersistentId, items, color);
	}

	public void lightItem(Item item, ColorEnum color) {
		lightItemSpecificColor(item.getFacility().getPersistentId().toString(), item.getPersistentId().toString(), color);
	}

	/**
	 * Light one item. Any subsequent activity on the aisle controller will wipe this away.
	 */
	public void lightItem(final String facilityPersistentId, final String inItemPersistentId) {
		Facility facility = checkFacility(facilityPersistentId);
		ColorEnum color = PropertyBehavior.getPropertyAsColor(facility, FacilityPropertyType.LIGHTCLR, defaultColor);
		lightItemSpecificColor(facilityPersistentId, inItemPersistentId, color);
	}

	/**
	 * Light one item. Any subsequent activity on the aisle controller will wipe this away.
	 */
	public void lightItemSpecificColor(final String facilityPersistentId, final String inItemPersistentId, ColorEnum color) {
		// should we throw if item not found? No. We can error and move on. This is called directly by the UI message processing.
		Item theItem = Item.staticGetDao().findByPersistentId(inItemPersistentId);
		if (theItem == null) {
			LOGGER.error("persistented id for item not found: " + inItemPersistentId);
			return;
		}
		List<Item> itemList = Lists.newArrayList();
		itemList.add(theItem);
		lightItemsSpecificColor(facilityPersistentId, itemList, color);
	}

	public void lightItemsSpecificColor(final String facilityPersistentId, final List<Item> items, ColorEnum color) {
		// checkFacility calls checkNotNull, which throws NPE. ok. Should always have facility.
		Facility facility = checkFacility(facilityPersistentId);
		List<LightLedsInstruction> instructions = Lists.newArrayList();
		for (Item item : items) {
			try {
				if (item.isLightable()) {
					LightLedsInstruction instruction = toLedsInstruction(facility, defaultLedsToLight, color, item);
					if (instruction != null) {
						instructions.add(instruction);
					}
				} else {
					LOGGER.warn("unable to light item: " + item);
				}
			} catch (Exception e) {
				LOGGER.warn("unable to light item: " + item, e);

			}
		}
		instructions = groupByControllerAndChanel(instructions);
		LedInstrListMessage message = new LedInstrListMessage(instructions);
		sendMessage(facility.getSiteControllerUsers(), message);
	}

	/**
	 * creates and returns the instruction showing active count for this work instruction.
	 * (Might want to move this so site controller code can use it similarly)
	 */
	private static PosControllerInstr getWiCountInstruction(String inControllerId,
		String inSourceGuid,
		byte inPosition,
		WorkInstruction inWorkInstruction) {
		Byte freq = (inWorkInstruction.getPlanMinQuantity() < inWorkInstruction.getPlanMaxQuantity()) ? PosControllerInstr.BLINK_FREQ
				: PosControllerInstr.SOLID_FREQ;
		return (new PosControllerInstr(inControllerId,
			inSourceGuid,
			inPosition,
			inWorkInstruction.getPlanQuantity().byteValue(),
			inWorkInstruction.getPlanMinQuantity().byteValue(),
			inWorkInstruction.getPlanMaxQuantity().byteValue(),
			freq,
			PosControllerInstr.BRIGHT_DUTYCYCLE));
	}

	/**
	 * creates and returns the instruction for flashing triple line
	 * (Might want to move this so site controller code can use it similarly)
	 */
	private static PosControllerInstr getTripleDashInstruction(String inControllerId, byte inPosition) {
		return (new PosControllerInstr(inControllerId,
			inPosition,
			PosControllerInstr.BITENCODED_SEGMENTS_CODE,
			PosControllerInstr.BITENCODED_TRIPLE_DASH,
			PosControllerInstr.BITENCODED_TRIPLE_DASH,
			PosControllerInstr.BLINK_FREQ,
			PosControllerInstr.BRIGHT_DUTYCYCLE));
	}

	/**
	 * creates and returns the instruction for clearing all poscons for this controller
	 * Only generated server-side
	 */
	private static PosControllerInstr getClearInstruction(String inControllerId) {
		PosControllerInstr messageClear = new PosControllerInstr();
		messageClear.setControllerId(inControllerId);
		messageClear.setRemove("all");
		messageClear.processRemoveField();
		return messageClear;
	}

	/**
	 * @param facility current facility
	 * @param wi specifies CHE source and quantity to display. If null is provided, the PosCon simply blinks triple bars
	 * @param location to light
	 * @param instructions provide an empty list to gather regenerated instructions
	 * @param clearedControllers provide an empty list if you'd like to remove all previous commands from the same source on the specified PosCon controller. Provide null otherwise. 
	 * This complicated function adds appropriate PosControllerInstr to the list that is passed in. Many calling contexts:
	 * - Light location via UX slot light location for a slot with poscon (typically a put wall). Action: light the one slot with triple bar.
	 * - Light location via UX tier light location whose slots have poscons (typically a put wall). Action: light each slot in the tier with triple bar.
	 * - Light location via UX bay light location whose slots have poscons (typically a put wall). Action: light each slot in the bay with triple bar.
	 * - Light location via UX slot light location for bay with poscon (typically a lower cost put wall with LEDs for slots.) Action: light the bay poscon with triple bar.
	 *  (The LED slots would light also via different code.)
	 * - Via ORDER_WALL, place order to a slot (poscons in slots). Action: give slot-wise order feedback on the poscon.
	 * - Via ORDER_WALL, place order to a slot (LED for slots; poscon for the bay). Action: give bay-wise order feedback on the poscon. Briefly flash the slot LEDs.
	 * - Via PUT_WALL, after scanning the item or gtin, if the slot in wall has poscon, show the plan count actively on the poscon.
	 * - Via PUT_WALL, after scanning the item or gtin, if the slot has LEDs, but bay has poscon, show the plan count actively on the poscon and the LEDs flash.
	 */
	public static void getInstructionsForPosConRange(final Facility facility,
		final WorkInstruction wi,
		final Location location,
		List<PosControllerInstr> instructions,
		HashSet<String> clearedControllers) {
		List<Location> locations = Lists.newArrayList();
		locations.add(location);
		getInstructionsForPosConRange(facility, wi, locations, instructions, clearedControllers);
	}

	public static void getInstructionsForPosConRange(final Facility facility,
		final WorkInstruction wi,
		final List<Location> locations,
		List<PosControllerInstr> instructions,
		HashSet<String> clearedControllers) {
		if (locations == null) {
			return;
		}
		if (clearedControllers == null) {
			clearedControllers = new HashSet<String>();
		}
		for (Location theLocation : locations) {
			if (theLocation.isLightablePoscon()) {
				LedController controller = theLocation.getEffectiveLedController();
				String posConController = controller == null ? "" : controller.getDeviceGuidStr();
				int posConIndex = theLocation.getPosconIndex();
				PosControllerInstr message = null;
				if (wi == null) {
					//If just trying to illuminate PosCons, remove all previous illumination instruction
					if (!clearedControllers.contains(posConController)) {
						PosControllerInstr messageClear = getClearInstruction(posConController);
						instructions.add(messageClear);
						clearedControllers.add(posConController);
					}
					//If work instruction is not provided, light location with triple bars
					message = getTripleDashInstruction(posConController, (byte) posConIndex);
				} else {
					Che che = wi.getAssignedChe();
					String sourceGuid = che == null ? "" : che.getDeviceGuidStr();
					message = getWiCountInstruction(posConController, sourceGuid, (byte) posConIndex, wi);

				}
				instructions.add(message);
			} else if (wi != null) {
				// for lower cost putwall, the bay may have a poscon even though the slots have LEDs.
				// This is only a situation for the active wi.
				Bay bay = theLocation.getParentAtLevel(Bay.class);
				if (bay != null && bay.isLightablePoscon()) {
					LedController controller = bay.getEffectiveLedController();
					String posConController = controller == null ? "" : controller.getDeviceGuidStr();
					int posConIndex = bay.getPosconIndex();
					PosControllerInstr message = null;
					Che che = wi.getAssignedChe();
					String sourceGuid = che == null ? "" : che.getDeviceGuidStr();
					message = getWiCountInstruction(posConController, sourceGuid, (byte) posConIndex, wi);
					instructions.add(message);
				}
			}

			// This child case is only to match LED lighting when a user does light location.
			// never do it when the wi is supplied.
			if (wi == null) {
				List<Location> children = theLocation.getActiveChildren();
				//if (!children.isEmpty()) {
				//for (Location child : children) {
				getInstructionsForPosConRange(facility, wi, children, instructions, clearedControllers);
				//}
				//}
			}
		}
	}

	// --------------------------------------------------------------------------
	/**
	 * lights the locations passed in if they will directly light. More commonly, the location will not light directly--
	 * calls to light the lightable children
	 */
	void lightChildLocations(final Facility facility, final List<Location> locations, ColorEnum color) {
		List<Location> leaves = Lists.newArrayList();
		//Do not light slots when lighting an aisle
		for (Location location : locations) {
			// DEV-1529. If this location is directly lightable, just add it to leaves list.
			if (location.isLightableAisleController() && (location.isBay() || location.isAisle())) {
				leaves.add(location);
			} else {
				// otherwise, call to add lightable children to leaves.
				getAllLedLightableLeaves(location, leaves, !location.isAisle());
			}
		}
		List<LightLedsInstruction> instructions = getLightInstructionsForMultipleLocations(facility, color, leaves);
		LedInstrListMessage message = new LedInstrListMessage(instructions);
		sendMessage(facility.getSiteControllerUsers(), message);
	}

	private void getAllLedLightableLeaves(final Location location, List<Location> leaves, boolean lightSlots) {
		List<Location> children = location.getActiveChildren();
		if (children.isEmpty() || (!lightSlots && location.isTier())) {
			if (location.isLightableAisleController()) {
				leaves.add(location);
			}
		} else {
			for (Location child : children) {
				getAllLedLightableLeaves(child, leaves, lightSlots);
			}
		}
	}

	/**
	 * This looks bizarre. Use the light service API as our general sendMessage API. Why?
	 * Discussion with Ivan. Definitely do not want static getInstance on a singleton factory for the webSocketManagerService.
	 * WorkService uses this API. The alternative is ok but has much more code duplication: inject the webSocketManagerService in many more
	 * class constructors, and modify tests that use those classes.
	*/
	public int sendMessage(Set<User> users, MessageABC message) {
		return WebSocketManagerService.getInstance().sendMessage(users, message);
	}

	private LightLedsInstruction toLedsInstruction(Facility facility, int maxNumLeds, final ColorEnum inColor, final Item inItem) {
		// Use our utility function to get the leds for the item
		Location itemLocation = inItem.getStoredLocation();
		if (itemLocation.isLightableAisleController()) {
			LedRange theRange = getLedRangeForLocationOffset(itemLocation, inItem.getMetersFromAnchor()).capLeds(maxNumLeds);
			LightLedsInstruction instruction = getLedCmdGroupListForRange(facility, inColor, itemLocation, theRange);
			return instruction;
		} else {
			return null;
		}
	}

	/**
	 * Called for LED putwall as order is placed into wall. Flash briefly.
	 */
	public void flashOneLocationInColor(Location locToLight, ColorEnum color, Facility facility) {
		if (locToLight == null || !locToLight.isLightableAisleController()) {
			LOGGER.error("bad call to flashOneLocationInColor");
			return;
		}

		List<Location> leaves = Lists.newArrayList();
		// using the general API. Only add the one location to leaves
		leaves.add(locToLight);
		List<LightLedsInstruction> instructions = getLightInstructionsForMultipleLocations(facility, color, leaves);
		LedInstrListMessage message = new LedInstrListMessage(instructions);
		sendMessage(facility.getSiteControllerUsers(), message);
	}

	// --------------------------------------------------------------------------
	/**
	 * Originally set to 4.
	 * For Walmart palletizer, we want to allow most of a 105 cm light tube
	 * Note: may need to modify this as the new "function" in aisle file may set to different numbers.
	 * 
	 */
	public static int getMaxLedsToLight(Location inLocation, int inPosNumTotal) {
		int returnValue = maxNormalLedsToLight;
		boolean looksLikeFullPalletSituation = (inLocation != null) && inLocation.isSlot()
				&& (inPosNumTotal < 31 && inPosNumTotal > 25);
		if (looksLikeFullPalletSituation)
			returnValue = inPosNumTotal;
		else {
			//DEV-1529 adds functions to the aisle file that may directly set LEDs for an aisle or bay. This is likely to be greater than 4.
			if (inLocation.isBay() || inLocation.isAisle())
				returnValue = Math.min(inPosNumTotal, maxMaxLedsToLight);
		}

		return returnValue;
	}

	/**
	 * This give the locations desired (configured) span with a sanity check for maximum value.
	 */
	private static int getLedNumberForLocation(Location inLocation) {
		Short firstLocLed = inLocation.getFirstLedNumAlongPath();
		Short lastLocLed = inLocation.getLastLedNumAlongPath();
		if (firstLocLed == null || lastLocLed == null)
			return 0;

		int span = lastLocLed - firstLocLed;
		if (span < 1)
			return 0;
		return getMaxLedsToLight(inLocation, span);
	}

	/**
	 * This is called quite directly for lighting an item, as the item knows its location and offset from anchor.
	 * Called a bit more indirectly for lighting after scanning Codeshelf tape.
	 */
	private LedRange getLedRangeForLocationOffset(Location inLocation, Double inMetersFromAnchor) {
		if (inLocation == null) {
			LOGGER.error("null location in getLedRangeForLocationOffset");
			return null;
		}
		Double metersFromAnchor = 0.0;
		if (inMetersFromAnchor != null)
			metersFromAnchor = inMetersFromAnchor;

		// to compute, we need the locations first and last led positions
		if (inLocation.isFacility())
			return LedRange.zero(); // was initialized to give values of 0,0

		int firstLocLed = inLocation.getFirstLedNumAlongPath();
		int lastLocLed = inLocation.getLastLedNumAlongPath();

		Double locationWidth = inLocation.getLocationWidthMeters();
		boolean lowerLedNearAnchor = inLocation.isLowerLedNearAnchor();

		LedRange theLedRange = LedRange.computeLedsToLight(firstLocLed,
			lastLocLed,
			lowerLedNearAnchor,
			locationWidth,
			metersFromAnchor,
			inLocation.isSlot());
		return theLedRange;
	}

	/**
	 * The trick here is to determine the anchor point direction, then convert cmFromLeft to meters from Anchor
	 * This calls getMetersFromAnchorGivenCmFromLeft() which may throw InputValidationException
	 */
	private LedRange getLedRangeForLocationCmFromLeft(Location inLocation, int inCmFromLeft) {
		if (inLocation == null) {
			LOGGER.error("null location in getLedRangeForLocationCmFromLeft");
			return null;
		}
		Double metersFromAnchor = inLocation.getMetersFromAnchorGivenCmFromLeft(inCmFromLeft);
		return getLedRangeForLocationOffset(inLocation, metersFromAnchor);
	}

	private LightLedsInstruction toLedsInstruction(Facility facility,
		int maxNumLeds,
		final ColorEnum inColor,
		final Location inLocation) {
		LedRange theRange = inLocation.getFirstLastLedsForLocation().capLeds(maxNumLeds);
		LightLedsInstruction instruction = getLedCmdGroupListForRange(facility, inColor, inLocation, theRange);
		return instruction;
	}

	private List<LightLedsInstruction> getLightInstructionsForMultipleLocations(Facility facility,
		ColorEnum diagnosticColor,
		List<Location> children) {
		List<LightLedsInstruction> instructions = Lists.newArrayList();
		for (Location child : children) {
			try {
				if (child.isLightableAisleController()) {
					int numLeds = getLedNumberForLocation(child);
					LightLedsInstruction instruction = toLedsInstruction(facility, numLeds, diagnosticColor, child);
					instructions.add(instruction);
				} else {
					LOGGER.warn("Unable to light location: " + child);
				}
			} catch (Exception e) {
				LOGGER.warn("Unable to light location: " + child, e);
			}
		}
		return groupByControllerAndChanel(instructions);
	}

	private List<LightLedsInstruction> groupByControllerAndChanel(List<LightLedsInstruction> instructions) {
		Map<ControllerChannelKey, LightLedsInstruction> byControllerChannel = Maps.newHashMap();
		for (LightLedsInstruction instruction : instructions) {
			ControllerChannelKey key = new ControllerChannelKey(instruction.getNetGuidStr(), instruction.getChannel());
			//merge messages per controller and key
			LightLedsInstruction instructionForKey = byControllerChannel.get(key);
			if (instructionForKey != null) {
				instruction = instructionForKey.merge(instruction);
			}
			byControllerChannel.put(key, instruction);
		}
		return new ArrayList<LightLedsInstruction>(byControllerChannel.values());
	}

	/**
	 * Utility function to create LED command group. Will return a list, which may be empty if there is nothing to send. Caller should check for empty list.
	 * Called now for setting WI LED pattern for inventory pick.
	 * Also called for directly lighting inventory item or location
	 * @param inNetGuidStr
	 * @param inItem
	 * @param inColor
	 */
	private LightLedsInstruction getLedCmdGroupListForRange(Facility facility,
		final ColorEnum inColor,
		Location inLocation,
		final LedRange ledRange) {
		int lightDuration = PropertyBehavior.getPropertyAsInt(facility, FacilityPropertyType.LIGHTSEC, defaultLightDurationSeconds);

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
		return new LightLedsInstruction(controller.getDeviceGuidStr(),
			controllerChannel,
			lightDuration,
			ImmutableList.of(ledCmdGroup));
	}

	private Facility checkFacility(final String facilityPersistentId) {
		return checkNotNull(Facility.staticGetDao().findByPersistentId(facilityPersistentId),
			"Unknown facility: %s",
			facilityPersistentId);
	}

	private Location checkLocation(Facility facility, final String inLocationNominalId) {
		Location theLocation = facility.findSubLocationById(inLocationNominalId);
		checkArgument(theLocation != null && !(theLocation.isFacility()), "Location nominalId unknown: %s", inLocationNominalId);
		return theLocation;
	}

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
