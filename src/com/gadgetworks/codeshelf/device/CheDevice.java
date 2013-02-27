/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2013, Jeffrey B. Williams, All rights reserved
 *  $Id: CheDevice.java,v 1.5 2013/02/27 01:17:02 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.device;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import com.gadgetworks.flyweight.command.NetAddress;
import com.gadgetworks.flyweight.command.NetMacAddress;
import com.gadgetworks.flyweight.controller.INetworkDevice;
import com.gadgetworks.flyweight.controller.NetworkDeviceStateEnum;

/**
 * @author jeffw
 *
 */
public class CheDevice implements INetworkDevice {

	private static final Logger		LOGGER	= LoggerFactory.getLogger(CheDevice.class);

	// MAC address.
	@Accessors(prefix = "m")
	@Getter
	private NetMacAddress			mMacAddress;

	// The CHE's net address.
	@Accessors(prefix = "m")
	@Getter
	@Setter
	private NetAddress				mNetAddress;

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

	public CheDevice(final NetMacAddress inMacAddress) {
		mMacAddress = inMacAddress;
		mCheStateEnum = CheStateEnum.IDLE;
	}

	@Override
	public final boolean doesMatch(NetMacAddress inMacAddress) {
		return ((mMacAddress != null) && (mMacAddress.equals(inMacAddress)));
	}

	@Override
	public final void commandReceived(String inCommandStr) {
		LOGGER.info("Remote command: " + inCommandStr);
	}
}
