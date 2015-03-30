package com.codeshelf.service;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
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
import com.codeshelf.model.LedRange;
import com.codeshelf.model.domain.Aisle;
import com.codeshelf.model.domain.DomainObjectProperty;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.Item;
import com.codeshelf.model.domain.LedController;
import com.codeshelf.model.domain.Location;
import com.codeshelf.model.domain.Tier;
import com.codeshelf.model.domain.WorkInstruction;
import com.codeshelf.ws.jetty.protocol.message.LightLedsInstruction;
import com.codeshelf.ws.jetty.protocol.message.MessageABC;
import com.codeshelf.ws.jetty.server.WebSocketManagerService;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Inject;

public class LightService implements IApiService {

	private static final Logger				LOGGER						= LoggerFactory.getLogger(LightService.class);

	private final WebSocketManagerService			webSocketManagerService;

	private final static ColorEnum			defaultColor				= ColorEnum.RED;
	private final static int				defaultLightDurationSeconds	= 20;

	// Originally 4 leds. The aisle file read and UI indicates 4 leds.  Was changed to 3 leds before aisle controller message splitting to allow more simultaneous lighting.
	// Should not longer be necessary. Between v6 and v10 there was some inconsistency between 3 and 4. Now consistent.  Ideally no configuration parameter for this because if set
	// otherwise, the UI still show 4. See DEV-411
	private final static int				defaultLedsToLight			= 4; 	// IMPORTANT. This should be synched with WIFactory.maxLedsToLight

	@Inject
	public LightService(WebSocketManagerService webSocketManagerService) {
		this.webSocketManagerService = webSocketManagerService;
	}
	
	// --------------------------------------------------------------------------
	public void lightLocation(final String facilityPersistentId, final String inLocationNominalId) {
		//Light LEDs
		final Facility facility = checkFacility(facilityPersistentId);
		ColorEnum color = PropertyService.getInstance().getPropertyAsColor(facility, DomainObjectProperty.LIGHTCLR, defaultColor);
		Location theLocation = checkLocation(facility, inLocationNominalId);
		lightChildLocations(facility, theLocation, color);
		
		//Light the POS range
		List<PosControllerInstr> instructions = new ArrayList<PosControllerInstr>();
		getInstructionsForPosConRange(facility, null, theLocation, instructions);
		final PosControllerInstrList posMessage = new PosControllerInstrList(instructions);
		sendMessage(facility.getSiteControllerUsers(), posMessage);
		//Modify all POS commands to clear their POSs instead.
		new Timer().schedule(new TimerTask() {
			@Override
			public void run() {
				LOGGER.info("AisleDeviceLogic expire timer fired.");
				for (PosControllerInstr instructions : posMessage.getInstructions()){
					instructions.getRemovePos().add(instructions.getPosition());
				}
				sendMessage(facility.getSiteControllerUsers(), posMessage);
			}
		}, 20000);
	}

	public void lightInventory(final String facilityPersistentId, final String inLocationNominalId) {
		Facility facility = checkFacility(facilityPersistentId);
		ColorEnum color = PropertyService.getInstance().getPropertyAsColor(facility, DomainObjectProperty.LIGHTCLR, defaultColor);
		Location theLocation = checkLocation(facility, inLocationNominalId);
		List<Item> items = theLocation.getInventoryInWorkingOrder();
		lightItemsSpecificColor(facilityPersistentId, items, color);
	}

	/**
	 * Light one item. Any subsequent activity on the aisle controller will wipe this away.
	 */
	public void lightItem(final String facilityPersistentId, final String inItemPersistentId) {
		Facility facility = checkFacility(facilityPersistentId);
		ColorEnum color = PropertyService.getInstance().getPropertyAsColor(facility, DomainObjectProperty.LIGHTCLR, defaultColor);
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
					instructions.add(instruction);
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
	
	void lightChildLocations(final Facility facility, final Location location, ColorEnum color) {
		List<Location> leaves = Lists.newArrayList();
		//Do not light slots when lighting an aisle
		getAllLedLightableLeaves(location, leaves, !location.isAisle());
		List<LightLedsInstruction> instructions = lightAllAtOnce(facility, defaultLedsToLight, color, leaves);
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

	private int sendMessage(Set<User> users, MessageABC message) {
		return webSocketManagerService.sendMessage(users, message);
	}

	private LightLedsInstruction toLedsInstruction(Facility facility, int maxNumLeds, final ColorEnum inColor, final Item inItem) {
		// Use our utility function to get the leds for the item
		LedRange theRange = inItem.getFirstLastLedsForItem().capLeds(maxNumLeds);
		Location itemLocation = inItem.getStoredLocation();
		if (itemLocation.isLightableAisleController()) {
			LightLedsInstruction instruction = getLedCmdGroupListForRange(facility, inColor, itemLocation, theRange);
			return instruction;
		} else {
			return null;
		}
	}

	private LightLedsInstruction toLedsInstruction(Facility facility, int maxNumLeds, final ColorEnum inColor, final Location inLocation) {
		LedRange theRange = inLocation.getFirstLastLedsForLocation().capLeds(maxNumLeds);
		LightLedsInstruction instruction = getLedCmdGroupListForRange(facility, inColor, inLocation, theRange);
		return instruction;
	}
	
	private List<LightLedsInstruction> lightAllAtOnce(Facility facility, int numLeds, ColorEnum diagnosticColor, List<Location> children) {
		List<LightLedsInstruction> instructions = Lists.newArrayList();
		for (Location child : children) {
			try {
				if (child.isLightableAisleController()) {
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
		return new LightLedsInstruction(controller.getDeviceGuidStr(), controllerChannel, lightDuration, ImmutableList.of(ledCmdGroup));
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
