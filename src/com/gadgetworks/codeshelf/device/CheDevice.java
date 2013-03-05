/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2013, Jeffrey B. Williams, All rights reserved
 *  $Id: CheDevice.java,v 1.15 2013/03/05 20:45:11 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.device;

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

import com.gadgetworks.flyweight.command.ColorEnum;
import com.gadgetworks.flyweight.command.CommandControlLight;
import com.gadgetworks.flyweight.command.CommandControlMessage;
import com.gadgetworks.flyweight.command.ICommand;
import com.gadgetworks.flyweight.command.NetEndpoint;
import com.gadgetworks.flyweight.command.NetGuid;
import com.gadgetworks.flyweight.controller.IRadioController;

/**
 * @author jeffw
 *
 */
public class CheDevice extends DeviceABC {

	private static final Logger				LOGGER				= LoggerFactory.getLogger(CheDevice.class);

	private static final String				BARCODE_DELIMITER	= "%";
	private static final String				COMMAND_PREFIX		= "X%";
	private static final String				USER_PREFIX			= "U%";
	private static final String				CONTAINER_PREFIX	= "O%";
	private static final String				LOCATION_PREFIX		= "L%";
	private static final String				ITEMID_PREFIX		= "I%";
	private static final String				POSITION_PREFIX		= "B%";

	// These are the message strings we send to the remote CHE.
	// Currently, these cannot be longer than 10 characters.
	private static final String				EMPTY_MSG			= "";
	private static final String				INVALID_SCAN_MSG	= "INVALID";
	private static final String				SCAN_USERID_MSG		= "SCAN BADGE";
	private static final String				SCAN_LOCATION_MSG	= "SCAN LOC";
	private static final String				SCAN_CONTAINER_MSG	= "SCAN CNTR";
	private static final String				SELECT_POSITION_MSG	= "SELECT POS";
	private static final String				PICK_COMPLETE_MSG	= "PICK CMPLT";

	private static final String				STARTWORK_COMMAND	= "START";
	private static final String				SETUP_COMMAND		= "SETUP";
	private static final String				SHORT_COMMAND		= "SHORT";
	private static final String				LOGOUT_COMMAND		= "LOGOUT";
	private static final String				RESUME_COMMAND		= "RESUME";
	private static final String				YES_COMMAND			= "YES";
	private static final String				NO_COMMAND			= "NO";

	// The CHE's current state.
	@Accessors(prefix = "m")
	@Getter
	@Setter
	private CheStateEnum					mCheStateEnum;

	// The CHE's current location.
	@Accessors(prefix = "m")
	@Getter
	@Setter
	private String							mLocation;

	// The CHE's container map.
	private String							mContainerInSetup;

	// The CHE's container map.
	private Map<String, String>				mContainersMap;

	// All WIs for all containers on the CHE.
	private List<DeployedWorkInstruction>	mAllPicksWiList;

	// The active pick WIs.
	private List<DeployedWorkInstruction>	mActivePickWiList;

	public CheDevice(final UUID inPersistentId, final NetGuid inGuid, final ICsDeviceManager inDeviceManager, final IRadioController inRadioController) {
		super(inPersistentId, inGuid, inDeviceManager, inRadioController);

		mCheStateEnum = CheStateEnum.IDLE;
		mContainersMap = new HashMap<String, String>();
		mAllPicksWiList = new ArrayList<DeployedWorkInstruction>();
		mActivePickWiList = new ArrayList<DeployedWorkInstruction>();
	}

	// --------------------------------------------------------------------------
	/**
	 * Send a display message to the CHE's embedded control device.
	 * @param inLine1Message
	 */
	private void sendDisplayCommand(final String inLine1Message, final String inLine2Message) {
		LOGGER.info("Display message: " + inLine1Message);
		ICommand command = new CommandControlMessage(NetEndpoint.PRIMARY_ENDPOINT, inLine1Message, inLine2Message);
		mRadioController.sendCommand(command, getAddress(), false);
	}

