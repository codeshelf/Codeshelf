/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2013, Jeffrey B. Williams, All rights reserved
 *  $Id: CheDeviceLogic.java,v 1.12 2013/09/05 03:26:03 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.device;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import lombok.Getter;
import lombok.experimental.Accessors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gadgetworks.codeshelf.device.AisleDeviceLogic.LedCmd;
import com.gadgetworks.codeshelf.model.WorkInstructionCount;
import com.gadgetworks.codeshelf.model.WorkInstructionStatusEnum;
import com.gadgetworks.codeshelf.model.WorkInstructionTypeEnum;
import com.gadgetworks.codeshelf.model.domain.WorkInstruction;
import com.gadgetworks.codeshelf.util.CompareNullChecker;
import com.gadgetworks.flyweight.command.CommandControlButton;
import com.gadgetworks.flyweight.command.CommandControlClearPosController;
import com.gadgetworks.flyweight.command.CommandControlDisplayMessage;
import com.gadgetworks.flyweight.command.CommandControlSetPosController;
import com.gadgetworks.flyweight.command.EffectEnum;
import com.gadgetworks.flyweight.command.ICommand;
import com.gadgetworks.flyweight.command.NetEndpoint;
import com.gadgetworks.flyweight.command.NetGuid;
import com.gadgetworks.flyweight.controller.INetworkDevice;
import com.gadgetworks.flyweight.controller.IRadioController;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

/**
 * @author jeffw
 *
 */
public class CheDeviceLogic extends DeviceLogicABC {
	// This code runs on the site controller, not the CHE.
	// The goal is to convert data and instructions to something that the CHE controller can consume and act on with minimal logic.

	private static final Logger		LOGGER									= LoggerFactory.getLogger(CheDeviceLogic.class);

	private static final String		COMMAND_PREFIX							= "X%";
	private static final String		USER_PREFIX								= "U%";
	private static final String		CONTAINER_PREFIX						= "C%";
	private static final String		LOCATION_PREFIX							= "L%";
	private static final String		ITEMID_PREFIX							= "I%";
	private static final String		POSITION_PREFIX							= "P%";

	// These are the message strings we send to the remote CHE.
	// Currently, these cannot be longer than 20 characters.
	// "SCAN START LOCATION" is at the 20 limit. If you change to "SCAN STARTING LOCATION", you get very bad behavior. The class loader will not find the CheDeviceLogic. Repeating throws.	
	private static final String		EMPTY_MSG								= cheLine("");
	private static final String		INVALID_SCAN_MSG						= cheLine("INVALID");
	private static final String		SCAN_USERID_MSG							= cheLine("SCAN BADGE");
	private static final String		SCAN_LOCATION_MSG						= cheLine("SCAN START LOCATION");
	private static final String		SCAN_CONTAINER_MSG						= cheLine("SCAN CONTAINER");
	private static final String		OR_START_WORK_MSG						= cheLine("OR START WORK");
	private static final String		SELECT_POSITION_MSG						= cheLine("SELECT POSITION");
	private static final String		SHORT_PICK_CONFIRM_MSG					= cheLine("CONFIRM SHORT");
	private static final String		PICK_COMPLETE_MSG						= cheLine("ALL WORK COMPLETE");
	private static final String		YES_NO_MSG								= cheLine("SCAN YES OR NO");
	private static final String		NO_CONTAINERS_SETUP_MSG					= cheLine("NO SETUP CONTAINERS");
	private static final String		POSITION_IN_USE_MSG						= cheLine("POSITION IN USE");
	private static final String		FINISH_SETUP_MSG						= cheLine("PLS SETUP CONTAINERS");
	private static final String		COMPUTE_WORK_MSG						= cheLine("COMPUTING WORK");
	private static final String		GET_WORK_MSG							= cheLine("GETTING WORK");
	private static final String		NO_WORK_MSG								= cheLine("NO WORK TO DO");
	private static final String				LOCATION_SELECT_REVIEW_MSG_LINE_1		= cheLine("REVIEW MISSING WORK");
	private static final String				LOCATION_SELECT_REVIEW_MSG_LINE_2		= cheLine("OR SCAN LOCATION");
	private static final String				LOCATION_SELECT_REVIEW_MSG_LINE_3		= cheLine("TO CONTINUE AS IS");

	private static final String		STARTWORK_COMMAND						= "START";
	private static final String		SETUP_COMMAND							= "SETUP";
	private static final String		SHORT_COMMAND							= "SHORT";
	private static final String		LOGOUT_COMMAND							= "LOGOUT";
	//private static final String		RESUME_COMMAND			= "RESUME";
	private static final String		YES_COMMAND								= "YES";
	private static final String		NO_COMMAND								= "NO";

	private static final Integer	maxCountForPositionControllerDisplay	= 99;

	// The CHE's current state.
	@Accessors(prefix = "m")
	@Getter
	private CheStateEnum			mCheStateEnum;

	// The CHE's current location.
	@Accessors(prefix = "m")
	@Getter
	private String					mLocationId;

	// The CHE's current user.
	@Accessors(prefix = "m")
	@Getter
	private String					mUserId;

	// The CHE's container map.
	private String					mContainerInSetup;

	// The CHE's container map.
	private Map<String, String>		mContainersMap;

	// All WIs for all containers on the CHE.
	@Accessors(prefix = "m")
	@Getter
	private List<WorkInstruction>	mAllPicksWiList;

	//Container Position to last SetInstruction Map
	@Accessors(prefix = "m")
	@Getter
	private Map<Byte, PosControllerInstr>	mPosToLastSetIntrMap;

	// The active pick WIs.
	@Accessors(prefix = "m")
	@Getter
	private List<WorkInstruction>	mActivePickWiList;

	private NetGuid					mLastLedControllerGuid;
	private boolean					mMultipleLastLedControllerGuids;															// Could have a list, but this will be quite rare.

	private WorkInstruction			mShortPickWi;
	private Integer					mShortPickQty;

	private boolean					connectedToServer						= true;

	public CheDeviceLogic(final UUID inPersistentId,
		final NetGuid inGuid,
		final ICsDeviceManager inDeviceManager,
		final IRadioController inRadioController) {
		super(inPersistentId, inGuid, inDeviceManager, inRadioController);

		mCheStateEnum = CheStateEnum.IDLE;
		mContainersMap = new HashMap<String, String>();
		mAllPicksWiList = new ArrayList<WorkInstruction>();
		mActivePickWiList = new ArrayList<WorkInstruction>();
		mPosToLastSetIntrMap = new HashMap<Byte, PosControllerInstr>();
	}

	@Override
	public final short getSleepSeconds() {
		return 180;
	}

	// The last-aisle-controller-for-this-CHE package.
	private void setLastLedControllerGuid(NetGuid inAisleControllerGuid) {
		if (mLastLedControllerGuid == null)
			mLastLedControllerGuid = inAisleControllerGuid;
		else if (mLastLedControllerGuid != inAisleControllerGuid) {
			mLastLedControllerGuid = inAisleControllerGuid;
			mMultipleLastLedControllerGuids = true;
		}
	}

	private NetGuid getLastLedControllerGuid() {
		return mLastLedControllerGuid;
	}

	private boolean wereMultipleLastLedControllerGuids() {
		return mMultipleLastLedControllerGuids;
	}

	private void clearLastLedControllerGuids() {
		mLastLedControllerGuid = null;
		mMultipleLastLedControllerGuids = false;
	}

