/*******************************************************************************
 *  ESController
 *  Copyright (c) 2005-2009, Jeffrey B. Williams, All rights reserved
 *  $Id: IDeviceMaintainer.java,v 1.1 2013/02/20 08:28:26 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.controller;

import java.util.List;


// --------------------------------------------------------------------------
/**
 *  This interface allows the higher lever users of CodeShelf to be able to provide a way to 
 *  manage the persistence of the INetworkDevices.  
 *  
 *  @author jeffw
 */
public interface IDeviceMaintainer {
	
	/**
	 *  Allow the persistence layer to provide a managed device by address.
	 *  @return
	 */
	INetworkDevice getNetworkDevice(NetAddress inAddress);
	
	/**
	 *  Allow the persistence layer to provide a list of managed devices.
	 *  @return
	 */
	List<INetworkDevice> getNetworkDevices();
	
	// --------------------------------------------------------------------------
	/**
	 *  Allow the persistence layer to locate a managed device by MacAddr.
	 *  @param inMacAddr
	 *  @return
	 */
	INetworkDevice findNetworkDeviceByMacAddr(NetMacAddress inMacAddr);
	
	// --------------------------------------------------------------------------
	/**
	 *  Request that the persistence layer persist changes made to the managed device.
	 *  (The low layer is just changing information such as Last Contact Time and Device State.)
	 *  @param inNetworkDevice	The device we're changing.
	 *  @param inPersistentDataChanged	FlyWeight changed persistent (permanent) information about the device.
	 */
	void deviceUpdated(INetworkDevice inNetworkDevice, boolean inPersistentDataChanged);

}
