/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2013, Jeffrey B. Williams, All rights reserved
 *  $Id: CheDeviceLogic.java,v 1.6 2013/07/17 05:48:13 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.device;

import java.sql.Timestamp;
import java.util.ArrayList;
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

import com.gadgetworks.codeshelf.model.WorkInstructionStatusEnum;
import com.gadgetworks.codeshelf.model.domain.WorkInstruction;
import com.gadgetworks.flyweight.command.ColorEnum;
import com.gadgetworks.flyweight.command.CommandControlLight;
import com.gadgetworks.flyweight.command.CommandControlMessage;
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
public class CheDeviceLogic extends AisleDeviceLogic {

	private static final Logger		LOGGER					= LoggerFactory.getLogger(CheDeviceLogic.class);

	private static final String		BARCODE_DELIMITER		= "%";
	private static final String		COMMAND_PREFIX			= "X%";
	private static final String		USER_PREFIX				= "U%";
	private static final String		CONTAINER_PREFIX		= "O%";
	private static final String		LOCATION_PREFIX			= "L%";
	private static final String		ITEMID_PREFIX			= "I%";
	private static final String		POSITION_PREFIX			= "B%";

	// These are the message strings we send to the remote CHE.
	// Currently, these cannot be longer than 10 characters.
	private static final String		EMPTY_MSG				= "                ";
	private static final String		INVALID_SCAN_MSG		= "INVALID         ";
	private static final String		SCAN_USERID_MSG			= "SCAN BADGE      ";
	private static final String		SCAN_LOCATION_MSG		= "SCAN LOCATION   ";
	private static final String		SCAN_CONTAINER_MSG		= "SCAN CONTAINER  ";
	private static final String		SELECT_POSITION_MSG		= "SELECT POSITION ";
	private static final String		SHORT_PICK_CONFIRM_MSG	= "CONFIRM SHORT   ";
	private static final String		PICK_COMPLETE_MSG		= "PICK COMPLETE   ";
	private static final String		YES_NO_MSG				= "YES OR NO       ";

	private static final String		STARTWORK_COMMAND		= "START";
	private static final String		SETUP_COMMAND			= "SETUP";
	private static final String		SHORT_COMMAND			= "SHORT";
	private static final String		LOGOUT_COMMAND			= "LOGOUT";
	private static final String		RESUME_COMMAND			= "RESUME";
	private static final String		YES_COMMAND				= "YES";
	private static final String		NO_COMMAND				= "NO";

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

	// --------------------------------------------------------------------------
	/**
	 * Send a display message to the CHE's embedded control device.
	 * @param inLine1Message
	 */
	private void sendDisplayCommand(final String inLine1Message, final String inLine2Message) {
		String msg1 = String.format("%-16s", inLine1Message);
		String msg2 = String.format("%-16s", inLine2Message);
		LOGGER.info("Display message: " + msg1 + " -- " + msg2);
		ICommand command = new CommandControlMessage(NetEndpoint.PRIMARY_ENDPOINT, msg1, msg2);
		mRadioController.sendCommand(command, getAddress(), true);
	}

	// --------------------------------------------------------------------------
	/**
	 * Send a light command to the CHE to light a position
	 * @param inPosition
	 */
	private void sendCheLightCommand(final Short inChanel, final EffectEnum inEffect, final List<LedSample> inLedSamples) {
		ICommand command = new CommandControlLight(NetEndpoint.PRIMARY_ENDPOINT, inChanel, inEffect, inLedSamples);
		mRadioController.sendCommand(command, getAddress(), true);
	}

