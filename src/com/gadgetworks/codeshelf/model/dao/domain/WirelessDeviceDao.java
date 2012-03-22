/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: WirelessDeviceDao.java,v 1.3 2012/03/22 07:35:11 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.dao.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.Query;
import com.gadgetworks.codeshelf.controller.IDeviceMaintainer;
import com.gadgetworks.codeshelf.controller.INetworkDevice;
import com.gadgetworks.codeshelf.controller.NetAddress;
import com.gadgetworks.codeshelf.controller.NetMacAddress;
import com.gadgetworks.codeshelf.model.dao.DaoException;
import com.gadgetworks.codeshelf.model.dao.GenericDao;
import com.gadgetworks.codeshelf.model.dao.IDaoRegistry;
import com.gadgetworks.codeshelf.model.dao.IDbFacade;
import com.gadgetworks.codeshelf.model.persist.WirelessDevice;
import com.gadgetworks.codeshelf.model.persist.WirelessDevice.IWirelessDeviceDao;
import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * @author jeffw
 *
 */
@Singleton
public final class WirelessDeviceDao extends GenericDao<WirelessDevice> implements IWirelessDeviceDao, IDeviceMaintainer {

	private static final Log				LOGGER	= LogFactory.getLog(WirelessDeviceDao.class);

	private Map<NetAddress, WirelessDevice>	mAddressLookupMap;

	@Inject
	public WirelessDeviceDao(final IDaoRegistry inDaoRegistry, final IDbFacade<WirelessDevice> inDbFacade) {
		super(WirelessDevice.class, inDaoRegistry, inDbFacade);
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.controller.IDeviceMaintainer#findNetworkDeviceByMacAddr(java.lang.String)
	 */
	public INetworkDevice findNetworkDeviceByMacAddr(NetMacAddress inMacAddr) {
		return findWirelessDeviceByMacAddr(inMacAddr);
	}

	/* --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.controller.IDeviceMaintainer#getNetworkDevice(com.gadgetworks.codeshelf.command.NetAddress)
	 */
	public INetworkDevice getNetworkDevice(NetAddress inAddress) {
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
				store((WirelessDevice) inNetworkDevice);
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
		//		if (!USE_DAO_CACHE) {
		Query<WirelessDevice> query = Ebean.createQuery(WirelessDevice.class);
		query.where().eq("mMacAddr", inMacAddr.toString());
		query = query.setUseCache(true);
		return query.findUnique();
		//		} else {
		//			WirelessDevice result = null;
		//			if (mCacheMap == null) {
		//				initCacheMap();
		//			}
		//			for (WirelessDevice wirelessDevice : mCacheMap.values()) {
		//				if ((wirelessDevice.getMacAddress().equals(inMacAddr))) {
		//					result = wirelessDevice;
		//				}
		//			}
		//			return result;
		//		}
	}
}
