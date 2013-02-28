/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2013, Jeffrey B. Williams, All rights reserved
 *  $Id: CheDevice.java,v 1.7 2013/02/28 06:24:52 jeffw Exp $
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
	private static final String		CONTAINER_BARCODE_PREFIX	= "C%";
	private static final String		LOCATION_BARCODE_PREFIX		= "L%";
	private static final String		ITEMID_BARCODE_PREFIX		= "I%";

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

		if (inCommandStr.equals(COMMAND_BARCODE_PREFIX + "LOGOUT")) {
			processLogout();
		} else {
			switch (mCheStateEnum) {
				case IDLE:
					idleStateScan(scanPrefixStr, scanStr);
					break;

				default:
					break;
			}
		}
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
	/**
	 */
	private void processLogout() {
		LOGGER.info("User logut");
		mCheStateEnum = CheStateEnum.IDLE;
		sendDisplayCommand("SCAN USERID");
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
			sendDisplayCommand("SCAN LOCATION");
		}
	}
}
