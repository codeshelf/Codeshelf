/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2013, Jeffrey B. Williams, All rights reserved
 *  $Id: CheDeviceLogic.java,v 1.12 2013/09/05 03:26:03 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.device;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import lombok.Getter;
import lombok.experimental.Accessors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gadgetworks.codeshelf.device.AisleDeviceLogic.LedCmd;
import com.gadgetworks.codeshelf.model.WorkInstructionCount;
import com.gadgetworks.codeshelf.model.domain.WorkInstruction;
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
import com.google.common.collect.Lists;

/**
 * CheDeviceLogic is now an abstract base class for CHE programs with different state machines.
 * See SetupOrdersDeviceLogic to get what CheDeviceLogic used to be all by itself.
 */
public class CheDeviceLogic extends DeviceLogicABC {
	// This code runs on the site controller, not the CHE.
	// The goal is to convert data and instructions to something that the CHE controller can consume and act on with minimal logic.

	private static final Logger				LOGGER									= LoggerFactory.getLogger(CheDeviceLogic.class);

	protected static final String			COMMAND_PREFIX							= "X%";
	protected static final String			USER_PREFIX								= "U%";
	protected static final String			CONTAINER_PREFIX						= "C%";
	protected static final String			LOCATION_PREFIX							= "L%";
	protected static final String			ITEMID_PREFIX							= "I%";
	protected static final String			POSITION_PREFIX							= "P%";

	// These are the message strings we send to the remote CHE.
	// Currently, these cannot be longer than 20 characters.
	// "SCAN START LOCATION" is at the 20 limit. If you change to "SCAN STARTING LOCATION", you get very bad behavior. The class loader will not find the CheDeviceLogic. Repeating throws.	
	protected static final String			EMPTY_MSG								= cheLine("");
	protected static final String			INVALID_SCAN_MSG						= cheLine("INVALID");
	protected static final String			SCAN_USERID_MSG							= cheLine("SCAN BADGE");
	protected static final String			SCAN_LOCATION_MSG						= cheLine("SCAN START LOCATION");
	protected static final String			OR_START_WORK_MSG						= cheLine("OR START WORK");
	protected static final String			SELECT_POSITION_MSG						= cheLine("SELECT POSITION");
	protected static final String			SHORT_PICK_CONFIRM_MSG					= cheLine("CONFIRM SHORT");
	protected static final String			PICK_COMPLETE_MSG						= cheLine("ALL WORK COMPLETE");
	protected static final String			YES_NO_MSG								= cheLine("SCAN YES OR NO");
	protected static final String			NO_CONTAINERS_SETUP_MSG					= cheLine("NO SETUP CONTAINERS");
	protected static final String			POSITION_IN_USE_MSG						= cheLine("POSITION IN USE");
	protected static final String			FINISH_SETUP_MSG						= cheLine("PLS SETUP CONTAINERS");
	protected static final String			COMPUTE_WORK_MSG						= cheLine("COMPUTING WORK");
	protected static final String			GET_WORK_MSG							= cheLine("GETTING WORK");
	protected static final String			NO_WORK_MSG								= cheLine("NO WORK TO DO");
	protected static final String			LOCATION_SELECT_REVIEW_MSG_LINE_1		= cheLine("REVIEW MISSING WORK");
	protected static final String			LOCATION_SELECT_REVIEW_MSG_LINE_2		= cheLine("OR SCAN LOCATION");
	protected static final String			LOCATION_SELECT_REVIEW_MSG_LINE_3		= cheLine("TO CONTINUE AS IS");
	protected static final String			SHOWING_ORDER_IDS_MSG					= cheLine("SHOWING ORDER IDS");
	protected static final String			SHOWING_WI_COUNTS						= cheLine("SHOWING WI COUNTS");

	protected static final String			INVALID_POSITION_MSG					= cheLine("INVALID POSITION");
	protected static final String			INVALID_CONTAINER_MSG					= cheLine("INVALID CONTAINER");
	protected static final String			CLEAR_ERROR_MSG_LINE_1					= cheLine("CLEAR ERROR TO");
	protected static final String			CLEAR_ERROR_MSG_LINE_2					= cheLine("CONTINUE");
	
	// Newer messages only used in Line_Scan mode. Some portion of the above are used for both Setup_Orders and Line_Scan, so keeping them all here.
	protected static final String			SCAN_LINE_MSG							= cheLine("SCAN ORDER LINE");
	

	protected static final String			STARTWORK_COMMAND						= "START";
	protected static final String			SETUP_COMMAND							= "SETUP";
	protected static final String			SHORT_COMMAND							= "SHORT";
	protected static final String			LOGOUT_COMMAND							= "LOGOUT";
	protected static final String			YES_COMMAND								= "YES";
	protected static final String			NO_COMMAND								= "NO";
	protected static final String			CLEAR_ERROR_COMMAND						= "CLEAR";

