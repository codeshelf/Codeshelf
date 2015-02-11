/*******************************************************************************
 *  FlyWeightController
 *  Copyright (c) 2005-2008, Jeffrey B. Williams, All rights reserved
 *  $Id: IRadioControllerEventListener.java,v 1.1 2013/03/03 23:27:21 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.flyweight.controller;

import com.gadgetworks.flyweight.command.NetGuid;


// --------------------------------------------------------------------------
/**
 *  @author jeffw
 */
public interface IRadioControllerEventListener {

	// --------------------------------------------------------------------------
	/**
	 *  Called when the controller gets an association request from a network device
	 *  If the device is one that we're managing then return true, otherwise return false.
	 *  The listener needs to maintain a persistent list of actor/devices that it manages.
	 *  @param inGuid	The GUID of the device that wants to associate
	 *  @return	true = yes, allow to associate, false = deny association.
	 */
	boolean canNetworkDeviceAssociate(NetGuid inGuid);
	
	// --------------------------------------------------------------------------
	/**
	 *  Called when the controller loses contact with a device from the network.
	 *  @param inNetworkDevice	The the device that got removed.
	 */
	void deviceLost(INetworkDevice inNetworkDevice);

	/**
	 * Called when device has associated and is active
	 */
	void deviceActive(INetworkDevice inNetworkDevice);
	
}