	// --------------------------------------------------------------------------
	/**
	 * Send a light command for this CHE on the specified LED controller.
	 * @param inPosition
	 */
	private void ledControllerSetLed(final NetGuid inControllerGuid,
		final Short inChannel,
		final Short inPosition,
		final ColorEnum inColor,
		final EffectEnum inEffect) {

		LOGGER.info("Light position: " + inPosition + " color: " + inColor);
		INetworkDevice device = mDeviceManager.getDeviceByGuid(inControllerGuid);
		if (device instanceof AisleDeviceLogic) {
			AisleDeviceLogic aisleDevice = (AisleDeviceLogic) device;
			LedCmd cmd = aisleDevice.getLedCmdFor(getGuid(), inChannel, inPosition);
			if (cmd == null) {
				aisleDevice.addLedCmdFor(getGuid(), inChannel, inPosition, inColor, inEffect);
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
		// Clear the CHE's own LEDs.
		clearLedCmdFor(getGuid());

		// Clear the LEDs for the last location the CHE worked.
		INetworkDevice device = mDeviceManager.getDeviceByGuid(mLastLedControllerGuid);
		if (device instanceof AisleDeviceLogic) {
			AisleDeviceLogic aisleDevice = (AisleDeviceLogic) device;
			aisleDevice.clearLedCmdFor(getGuid());
		}
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.flyweight.controller.INetworkDevice#start()
	 */
	public final void start() {
		//setState(CheStateEnum.IDLE);
		setState(mCheStateEnum);
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
		mAllPicksWiList.clear();
		mAllPicksWiList.addAll(inWorkItemList);
		for (WorkInstruction wi : inWorkItemList) {
			LOGGER.info("WI: Loc: " + wi.getLocationId() + " SKU: " + wi.getItemId());
		}
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.flyweight.controller.INetworkDevice#commandReceived(java.lang.String)
	 */
	@Override
	public final void commandReceived(String inCommandStr) {
		LOGGER.info("Remote command: " + inCommandStr);

		String scanPrefixStr = "";
		String scanStr = inCommandStr;
		int prefixCharPos = inCommandStr.indexOf(BARCODE_DELIMITER);
		if (prefixCharPos > 0) {
			scanPrefixStr = inCommandStr.substring(0, prefixCharPos + 1);
			scanStr = inCommandStr.substring(prefixCharPos + 1);
		}

		// A command scan is always an option at any state.
		if (inCommandStr.startsWith(COMMAND_PREFIX)) {
			processCommandScan(scanStr);
		} else if (inCommandStr.startsWith(POSITION_PREFIX)) {
			processButtonScan(scanStr);
		} else {
			switch (mCheStateEnum) {
				case IDLE:
					processIdleStateScan(scanPrefixStr, scanStr);
					break;

				case LOCATION_SETUP:
					processLocationScan(scanPrefixStr, scanStr);
					break;

				case CONTAINER_SELECT:
					processContainerSelectScan(scanPrefixStr, scanStr);
					break;

				case CONTAINER_POSITION:
					// The only thing that makes sense in this mode is a button press (or a logout covered above).
					setStateWithInvalid(mCheStateEnum);
					break;

				default:
					break;
			}
		}
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

			case LOCATION_SETUP:
				sendDisplayCommand(SCAN_LOCATION_MSG, EMPTY_MSG);
				break;

			case CONTAINER_SELECT:
				sendDisplayCommand(SCAN_CONTAINER_MSG, EMPTY_MSG);
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
	private void setStateWithInvalid(final CheStateEnum inCheState) {
		mCheStateEnum = inCheState;

		switch (inCheState) {
			case IDLE:
				sendDisplayCommand(SCAN_USERID_MSG, INVALID_SCAN_MSG);
				break;

			case LOCATION_SETUP:
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

		List<LedSample> sampleList = new ArrayList<LedSample>();
		LedSample sample = new LedSample(CommandControlLight.POSITION_ALL, ColorEnum.RED);
		sampleList.add(sample);
		sendCheLightCommand(CommandControlLight.CHANNEL_ALL, EffectEnum.ERROR, sampleList);
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
				setupWork();
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
	}

	// --------------------------------------------------------------------------
	/**
	 * The user scanned the SETUP command to start a new batch of containers for the CHE.
	 */
	private void setupWork() {
		LOGGER.info("Setup work");

		if (mCheStateEnum.equals(CheStateEnum.PICK_COMPLETE)) {
			mContainersMap.clear();
			mContainerInSetup = "";
			setState(CheStateEnum.LOCATION_SETUP);
		} else {
			// Stay in the same state - the scan made no sense.
			setStateWithInvalid(mCheStateEnum);
		}
	}

	// --------------------------------------------------------------------------
	/**
	 * The user scanned the START command to start work on the WIs for this CHE.
	 */
	private void startWork() {
		LOGGER.info("Start work");

		if ((mContainersMap.values().size() > 0) && (mCheStateEnum.equals(CheStateEnum.CONTAINER_SELECT))) {
			mContainerInSetup = "";
			if (getCheStateEnum() != CheStateEnum.DO_PICK) {
				setState(CheStateEnum.DO_PICK);
			}
			doNextPick();
		} else {
			// Stay in the same state - the scan made no sense.
			setStateWithInvalid(mCheStateEnum);
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
			setStateWithInvalid(mCheStateEnum);
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
				setStateWithInvalid(mCheStateEnum);
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
			WorkInstruction wi = mActivePickWiList.remove(0);
			if (wi != null) {
				// Add it to the list of completed WIs.
				mCompletedWiList.add(wi);

				wi.setActualQuantity(0);
				wi.setPickerId(mUserId);
				wi.setCompleted(new Timestamp(System.currentTimeMillis()));
				wi.setStatusEnum(WorkInstructionStatusEnum.SHORT);

				mDeviceManager.completeWi(getGuid().getHexStringNoPrefix(), getPersistentId(), wi);
				LOGGER.info("Pick shorted: " + wi);

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
		} else if (mAllPicksWiList.size() == 0) {
			// There are no more WIs, so the pick is complete.

			// Clear the existing LEDs.
			if (mLastLedControllerGuid != null) {
				ledControllerClearLeds();
			}

			// Blink the complete and incomplete containers.
			for (WorkInstruction wi : mCompletedWiList) {
				Short position = 0;
				for (Entry<String, String> mapEntry : mContainersMap.entrySet()) {
					if (mapEntry.getValue().equals(wi.getContainerId())) {
						// The actual position is zero-based.
						position = (short) (Short.valueOf(mapEntry.getKey()) - 1);
					}
				}

				if (!wi.getStatusEnum().equals(WorkInstructionStatusEnum.COMPLETE)) {
					ledControllerSetLed(getGuid(), CommandControlLight.CHANNEL1, position, ColorEnum.RED, EffectEnum.FLASH);
				} else {
					ledControllerSetLed(getGuid(), CommandControlLight.CHANNEL1, position, wi.getLedColorEnum(), EffectEnum.FLASH);
				}
			}
			ledControllerShowLeds(getGuid());

			setState(CheStateEnum.PICK_COMPLETE);
		} else {
			// Loop through each container to see if there is a WI for that container at the next location.
			// The "next location" is the first location we find for the next pick.
			String firstLocationId = null;
			String firstItemId = null;
			for (String containerId : mContainersMap.values()) {
				Iterator<WorkInstruction> wiIter = mAllPicksWiList.iterator();
				while (wiIter.hasNext()) {
					WorkInstruction wi = wiIter.next();
					if (wi.getContainerId().equals(containerId)) {
						if ((firstLocationId == null) || (firstLocationId.equals(wi.getLocationId()))) {
							if ((firstItemId == null) || (firstItemId.equals(wi.getItemId()))) {
								firstLocationId = wi.getLocationId();
								firstItemId = wi.getItemId();
								wi.setStarted(new Timestamp(System.currentTimeMillis()));
								mActivePickWiList.add(wi);
								wiIter.remove();
							}
						}
					} else {
						break;
					}
				}
			}
			showActivePicks();
		}
	}

	// --------------------------------------------------------------------------
	/**
	 * Send to the LED controller the active picks for the work instruction that's active on the CHE now.
	 */
	private void showActivePicks() {
		// The first WI has the SKU and location info.
		WorkInstruction firstWi = mActivePickWiList.get(0);

		// Send the CHE a display command (any of the WIs has the info we need).
		if (getCheStateEnum() != CheStateEnum.DO_PICK) {
			setState(CheStateEnum.DO_PICK);
		}
		sendDisplayCommand(firstWi.getPickInstruction() + "  " + firstWi.getItemId(), firstWi.getDescription());

		INetworkDevice ledController = mRadioController.getNetworkDevice(new NetGuid(firstWi.getLedControllerId()));
		if (ledController != null) {

			Short startLedNum = firstWi.getLedFirstPos();
			Short endLedNum = firstWi.getLedLastPos();

			// Put them into increasing order rather than order along the path.
			// (It might be reversed because the travel direction is opposite the LED strip direction.)
			if (startLedNum > endLedNum) {
				Short temp = endLedNum;
				endLedNum = startLedNum;
				startLedNum = temp;
			}

			// Clear the last LED if there was one.
			if (mLastLedControllerGuid != null) {
				ledControllerClearLeds();
			}

			// Send the location display command.
			for (short position = startLedNum; position <= endLedNum; position++) {
				ledControllerSetLed(ledController.getGuid(),
					firstWi.getLedChannel(),
					position,
					firstWi.getLedColorEnum(),
					EffectEnum.FLASH);
			}
			if ((ledController.getDeviceStateEnum() != null)
					&& (ledController.getDeviceStateEnum() == NetworkDeviceStateEnum.STARTED)) {
				ledControllerShowLeds(ledController.getGuid());
			}
		}

		// Now create a light instruction for each position.
		for (WorkInstruction wi : mActivePickWiList) {
			for (Entry<String, String> mapEntry : mContainersMap.entrySet()) {
				if (mapEntry.getValue().equals(wi.getContainerId())) {
					ledControllerSetLed(getGuid(), CommandControlLight.CHANNEL1,
					// The LED positions are zero-based.
						(short) (Short.valueOf(mapEntry.getKey()) - 1),
						wi.getLedColorEnum(),
						EffectEnum.FLASH);
				}
			}
		}
		ledControllerShowLeds(getGuid());
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inPrefixScanStr
	 * @param inScanStr
	 */
	private void processIdleStateScan(final String inScanPrefixStr, final String inScanStr) {

		if (USER_PREFIX.equals(inScanPrefixStr)) {
			mUserId = inScanStr;
			setState(CheStateEnum.LOCATION_SETUP);
		} else {
			LOGGER.info("Not a user ID: " + inScanStr);
			setStateWithInvalid(CheStateEnum.IDLE);
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
			setState(CheStateEnum.CONTAINER_SELECT);
		} else {
			LOGGER.info("Not a location ID: " + inScanStr);
			setStateWithInvalid(CheStateEnum.LOCATION_SETUP);
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
			setState(CheStateEnum.CONTAINER_POSITION);
		} else {
			LOGGER.info("Not a container ID: " + inScanStr);
			setStateWithInvalid(CheStateEnum.CONTAINER_SELECT);
		}
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inButtonStr
	 */
	private void processButtonScan(String inScanStr) {
		if (mCheStateEnum.equals(CheStateEnum.CONTAINER_POSITION)) {
			setState(CheStateEnum.CONTAINER_SELECT);
			if (mContainersMap.get(inScanStr) == null) {
				mContainersMap.put(inScanStr, mContainerInSetup);
				mContainerInSetup = "";
				List<String> containerIdList = new ArrayList<String>(mContainersMap.values());
				mDeviceManager.requestCheWork(getGuid().getHexStringNoPrefix(), getPersistentId(), mLocationId, containerIdList);
			} else {
				LOGGER.info("Position in use: " + inScanStr);
				setStateWithInvalid(CheStateEnum.CONTAINER_POSITION);
			}
		} else if (mCheStateEnum.equals(CheStateEnum.DO_PICK)) {
			// Complete the active WI at the selected position.
			String containerId = mContainersMap.get(inScanStr);
			if (containerId != null) {
				Iterator<WorkInstruction> wiIter = mActivePickWiList.iterator();
				while (wiIter.hasNext()) {
					WorkInstruction wi = wiIter.next();
					if (wi.getContainerId().equals(containerId)) {

						// Add it to the list of completed WIs.
						mCompletedWiList.add(wi);

						// HACK HACK HACK
						// StitchFix is the first client and they only pick one item - ever.
						// When we have h/w that picks more than one item we'll address this.
						wi.setActualQuantity(1);
						wi.setPickerId(mUserId);
						wi.setCompleted(new Timestamp(System.currentTimeMillis()));
						wi.setStatusEnum(WorkInstructionStatusEnum.COMPLETE);

						mDeviceManager.completeWi(getGuid().getHexStringNoPrefix(), getPersistentId(), wi);
						LOGGER.info("Pick completed: " + wi);
						wiIter.remove();
					}
				}

				if (mActivePickWiList.size() > 0) {
					// If there's more active picks then show them.
					showActivePicks();
				} else {
					// There's no more active picks, so move to the next set.
					doNextPick();
				}
			} else {
				setStateWithInvalid(mCheStateEnum);
			}
		} else {
			// Random button press - leave the state alone.
			LOGGER.info("Random button press: " + inScanStr);
			setStateWithInvalid(mCheStateEnum);
		}
	}
}