	protected static final Integer			maxCountForPositionControllerDisplay	= 99;

	// The CHE's current state.
	@Accessors(prefix = "m")
	@Getter
	protected CheStateEnum					mCheStateEnum;

	// The CHE's current user.
	@Accessors(prefix = "m")
	@Getter
	protected String						mUserId;

	// All WIs for all containers on the CHE.
	@Accessors(prefix = "m")
	@Getter
	protected List<WorkInstruction>			mAllPicksWiList;

	//Container Position to last SetInstruction Map
	@Accessors(prefix = "m")
	@Getter
	private Map<Byte, PosControllerInstr>	mPosToLastSetIntrMap;

	// The active pick WIs.
	@Accessors(prefix = "m")
	@Getter
	protected List<WorkInstruction>			mActivePickWiList;

	private NetGuid							mLastLedControllerGuid;
	private boolean							mMultipleLastLedControllerGuids;															// Could have a list, but this will be quite rare.

	protected WorkInstruction				mShortPickWi;
	protected Integer						mShortPickQty;

	protected boolean						connectedToServer						= true;

	public CheDeviceLogic(final UUID inPersistentId,
		final NetGuid inGuid,
		final ICsDeviceManager inDeviceManager,
		final IRadioController inRadioController) {
		super(inPersistentId, inGuid, inDeviceManager, inRadioController);

		mCheStateEnum = CheStateEnum.IDLE;
		mAllPicksWiList = new ArrayList<WorkInstruction>();
		mActivePickWiList = new ArrayList<WorkInstruction>();
		mPosToLastSetIntrMap = new HashMap<Byte, PosControllerInstr>();
	}

	@Override
	public final short getSleepSeconds() {
		return 180;
	}

