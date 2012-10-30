/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: MockWirelessDeviceDao.java,v 1.3 2012/10/30 15:21:34 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.dao;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.gadgetworks.codeshelf.controller.INetworkDevice;
import com.gadgetworks.codeshelf.controller.NetAddress;
import com.gadgetworks.codeshelf.controller.NetMacAddress;
import com.gadgetworks.codeshelf.model.domain.WirelessDevice;
import com.gadgetworks.codeshelf.model.domain.WirelessDevice.IWirelessDeviceDao;

/**
 * @author jeffw
 *
 */
public class MockWirelessDeviceDao extends MockDao<WirelessDevice> implements IWirelessDeviceDao {

	private Map<String, WirelessDevice>	mStorage	= new HashMap<String, WirelessDevice>();

	public MockWirelessDeviceDao() {

	}

	@Override
	public List<INetworkDevice> getNetworkDevices() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void deviceUpdated(INetworkDevice inNetworkDevice, boolean inPersistentDataChanged) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public WirelessDevice findWirelessDeviceByMacAddr(NetMacAddress inMacAddr) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public INetworkDevice findNetworkDeviceByMacAddr(NetMacAddress inMacAddr) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public INetworkDevice getNetworkDevice(NetAddress inAddress) {
		// TODO Auto-generated method stub
		return null;
	}
}
