/*******************************************************************************
 *  OmniBox
 *  Copyright (c) 2005-2007, Jeffrey B. Williams, All rights reserved
 *  $Id: INetworkDevice.java,v 1.7 2013/03/03 23:27:21 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.flyweight.controller;

import com.gadgetworks.flyweight.command.NetAddress;
import com.gadgetworks.flyweight.command.NetGuid;

// --------------------------------------------------------------------------
/**
 *  @author jeffw
 */
public interface INetworkDevice {

	String	HW_VERSION_KEY		= "hw_version";
	String	SW_VERSION_KEY		= "sw_version";

	byte	PROTOCOL_VERSION_1	= 1;

	// --------------------------------------------------------------------------
	/**
	 *  Every device has a globally unique ID.
	 *  @return	The GUID for this device.
	 */
	NetGuid getGuid();
	
	// --------------------------------------------------------------------------
	/**
	 * Set the GUID for this device.
	 * @param inGuid
	 */
	// You can't set this - it's immutable for the object, so set it at construction.
	// void setGuid(NetGuid inGuid);

	// --------------------------------------------------------------------------
	/**
	 *  Get the network address assigned to this device's session.
	 *  @return	The network address
	 */
	NetAddress getAddress();

	// --------------------------------------------------------------------------
	/**
	 *  Set the network address for this device.
	 *  @param inNetAddr	The network address
	 */
	void setAddress(NetAddress inNetAddr);

	// --------------------------------------------------------------------------
	/**
	 *  Set the state for this device.
	 *  @param inSessionState	The device's session state.
	 */
	void setDeviceStateEnum(NetworkDeviceStateEnum inStatus);

	// --------------------------------------------------------------------------
	/**
	 *  Get the network state for this device.
	 *  @return	The device's network status.
	 */
	NetworkDeviceStateEnum getDeviceStateEnum();

	// --------------------------------------------------------------------------
	/**
	 *  Determine if this device's session matches a device GUID.
	 *  @param inGuid	The GUID
	 *  @return	True if there is a match.
	 */
	boolean doesMatch(NetGuid inGuid);

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
	void setLastContactTime(long inContactTime);

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
	 * We get this message when the device first starts on the network.
	 */
	void start();

	// --------------------------------------------------------------------------
	/**
	 *  The user pressed a button on the remote.
	 *  @param inButtonNumberPressed
	 */
	void commandReceived(String inCommandStr);
	
}
