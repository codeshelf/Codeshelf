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
import lombok.Setter;
import lombok.experimental.Accessors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gadgetworks.codeshelf.device.AisleDeviceLogic.LedCmd;
import com.gadgetworks.codeshelf.model.WorkInstructionStatusEnum;
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
import com.gadgetworks.flyweight.controller.NetworkDeviceStateEnum;

/**
 * @author jeffw
 *
 */
public class CheDeviceLogic extends DeviceLogicABC {
	// This code runs on the site controller, not the CHE.
	// The goal is to convert data and instructions to something that the CHE controller can consume and act on with minimal logic.

	private static final Logger		LOGGER					= LoggerFactory.getLogger(CheDeviceLogic.class);

	private static final String		COMMAND_PREFIX			= "X%";
	private static final String		USER_PREFIX				= "U%";
	private static final String		CONTAINER_PREFIX		= "C%";
	private static final String		LOCATION_PREFIX			= "L%";
	private static final String		ITEMID_PREFIX			= "I%";
	private static final String		POSITION_PREFIX			= "P%";

	// These are the message strings we send to the remote CHE.
	// Currently, these cannot be longer than 10 characters.
	private static final String		EMPTY_MSG				= "                    ";
	private static final String		INVALID_SCAN_MSG		= "INVALID             ";
	private static final String		SCAN_USERID_MSG			= "SCAN BADGE          ";							//		 new String(new byte[] { 0x7c, (byte) 0x05 });
	private static final String		SCAN_LOCATION_MSG		= "SCAN LOCATION       ";
	private static final String		SCAN_CONTAINER_MSG		= "SCAN CONTAINER      ";
	private static final String		OR_START_WORK_MSG		= "OR START WORK       ";
	private static final String		SELECT_POSITION_MSG		= "SELECT POSITION     ";
	private static final String		SHORT_PICK_CONFIRM_MSG	= "CONFIRM SHORT       ";
	private static final String		PICK_COMPLETE_MSG		= "ALL WORK COMPLETE   ";
	private static final String		YES_NO_MSG				= "SCAN YES OR NO      ";
	private static final String		NO_CONTAINERS_SETUP_MSG	= "NO SETUP CONTAINERS ";
	private static final String		POSITION_IN_USE_MSG		= "POSITION IN USE     ";
	private static final String		FINISH_SETUP_MSG		= "PLS SETUP CONTAINERS";
	private static final String		COMPUTE_WORK_MSG		= "COMPUTING WORK      ";
	private static final String		GET_WORK_MSG			= "GETTING WORK        ";

	private static final String		STARTWORK_COMMAND		= "START";
	private static final String		SETUP_COMMAND			= "SETUP";
	private static final String		SHORT_COMMAND			= "SHORT";
	private static final String		LOGOUT_COMMAND			= "LOGOUT";
	private static final String		RESUME_COMMAND			= "RESUME";
	private static final String		YES_COMMAND				= "YES";
	private static final String		NO_COMMAND				= "NO";
	
	private static final Integer maxCountForPositionControllerDisplay = 99;

	// The CHE's current state.
	@Accessors(prefix = "m")
	@Getter
	@Setter
	private CheStateEnum			mCheStateEnum;

	// The CHE's current location.
	@Accessors(prefix = "m")
	@Getter
	@Setter
	private String					mLocationId;

	// The CHE's current user.
	@Accessors(prefix = "m")
	@Getter
	@Setter
	private String					mUserId;

	// The CHE's container map.
	private String					mContainerInSetup;

	// The CHE's container map.
	private Map<String, String>		mContainersMap;

	// All WIs for all containers on the CHE.
	private List<WorkInstruction>	mAllPicksWiList;

	// The active pick WIs.
	private List<WorkInstruction>	mActivePickWiList;

	// The completed  WIs.
	private List<WorkInstruction>	mCompletedWiList;

	private NetGuid					mLastLedControllerGuid;

	private WorkInstruction			mShortPickWi;
	private Integer					mShortPickQty;

	public CheDeviceLogic(final UUID inPersistentId,
		final NetGuid inGuid,
		final ICsDeviceManager inDeviceManager,
		final IRadioController inRadioController) {
		super(inPersistentId, inGuid, inDeviceManager, inRadioController);

		mCheStateEnum = CheStateEnum.IDLE;
		mContainersMap = new HashMap<String, String>();
		mAllPicksWiList = new ArrayList<WorkInstruction>();
		mActivePickWiList = new ArrayList<WorkInstruction>();
		mCompletedWiList = new ArrayList<WorkInstruction>();
	}