	public String getDeviceType() {
		LOGGER.error("getDeviceType(): Should have specific instance of this abstract type");
		return CsDeviceManager.DEVICETYPE_CHE; 
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
	protected void sendDisplayCommand(final String inLine1Message, final String inLine2Message) {
		sendDisplayCommand(inLine1Message, inLine2Message, "", "");
	}

	protected void setState(final CheStateEnum inCheState) {

	}

	// --------------------------------------------------------------------------
	/**
	 * @param inLine1Message
	 * @param inLine2Message
	 * @param inLine3Message
	 * @param inLine4Message
	 */
	protected void sendDisplayCommand(final String inLine1Message,
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
	private void sendDisplayWorkInstruction(WorkInstruction wi) {
		LOGGER.info("need override for sendDisplayWorkInstruction()");
		// much of the function in SetupOrdersDeviceLogic might be appropriate.
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
	protected void sendPositionControllerInstructions(List<PosControllerInstr> inInstructions) {
		LOGGER.info("Sending PosCon Instructions {}", inInstructions);
		//Update the last sent posControllerInstr for the position 
		for (PosControllerInstr instr : inInstructions) {
			if (PosControllerInstr.POSITION_ALL.equals(instr.getPosition())) {
				//A POS_ALL instruction overrides all previous instructions
				mPosToLastSetIntrMap.clear();
			}
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
	protected void ledControllerSetLed(final NetGuid inControllerGuid,
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
	protected void ledControllerShowLeds(final NetGuid inControllerGuid) {
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
	protected void ledControllerClearLeds() {
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
	protected void ledControllerClearLeds(final NetGuid inLedControllerId) {
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

	/**
	 * @return - Returns the PosControllerInstr for the position given the count if any is warranted. Null otherwise.
	 */
	public PosControllerInstr getCartRunFeedbackInstructionForCount(WorkInstructionCount wiCount, byte position) {
		//if wiCount is null then the server did have any WIs for the order.
		//this is an "unknown" order id
		if (wiCount == null) {
			//Unknown order id matches "done for now" - dim, solid, dashes
			return new PosControllerInstr(position,
				PosControllerInstr.BITENCODED_SEGMENTS_CODE,
				PosControllerInstr.BITENCODED_LED_DASH,
				PosControllerInstr.BITENCODED_LED_DASH,
				PosControllerInstr.SOLID_FREQ.byteValue(),
				PosControllerInstr.DIM_DUTYCYCLE.byteValue());
		} else {
			byte count = (byte) wiCount.getGoodCount();
			LOGGER.info("Position Feedback: Poisition {} Counts {}", position, wiCount);
			if (count == 0) {
				//0 good WI's means dim display
				if (wiCount.hasBadCounts()) {
					//If there any bad counts then we are "done for now" - dim, solid, dashes
					return new PosControllerInstr(position,
						PosControllerInstr.BITENCODED_SEGMENTS_CODE,
						PosControllerInstr.BITENCODED_LED_DASH,
						PosControllerInstr.BITENCODED_LED_DASH,
						PosControllerInstr.SOLID_FREQ.byteValue(),
						PosControllerInstr.DIM_DUTYCYCLE.byteValue());
				} else {
					if (wiCount.getCompleteCount() == 0) {
						//This should not be possible (unless we only had a single HK WI, which would be a bug)
						//We will log this for now and treat it as a completed WI
						LOGGER.error("WorkInstructionCount has no counts {};", wiCount);
					}
					//Ready for packout - solid, dim, "oc"
					return new PosControllerInstr(position,
						PosControllerInstr.BITENCODED_SEGMENTS_CODE,
						PosControllerInstr.BITENCODED_LED_C,
						PosControllerInstr.BITENCODED_LED_O,
						PosControllerInstr.SOLID_FREQ.byteValue(),
						PosControllerInstr.DIM_DUTYCYCLE.byteValue());
				}
			} else {
				//No feedback is count > 0
				return null;
			}
		}
	}

	// --------------------------------------------------------------------------
	/* 
	 */
	public void processNonCommandScan(String inScanPrefixStr, String inContent) {
		LOGGER.error("processNonCommandScan needs override");
	}

		// --------------------------------------------------------------------------
	/* 
	 */
	@Override
	public void scanCommandReceived(String inCommandStr) {
		if (!connectedToServer) {
			LOGGER.debug("NotConnectedToServer: Ignoring scan command: " + inCommandStr);
			return;
		}

		LOGGER.info(this + " received scan command: " + inCommandStr);

		String scanPrefixStr = getScanPrefix(inCommandStr);
		String scanStr = getScanContents(inCommandStr, scanPrefixStr);

		// Command scans actions are determined by the scan content (the command issued) then state because they are more likely to be state independent
		if (inCommandStr.startsWith(COMMAND_PREFIX)) {
			processCommandScan(scanStr);
		} else {
			processNonCommandScan(scanPrefixStr, scanStr);
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
			processButtonPress((int) inButtonCommand.getPosNum(), (int) inButtonCommand.getValue(), inButtonCommand.getPosNum());
		} else {
			LOGGER.debug("NotConnectedToServer: Ignoring button command: " + inButtonCommand);
		}
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inScanStr
	 * @return
	 */
	protected String getScanPrefix(String inScanStr) {

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
	protected String getScanContents(String inScanStr, String inPrefix) {
		return inScanStr.substring(inPrefix.length());
	}

	// --------------------------------------------------------------------------
	/**
	 * Change state and display error message
	 * Send the LED error status as well (color: red effect: error channel: 0).
	 * 
	 */
	protected void invalidScanMsg(final CheStateEnum inCheState) {
		LOGGER.error("invalidScanMsg() override needed");
		// guessing at a start
		// mCheStateEnum = inCheState;
		//sendErrorCodeToAllPosCons();
	}

	// --------------------------------------------------------------------------
	/**
	 * Stay in the same state, but make the status invalid.
	 * Send the LED error status as well (color: red effect: error channel: 0).
	 * 
	 */
	protected void invalidScanMsg(String lineOne, String lineTwo, String lineThree, String lineFour) {
		sendDisplayCommand(lineOne, lineTwo, lineThree, lineFour);
		sendErrorCodeToAllPosCons();
	}

	/**
	 * Send Error Code to all PosCons
	 */
	protected void sendErrorCodeToAllPosCons() {
		List<PosControllerInstr> instructions = Lists.newArrayList(new PosControllerInstr(PosControllerInstr.POSITION_ALL,
			PosControllerInstr.BITENCODED_SEGMENTS_CODE,
			PosControllerInstr.BITENCODED_LED_E,
			PosControllerInstr.BITENCODED_LED_BLANK,
			PosControllerInstr.SOLID_FREQ, // change from BLINK_FREQ
			PosControllerInstr.MED_DUTYCYCLE)); // change from BRIGHT_DUTYCYCLE v6
		sendPositionControllerInstructions(instructions);
	}

	/** 
	 * Command scans are split out by command then state because they are more likely to be state independent
	 */
	protected void processCommandScan(final String inScanStr) {
		LOGGER.error("processCommandScan() needs override");
	}

	// --------------------------------------------------------------------------
	/**
	 */
	protected void logout() {
		LOGGER.info("User logut");
		// Clear all of the container IDs we were tracking.
		mPosToLastSetIntrMap.clear();
		mActivePickWiList.clear();
		mAllPicksWiList.clear();
		setState(CheStateEnum.IDLE);

		forceClearAllLedsForThisCheDevice();
		clearAllPositionControllers();
	}

	/**
	 * Setup the CHE by clearing all the datastructures
	 */
	protected void setupChe() {
		clearAllPositionControllers();
		mPosToLastSetIntrMap.clear();
		setState(CheStateEnum.CONTAINER_SELECT);
	}

	protected void setupCommandReceived() {
		LOGGER.error("setupCommandReceived() needs override");
	}

	protected void clearErrorCommandReceived() {
		LOGGER.error("clearErrorCommandReceived() needs override");
	}

	// --------------------------------------------------------------------------
	/**
	 * The user scanned the SHORT_PICK command to tell us there is no product to pick.
	 */
	protected void shortPickCommandReceived() {
		LOGGER.error("shortPickCommandReceived() needs override");
	}

	// --------------------------------------------------------------------------
	/**
	 * The user scanned YES or NO.
	 * @param inScanStr
	 */
	protected void yesOrNoCommandReceived(final String inScanStr) {
		LOGGER.error("yesOrNoCommandReceived() needs override");
	}

	// --------------------------------------------------------------------------
	/**
	 * The guts of the short transaction
	 * Update the WI fields, and call out to mDeviceManager to share it back to the server.
	 */
	protected void doShortTransaction(final WorkInstruction inWi, final Integer inActualPickQuantity) {
		LOGGER.error("doShortTransaction() override needed");
		// much of the SetupOrdersDeviceLogic one is appropriate still appropriate
	}

	protected void showCartRunFeedbackIfNeeded(Byte inPosition) {
		LOGGER.error("base class call for showCartRunFeedbackIfNeeded. Need override");
	}

	// --------------------------------------------------------------------------
	/**
	 * YES or NO confirm the short pick.
	 * @param inScanStr
	 */
	protected void confirmShortPick(final String inScanStr) {
		LOGGER.error("confirmShortPick() needs override");
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
	 * Is this useful linescan?  If not, move as private function to SetupOrdersDeviceLogic
	 */
	protected void processPickComplete() {
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
	protected byte byteValueForPositionDisplay(Integer inInt) {
		if (inInt > maxCountForPositionControllerDisplay)
			return maxCountForPositionControllerDisplay.byteValue();
		else
			return inInt.byteValue();
	}

	// --------------------------------------------------------------------------
	/**
	 * Sort the command groups by their position. It is important that lower ones go out first.
	 */
	class CmdGroupComparator implements Comparator<LedCmdGroup> {

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

	protected String getMyGuidStrForLog() {
		return getGuid().getHexStringNoPrefix();
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inWi
	 */
	protected void clearLedControllersForWi(final WorkInstruction inWi) {

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
	 * Complete the active WI at the selected position.
	 * @param inButtonNum
	 * @param inQuantity
	 * @param buttonPosition 
	 */
	protected void processButtonPress(Integer inButtonNum, Integer inQuantity, Byte buttonPosition) {
		LOGGER.error("processButtonPress() needs override");
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inWi
	 * @param inQuantity
	 */
	protected void processShortPick(WorkInstruction inWi, Integer inQuantity) {
		LOGGER.error("processShortPick() needs override");
	}

	protected void clearOnePositionController(Byte inPosition) {
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
	 * @param totalWorkInstructionCount
	 * @param containerToWorkInstructionCountMap - Map containerIds to WorkInstructionCount objects
	 * Not final only because we let CsDeviceManager call this generically.
	 */
	public void processWorkInstructionCounts(final Integer totalWorkInstructionCount,
		final Map<String, WorkInstructionCount> containerToWorkInstructionCountMap) {
		LOGGER.error("Inappropriate call to processWorkInstructionCounts()");
	}

	// --------------------------------------------------------------------------
	/**
	 * Give the CHE the work it needs to do for a container.
	 * This is recomputed at the server for ALL containers on the CHE and returned in work-order.
	 * Whatever the CHE thought it needed to do before is now invalid and is replaced by what we send here.
	 * @param inContainerId
	 * @param inWorkItemList
	 * Only not final because we let CsDeviceManager call this generically.
	 */
	public void assignWork(final List<WorkInstruction> inWorkItemList) {
		LOGGER.error("Inappropriate call to assignWork()");

	}

	// --------------------------------------------------------------------------
	/**
	 * return the button for this container ID. Mostly private use, but public for unit test convenience
	 * we let CsDeviceManager call this generically for CheDeviceLogic
	 */
	public byte buttonFromContainer(String inContainerId) {
		LOGGER.error("Inappropriate call to buttonFromContainer()");
		return 0;
	}

	// --------------------------------------------------------------------------
	/**
	 */
	protected void clearAllPositionControllers() {
		clearOnePositionController(PosControllerInstr.POSITION_ALL);
	}

	@Override
	public String toString() {
		return Objects.toStringHelper(this).add("netGuid", getGuid()).toString();
	}

	/**
	 *  Currently, these cannot be longer than 20 characters.
	 */
	protected static String cheLine(String message) {
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
