/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: IWirelessDeviceDao.java,v 1.1 2012/03/17 23:49:23 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.dao;

import com.gadgetworks.codeshelf.controller.IDeviceMaintainer;
import com.gadgetworks.codeshelf.controller.INetworkDevice;
import com.gadgetworks.codeshelf.controller.NetAddress;
import com.gadgetworks.codeshelf.controller.NetMacAddress;
import com.gadgetworks.codeshelf.model.persist.WirelessDevice;

/**
 * @author jeffw
 *
 */
public interface IWirelessDeviceDao extends IGenericDao<WirelessDevice>, IDeviceMaintainer {

	WirelessDevice findWirelessDeviceByMacAddr(NetMacAddress inMacAddr);
	
	INetworkDevice findNetworkDeviceByMacAddr(NetMacAddress inMacAddr);
	
	INetworkDevice getNetworkDevice(NetAddress inAddress);
	
}
