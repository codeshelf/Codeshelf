/*******************************************************************************
 *  OmniBox
 *  Copyright (c) 2005-2007, Jeffrey B. Williams, All rights reserved
 *  $Id: INetworkDevice.java,v 1.1 2013/02/20 08:28:25 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.flyweight.controller;

import com.gadgetworks.flyweight.command.NetAddress;
import com.gadgetworks.flyweight.command.NetMacAddress;

// --------------------------------------------------------------------------
/**
 *  @author jeffw
 */
public interface INetworkDevice {

	String	HW_VERSION_KEY		= "hw_version";
	String	SW_VERSION_KEY		= "sw_version";

	byte	PROTOCOL_VERSION_1	= 1;
	int		UNIQUEID_BYTES		= 8;

	// --------------------------------------------------------------------------
	/**
	 *  Every device has a globally unique ID.
	 *  @return	The MacAddr for this device.
	 */
	NetMacAddress getMacAddress();

	// --------------------------------------------------------------------------
	/**
	 *  Get the network address assigned to this device's session.
	 *  @return	The network address
	 */
	NetAddress getNetAddress();

	// --------------------------------------------------------------------------
	/**
	 *  Set the network address for this device.
	 *  @param inNetAddr	The network address
	 */
	void setNetAddress(NetAddress inNetAddr);

	// --------------------------------------------------------------------------
	/**
	 *  Set the state for this device.
	 *  @param inSessionState	The device's session state.
	 */
	void setNetworkDeviceState(NetworkDeviceStateEnum inStatus);

	// --------------------------------------------------------------------------
	/**
	 *  Get the network state for this device.
	 *  @return	The device's network status.
	 */
	NetworkDeviceStateEnum getNetworkDeviceState();

	// --------------------------------------------------------------------------
	/**
	 *  Determine if this device's session matches a device MacAddr.
	 *  @param inMacAddrStr	The MacAddr
	 *  @return	True if there is a match.
	 */
	boolean doesMatch(String inMacAddrStr);

	// --------------------------------------------------------------------------
	/**
	 *  Get the time that we last heard from this device during this session.
	 *  @return	The last contact time.
	 */
	long getLastContactTime();

	// --------------------------------------------------------------------------
	/**
	 *  Set the time that we last heard from this device during this session.
	 *  @param inContactTime	The last contact time.
	 */
	void setLastContactTime(Long inContactTime);

	// --------------------------------------------------------------------------
	/**
	 *  Get the last battery level (0 - 100).
	 *  @return	The last battery level reading.
	 */
	short getLastBatteryLevel();

	// --------------------------------------------------------------------------
	/**
	 *  Set the last battery level reading (0 - 100).
	 *  @param inLastBatteryLevel	The last battery level reading.
	 */
	void setLastBatteryLevel(short inLastBatteryLevel);

	// --------------------------------------------------------------------------
	/**
	 *  The user pressed a button on the remote.
	 *  @param inButtonNumberPressed
	 */
	void commandReceived(String inCommandStr);

	// --------------------------------------------------------------------------
	/**
	 *  @param inDeviceType
	 */
	void setDeviceType(short inDeviceType);

	// --------------------------------------------------------------------------
	/**
	 *  @param inDeviceDescription
	 */
	void setDesc(String inDeviceDescription);
}
