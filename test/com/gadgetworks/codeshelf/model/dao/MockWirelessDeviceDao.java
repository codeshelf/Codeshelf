/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: MockWirelessDeviceDao.java,v 1.1 2012/10/13 22:14:24 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.dao;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.avaje.ebean.Query;
import com.gadgetworks.codeshelf.controller.INetworkDevice;
import com.gadgetworks.codeshelf.controller.NetAddress;
import com.gadgetworks.codeshelf.controller.NetMacAddress;
import com.gadgetworks.codeshelf.model.domain.IDomainObject;
import com.gadgetworks.codeshelf.model.domain.WirelessDevice;
import com.gadgetworks.codeshelf.model.domain.WirelessDevice.IWirelessDeviceDao;

/**
 * @author jeffw
 *
 */
public class MockWirelessDeviceDao implements IWirelessDeviceDao {

	private Map<String, WirelessDevice>	mStorage	= new HashMap<String, WirelessDevice>();

	public MockWirelessDeviceDao() {

	}

	public void registerDAOListener(IDaoListener inListener) {

	}

	public void unregisterDAOListener(IDaoListener inListener) {

	}

	public void removeDAOListeners() {

	}

	public final Query<WirelessDevice> query() {
		// TODO Auto-generated method stub
		return null;
	}

	public final WirelessDevice findByPersistentId(Long inPersistentId) {
		// TODO Auto-generated method stub
		return null;
	}

	public final WirelessDevice findByDomainId(IDomainObject inParentObject, String inDomainId) {
		String domainId = "";
		if (inParentObject != null) {
			domainId = inParentObject.getFullDomainId() + "." + inDomainId;
		} else {
			domainId = inDomainId;
		}
		return mStorage.get(domainId);
	}

	public final List<WirelessDevice> findByPersistentIdList(List<Long> inPersistentIdList) {
		return null;
	}

	public final List<WirelessDevice> findByFilter(String inFilter, Map<String, Object> inFilterParams) {
		return null;
	}

	public void store(WirelessDevice inDomainObject) throws DaoException {
		mStorage.put(inDomainObject.getFullDomainId(), inDomainObject);
	}

	public void delete(WirelessDevice inDomainObject) throws DaoException {
		mStorage.remove(inDomainObject).getFullDomainId();
	}

	public List<WirelessDevice> getAll() {
		return new ArrayList(mStorage.values());
	}

	public void pushNonPersistentUpdates(WirelessDevice inDomainObject) {
	}

	public Class<WirelessDevice> getDaoClass() {
		return null;
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
