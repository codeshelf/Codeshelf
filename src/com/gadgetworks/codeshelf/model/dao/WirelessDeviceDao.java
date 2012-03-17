/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: WirelessDeviceDao.java,v 1.2 2012/03/17 09:07:02 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.dao;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.Query;
import com.gadgetworks.codeshelf.controller.IDeviceMaintainer;
import com.gadgetworks.codeshelf.controller.INetworkDevice;
import com.gadgetworks.codeshelf.controller.NetAddress;
import com.gadgetworks.codeshelf.controller.NetMacAddress;
import com.gadgetworks.codeshelf.model.persist.WirelessDevice;

/**
 * @author jeffw
 *
 */
public class WirelessDeviceDao<T extends WirelessDevice> extends GenericDao<T> implements IDeviceMaintainer{

	private static final Log			LOGGER		= LogFactory.getLog(WirelessDeviceDao.class);

	private Map<NetAddress, WirelessDevice>	mAddressLookupMap;

	public WirelessDeviceDao(final Class<T> inClass) {
		super(inClass);
	}

	// --------------------------------------------------------------------------
	/**
	 */
	protected void initCacheMap() {
		Query<T> query = Ebean.createQuery(mClass);
		query = query.setUseCache(true);
		Collection<T> wirelessDevices = query.findList();
		mCacheMap = new HashMap<Long, T>();
		mAddressLookupMap = new HashMap<NetAddress, WirelessDevice>();
		for (T wirelessDevice : wirelessDevices) {
			mCacheMap.put(wirelessDevice.getPersistentId(), wirelessDevice);
			mAddressLookupMap.put(wirelessDevice.getNetAddress(), wirelessDevice);
		}
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.controller.IDeviceMaintainer#findNetworkDeviceByMacAddr(java.lang.String)
	 */
	public final INetworkDevice findNetworkDeviceByMacAddr(NetMacAddress inMacAddr) {
		return findWirelessDeviceByMacAddr(inMacAddr);
	}

	/* --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.controller.IDeviceMaintainer#getNetworkDevice(com.gadgetworks.codeshelf.command.NetAddress)
	 */
	public final INetworkDevice getNetworkDevice(NetAddress inAddress) {
		return mAddressLookupMap.get(inAddress);
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.controller.IDeviceMaintainer#getNetworkDevices()
	 */
	public final List<INetworkDevice> getNetworkDevices() {
		return new ArrayList<INetworkDevice>(this.getAll());
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.controller.IDeviceMaintainer#persistChanges(com.gadgetworks.codeshelf.controller.INetworkDevice)
	 */
	public final void deviceUpdated(INetworkDevice inNetworkDevice, boolean inPersistentDataChanged) {
		if (inPersistentDataChanged) {
			try { 
				store((T) inNetworkDevice);
			} catch (DaoException e) {
				LOGGER.error("", e);
			}
		} else {
			pushNonPersistentUpdates((WirelessDevice) inNetworkDevice);
		}
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.model.dao.ISystemDAO#findWirelessDeviceByMacAddr(java.lang.String)
	 */
	public final WirelessDevice findWirelessDeviceByMacAddr(NetMacAddress inMacAddr) {
		if (!USE_DAO_CACHE) {
			Query<WirelessDevice> query = Ebean.createQuery(WirelessDevice.class);
			query.where().eq("mMacAddr", inMacAddr.toString());
			query = query.setUseCache(true);
			return query.findUnique();
		} else {
			WirelessDevice result = null;
			if (mCacheMap == null) {
				initCacheMap();
			}
			for (WirelessDevice wirelessDevice : mCacheMap.values()) {
				if ((wirelessDevice.getMacAddress().equals(inMacAddr))) {
					result = wirelessDevice;
				}
			}
			return result;
		}
	}
}
