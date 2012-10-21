/*******************************************************************************
 *  OmniBox
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: INetworkDevice.java,v 1.5 2012/10/21 02:02:18 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.controller;

import com.gadgetworks.codeshelf.command.CommandControlABC;

// --------------------------------------------------------------------------
/**
 *  @author jeffw
 */
public interface INetworkDevice {

	String	BEAN_ID				= "INetworkDevice";

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
	 * Sometimes a network device gets changes to its underlying data, but at the level of network controller
	 * we have no idea what/how that happens.  Instead we leave it up to the implementation to decide how it wants 
	 * to persist any changes.  (If at all.)
	 * N.B. The upper level may change the instance of the object as a side effect of using an ORM.  We need
	 * to make sure we have the latest instance, so we don't have stale info.
	 */
	//INetworkDevice persistChanges();

	// --------------------------------------------------------------------------
	/**
	 *  Called each time we receive a control command from one of the remotes.
	 *  @param inCommand	The command we received.
	 */
	void controlCommandReceived(CommandControlABC inCommandControl);

	// --------------------------------------------------------------------------
	/**
	 *  The user pressed a button on the remote.
	 *  @param inButtonNumberPressed
	 */
	void buttonCommandReceived(byte inButtonNumberPressed, byte inButtonFunction);

	// --------------------------------------------------------------------------
	/**
	 *  Return a string that represents the hardware type associated with this device.
	 *  @return
	 */
	String getHwDesc();

	// --------------------------------------------------------------------------
	/**
	 *  Return a string that represents the software version associated with this device.
	 *  @return
	 */
	String getSwRevision();

	// --------------------------------------------------------------------------
	/**
	 *  Add a new key-value-pair (KVP) matching criteria for the actor.  These KVPs are used to match the actor to parts.
	 *  @param inKey       The lookup key.
	 *  @param inValue     The value.
	 */
	void addKeyValuePair(String inKey, String inValue);

	// --------------------------------------------------------------------------
	/**
	 *  The actor descriptor query-response tells us how many KVPs the actor has.
	 *  @return	The number of KVPs the actor said it had when it responsed to the actor descriptor query.
	 */
	short getExpectedKvpCount();

	// --------------------------------------------------------------------------
	/**
	 *  We keep track of how many unique actor descriptors we've received from the remote actor.
	 *  @return	The number of KVPs stored on the actor.
	 */
	short getStoredKvpCount();

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

	// --------------------------------------------------------------------------
	/**
	 *  @param inKVPCount
	 */
	void setExpectedKvpCount(short inKvpCount);

}