	// --------------------------------------------------------------------------
	/**
	 * Send a light command to the CHE to light a position
	 * @param inPosition
	 */
	private void sendLightCommand(final Short inPosition, final ColorEnum inColor) {
		LOGGER.info("Light position: " + inPosition);
		ICommand command = new CommandControlLight(NetEndpoint.PRIMARY_ENDPOINT, CommandControlLight.CHANNEL1, inPosition, inColor, CommandControlLight.EFFECT_FLASH);
		mRadioController.sendCommand(command, getAddress(), false);
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.flyweight.controller.INetworkDevice#start()
	 */
	@Override
	public final void start() {
		mCheStateEnum = CheStateEnum.IDLE;
		sendDisplayCommand(SCAN_USERID_MSG, EMPTY_MSG);
	}

	// --------------------------------------------------------------------------
	/**
	 * Give the CHE the work it needs to do for a container.
	 * This is recomputed at the server for ALL containers on the CHE and returned in work-order.
	 * Whatever the CHE thought it needed to do before is now invalid and is replaced by what we send here.
	 * @param inContainerId
	 * @param inWorkItemList
	 */
	public final void assignWork(final List<DeployedWorkInstruction> inWorkItemList) {
		mAllPicksWiList.clear();
		mAllPicksWiList.addAll(inWorkItemList);
		for (DeployedWorkInstruction wi : inWorkItemList) {
			LOGGER.info("WI: Loc: " + wi.getLocation() + " SKU: " + wi.getSkuId() + " cmd: " + wi.getAisleControllerCmd());
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

			case PICK_COMPLETE:
				sendDisplayCommand(PICK_COMPLETE_MSG, EMPTY_MSG);
				break;

			default:
				break;
		}
	}

	// --------------------------------------------------------------------------
	/**
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

		sendLightCommand(CommandControlLight.POSITION_ALL, ColorEnum.RED);
	}

	// --------------------------------------------------------------------------
	/**
	 */
	private void processCommandScan(final String inScanStr) {

		switch (inScanStr) {
			case LOGOUT_COMMAND:
				logout();
				break;

			case STARTWORK_COMMAND:
				startWork();
				break;

			case SETUP_COMMAND:
				setupWork();
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
			setState(CheStateEnum.DO_PICK);
			doNextPick();
		} else {
			// Stay in the same state - the scan made no sense.
			setStateWithInvalid(mCheStateEnum);
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
			setState(CheStateEnum.PICK_COMPLETE);
		} else {
			// Loop through each container to see if there is a WI for that container at the next location.
			// The "next location" is the first location we find for the next pick.
			String firstLocation = null;
			for (String containerId : mContainersMap.values()) {
				Iterator<DeployedWorkInstruction> wiIter = mAllPicksWiList.iterator();
				while (wiIter.hasNext()) {
					DeployedWorkInstruction wi = wiIter.next();
					if (((firstLocation == null) || (firstLocation.equals(wi.getLocation()))) && (wi.getContainerId().equals(containerId))) {
						firstLocation = wi.getLocation();
						mActivePickWiList.add(wi);
						wiIter.remove();
					} else {
						break;
					}
				}
			}
			showActivePicks();
		}
	}

	private void showActivePicks() {
		// The first WI has the SKU and location info.
		DeployedWorkInstruction firstWi = mActivePickWiList.get(0);

		// Now create a light instruction for each position.
		sendDisplayCommand(firstWi.getLocation(), firstWi.getSkuId());
		for (DeployedWorkInstruction wi : mActivePickWiList) {
			for (Entry<String, String> mapEntry : mContainersMap.entrySet()) {
				if (mapEntry.getValue().equals(wi.getContainerId())) {
					sendLightCommand(Short.valueOf(mapEntry.getKey()), wi.getColor());
				}
			}
		}
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inPrefixScanStr
	 * @param inScanStr
	 */
	private void processIdleStateScan(final String inScanPrefixStr, final String inScanStr) {

		if (USER_PREFIX.equals(inScanPrefixStr)) {
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
			setLocation(inScanStr);
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
				mDeviceManager.requestCheWork(getGuid().getHexStringNoPrefix(), getPersistentId(), mLocation, containerIdList);
			} else {
				LOGGER.info("Position in use: " + inScanStr);
				setStateWithInvalid(CheStateEnum.CONTAINER_POSITION);
			}
		} else if (mCheStateEnum.equals(CheStateEnum.DO_PICK)) {
			// Complete the active WI at the selected position.
			String containerId = mContainersMap.get(inScanStr);
			if (containerId != null) {
				Iterator<DeployedWorkInstruction> wiIter = mActivePickWiList.iterator();
				while (wiIter.hasNext()) {
					DeployedWorkInstruction wi = wiIter.next();
					if (wi.getContainerId().equals(containerId)) {
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