	public final short getSleepSeconds() {
		return 180;
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
		LOGGER.info("Display message: line1: " + inLine1Message);
		LOGGER.info("Display message: line2: " + inLine2Message);
		LOGGER.info("Display message: line3: " + inLine3Message);
		LOGGER.info("Display message: line4: " + inLine4Message);
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
	private void sendDisplayWorkInstruction(final String inPickInstructions, final String inDescription, final Integer inPlanQuantity) {
		String displayDescription = inDescription;
		if (inPlanQuantity > maxCountForPositionControllerDisplay - 1) {
			String countStr = inPlanQuantity.toString();
			displayDescription = countStr + " " + inDescription;
		}
		
		String[] descriptionLine = { "", "", ""};
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
		
		// Note: pickInstruction is more or less a location. Commonly a location alias, but may be a locationId or DDcId.
		sendDisplayCommand(inPickInstructions, descriptionLine[0], descriptionLine[1], descriptionLine[2]);
	}

	// --------------------------------------------------------------------------
	/**
	 * Send a pick request command to the CHE to light a position
	 * @param inPos
	 * @param inReqQty
	 * @param inMinQty
	 * @param inMaxQty
	 */
	private void sendPickRequestCommand(List<PosControllerInstr> inInstructions) {
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

		LOGGER.info("Light position: " + inLedSample.getPosition() + " color: " + inLedSample.getColor());
		INetworkDevice device = mDeviceManager.getDeviceByGuid(inControllerGuid);
		if (device instanceof AisleDeviceLogic) {
			AisleDeviceLogic aisleDevice = (AisleDeviceLogic) device;
			LedCmd cmd = aisleDevice.getLedCmdFor(getGuid(), inChannel, inLedSample.getPosition());
			if (cmd == null) {
				aisleDevice.addLedCmdFor(getGuid(), inChannel, inLedSample, inEffect);
			}
		}

		// Remember the last non-CHE LED controller we used.
		if (!(device instanceof CheDeviceLogic)) {
			mLastLedControllerGuid = inControllerGuid;
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
	 * Clear the LEDs for this CHE on this specified LED controller.
	 * @param inGuid
	 */
	private void ledControllerClearLeds() {
		// Clear the LEDs for the last location the CHE worked.
		INetworkDevice device = mDeviceManager.getDeviceByGuid(mLastLedControllerGuid);
		if (device instanceof AisleDeviceLogic) {
			AisleDeviceLogic aisleDevice = (AisleDeviceLogic) device;
			aisleDevice.clearLedCmdFor(getGuid());
		}
	}

	// --------------------------------------------------------------------------
	/**
	 * Clear the LEDs for this CHE on this specified LED controller.
	 * @param inGuid
	 */
	private void ledControllerClearLeds(final NetGuid inLedControllerId) {
		// Clear the LEDs for the last location the CHE worked.
		INetworkDevice device = mDeviceManager.getDeviceByGuid(inLedControllerId);
		if (device instanceof AisleDeviceLogic) {
			AisleDeviceLogic aisleDevice = (AisleDeviceLogic) device;
			aisleDevice.clearLedCmdFor(getGuid());
		}
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.flyweight.controller.INetworkDevice#start()
	 */
	public final void startDevice() {
		setState(mCheStateEnum);
		setLastAckId((byte) 0);
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inWorkInstructionCount
	 */
	public final void assignComputedWorkCount(final Integer inWorkInstructionCount) {
		// The back-end returned the work instruction count.
		if (inWorkInstructionCount > 0) {
			setState(CheStateEnum.LOCATION_SELECT);
		} else {
			setState(CheStateEnum.PICK_COMPLETE);
		}
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
			sendDisplayCommand(PICK_COMPLETE_MSG, EMPTY_MSG);
			setState(CheStateEnum.PICK_COMPLETE);
		} else {
			mActivePickWiList.clear();
			mAllPicksWiList.clear();
			mCompletedWiList.clear();
			mAllPicksWiList.addAll(inWorkItemList);
			doNextPick();
			setState(CheStateEnum.DO_PICK);
		}
	}

	// --------------------------------------------------------------------------
	/**
	 */
	public final void signalNetworkDown() {
		//sendDisplayCommand("NETWORK DOWN", "");
	}

	// --------------------------------------------------------------------------
	/**
	 */
	public final void signalNetworkUp() {
		//sendDisplayCommand("NETWORK UP", "");
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.flyweight.controller.INetworkDevice#commandReceived(java.lang.String)
	 */
	@Override
	public final void scanCommandReceived(String inCommandStr) {
		LOGGER.info("Remote command: " + inCommandStr);

		String scanPrefixStr = getScanPrefix(inCommandStr);
		String scanStr = getScanContents(inCommandStr, scanPrefixStr);

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
		// Send a command to clear the position, so the controller knows we've gotten the button press.
		clearOnePositionController(inButtonCommand.getPosNum());
		processButtonPress((int) inButtonCommand.getPosNum(), (int) inButtonCommand.getValue());
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

			case DO_PICK:
				if (wasSameState) {
					showActivePicks();
				}
				break;

			case PICK_COMPLETE:
				sendDisplayCommand(PICK_COMPLETE_MSG, EMPTY_MSG);
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

			default:
				break;
		}

		List<PosControllerInstr> instructions = new ArrayList<PosControllerInstr>();
		PosControllerInstr instruction = new PosControllerInstr(PosControllerInstr.POSITION_ALL,
			PosControllerInstr.ERROR_CODE_QTY,
			PosControllerInstr.ZERO_QTY,
			PosControllerInstr.ZERO_QTY,
			PosControllerInstr.BLINK_FREQ,
			PosControllerInstr.BLINK_DUTYCYCLE);
		instructions.add(instruction);
		sendPickRequestCommand(instructions);
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
		mContainersMap.clear();
		mContainerInSetup = "";
		mActivePickWiList.clear();
		mAllPicksWiList.clear();
		setState(CheStateEnum.IDLE);

		ledControllerClearLeds();
		clearAllPositionControllers();
	}

	// --------------------------------------------------------------------------
	/**
	 * The user scanned the SETUP command to start a new batch of containers for the CHE.
	 */
	private void setupChe() {
		LOGGER.info("Setup work");

		if (mCheStateEnum.equals(CheStateEnum.PICK_COMPLETE)) {
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
			setState(CheStateEnum.SHORT_PICK_CONFIRM);
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
				// Add it to the list of completed WIs.
				mCompletedWiList.add(wi);

				wi.setActualQuantity(mShortPickQty);
				wi.setPickerId(mUserId);
				wi.setCompleted(new Timestamp(System.currentTimeMillis()));
				wi.setStatusEnum(WorkInstructionStatusEnum.SHORT);
				mActivePickWiList.remove(wi);

				mDeviceManager.completeWi(getGuid().getHexStringNoPrefix(), getPersistentId(), wi);
				LOGGER.info("Pick shorted: " + wi);

				clearLedControllersForWi(wi);

				if (mActivePickWiList.size() > 0) {
					// If there's more active picks then show them.
					showActivePicks();
				} else {
					// There's no more active picks, so move to the next set.
					doNextPick();
				}
			}
		} else {
			// Just return to showing the active picks.
			showActivePicks();
		}
	}

	// --------------------------------------------------------------------------
	/**
	 */
	private void doNextPick() {
		LOGGER.info("Next pick");

		if (mActivePickWiList.size() > 0) {
			// There are still picks in the active list.
			showActivePicks();
		} else {
			if (selectNextActivePicks()) {
				showActivePicks();
			} else {
				processPickComplete();
			}
		}
	}

	// --------------------------------------------------------------------------
	/**
	 * Sort the WIs by their distance along the path.
	 */
	private class WiDistanceComparator implements Comparator<WorkInstruction> {

		public int compare(WorkInstruction inWi1, WorkInstruction inWi2) {
			if (inWi1 == null) {
				return -1;
			} else if (inWi2 == null) {
				return 1;
			} else {
				return inWi1.getPosAlongPath().compareTo(inWi2.getPosAlongPath());
			}
		}
	};

	// --------------------------------------------------------------------------
	/**
	 * Sort the WIs by their distance along the path.
	 */
	private class WiGroupSortComparator implements Comparator<WorkInstruction> {

		public int compare(WorkInstruction inWi1, WorkInstruction inWi2) {
			if (inWi1 == null) {
				return -1;
			} else if (inWi2 == null) {
				return 1;
			} else {
				return inWi1.getGroupAndSortCode().compareTo(inWi2.getGroupAndSortCode());
			}
		}
	};

	// --------------------------------------------------------------------------
	/**
	 */
	private boolean selectNextActivePicks() {
		boolean result = false;

		// Loop through each container to see if there is a WI for that container at the next location.
		// The "next location" is the first location we find for the next pick.
		String firstLocationId = null;
		String firstItemId = null;
		Collections.sort(mAllPicksWiList, new WiGroupSortComparator());
		for (WorkInstruction wi : mAllPicksWiList) {
			for (String containerId : mContainersMap.values()) {
				// If the WI is for this container then consider it.
				if (wi.getContainerId().equals(containerId)) {
					// If the WI is INPROGRESS or NEW then consider it.
					if ((wi.getStatusEnum().equals(WorkInstructionStatusEnum.NEW))
							|| (wi.getStatusEnum().equals(WorkInstructionStatusEnum.INPROGRESS))) {
						if ((firstLocationId == null) || (firstLocationId.equals(wi.getLocationId()))) {
							if ((firstItemId == null) || (firstItemId.equals(wi.getItemId()))) {
								firstLocationId = wi.getLocationId();
								firstItemId = wi.getItemId();
								wi.setStarted(new Timestamp(System.currentTimeMillis()));
								mActivePickWiList.add(wi);
								result = true;
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
		if (mLastLedControllerGuid != null) {
			ledControllerClearLeds();
		}

		ledControllerShowLeds(getGuid());

		mCompletedWiList.clear();
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
	 * Send to the LED controller the active picks for the work instruction that's active on the CHE now.
	 */
	private void showActivePicks() {
		if (mActivePickWiList.size() > 0) {
			// The first WI has the SKU and location info.
			WorkInstruction firstWi = mActivePickWiList.get(0);

			// Send the CHE a display command (any of the WIs has the info we need).
			if (getCheStateEnum() != CheStateEnum.DO_PICK) {
				setState(CheStateEnum.DO_PICK);
			}
			ledControllerClearLeds();
			sendDisplayWorkInstruction(firstWi.getPickInstruction(), firstWi.getDescription(), firstWi.getPlanQuantity());

			List<LedCmdGroup> ledCmdGroups = LedCmdGroupSerializer.deserializeLedCmdString(firstWi.getLedCmdStream());

			INetworkDevice lastLedController = null;
			for (Iterator iterator = ledCmdGroups.iterator(); iterator.hasNext();) {
				LedCmdGroup ledCmdGroup = (LedCmdGroup) iterator.next();

				INetworkDevice ledController = mRadioController.getNetworkDevice(new NetGuid(ledCmdGroup.getControllerId()));
				if (ledController != null) {

					Short startLedNum = ledCmdGroup.getPosNum();
					Short currLedNum = startLedNum;

					// Clear the last LED commands to this controller if the last controller was different.
					if ((lastLedController != null) && (!ledController.equals(lastLedController))) {
						ledControllerClearLeds();
						lastLedController = ledController;
					}

					for (LedSample ledSample : ledCmdGroup.getLedSampleList()) {

						ledSample.setPosition(currLedNum++);

						// Send the LED display command.
						ledControllerSetLed(ledController.getGuid(), ledCmdGroup.getChannelNum(), ledSample, EffectEnum.FLASH);

					}

					if ((ledController.getDeviceStateEnum() != null)
							&& (ledController.getDeviceStateEnum() == NetworkDeviceStateEnum.STARTED)) {
						ledControllerShowLeds(ledController.getGuid());
					}
				}
			}

			// Now create a light instruction for each position.
			List<PosControllerInstr> instructions = new ArrayList<PosControllerInstr>();
			for (WorkInstruction wi : mActivePickWiList) {
				for (Entry<String, String> mapEntry : mContainersMap.entrySet()) {
					if (mapEntry.getValue().equals(wi.getContainerId())) {
						PosControllerInstr instruction = new PosControllerInstr(Byte.valueOf(mapEntry.getKey()),
							byteValueForPositionDisplay(firstWi.getPlanQuantity()),
							byteValueForPositionDisplay(firstWi.getPlanMinQuantity()),
							byteValueForPositionDisplay(firstWi.getPlanMaxQuantity()),
							PosControllerInstr.BRIGHT_FREQ,
							PosControllerInstr.BRIGHT_DUTYCYCLE);
						instructions.add(instruction);
					}
				}
			}
			sendPickRequestCommand(instructions);

		}
		ledControllerShowLeds(getGuid());
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inWi
	 */
	private void clearLedControllersForWi(final WorkInstruction inWi) {

		List<LedCmdGroup> ledCmdGroups = LedCmdGroupSerializer.deserializeLedCmdString(inWi.getLedCmdStream());

		for (Iterator iterator = ledCmdGroups.iterator(); iterator.hasNext();) {
			LedCmdGroup ledCmdGroup = (LedCmdGroup) iterator.next();

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
			setLocationId(inScanStr);

			List<String> containerIdList = new ArrayList<String>(mContainersMap.values());
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
		if (CONTAINER_PREFIX.equals(inScanPrefixStr)) {

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

		// Add it to the list of completed WIs.
		mCompletedWiList.add(inWi);

		inWi.setActualQuantity(inQuantity);
		inWi.setPickerId(mUserId);
		inWi.setCompleted(new Timestamp(System.currentTimeMillis()));
		inWi.setStatusEnum(WorkInstructionStatusEnum.COMPLETE);

		mDeviceManager.completeWi(getGuid().getHexStringNoPrefix(), getPersistentId(), inWi);
		LOGGER.info("Pick completed: " + inWi);

		mActivePickWiList.remove(inWi);

		clearLedControllersForWi(inWi);

		if (mActivePickWiList.size() > 0) {
			// If there's more active picks then show them.
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
		sendPickRequestCommand(instructions);
	}
}
