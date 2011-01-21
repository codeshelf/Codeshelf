/*******************************************************************************
 *  FlyWeightController
 *  Copyright (c) 2005-2008, Jeffrey B. Williams, All rights reserved
 *  $Id: IControllerEventListener.java,v 1.1 2011/01/21 01:08:21 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.controller;


// --------------------------------------------------------------------------
/**
 *  @author jeffw
 */
public interface IControllerEventListener {

	String	BEAN_ID				= "ControllerEventListener";
	
	// --------------------------------------------------------------------------
	/**
	 *  Called when the controller gets an association request from a network device
	 *  If the device is one that we're managing then return true, otherwise return false.
	 *  The listener needs to maintain a persistent list of actor/devices that it manages.
	 *  @param inGUID	The GUID of the device that wants to associate
	 *  @return	true = yes, allow to associate, false = deny association.
	 */
	boolean canNetworkDeviceAssociate(String inGUID);
	
	// --------------------------------------------------------------------------
	/**
	 *  Called when the controller sees a new device on the network.
	 *  @param inNetworkDevice	The the device that got added.
	 */
	void deviceAdded(INetworkDevice inNetworkDevice);
	
	// --------------------------------------------------------------------------
	/**
	 *  Called when the controller removes a device from the network.
	 *  @param inNetworkDevice	The the device that got removed.
	 */
	void deviceRemoved(INetworkDevice inNetworkDevice);
	
	// --------------------------------------------------------------------------
	/**
	 *  Called when the controller loses contact with a device from the network.
	 *  @param inNetworkDevice	The the device that got removed.
	 */
	void deviceLost(INetworkDevice inNetworkDevice);
	
}