	// --------------------------------------------------------------------------
	/**
	 * Send a display message to the CHE's embedded control device.
	 * @param inLine1Message
	 * @param inLine2Message
	 */
	private void sendDisplayCommand(final String inLine1Message, final String inLine2Message) {
		sendDisplayCommand(inLine1Message, inLine2Message, "", "");
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inLine1Message
	 * @param inLine2Message
	 * @param inLine3Message
	 * @param inLine4Message
	 */
	private void sendDisplayCommand(final String inLine1Message,
		final String inLine2Message,
		final String inLine3Message,
		final String inLine4Message) {
		// DEV-459 if this CHE is not associated, there is no point in sending out a display.
		// Lots of upstream code generates display messages.
		if (!this.isDeviceAssociated()) {
			LOGGER.debug("skipping send display for unassociated " + this.getMyGuidStrForLog());
			// This is far less logging than if the command actually goes, so might as well say what is going on.
			return;
		}

		String displayString = "Display message for " + getMyGuidStrForLog();
		if (!inLine1Message.isEmpty())
			displayString += " line1: " + inLine1Message;
		if (!inLine2Message.isEmpty())
			displayString += " line2: " + inLine2Message;
		if (!inLine3Message.isEmpty())
			displayString += " line3: " + inLine3Message;
		if (!inLine4Message.isEmpty())
			displayString += " line4: " + inLine4Message;
		LOGGER.info(displayString);
		ICommand command = new CommandControlDisplayMessage(NetEndpoint.PRIMARY_ENDPOINT,
			inLine1Message,
			inLine2Message,
			inLine3Message,
			inLine4Message);
		mRadioController.sendCommand(command, getAddress(), true);
	}

	// --------------------------------------------------------------------------
	/**
	 * Breakup the description into three static lines no longer than 20 characters.
	 * Except the last line can be up to 40 characters (since it scrolls).
	 * Important change from v3. If quantity > 98, then tweak the description adding the count to the start.
	 * @param inPickInstructions
	 * @param inDescription
	 */
	private void sendDisplayWorkInstruction(final String inPickInstructions,
		final String inDescription,
		final Integer inPlanQuantity,
		boolean inShortInstructionNeeded) {
		String displayDescription = inDescription;
		if (inPlanQuantity > maxCountForPositionControllerDisplay - 1) {
			String countStr = inPlanQuantity.toString();
			displayDescription = countStr + " " + inDescription;
		}

		String[] descriptionLine = { "", "", "" };
		int pos = 0;
		for (int line = 0; line < 3; line++) {
			if (pos < displayDescription.length()) {
				int toGet = Math.min(20, displayDescription.length() - pos);
				descriptionLine[line] = displayDescription.substring(pos, pos + toGet);
				pos += toGet;
			}
		}

		// Check if there is more description to add to the last line.
		if (pos < displayDescription.length()) {
			int toGet = Math.min(20, displayDescription.length() - pos);
			descriptionLine[2] += displayDescription.substring(pos, pos + toGet);
		}

		if (inShortInstructionNeeded)
			descriptionLine[2] = "DECREMENT POSITION";

		// Note: pickInstruction is more or less a location. Commonly a location alias, but may be a locationId or DDcId.
		// GoodEggs many locations orders hitting too long case
		String cleanedPickInstructions = inPickInstructions;
		if (cleanedPickInstructions.length() > 19)
			cleanedPickInstructions = cleanedPickInstructions.substring(0, 19);
		sendDisplayCommand(cleanedPickInstructions, descriptionLine[0], descriptionLine[1], descriptionLine[2]);
	}

	// --------------------------------------------------------------------------
	/**
	 * Send a command to set update the position controller
	 * Common Use: Send a pick request command to the CHE to light a position
	 * 
	 * @param inPos
	 * @param inReqQty
	 * @param inMinQty
	 * @param inMaxQty
	 */
	private void sendPositionControllerInstructions(List<PosControllerInstr> inInstructions) {
		//Update the last sent posControllerInstr for the position 
		for (PosControllerInstr instr : inInstructions) {
			mPosToLastSetIntrMap.put(instr.getPosition(), instr);
		}

		ICommand command = new CommandControlSetPosController(NetEndpoint.PRIMARY_ENDPOINT, inInstructions);
		mRadioController.sendCommand(command, getAddress(), true);
	}

	// --------------------------------------------------------------------------
	/**
	 * Send a light command for this CHE on the specified LED controller.
	 * @param inPosition
	 */
	private void ledControllerSetLed(final NetGuid inControllerGuid,
		final Short inChannel,
		final LedSample inLedSample,
		final EffectEnum inEffect) {

		// The caller logs more concisely. This would log each LED separately.
		// LOGGER.info("Light position: " + inLedSample.getPosition() + " color: " + inLedSample.getColor());
		INetworkDevice device = mDeviceManager.getDeviceByGuid(inControllerGuid);
		if (device instanceof AisleDeviceLogic) {
			AisleDeviceLogic aisleDevice = (AisleDeviceLogic) device;
			LedCmd cmd = aisleDevice.getLedCmdFor(getGuid(), inChannel, inLedSample.getPosition());
			if (cmd == null) {
				aisleDevice.addLedCmdFor(getGuid(), inChannel, inLedSample, inEffect);
			}
		}

		// Remember any aisle controller we sent messages with lights to.
		if (!(device instanceof CheDeviceLogic)) {
			setLastLedControllerGuid(inControllerGuid);
		}
	}

	// --------------------------------------------------------------------------
	/**
	 * After we've set all of the LEDs, tell the controller to broadcast them updates.
	 * @param inControllerGuid
	 */
	private void ledControllerShowLeds(final NetGuid inControllerGuid) {
		INetworkDevice device = mDeviceManager.getDeviceByGuid(inControllerGuid);
		if (device instanceof AisleDeviceLogic) {
			AisleDeviceLogic aisleDevice = (AisleDeviceLogic) device;
			aisleDevice.updateLeds();
		}
	}

	// --------------------------------------------------------------------------
	/**
	 * Called at logout. (or the multiple case below). Trying to provide a means to self-correct.
	 * Clears aisle controllers only, not position controllers on this CHE
	 * Side effect: clears its information on what was previously sent to.
	 */
	private void forceClearAllLedsForThisCheDevice() {
		List<AisleDeviceLogic> aList = mDeviceManager.getAisleControllers();
		for (AisleDeviceLogic theAisleDevice : aList) {
			theAisleDevice.removeLedCmdsForCheAndSend(getGuid());
		}
		clearLastLedControllerGuids(); // Setting the state that we have nothing more to clear for this CHE.		
	}

	// --------------------------------------------------------------------------
	/**
	 * Clear the LEDs for this CHE one whatever LED controllers it last sent to.
	 * Side effect: clears its information on what was previously sent to.
	 * @param inGuid
	 */
	private void ledControllerClearLeds() {
		// Clear the LEDs for the last location(s) the CHE worked.
		NetGuid aisleControllerGuid = getLastLedControllerGuid();
		if (aisleControllerGuid == null)
			return;

		if (wereMultipleLastLedControllerGuids()) {
			// check all
			LOGGER.warn("Needing to clear multiple aisle controllers for one CHE device clear. This case should be unusual.");
			forceClearAllLedsForThisCheDevice();
		}
		// Normal case. Just clear the one aisle device we know we sent to last.
		else {
			INetworkDevice device = mDeviceManager.getDeviceByGuid(aisleControllerGuid);
			if (device instanceof AisleDeviceLogic) {
				AisleDeviceLogic aisleDevice = (AisleDeviceLogic) device;
				aisleDevice.removeLedCmdsForCheAndSend(getGuid());
			}
			clearLastLedControllerGuids(); // Setting the state that we have nothing more to clear for this CHE.
		}
	}

	// --------------------------------------------------------------------------
	/**
	 * Clear the LEDs for this CHE on this specified LED (aisle) controller.
	 * No side effect: does not clears its information on what was previously sent to.
	 * This is called on completing one work instruction only. The work instruction knows its aisle controller (in theory)
	 * Would only be necessary for simultaneous multiple WI dispatch. Otherwise, could just as well call the above
	 * @param inGuid
	 */
	private void ledControllerClearLeds(final NetGuid inLedControllerId) {
		// Clear the LEDs for the last location the CHE worked.
		INetworkDevice device = mDeviceManager.getDeviceByGuid(inLedControllerId);
		if (device instanceof AisleDeviceLogic) {
			AisleDeviceLogic aisleDevice = (AisleDeviceLogic) device;
			aisleDevice.removeLedCmdsForCheAndSend(getGuid());
		}

		clearLastLedControllerGuids(); // Not sure this is right!!! Need tests in CD_0043		
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.flyweight.controller.INetworkDevice#start()
	 */
	@Override
	public final void startDevice() {
		LOGGER.info("Start CHE controller (after association) ");

		// setState(mCheStateEnum);  Always, after start, there is the device associate chain and redisplay which will call setState(mCheStateEnum);
		setLastAckId((byte) 0);
	}

	// --------------------------------------------------------------------------
	/**
	 * @param totalWorkInstructionCount
	 * @param containerToWorkInstructionCountMap - Map containerIds to WorkInstructionCount objects
	 */
	public final void processWorkInstructionCounts(final Integer totalWorkInstructionCount,
		final Map<String, WorkInstructionCount> containerToWorkInstructionCountMap) {
		// The back-end returned the work instruction count.
		if (totalWorkInstructionCount > 0) {
			//TODO Use the map to determine if we need to go to location_select or review
			boolean doesNeedReview = false;
			for (WorkInstructionCount wiCount : containerToWorkInstructionCountMap.values()) {
				if (wiCount.getGoodCount() == 0 || wiCount.hasNonGoodCounts()) {
					doesNeedReview = true;
					break;
				}
			}
			LOGGER.info("Got Counts {}", containerToWorkInstructionCountMap);
			//Show counts on position controllers
			this.showPositionFeedback(containerToWorkInstructionCountMap);

			if(doesNeedReview) {
				setState(CheStateEnum.LOCATION_SELECT_REVIEW);
			} else {
				setState(CheStateEnum.LOCATION_SELECT);
			}
			
		} else {
			setState(CheStateEnum.NO_WORK);
		}
	}

	/** Shows the count feedback on the position controller
	 * @param containerToWorkInstructionCountMap - Map of containerIds to WorkInstructionCount
	 */
	private void showPositionFeedback(Map<String, WorkInstructionCount> containerToWorkInstructionCountMap) {
		//Generate position controller commands
		List<PosControllerInstr> instructions = new ArrayList<PosControllerInstr>();

		for (Entry<String, String> containerMapEntry : mContainersMap.entrySet()) {
			String containerId = containerMapEntry.getValue();
			byte position = Byte.valueOf(containerMapEntry.getKey());
			WorkInstructionCount wiCount = containerToWorkInstructionCountMap.get(containerId);
			//count should never be null. We could also move the logic that determines whether we have an invalid
			//container id as one that has no count.
			byte count = (byte) wiCount.getGoodCount();
			LOGGER.info("Position Feedback: Poisition {} Counts {}", position, wiCount);
			if (count == 0 || wiCount.hasNonGoodCounts()) {
				//Flash
				instructions.add(new PosControllerInstr(position,
					count,
					count,
					count,
					PosControllerInstr.BLINK_FREQ.byteValue(),
					PosControllerInstr.BLINK_DUTYCYCLE.byteValue()));
			} else {
				//No Flash
				instructions.add(new PosControllerInstr(position,
					count,
					count,
					count,
					PosControllerInstr.BRIGHT_FREQ.byteValue(),
					PosControllerInstr.BRIGHT_DUTYCYCLE.byteValue()));
			}
		}

		//Show counts on position controllers
		sendPositionControllerInstructions(instructions);
	}

	// --------------------------------------------------------------------------
	/**
	 * Give the CHE the work it needs to do for a container.
	 * This is recomputed at the server for ALL containers on the CHE and returned in work-order.
	 * Whatever the CHE thought it needed to do before is now invalid and is replaced by what we send here.
	 * @param inContainerId
	 * @param inWorkItemList
	 */
	public final void assignWork(final List<WorkInstruction> inWorkItemList) {

		for (WorkInstruction wi : inWorkItemList) {
			LOGGER.info("WI: Loc: " + wi.getLocationId() + " SKU: " + wi.getItemId() + " Instr: " + wi.getPickInstruction());
		}

		if (inWorkItemList.size() == 0) {
			setState(CheStateEnum.NO_WORK);
		} else {
			mActivePickWiList.clear();
			mAllPicksWiList.clear();
			mAllPicksWiList.addAll(inWorkItemList);
			doNextPick();
			// setState(CheStateEnum.DO_PICK);  // doNextPick will set the state.
		}
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.flyweight.controller.INetworkDevice#commandReceived(java.lang.String)
	 */
	@Override
	public final void scanCommandReceived(String inCommandStr) {
		if (!connectedToServer) {
			LOGGER.debug("NotConnectedToServer: Ignoring scan command: " + inCommandStr);
			return;
		}

		LOGGER.info(this + " received scan command: " + inCommandStr);

		String scanPrefixStr = getScanPrefix(inCommandStr);
		String scanStr = getScanContents(inCommandStr, scanPrefixStr);

		// Cannot clear position controller on SHORT. (However, we would kind of like to change them all to flash their current number.)
		if (!scanStr.equals(SHORT_COMMAND) && (mCheStateEnum != CheStateEnum.SHORT_PICK_CONFIRM))
			clearAllPositionControllers();

		// A command scan is always an option at any state.
		if (inCommandStr.startsWith(COMMAND_PREFIX)) {
			processCommandScan(scanStr);
		} else {
			switch (mCheStateEnum) {
				case IDLE:
					processIdleStateScan(scanPrefixStr, scanStr);
					break;

				case LOCATION_SELECT:
					processLocationScan(scanPrefixStr, scanStr);
					break;

				case LOCATION_SELECT_REVIEW:
					processLocationScan(scanPrefixStr, scanStr);
					break;

				case CONTAINER_SELECT:
					processContainerSelectScan(scanPrefixStr, scanStr);
					break;

				case CONTAINER_POSITION:
					// The only thing that makes sense in this state is a position assignment (or a logout covered above).
					processContainerPosition(scanPrefixStr, scanStr);
					break;

				case DO_PICK:
					// At any time during the pick we can change locations.
					if (scanPrefixStr.equals(LOCATION_PREFIX)) {
						processLocationScan(scanPrefixStr, scanStr);
					}
					break;

				default:
					break;
			}
		}
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.flyweight.controller.INetworkDevice#buttonCommandReceived(com.gadgetworks.flyweight.command.CommandControlButton)
	 */
	@Override
	public void buttonCommandReceived(CommandControlButton inButtonCommand) {
		if (connectedToServer) {
			// Send a command to clear the position, so the controller knows we've gotten the button press.
			clearOnePositionController(inButtonCommand.getPosNum());
			processButtonPress((int) inButtonCommand.getPosNum(), (int) inButtonCommand.getValue());
		} else {
			LOGGER.debug("NotConnectedToServer: Ignoring button command: " + inButtonCommand);
		}
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inScanStr
	 * @return
	 */
	private String getScanPrefix(String inScanStr) {

		String result = "";

		if (inScanStr.startsWith(COMMAND_PREFIX)) {
			result = COMMAND_PREFIX;
		} else if (inScanStr.startsWith(USER_PREFIX)) {
			result = USER_PREFIX;
		} else if (inScanStr.startsWith(CONTAINER_PREFIX)) {
			result = CONTAINER_PREFIX;
		} else if (inScanStr.startsWith(LOCATION_PREFIX)) {
			result = LOCATION_PREFIX;
		} else if (inScanStr.startsWith(ITEMID_PREFIX)) {
			result = ITEMID_PREFIX;
		} else if (inScanStr.startsWith(POSITION_PREFIX)) {
			result = POSITION_PREFIX;
		}

		return result;
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inScanStr
	 * @param inPrefix
	 * @return
	 */
	private String getScanContents(String inScanStr, String inPrefix) {
		return inScanStr.substring(inPrefix.length());
	}

	// --------------------------------------------------------------------------
	/**
	 */
	private void setState(final CheStateEnum inCheState) {
		boolean wasSameState = inCheState == mCheStateEnum;
		mCheStateEnum = inCheState;
		LOGGER.debug("switching to state: " + inCheState + " sameState: " + wasSameState);

		switch (inCheState) {
			case IDLE:
				sendDisplayCommand(SCAN_USERID_MSG, EMPTY_MSG);
				break;

			case COMPUTE_WORK:
				sendDisplayCommand(COMPUTE_WORK_MSG, EMPTY_MSG);
				break;

			case GET_WORK:
				sendDisplayCommand(GET_WORK_MSG, EMPTY_MSG);
				break;

			case LOCATION_SELECT:
				sendDisplayCommand(SCAN_LOCATION_MSG, EMPTY_MSG);
				break;

			case LOCATION_SELECT_REVIEW:
				sendDisplayCommand(LOCATION_SELECT_REVIEW_MSG_LINE_1,
					LOCATION_SELECT_REVIEW_MSG_LINE_2,
					LOCATION_SELECT_REVIEW_MSG_LINE_3,
					EMPTY_MSG);
				break;

			case CONTAINER_SELECT:
				if (mContainersMap.size() < 1) {
					sendDisplayCommand(SCAN_CONTAINER_MSG, EMPTY_MSG);
				} else {
					sendDisplayCommand(SCAN_CONTAINER_MSG, OR_START_WORK_MSG);
				}
				break;

			case CONTAINER_POSITION:
				sendDisplayCommand(SELECT_POSITION_MSG, EMPTY_MSG);
				break;

			case SHORT_PICK_CONFIRM:
				sendDisplayCommand(SHORT_PICK_CONFIRM_MSG, YES_NO_MSG);
				break;

			case SHORT_PICK:
				// first try. Show normally, but based on state, the wi min count will be set to zero.
				showActivePicks();
				break;

			case DO_PICK:
				showActivePicks(); // used to only fire if not already in this state. Now if setState(DO_PICK) is called, it always calls showActivePicks.
				// fewer direct calls to showActivePicks elsewhere.
				break;

			case PICK_COMPLETE:
				sendDisplayCommand(PICK_COMPLETE_MSG, EMPTY_MSG);
				break;

			case NO_WORK:
				sendDisplayCommand(NO_WORK_MSG, EMPTY_MSG);
				break;

			default:
				break;
		}
	}

	// --------------------------------------------------------------------------
	/**
	 * Stay in the same state, but make the status invalid.
	 * Send the LED error status as well (color: red effect: error channel: 0).
	 * 
	 */
	private void invalidScanMsg(final CheStateEnum inCheState) {
		mCheStateEnum = inCheState;

		byte positionControllerToSendTo = PosControllerInstr.POSITION_ALL;
		boolean sendPositionControllerInstructions = true;

		switch (inCheState) {
			case IDLE:
				sendDisplayCommand(SCAN_USERID_MSG, INVALID_SCAN_MSG);
				break;

			case LOCATION_SELECT:
				sendDisplayCommand(SCAN_LOCATION_MSG, INVALID_SCAN_MSG);
				break;

			case CONTAINER_SELECT:
				sendDisplayCommand(SCAN_CONTAINER_MSG, INVALID_SCAN_MSG);
				break;

			case CONTAINER_POSITION:
				sendDisplayCommand(SELECT_POSITION_MSG, INVALID_SCAN_MSG);
				break;

			case DO_PICK:
				/* New attempts, but left as the default.  Test cases
				 * Short a housekeeping work instruction. (All E's is bad.)
				 * Yes or No on a normal pick
				 */

				// positionControllerToSendTo =  just the one, if we can figure it out. Or
				// sendPositionControllerInstructions = false;
				break;

			default:
				break;
		}

		if (sendPositionControllerInstructions) {
			List<PosControllerInstr> instructions = new ArrayList<PosControllerInstr>();
			PosControllerInstr instruction = new PosControllerInstr(positionControllerToSendTo,
				PosControllerInstr.ERROR_CODE_QTY,
				PosControllerInstr.ZERO_QTY,
				PosControllerInstr.ZERO_QTY,
				PosControllerInstr.MED_FREQ, // change from BLINK_FREQ
				PosControllerInstr.MED_DUTYCYCLE); // change from BLINK_DUTYCYCLE v6
			instructions.add(instruction);
			sendPositionControllerInstructions(instructions);
		}
	}

	// --------------------------------------------------------------------------
	/**
	 */
	private void processCommandScan(final String inScanStr) {

		switch (inScanStr) {
			case LOGOUT_COMMAND:
				logout();
				break;

			case SETUP_COMMAND:
				setupChe();
				break;

			case STARTWORK_COMMAND:
				startWork();
				break;

			case SHORT_COMMAND:
				shortPick();
				break;

			case YES_COMMAND:
			case NO_COMMAND:
				processYesOrNoCommand(inScanStr);
				break;

			default:
				break;
		}
	}

	// --------------------------------------------------------------------------
	/**
	 */
	private void logout() {
		LOGGER.info("User logut");
		// Clear all of the container IDs we were tracking.
		mPosToLastSetIntrMap.clear();
		mContainersMap.clear();
		mContainerInSetup = "";
		mActivePickWiList.clear();
		mAllPicksWiList.clear();
		setState(CheStateEnum.IDLE);

		forceClearAllLedsForThisCheDevice();
		clearAllPositionControllers();
	}

	// --------------------------------------------------------------------------
	/**
	 * The user scanned the SETUP command to start a new batch of containers for the CHE.
	 */
	private void setupChe() {
		LOGGER.info("Setup work");

		if (mCheStateEnum.equals(CheStateEnum.PICK_COMPLETE) || mCheStateEnum.equals(CheStateEnum.NO_WORK)) {
			mPosToLastSetIntrMap.clear();
			mContainersMap.clear();
			mContainerInSetup = "";
			setState(CheStateEnum.CONTAINER_SELECT);
		} else {
			// Stay in the same state - the scan made no sense.
			invalidScanMsg(mCheStateEnum);
		}
	}

	// --------------------------------------------------------------------------
	/**
	 * The start work command simply tells the user to select a starting location.
	 * It's a psychological step that makes more sense.
	 */
	private void startWork() {
		if (mContainersMap.values().size() > 0) {
			mContainerInSetup = "";
			if (getCheStateEnum() != CheStateEnum.DO_PICK) {
				setState(CheStateEnum.DO_PICK);
			}
			List<String> containerIdList = new ArrayList<String>(mContainersMap.values());
			mDeviceManager.computeCheWork(getGuid().getHexStringNoPrefix(), getPersistentId(), containerIdList);

			setState(CheStateEnum.COMPUTE_WORK);
		} else {
			// Stay in the same state - the scan made no sense.
			sendDisplayCommand(NO_CONTAINERS_SETUP_MSG, FINISH_SETUP_MSG);
		}
	}

	// --------------------------------------------------------------------------
	/**
	 * The user scanned the SHORT_PICK command to tell us there is no product to pick.
	 */
	private void shortPick() {
		if (mActivePickWiList.size() > 0) {
			// short scan of housekeeping work instruction makes no sense
			WorkInstruction wi = mActivePickWiList.get(0);
			if (wi.amIHouseKeepingWi())
				invalidScanMsg(mCheStateEnum); // Invalid to short a housekeep
			else
				setState(CheStateEnum.SHORT_PICK); // Used to be SHORT_PICK_CONFIRM
		} else {
			// Stay in the same state - the scan made no sense.
			invalidScanMsg(mCheStateEnum);
		}
	}

	// --------------------------------------------------------------------------
	/**
	 * The user scanned YES or NO.
	 * @param inScanStr
	 */
	private void processYesOrNoCommand(final String inScanStr) {
		switch (mCheStateEnum) {
			case SHORT_PICK_CONFIRM:
				confirmShortPick(inScanStr);
				break;

			default:
				// Stay in the same state - the scan made no sense.
				invalidScanMsg(mCheStateEnum);
				break;
		}
	}

	// --------------------------------------------------------------------------
	/**
	 * The inShortWi was just shorted.
	 * Is inProposedWi equivalent enough that it should also short?
	 */
	private Boolean sameProductLotEtc(final WorkInstruction inProposedWi, WorkInstruction inShortWi) {
		// Initially, just look at the denormalized item Id.
		String shortId = inShortWi.getItemId();
		String proposedId = inProposedWi.getItemId();
		if (shortId == null || proposedId == null) {
			LOGGER.error("sameProductLotEtc has null value");
			return false;
		}
		if (shortId.compareTo(proposedId) == 0) {
			LOGGER.info("SHORT AHEAD " + inProposedWi);
			return true;
		}

		return false;
	}

	// --------------------------------------------------------------------------
	/**
	 * The inShortWi was just shorted.
	 * Is inProposedWi later in sequence?
	 */
	private Boolean laterWi(final WorkInstruction inProposedWi, final WorkInstruction inShortWi) {
		String proposedSort = inProposedWi.getGroupAndSortCode();
		String shortedSort = inShortWi.getGroupAndSortCode();
		if (proposedSort == null) {
			LOGGER.error("laterWiSameProduct has wi with no sort code");
			return false;
		}
		if (shortedSort.compareTo(proposedSort) < 0)
			return true;

		return false;
	}

	// --------------------------------------------------------------------------
	/**
	 * Is this an uncompleted housekeeping WI, that if just ahead of a short-ahead job is no longer needed?
	 * Calling context is just ahead of short-ahead, so this does not need to try to check that. But knowing that, can look for other possible errors.
	 */
	private Boolean unCompletedUnneededHousekeep(final WorkInstruction inWi) {
		// Keep in mind this odd-ball case:
		// One short will short ahead and get some shorts and repeatPos.
		// A later short will short ahead and look for more. It is possible that one of the earlier short ahead is complete and just before it.
		WorkInstructionTypeEnum theType = inWi.getType();
		WorkInstructionStatusEnum theStatus = inWi.getStatus();
		if (theStatus != WorkInstructionStatusEnum.NEW && theStatus != WorkInstructionStatusEnum.SHORT) {
			LOGGER.error("bad calling context 1 for unCompletedUnneededHousekeep");
			return false;
		}

		if (theType == WorkInstructionTypeEnum.HK_REPEATPOS || theType == WorkInstructionTypeEnum.HK_BAYCOMPLETE) {
			return true;
		}

		return false;
	}

	// --------------------------------------------------------------------------
	/**
	 * The inShortWi was just shorted.
	 * Compare later wis to that. If the same product, short those also, removing unnecessary housekeeping work instructions
	 */
	private void doShortAheads(final WorkInstruction inShortWi) {
		// Look for more wis in the CHE's job list that must short also if this one did.
		// Short and notify them, and remove so the worker will not encounter them. 
		// Also remove unnecessary repeats and bay changes. The algorithm for housekeep removal is:
		// - There may be bay changes or repeat containers. And only one housekeep between at a time (for now--this may break)
		// - So track the previous work instruction as we iterate. If there is a housekeep right before a short ahead, we assume it is not needed.
		// - Repeat: definitely not needed. Baychange: maybe not needed. Better to over-remove than leave a final bay change at the end of the list.

		Integer laterCount = 0;
		Integer toShortCount = 0;
		Integer removeHousekeepCount = 0;
		// Algorithm:  The all picks list is ordered by sequence. So consider anything in that list with later sequence.
		// One or two of these might also be in mActivePickWiList if it is a simultaneous work instruction pick that we are on.  Remove if found there.
		WorkInstruction prevWi = null;
		for (WorkInstruction wi : mAllPicksWiList) {
			if (laterWi(wi, inShortWi)) {
				laterCount++;
				if (sameProductLotEtc(wi, inShortWi)) {
					// When simultaneous pick work instructions return, we must not short ahead within the simultaneous pick. See CD_0043 for details.
					if (!mActivePickWiList.contains(wi)) {
						toShortCount++;
						// housekeeps that are not the current job should not be in mActivePickWiList. Just check that possibility to confirm our understanding.
						if (unCompletedUnneededHousekeep(prevWi)) {
							if (mActivePickWiList.contains(prevWi))
								LOGGER.error("unanticipated housekeep in mActivePickWiList in doShortAheads");
							removeHousekeepCount++;
							doCompleteUnneededHousekeep(prevWi);
						}
						// Short aheads will always set the actual pick quantity to zero.
						doShortTransaction(wi, 0);
					}
				}
			}
			prevWi = wi;
		}

		// Let's only report all this if there is a short ahead. We do not need to see in the log that we considered after every short.
		if (toShortCount > 0) {
			String reportShortAheadDetails = "Considered " + laterCount + " later jobs for short-ahead.";
			if (toShortCount > 0)
				reportShortAheadDetails += " Shorted " + toShortCount + " more.";
			if (removeHousekeepCount > 0)
				reportShortAheadDetails += " Also removed " + removeHousekeepCount + " housekeeping instructions.";
			LOGGER.info(reportShortAheadDetails);
		}

	}

	// --------------------------------------------------------------------------
	/**
	 * The guts of the short transaction
	 * Update the WI fields, and call out to mDeviceManager to share it back to the server.
	 */
	private void doShortTransaction(final WorkInstruction inWi, final Integer inActualPickQuantity) {

		inWi.setActualQuantity(inActualPickQuantity);
		inWi.setPickerId(mUserId);
		inWi.setCompleted(new Timestamp(System.currentTimeMillis()));
		inWi.setStatus(WorkInstructionStatusEnum.SHORT);

		// normal short will be in mActivePickWiList.
		// short-aheads will not be.
		if (mActivePickWiList.contains(inWi))
			mActivePickWiList.remove(inWi);

		mDeviceManager.completeWi(getGuid().getHexStringNoPrefix(), getPersistentId(), inWi);

	}

	// --------------------------------------------------------------------------
	/**
	 * Update the WI fields to complete-ahead an unneeded housekeep, and call out to mDeviceManager to share it back to the server.
	 */
	private void doCompleteUnneededHousekeep(final WorkInstruction inWi) {

		inWi.setActualQuantity(0);
		inWi.setPickerId(mUserId);
		inWi.setCompleted(new Timestamp(System.currentTimeMillis()));
		inWi.setStatus(WorkInstructionStatusEnum.COMPLETE);

		mDeviceManager.completeWi(getGuid().getHexStringNoPrefix(), getPersistentId(), inWi);
	}

	// --------------------------------------------------------------------------
	/**
	 * YES or NO confirm the short pick.
	 * @param inScanStr
	 */
	private void confirmShortPick(final String inScanStr) {
		if (inScanStr.equals(YES_COMMAND)) {
			// HACK HACK HACK
			// StitchFix is the first client and they only pick one item at a time - ever.
			// When we have h/w that picks more than one item we'll address this.
			WorkInstruction wi = mShortPickWi;
			;
			if (wi != null) {
				doShortTransaction(wi, mShortPickQty);

				LOGGER.info("Pick shorted: " + wi);

				clearLedControllersForWi(wi);

				doShortAheads(wi); // Jobs for the same product on the cart should automatically short, and not subject the user to them.

				if (mActivePickWiList.size() > 0) {
					// If there's more active picks then show them.
					// This is tricky. Simultaneous work instructions: which was short? All of them?
					LOGGER.error("Simulataneous work instructions turned off currently, so unexpected case in confirmShortPick");
					showActivePicks();
				} else {
					// There's no more active picks, so move to the next set.
					doNextPick();
				}
			}
		} else {
			// Just return to showing the active picks.
			setState(CheStateEnum.DO_PICK);
			// showActivePicks();
		}
	}

	// --------------------------------------------------------------------------
	/**  doNextPick has a major side effect of setState(DO_PICK) if there is more work.
	 *   Then setState(DO_PICK) calls showActivePicks()
	 */
	private void doNextPick() {
		LOGGER.debug(this + "doNextPick");

		if (mActivePickWiList.size() > 0) {
			// There are still picks in the active list.
			LOGGER.error("Unexpected case in doNextPick");
			showActivePicks();
			// each caller to doNextPick already checked mActivePickWiList.size(). Therefore new situation if found

		} else {

			if (selectNextActivePicks()) {
				setState(CheStateEnum.DO_PICK); // This will cause showActivePicks();
				// showActivePicks();
			} else {
				processPickComplete();
			}
		}
	}

	// --------------------------------------------------------------------------
	/**
	 * Sort the WIs by their distance along the path.
	private class WiDistanceComparator implements Comparator<WorkInstruction> {

		public int compare(WorkInstruction inWi1, WorkInstruction inWi2) {
			int value = CompareNullChecker.compareNulls(inWi1, inWi2);
			if (value != 0)
				return value;

			Double wi1Pos = inWi1.getPosAlongPath();
			Double wi2Pos = inWi2.getPosAlongPath();
			value = CompareNullChecker.compareNulls(wi1Pos, wi2Pos);
			if (value != 0)
				return value;

			return wi1Pos.compareTo(wi2Pos);
		}
	};
	 */

	// --------------------------------------------------------------------------
	/**
	 * Sort the WIs by their sort code.
	 */
	private class WiGroupSortComparator implements Comparator<WorkInstruction> {

		@Override
		public int compare(WorkInstruction inWi1, WorkInstruction inWi2) {

			int value = CompareNullChecker.compareNulls(inWi1, inWi2);
			if (value != 0)
				return value;

			String w1Sort = inWi1.getGroupAndSortCode();
			String w2Sort = inWi2.getGroupAndSortCode();
			value = CompareNullChecker.compareNulls(w1Sort, w2Sort);
			if (value != 0)
				return value;

			return w1Sort.compareTo(w2Sort);
		}
	};

	// --------------------------------------------------------------------------
	/**
	 */
	private boolean selectNextActivePicks() {
		final boolean kDoMultipleWiPicks = false;

		boolean result = false;

		// Loop through each container to see if there is a WI for that container at the next location.
		// The "next location" is the first location we find for the next pick.
		String firstLocationId = null;
		String firstItemId = null;
		Collections.sort(mAllPicksWiList, new WiGroupSortComparator());
		for (WorkInstruction wi : mAllPicksWiList) {
			if (mContainersMap.values().isEmpty()) {
				LOGGER.warn(this + " assigned work but no containers assigned");
			}
			for (String containerId : mContainersMap.values()) {
				// If the WI is for this container then consider it.
				if (wi.getContainerId().equals(containerId)) {
					// If the WI is INPROGRESS or NEW then consider it.
					if ((wi.getStatus().equals(WorkInstructionStatusEnum.NEW))
							|| (wi.getStatus().equals(WorkInstructionStatusEnum.INPROGRESS))) {
						if ((firstLocationId == null) || (firstLocationId.equals(wi.getLocationId()))) {
							if ((firstItemId == null) || (firstItemId.equals(wi.getItemId()))) {
								firstLocationId = wi.getLocationId();
								firstItemId = wi.getItemId();
								wi.setStarted(new Timestamp(System.currentTimeMillis()));
								mActivePickWiList.add(wi);
								result = true;
								if (!kDoMultipleWiPicks)
									return true; // bail here instead of continuing to next wi in mAllPicksWiList, looking for location/item match
							}
						}
					}
				}
			}
		}

		return result;
	}

	// --------------------------------------------------------------------------
	/**
	 */
	private void processPickComplete() {
		// There are no more WIs, so the pick is complete.

		// Clear the existing LEDs.
		ledControllerClearLeds(); // this checks getLastLedControllerGuid(), and bails if null.

		// CD_0041 is there a need for this?
		ledControllerShowLeds(getGuid());

		setState(CheStateEnum.PICK_COMPLETE);
	}

	// --------------------------------------------------------------------------
	/**
	 * Our position controllers may only display up to 99. Integer overflows into bytes go negative, which have special meanings.
	 * So, any value larger than 99 shall display 99. For those cases, we show the count in the description.
	 */
	private byte byteValueForPositionDisplay(Integer inInt) {
		if (inInt > maxCountForPositionControllerDisplay)
			return maxCountForPositionControllerDisplay.byteValue();
		else
			return inInt.byteValue();
	}

	// --------------------------------------------------------------------------
	/**
	 * Sort the command groups by their position. It is important that lower ones go out first.
	 */
	private class CmdGroupComparator implements Comparator<LedCmdGroup> {

		@Override
		public int compare(LedCmdGroup inGroup1, LedCmdGroup inGroup2) {
			if (inGroup1 == null) {
				return -1;
			} else if (inGroup2 == null) {
				return 1;
			} else {
				return inGroup1.compareTo(inGroup2);
			}
		}
	};

	private String getMyGuidStrForLog() {
		return getGuid().getHexStringNoPrefix();
	}

	// --------------------------------------------------------------------------
	/**
	 * Send to the LED controller the active picks for the work instruction that's active on the CHE now.
	 */
	private void showActivePicks() {
		final int kMaxLedSetsToLog = 20;

		if (mActivePickWiList.size() > 0) {
			// The first WI has the SKU and location info.
			WorkInstruction firstWi = mActivePickWiList.get(0);

			// Send the CHE a display command (any of the WIs has the info we need).
			if (getCheStateEnum() != CheStateEnum.DO_PICK && getCheStateEnum() != CheStateEnum.SHORT_PICK) {
				LOGGER.error("unanticipated state in showActivePicks");
				setState(CheStateEnum.DO_PICK);
			}

			// Tell the last aisle controller this device displayed on, to remove any led commands for this.
			ledControllerClearLeds();

			// This part is easy. Just display on the CHE controller
			if (getCheStateEnum() == CheStateEnum.SHORT_PICK)
				sendDisplayWorkInstruction(firstWi.getPickInstruction(), firstWi.getDescription(), firstWi.getPlanQuantity(), true);
			else
				sendDisplayWorkInstruction(firstWi.getPickInstruction(), firstWi.getDescription(), firstWi.getPlanQuantity(), false);

			// Not as easy. Clear this CHE's last leds off of aisle controller(s), and tell aisle controller(s) what to light next
			// List<LedCmdGroup> ledCmdGroups = LedCmdGroupSerializer.deserializeLedCmdString(firstWi.getLedCmdStream());
			String wiCmdString = firstWi.getLedCmdStream();
			LOGGER.info("deserialize and send out this WI cmd string: " + wiCmdString);
			List<LedCmdGroup> ledCmdGroups = LedCmdGroupSerializer.deserializeLedCmdString(wiCmdString);
			if (!LedCmdGroupSerializer.verifyLedCmdGroupList(ledCmdGroups))
				LOGGER.error("WI cmd string did not deserialize properly");

			// It is important sort the CmdGroups.
			Collections.sort(ledCmdGroups, new CmdGroupComparator());

			INetworkDevice lastLedController = null;
			// This is not about clearing controllers/channels this CHE had lights on for.  Rather, it was about iterating the command groups and making sure
			// we do not clear out the first group when adding on a second. This is a concern for simultaneous multiple dispatch--not currently done.

			String myGuidStr = getMyGuidStrForLog();

			for (Iterator<LedCmdGroup> iterator = ledCmdGroups.iterator(); iterator.hasNext();) {
				LedCmdGroup ledCmdGroup = iterator.next();

				// The WI's ledCmdStream includes the controller ID. Usually only one command group per WI. So, we are setting ledController as the aisleDeviceLogic for the next WI's lights
				NetGuid nextControllerGuid = new NetGuid(ledCmdGroup.getControllerId());
				INetworkDevice ledController = mRadioController.getNetworkDevice(nextControllerGuid);
				
				if (ledController != null) {
					// jr/hibernate. See null channel in testPickViaChe test. Screen
					Short cmdGroupChannnel = ledCmdGroup.getChannelNum();
					if (cmdGroupChannnel == null || cmdGroupChannnel == 0) {
						String wiInfo = firstWi.getGroupAndSortCode() + "--  item: " + firstWi.getItemId() + "  cntr: " + firstWi.getContainerId();
						LOGGER.error("Bad channel after deserializing LED command from the work instruction for sequence" + wiInfo);
						continue;
					}

					

					Short startLedNum = ledCmdGroup.getPosNum();
					Short currLedNum = startLedNum;

					// Clear the last LED commands to this controller if the last controller was different.
					if ((lastLedController != null) && (!ledController.equals(lastLedController))) {
						ledControllerClearLeds(nextControllerGuid);
						lastLedController = ledController;
					}

					NetGuid ledControllerGuid = ledController.getGuid();
					String controllerGuidStr = ledControllerGuid.getHexStringNoPrefix();
					// short cmdGroupChannnel = ledCmdGroup.getChannelNum();
					String toLogString = "CHE " + myGuidStr + " telling " + controllerGuidStr + " to set LEDs. " + EffectEnum.FLASH;
					Integer setCount = 0;
					for (LedSample ledSample : ledCmdGroup.getLedSampleList()) {

						// why are we doing this? Aren't the samples made correctly?
						ledSample.setPosition(currLedNum++);

						// Add this LED display to the aisleController. We are accumulating the log information here rather than logging separate in the called routine.
						ledControllerSetLed(ledControllerGuid, cmdGroupChannnel, ledSample, EffectEnum.FLASH);

						// Log concisely instead of each ledCmd individually
						setCount++;
						if (setCount <= kMaxLedSetsToLog)
							toLogString = toLogString + " " + ledSample.getPosition() + ":" + ledSample.getColor();
					}
					if (setCount > 0)
						LOGGER.info(toLogString);
					if (setCount > kMaxLedSetsToLog)
						LOGGER.info("And more LED not logged. Total LED Sets this update = " + setCount);

					if (ledController.isDeviceAssociated()) {
						ledControllerShowLeds(ledControllerGuid);
					}
				}
			}

			// Housekeeping moves will result in a single work instruction in the active pickes. Enum tells if housekeeping.
			if (!sendHousekeepingDisplay()) {
				byte planQuantityForPositionController = byteValueForPositionDisplay(firstWi.getPlanQuantity());
				byte minQuantityForPositionController = byteValueForPositionDisplay(firstWi.getPlanMinQuantity());
				byte maxQuantityForPositionController = byteValueForPositionDisplay(firstWi.getPlanMaxQuantity());
				if (getCheStateEnum() == CheStateEnum.SHORT_PICK)
					minQuantityForPositionController = byteValueForPositionDisplay(0); // allow shorts to decrement on position controller down to zero

				// Also pretty easy. Light the position controllers on this CHE
				byte freq = PosControllerInstr.BRIGHT_FREQ;
				byte brightness = PosControllerInstr.BRIGHT_DUTYCYCLE;
				// blink is a weak indicator that decrement button is active, usually as a consequence of short pick. (Max difference is also possible for discretionary picks)
				if (planQuantityForPositionController != minQuantityForPositionController
						|| planQuantityForPositionController != maxQuantityForPositionController) {
					freq = PosControllerInstr.BLINK_DUTYCYCLE;
					brightness = PosControllerInstr.BLINK_DUTYCYCLE;
				}

				List<PosControllerInstr> instructions = new ArrayList<PosControllerInstr>();
				for (WorkInstruction wi : mActivePickWiList) {
					for (Entry<String, String> mapEntry : mContainersMap.entrySet()) {
						if (mapEntry.getValue().equals(wi.getContainerId())) {
							PosControllerInstr instruction = new PosControllerInstr(Byte.valueOf(mapEntry.getKey()),
								planQuantityForPositionController,
								minQuantityForPositionController,
								maxQuantityForPositionController,
								freq,
								brightness);
							instructions.add(instruction);
						}
					}
				}
				sendPositionControllerInstructions(instructions);
			}

		}
		ledControllerShowLeds(getGuid());
	}

	// --------------------------------------------------------------------------
	/**
	 * return the button for this container ID. Mostly private use, but public for unit test convenience
	 */
	public byte buttonFromContainer(String inContainerId) {
		// must we do linear search? The code does throughout. Seems like map direct lookup would be fine.
		for (Entry<String, String> mapEntry : mContainersMap.entrySet()) {
			if (mapEntry.getValue().equals(inContainerId)) {
				return Byte.valueOf(mapEntry.getKey());
			}
		}
		return 0;
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inWi
	 */
	private void clearLedControllersForWi(final WorkInstruction inWi) {

		List<LedCmdGroup> ledCmdGroups = LedCmdGroupSerializer.deserializeLedCmdString(inWi.getLedCmdStream());

		for (Iterator<LedCmdGroup> iterator = ledCmdGroups.iterator(); iterator.hasNext();) {
			LedCmdGroup ledCmdGroup = iterator.next();

			INetworkDevice ledController = mRadioController.getNetworkDevice(new NetGuid(ledCmdGroup.getControllerId()));
			if (ledController != null) {
				ledControllerClearLeds(ledController.getGuid());
			}
		}
	}

	// --------------------------------------------------------------------------
	/**
	 * The CHE is in the Idle state (not logged in), so if we get a user scan then start the setup process.
	 * @param inPrefixScanStr
	 * @param inScanStr
	 */
	private void processIdleStateScan(final String inScanPrefixStr, final String inScanStr) {

		if (USER_PREFIX.equals(inScanPrefixStr)) {
			setState(CheStateEnum.CONTAINER_SELECT);
		} else {
			LOGGER.info("Not a user ID: " + inScanStr);
			invalidScanMsg(CheStateEnum.IDLE);
		}
	}

	// --------------------------------------------------------------------------
	/**
	 * @param insScanPrefixStr
	 * @param inScanStr
	 */
	private void processLocationScan(final String inScanPrefixStr, String inScanStr) {
		if (LOCATION_PREFIX.equals(inScanPrefixStr)) {
			this.mLocationId = inScanStr;
			mDeviceManager.getCheWork(getGuid().getHexStringNoPrefix(), getPersistentId(), inScanStr);

			setState(CheStateEnum.GET_WORK);

		} else {
			LOGGER.info("Not a location ID: " + inScanStr);
			invalidScanMsg(mCheStateEnum);
		}
	}

	// --------------------------------------------------------------------------
	/**
	 * @param insScanPrefixStr
	 * @param inScanStr
	 */
	private void processContainerSelectScan(final String inScanPrefixStr, String inScanStr) {
		// DEV-518. Also accept the raw order ID.
		if (inScanPrefixStr.isEmpty() || CONTAINER_PREFIX.equals(inScanPrefixStr)) {

			mContainerInSetup = inScanStr;

			// Check to see if this container is already setup in a position.
			Iterator<Entry<String, String>> setIterator = mContainersMap.entrySet().iterator();
			while (setIterator.hasNext()) {
				Entry<String, String> entry = setIterator.next();
				if (entry.getValue().equals(mContainerInSetup)) {
					setIterator.remove();
					break;
				}
			}
			// It would be cool if we could check here. Call to REST API on the server? Needs to not block for long, though.
			// When we scan a container, that container either should match a cross batch order detail, or match an outbound order's preassigned container. If not, 
			// this is a "ride along" error. Would be nice if the user could see it immediately.

			showAssignedPositions();
			setState(CheStateEnum.CONTAINER_POSITION);
		} else {
			LOGGER.info("Not a container ID: " + inScanStr);
			invalidScanMsg(CheStateEnum.CONTAINER_SELECT);
		}
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inScanPrefixStr
	 * @param inScanStr
	 */
	private void processContainerPosition(final String inScanPrefixStr, String inScanStr) {
		if (POSITION_PREFIX.equals(inScanPrefixStr)) {
			if (mContainersMap.get(inScanStr) == null) {
				mContainersMap.put(inScanStr, mContainerInSetup);
				mContainerInSetup = "";
				showAssignedPositions();
				setState(CheStateEnum.CONTAINER_SELECT);
			} else {
				sendDisplayCommand(SELECT_POSITION_MSG, POSITION_IN_USE_MSG);
				mCheStateEnum = CheStateEnum.CONTAINER_POSITION;
			}
		} else {
			invalidScanMsg(CheStateEnum.CONTAINER_POSITION);
		}
	}

	// --------------------------------------------------------------------------
	/**
	 * Complete the active WI at the selected position.
	 * @param inButtonNum
	 * @param inQuantity
	 */
	private void processButtonPress(Integer inButtonNum, Integer inQuantity) {
		String containerId = getContainerIdFromButtonNum(inButtonNum);
		if (containerId == null) {
			// Simply ignore button presses when there is no container.
			//invalidScanMsg(mCheStateEnum);
		} else {
			WorkInstruction wi = getWorkInstructionForContainerId(containerId);
			if (wi == null) {
				// Simply ignore button presses when there is no work instruction.
				//invalidScanMsg(mCheStateEnum);
			} else {
				String itemId = wi.getItemId();
				LOGGER.info("Button #" + inButtonNum + " for " + containerId + " / " + itemId);
				if (inQuantity >= wi.getPlanMinQuantity()) {
					processNormalPick(wi, inQuantity);
				} else {
					// More kludge for count > 99 case
					Integer planQuantity = wi.getPlanQuantity();
					if (inQuantity == maxCountForPositionControllerDisplay && planQuantity > maxCountForPositionControllerDisplay)
						processNormalPick(wi, planQuantity); // Assume all were picked. No way for user to tell if more than 98 given.
					else
						processShortPick(wi, inQuantity);
				}
			}
		}
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inContainerId
	 * @return
	 */
	private WorkInstruction getWorkInstructionForContainerId(String inContainerId) {
		WorkInstruction result = null;
		Iterator<WorkInstruction> wiIter = mActivePickWiList.iterator();
		while (wiIter.hasNext()) {
			WorkInstruction wi = wiIter.next();
			if (wi.getContainerId().equals(inContainerId)) {
				result = wi;
			}
		}
		return result;
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inButtonNum
	 * @return
	 */
	private String getContainerIdFromButtonNum(Integer inButtonNum) {
		return mContainersMap.get(Integer.toString(inButtonNum));
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inWi
	 * @param inQuantity
	 */
	private void processNormalPick(WorkInstruction inWi, Integer inQuantity) {

		inWi.setActualQuantity(inQuantity);
		inWi.setPickerId(mUserId);
		inWi.setCompleted(new Timestamp(System.currentTimeMillis()));
		inWi.setStatus(WorkInstructionStatusEnum.COMPLETE);

		mDeviceManager.completeWi(getGuid().getHexStringNoPrefix(), getPersistentId(), inWi);
		LOGGER.info("Pick completed: " + inWi);

		mActivePickWiList.remove(inWi);

		clearLedControllersForWi(inWi);

		if (mActivePickWiList.size() > 0) {
			// If there's more active picks then show them.
			LOGGER.error("Simulataneous work instructions turned off currently, so unexpected case in processNormalPick");
			showActivePicks();
		} else {
			// There's no more active picks, so move to the next set.
			doNextPick();
		}
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inWi
	 * @param inQuantity
	 */
	private void processShortPick(WorkInstruction inWi, Integer inQuantity) {
		setState(CheStateEnum.SHORT_PICK_CONFIRM);
		mShortPickWi = inWi;
		mShortPickQty = inQuantity;
	}

	private void clearOnePositionController(Byte inPosition) {
		ICommand command = new CommandControlClearPosController(NetEndpoint.PRIMARY_ENDPOINT, inPosition);

		//Remove lastSent Set Instr from map to indicate the clear
		if (PosControllerInstr.POSITION_ALL.equals(inPosition)) {
			mPosToLastSetIntrMap.clear();
		} else {
			mPosToLastSetIntrMap.remove(inPosition);
		}

		mRadioController.sendCommand(command, getAddress(), true);
	}

	// --------------------------------------------------------------------------
	/**
	 */
	private void clearAllPositionControllers() {
		clearOnePositionController(PosControllerInstr.POSITION_ALL);
	}

	// --------------------------------------------------------------------------
	/**
	 * Determine if the mActivePickWiList represents a housekeeping move. If so, display it and return true
	 */
	private boolean sendHousekeepingDisplay() {
		boolean returnBool = false;
		if (mActivePickWiList.size() == 1) {
			WorkInstruction wi = mActivePickWiList.get(0);
			if (wi == null)
				LOGGER.error("misunderstanding in sendHousekeepingDisplay");
			else {
				WorkInstructionTypeEnum theEnum = wi.getType();
				if (theEnum == WorkInstructionTypeEnum.HK_BAYCOMPLETE) {
					returnBool = true;
					showSpecialPositionCode(PosControllerInstr.BAY_COMPLETE_CODE, wi.getContainerId());
				} else if (theEnum == WorkInstructionTypeEnum.HK_REPEATPOS) {
					returnBool = true;
					showSpecialPositionCode(PosControllerInstr.REPEAT_CONTAINER_CODE, wi.getContainerId());
				}
			}
		}
		return returnBool;
	}

	// --------------------------------------------------------------------------
	/**
	 * This shows all the positions already assigned to containers in the mContainersMap
	 */
	private void showAssignedPositions() {
		List<PosControllerInstr> instructions = new ArrayList<PosControllerInstr>();
		for (String pos : mContainersMap.keySet()) {
			PosControllerInstr instruction = new PosControllerInstr(Byte.valueOf(pos),
				PosControllerInstr.POSITION_ASSIGNED_CODE,
				PosControllerInstr.POSITION_ASSIGNED_CODE,
				PosControllerInstr.POSITION_ASSIGNED_CODE,
				PosControllerInstr.MED_FREQ,
				PosControllerInstr.MED_DUTYCYCLE);
			instructions.add(instruction);
		}
		sendPositionControllerInstructions(instructions);
	}

	// --------------------------------------------------------------------------
	/**
	 */
	private void showSpecialPositionCode(Byte inSpecialQuantityCode, String inContainerId) {
		boolean codeUnderstood = false;
		Byte codeToSend = inSpecialQuantityCode;
		if (inSpecialQuantityCode == PosControllerInstr.BAY_COMPLETE_CODE)
			codeUnderstood = true;
		else if (inSpecialQuantityCode == PosControllerInstr.REPEAT_CONTAINER_CODE)
			codeUnderstood = true;

		if (!codeUnderstood) {
			codeToSend = PosControllerInstr.ERROR_CODE_QTY;
			LOGGER.error("showSpecialPositionCode: unknown quantity code in ");
		}
		List<PosControllerInstr> instructions = new ArrayList<PosControllerInstr>();
		for (Entry<String, String> mapEntry : mContainersMap.entrySet()) {
			if (mapEntry.getValue().equals(inContainerId)) {
				PosControllerInstr instruction = new PosControllerInstr(Byte.valueOf(mapEntry.getKey()),
					codeToSend,
					codeToSend,
					codeToSend,
					PosControllerInstr.MED_FREQ, // change from BRIGHT_FREQ
					PosControllerInstr.MED_DUTYCYCLE); // change from BRIGHT_DUTYCYCLE v6
				instructions.add(instruction);
			}
		}
		if (instructions.size() > 0)
			sendPositionControllerInstructions(instructions);
		else
			LOGGER.error("container match not found in showSpecialPositionCode");
	}

	// --------------------------------------------------------------------------
	/** Light position controllers appropriately for bay change, keyed off the previous work instruction's containerId
	 */
	@SuppressWarnings("unused")
	private void showBayChange(String inContainerId) {
		showSpecialPositionCode(PosControllerInstr.BAY_COMPLETE_CODE, inContainerId);
	}

	// --------------------------------------------------------------------------
	/** Light position controllers appropriately for repeat container, keyed off the previous work instruction's containerId
	 */
	@SuppressWarnings("unused")
	private void showRepeatContainer(String inContainerId) {
		showSpecialPositionCode(PosControllerInstr.REPEAT_CONTAINER_CODE, inContainerId);
	}

	@Override
	public String toString() {
		return Objects.toStringHelper(this).add("netGuid", getGuid()).toString();
	}

	/**
	 *  Currently, these cannot be longer than 20 characters.
	 */
	private static String cheLine(String message) {
		Preconditions.checkNotNull(message, "Message cannot be null");
		Preconditions.checkArgument(message.length() <= 20, "Message '%s' will not fit on che display", message);
		return Strings.padEnd(message, 20 - message.length(), ' ');
	}

	public void disconnectedFromServer() {
		connectedToServer = false;
		sendDisplayCommand("Server Connection", "Unavailable", "Please Wait...", "");
	}

	public void connectedToServer() {
		connectedToServer = true;
		redisplayState();

	}

	private void redisplayState() {
		setState(mCheStateEnum);
	}
}
