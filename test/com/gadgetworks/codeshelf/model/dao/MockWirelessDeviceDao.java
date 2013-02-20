/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: MockWirelessDeviceDao.java,v 1.4 2013/02/20 08:28:26 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.dao;

import java.util.HashMap;
import java.util.Map;

import com.gadgetworks.codeshelf.model.domain.WirelessDevice;
import com.gadgetworks.codeshelf.model.domain.WirelessDevice.IWirelessDeviceDao;
import com.gadgetworks.flyweight.command.NetAddress;
import com.gadgetworks.flyweight.command.NetMacAddress;
import com.gadgetworks.flyweight.controller.INetworkDevice;

/**
 * @author jeffw
 *
 */
public class MockWirelessDeviceDao extends MockDao<WirelessDevice> implements IWirelessDeviceDao {

	private Map<String, WirelessDevice>	mStorage	= new HashMap<String, WirelessDevice>();

	public MockWirelessDeviceDao() {

	}

	@Override
	public final WirelessDevice findWirelessDeviceByMacAddr(NetMacAddress inMacAddr) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public final INetworkDevice findNetworkDeviceByMacAddr(NetMacAddress inMacAddr) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public final INetworkDevice getNetworkDevice(NetAddress inAddress) {
		// TODO Auto-generated method stub
		return null;
	}
}
