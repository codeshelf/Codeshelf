/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2013, Jeffrey B. Williams, All rights reserved
 *  $Id: CheDevice.java,v 1.9 2013/03/02 02:22:30 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.device;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gadgetworks.flyweight.command.CommandControlMessage;
import com.gadgetworks.flyweight.command.ICommand;
import com.gadgetworks.flyweight.command.NetAddress;
import com.gadgetworks.flyweight.command.NetEndpoint;
import com.gadgetworks.flyweight.command.NetGuid;
import com.gadgetworks.flyweight.controller.IController;
import com.gadgetworks.flyweight.controller.INetworkDevice;
import com.gadgetworks.flyweight.controller.NetworkDeviceStateEnum;

/**
 * @author jeffw
 *
 */
public class CheDevice implements INetworkDevice {

	private static final Logger		LOGGER						= LoggerFactory.getLogger(CheDevice.class);

	private static final String		BARCODE_DELIMITER			= "%";
	private static final String		COMMAND_BARCODE_PREFIX		= "X%";
	private static final String		USER_BARCODE_PREFIX			= "U%";
	private static final String		CONTAINER_BARCODE_PREFIX	= "O%";
	private static final String		LOCATION_BARCODE_PREFIX		= "L%";
	private static final String		ITEMID_BARCODE_PREFIX		= "I%";

	// These are the message strings we send to the remote CHE.
	// Currently, these cannot be longer than 10 characters.
	private static final String		EMPTY_MSG					= "";
	private static final String		ERROR_MSG					= "ERROR";
	private static final String		SCAN_USERID_MSG				= "SCAN BADGE";
	private static final String		SCAN_LOCATION_MSG			= "SCAN LOC";
	private static final String		SCAN_CONTAINER_MSG			= "SCAN CNTR";
	private static final String		SELECT_POSITION_MSG			= "SELECT POS";

	private static final String		LOGOUT_COMMAND				= "LOGOUT";

	// MAC address.
	@Accessors(prefix = "m")
	@Getter
	private NetGuid					mGuid;

	// The CHE's net address.
	@Accessors(prefix = "m")
	@Getter
	@Setter
	private NetAddress				mAddress;

	// The network device state.
	@Accessors(prefix = "m")
	@Getter
	@Setter
	private NetworkDeviceStateEnum	mDeviceStateEnum;

	// The CHE's current state.
	@Accessors(prefix = "m")
	@Getter
	@Setter
	private CheStateEnum			mCheStateEnum;

	// The last known battery level.
	@Accessors(prefix = "m")
	@Getter
	@Setter
	private short					mLastBatteryLevel;

	// The last time we had contact.
	@Accessors(prefix = "m")
	@Getter
	@Setter
	private long					mLastContactTime;

	// The controller for this device..
	@Accessors(prefix = "m")
	@Getter
	@Setter
	private IController				mController;

	// The CHE's current location.
	@Accessors(prefix = "m")
	@Getter
	@Setter
	private String					mLocation;

	// The CHE's current location.
	@Accessors(prefix = "m")
	private List<String>			mContainerIds;

	public CheDevice(final NetGuid inGuid) {
		mGuid = inGuid;
		mCheStateEnum = CheStateEnum.IDLE;
		mContainerIds = new ArrayList<String>();
	}

	@Override
	public final boolean doesMatch(NetGuid inGuid) {
		return ((mGuid != null) && (mGuid.equals(inGuid)));
	}

	// --------------------------------------------------------------------------
	/**
	 * Send a display message to the CHE's embedded control device.
	 * @param inLine1Message
	 */
	private void sendDisplayCommand(final String inLine1Message, final String inLine2Message) {
		LOGGER.info("Display message: " + inLine1Message);
		ICommand command = new CommandControlMessage(NetEndpoint.PRIMARY_ENDPOINT, inLine1Message, inLine2Message);
		mController.sendCommand(command, mAddress, false);

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
		if (inCommandStr.startsWith(COMMAND_BARCODE_PREFIX)) {
			processCommandScan(scanStr);
		} else {
			switch (mCheStateEnum) {
				case IDLE:
					idleStateScan(scanPrefixStr, scanStr);
					break;

				case LOCATION_SETUP:
					locationScan(scanPrefixStr, scanStr);

				case CONTAINER_SELECT:
					containerSelectScan(scanPrefixStr, scanStr);
					break;

				default:
					break;
			}
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
		mContainerIds.clear();
		setState(CheStateEnum.IDLE);
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

			default:
				break;
		}
	}

	// --------------------------------------------------------------------------
	/**
	 */
	private void setStateError(final CheStateEnum inCheState) {
		mCheStateEnum = inCheState;

		switch (inCheState) {
			case IDLE:
				sendDisplayCommand(SCAN_USERID_MSG, ERROR_MSG);
				break;

			case LOCATION_SETUP:
				sendDisplayCommand(SCAN_LOCATION_MSG, ERROR_MSG);
				break;

			case CONTAINER_SELECT:
				sendDisplayCommand(SCAN_CONTAINER_MSG, ERROR_MSG);
				break;

			case CONTAINER_POSITION:
				sendDisplayCommand(SELECT_POSITION_MSG, ERROR_MSG);
				break;

			default:
				break;
		}
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inPrefixScanStr
	 * @param inScanStr
	 */
	private void idleStateScan(final String inScanPrefixStr, final String inScanStr) {

		if (USER_BARCODE_PREFIX.equals(inScanPrefixStr)) {
			setState(CheStateEnum.LOCATION_SETUP);
		} else {
			LOGGER.info("Not a user ID: " + inScanStr);
			setStateError(CheStateEnum.IDLE);
		}
	}

	// --------------------------------------------------------------------------
	/**
	 * @param insScanPrefixStr
	 * @param inScanStr
	 */
	private void locationScan(final String inScanPrefixStr, String inScanStr) {
		if (LOCATION_BARCODE_PREFIX.equals(inScanPrefixStr)) {
			setLocation(inScanStr);
			setState(CheStateEnum.CONTAINER_SELECT);
		} else {
			LOGGER.info("Not a location ID: " + inScanStr);
			setStateError(CheStateEnum.LOCATION_SETUP);
		}
	}

	// --------------------------------------------------------------------------
	/**
	 * @param insScanPrefixStr
	 * @param inScanStr
	 */
	private void containerSelectScan(final String inScanPrefixStr, String inScanStr) {
		if (CONTAINER_BARCODE_PREFIX.equals(inScanPrefixStr)) {
			mContainerIds.add(inScanStr);
			setState(CheStateEnum.CONTAINER_POSITION);
		} else {
			LOGGER.info("Not a container ID: " + inScanStr);
			setStateError(CheStateEnum.CONTAINER_SELECT);
		}
	}
}
