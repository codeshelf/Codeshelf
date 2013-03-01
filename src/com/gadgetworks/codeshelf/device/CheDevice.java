/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2013, Jeffrey B. Williams, All rights reserved
 *  $Id: CheDevice.java,v 1.8 2013/03/01 21:23:25 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.device;

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
	private static final String		LOGIN						= "LOGIN";
	private static final String		SCAN_USERID					= "SCAN BADGE";
	private static final String		SCAN_LOCATION				= "SCAN LOC";
	private static final String		SCAN_CONTAINER				= "SCAN CNTR";

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

	public CheDevice(final NetGuid inGuid) {
		mGuid = inGuid;
		mCheStateEnum = CheStateEnum.IDLE;
	}

	@Override
	public final boolean doesMatch(NetGuid inGuid) {
		return ((mGuid != null) && (mGuid.equals(inGuid)));
	}

	// --------------------------------------------------------------------------
	/**
	 * Send a display message to the CHE's embedded control device.
	 * @param inMessage
	 */
	private void sendDisplayCommand(final String inMessage) {
		ICommand command = new CommandControlMessage(NetEndpoint.PRIMARY_ENDPOINT, inMessage);
		mController.sendCommand(command, mAddress, false);

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
		mCheStateEnum = CheStateEnum.IDLE;
		sendDisplayCommand(SCAN_USERID);
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inPrefixScanStr
	 * @param inScanStr
	 */
	private void idleStateScan(final String inScanPrefixStr, final String inScanStr) {

		if (USER_BARCODE_PREFIX.equals(inScanPrefixStr)) {
			mCheStateEnum = CheStateEnum.LOCATION_SETUP;
			LOGGER.info("User login: " + inScanStr);
			sendDisplayCommand(SCAN_LOCATION);
		} else {
			LOGGER.info("Not a user ID: " + inScanStr);
			sendDisplayCommand(SCAN_USERID);
		}
	}

	// --------------------------------------------------------------------------
	/**
	 * @param insScanPrefixStr
	 * @param inScanStr
	 */
	private void locationScan(final String inScanPrefixStr, String inScanStr) {
		if (LOCATION_BARCODE_PREFIX.equals(inScanPrefixStr)) {
			mCheStateEnum = CheStateEnum.CONTAINER_SELECT;
			LOGGER.info("Location: " + inScanStr);
			sendDisplayCommand(SCAN_CONTAINER);
		}
	}
}
