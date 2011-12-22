/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: SystemDAO.java,v 1.7 2011/12/22 11:46:31 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.dao;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.swt.widgets.Display;

import com.avaje.ebean.BeanState;
import com.avaje.ebean.Ebean;
import com.avaje.ebean.EbeanServer;
import com.avaje.ebean.EbeanServerFactory;
import com.avaje.ebean.LogLevel;
import com.avaje.ebean.Query;
import com.avaje.ebean.config.ServerConfig;
import com.gadgetworks.codeshelf.application.Util;
import com.gadgetworks.codeshelf.controller.INetworkDevice;
import com.gadgetworks.codeshelf.controller.NetAddress;
import com.gadgetworks.codeshelf.controller.NetMacAddress;
import com.gadgetworks.codeshelf.controller.NetworkId;
import com.gadgetworks.codeshelf.model.persist.Aisle;
import com.gadgetworks.codeshelf.model.persist.CodeShelfNetwork;
import com.gadgetworks.codeshelf.model.persist.ControlGroup;
import com.gadgetworks.codeshelf.model.persist.DBProperty;
import com.gadgetworks.codeshelf.model.persist.Facility;
import com.gadgetworks.codeshelf.model.persist.PersistABC;
import com.gadgetworks.codeshelf.model.persist.PersistentProperty;
import com.gadgetworks.codeshelf.model.persist.PickTag;
import com.gadgetworks.codeshelf.model.persist.WirelessDevice;

// --------------------------------------------------------------------------
/**
 * @author jeffw
 */

public final class SystemDAO implements ISystemDAO {

	private static final Log					LOGGER			= LogFactory.getLog(SystemDAO.class);

	private List<IDAOListener>					mListeners;

	private boolean								mUseDAOCache	= USE_CACHE;

	private Map<Integer, PersistentProperty>	mPersistentPropertyCacheMap;
	private Map<Integer, WirelessDevice>		mWirelessDeviceCacheMap;
	private Map<Integer, CodeShelfNetwork>		mCodeShelfNetworkCacheMap;
	private Map<Integer, ControlGroup>			mControlGroupCacheMap;
	private Map<NetAddress, WirelessDevice>		mAddressLookupMap;
	private Map<Integer, Facility>				mFacilityCacheMap;
	private Map<Integer, Aisle>					mAisleCacheMap;

	public SystemDAO() {
		mListeners = new ArrayList<IDAOListener>();
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inObject
	 */
	private void privateBroadcastAdd(final Object inObject) {
		Display display = Display.getDefault();
		display.asyncExec(new Runnable() {
			public void run() {
				for (IDAOListener daoListener : mListeners) {
					daoListener.objectAdded(inObject);
				}
			}
		});

	}

	// --------------------------------------------------------------------------
	/**
	 * @param inObject
	 */
	private void privateBroadcastUpdate(final Object inObject) {
		Display display = Display.getDefault();
		display.asyncExec(new Runnable() {
			public void run() {
				for (IDAOListener daoListener : mListeners) {
					daoListener.objectUpdated(inObject);
				}
			}
		});
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inObject
	 */
	private void privateBroadcastDelete(final Object inObject) {
		Display display = Display.getDefault();
		display.asyncExec(new Runnable() {
			public void run() {
				for (IDAOListener daoListener : mListeners) {
					daoListener.objectDeleted(inObject);
				}
			}
		});
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.model.dao.ISystemDAO#pushNonPersistentAccountUpdates(com.gadgetworks.codeshelf.model.persist.Account)
	 */
	public void pushNonPersistentUpdates(PersistABC inPerstitentObject) {
		privateBroadcastUpdate(inPerstitentObject);
	}

	/*
	 * --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * 
	 * @see com.gadgetworks.codeshelf.model.dao.ISystemDAO#registerDAOListener(com.gadgetworks.codeshelf.model.dao.IDAOListener)
	 */
	public void registerDAOListener(IDAOListener inListener) {
		mListeners.add(inListener);
	}

	/*
	 * --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * 
	 * @see com.gadgetworks.codeshelf.model.dao.ISystemDAO#unRegisterDAOListener(com.gadgetworks.codeshelf.model.dao.IDAOListener)
	 */
	public void unregisterDAOListener(IDAOListener inListener) {
		mListeners.remove(inListener);
	}

	/*
	 * --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * 
	 * @see com.gadgetworks.codeshelf.model.dao.ISystemDAO#unRegisterDAOListener(com.gadgetworks.codeshelf.model.dao.IDAOListener)
	 */
	public void removeDAOListeners() {
		mListeners.clear();
	}

	// --------------------------------------------------------------------------
	/**
	 */
	private void initPersistentPropertyCacheMap() {
		Query<PersistentProperty> query = Ebean.createQuery(PersistentProperty.class);
		query = query.setUseCache(true);
		Collection<PersistentProperty> accounts = query.findList();
		mPersistentPropertyCacheMap = new HashMap<Integer, PersistentProperty>();
		for (PersistentProperty account : accounts) {
			mPersistentPropertyCacheMap.put(account.getPersistentId(), account);
		}
	}

	// --------------------------------------------------------------------------
	/**
	 */
	private void initWirelessDeviceCacheMap() {
		Query<WirelessDevice> query = Ebean.createQuery(WirelessDevice.class);
		query = query.setUseCache(true);
		Collection<WirelessDevice> wirelessDevices = query.findList();
		mWirelessDeviceCacheMap = new HashMap<Integer, WirelessDevice>();
		mAddressLookupMap = new HashMap<NetAddress, WirelessDevice>();
		for (WirelessDevice wirelessDevice : wirelessDevices) {
			mWirelessDeviceCacheMap.put(wirelessDevice.getPersistentId(), wirelessDevice);
			mAddressLookupMap.put(wirelessDevice.getNetAddress(), wirelessDevice);
		}
	}

	// --------------------------------------------------------------------------
	/**
	 */
	private void initCodeShelfNetworkCacheMap() {
		Query<CodeShelfNetwork> query = Ebean.createQuery(CodeShelfNetwork.class);
		query = query.setUseCache(true);
		Collection<CodeShelfNetwork> networks = query.findList();
		mCodeShelfNetworkCacheMap = new HashMap<Integer, CodeShelfNetwork>();
		for (CodeShelfNetwork network : networks) {
			mCodeShelfNetworkCacheMap.put(network.getPersistentId(), network);
		}
	}

	// --------------------------------------------------------------------------
	/**
	 */
	private void initControlGroupCacheMap() {
		Query<ControlGroup> query = Ebean.createQuery(ControlGroup.class);
		query = query.setUseCache(true);
		Collection<ControlGroup> groups = query.findList();
		mControlGroupCacheMap = new HashMap<Integer, ControlGroup>();
		for (ControlGroup group : groups) {
			mControlGroupCacheMap.put(group.getPersistentId(), group);
		}
	}

	// --------------------------------------------------------------------------
	/**
	 */
	private void initFacilityCacheMap() {
		Query<Facility> query = Ebean.createQuery(Facility.class);
		query = query.setUseCache(true);
		Collection<Facility> facilities = query.findList();
		mFacilityCacheMap = new HashMap<Integer, Facility>();
		for (Facility facility : facilities) {
			mFacilityCacheMap.put(facility.getPersistentId(), facility);
		}
	}

	// --------------------------------------------------------------------------
	/**
	 */
	private void initAisleCacheMap() {
		Query<Aisle> query = Ebean.createQuery(Aisle.class);
		query = query.setUseCache(true);
		Collection<Aisle> aisles = query.findList();
		mAisleCacheMap = new HashMap<Integer, Aisle>();
		for (Aisle aisle : aisles) {
			mAisleCacheMap.put(aisle.getPersistentId(), aisle);
		}
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inObject
	 * @return
	 */
	public boolean isObjectPersisted(PersistABC inObject) {
		boolean result = false;

		BeanState state = Ebean.getBeanState(inObject);
		// If there is a bean state and it's not new then this object was once persisted.
		if ((state != null) && (!state.isNew())) {
			result = true;
		}

		return result;
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.model.dao.ISystemDAO#loadDBProperty(long)
	 */
	public DBProperty loadDBProperty(Integer inID) {
		return Ebean.find(DBProperty.class, inID);
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.model.dao.ISystemDAO#findDBProperty(java.lang.String)
	 */
	public DBProperty findDBProperty(String inPropertyID) {
		Query<DBProperty> query = Ebean.createQuery(DBProperty.class);
		query.where().eq("mPropertyID", inPropertyID);
		query = query.setUseCache(true);
		return query.findUnique();
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.model.dao.ISystemDAO#storeDBProperty(com.gadgetworks.codeshelf.model.persist.DBProperty)
	 */
	public void storeDBProperty(DBProperty inDBProperty) {
		if (inDBProperty.getPersistentId() == null) {
			Ebean.save(inDBProperty);
			privateBroadcastAdd(inDBProperty);
		} else {
			Ebean.save(inDBProperty);
			privateBroadcastUpdate(inDBProperty);
		}
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.model.dao.ISystemDAO#deleteDBProperty(com.gadgetworks.codeshelf.model.persist.DBProperty)
	 */
	public void deleteDBProperty(DBProperty inDBProperty) {
		Ebean.delete(inDBProperty);
		privateBroadcastDelete(inDBProperty);
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.model.dao.ISystemDAO#getDBProperties()
	 */
	public Collection<DBProperty> getDBProperties() {
		Query<DBProperty> query = Ebean.createQuery(DBProperty.class);
		query = query.setUseCache(true);
		return query.findList();
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.model.dao.ISystemDAO#loadPersistentProperty(long)
	 */
	public PersistentProperty loadPersistentProperty(Integer inID) {
		if (!mUseDAOCache) {
			return Ebean.find(PersistentProperty.class, inID);
		} else {
			if (mPersistentPropertyCacheMap == null) {
				initPersistentPropertyCacheMap();
			}
			return mPersistentPropertyCacheMap.get(inID);
		}
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.model.dao.ISystemDAO#findPersistentProperty(java.lang.String)
	 */
	public PersistentProperty findPersistentProperty(String inPropertyID) {
		if (!mUseDAOCache) {
			Query<PersistentProperty> query = Ebean.createQuery(PersistentProperty.class);
			query.where().eq("mPropertyID", inPropertyID);
			query = query.setUseCache(true);
			return query.findUnique();
		} else {
			PersistentProperty result = null;
			if (mPersistentPropertyCacheMap == null) {
				initPersistentPropertyCacheMap();
			}
			for (PersistentProperty property : mPersistentPropertyCacheMap.values()) {
				if (property.getId().equals(inPropertyID)) {
					result = property;
				}
			}
			return result;
		}

	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.model.dao.ISystemDAO#storePersistentProperty(com.gadgetworks.codeshelf.model.persist.PersistentProperty)
	 */
	public void storePersistentProperty(PersistentProperty inPersistentProperty) {

		if (inPersistentProperty.getPersistentId() == null) {
			Ebean.save(inPersistentProperty);
			privateBroadcastAdd(inPersistentProperty);
		} else {
			Ebean.save(inPersistentProperty);
			privateBroadcastUpdate(inPersistentProperty);
		}
		if (mUseDAOCache) {
			if (mPersistentPropertyCacheMap == null) {
				initPersistentPropertyCacheMap();
			}
			mPersistentPropertyCacheMap.put(inPersistentProperty.getPersistentId(), inPersistentProperty);
		}
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.model.dao.ISystemDAO#deletePersistentProperty(com.gadgetworks.codeshelf.model.persist.PersistentProperty)
	 */
	public void deletePersistentProperty(PersistentProperty inPersistentProperty) {
		if (mUseDAOCache) {
			if (mPersistentPropertyCacheMap == null) {
				initPersistentPropertyCacheMap();
			}
			mPersistentPropertyCacheMap.remove(inPersistentProperty.getPersistentId());
		}
		Ebean.delete(inPersistentProperty);
		privateBroadcastDelete(inPersistentProperty);
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.model.dao.ISystemDAO#getPersistentProperties()
	 */
	public Collection<PersistentProperty> getPersistentProperties() {
		if (!mUseDAOCache) {
			Query<PersistentProperty> query = Ebean.createQuery(PersistentProperty.class);
			query = query.setUseCache(true);
			return query.findList();
		} else {
			if (mPersistentPropertyCacheMap == null) {
				initPersistentPropertyCacheMap();
			}
			// Use the accounts cache.
			return mPersistentPropertyCacheMap.values();
		}
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.model.dao.ISystemDAO#loadWirelessDevice(long)
	 */
	public WirelessDevice loadWirelessDevice(Integer inID) {
		if (!mUseDAOCache) {
			return Ebean.find(WirelessDevice.class, inID);
		} else {
			if (mWirelessDeviceCacheMap == null) {
				initWirelessDeviceCacheMap();
			}
			return mWirelessDeviceCacheMap.get(inID);
		}
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.model.dao.ISystemDAO#findWirelessDeviceByMacAddr(java.lang.String)
	 */
	public WirelessDevice findWirelessDeviceByMacAddr(NetMacAddress inMacAddr) {
		if (!mUseDAOCache) {
			Query<WirelessDevice> query = Ebean.createQuery(WirelessDevice.class);
			query.where().eq("mMacAddr", inMacAddr.toString());
			query = query.setUseCache(true);
			return query.findUnique();
		} else {
			WirelessDevice result = null;
			if (mWirelessDeviceCacheMap == null) {
				initWirelessDeviceCacheMap();
			}
			for (WirelessDevice wirelessDevice : mWirelessDeviceCacheMap.values()) {
				if ((wirelessDevice.getMacAddress().equals(inMacAddr))) {
					result = wirelessDevice;
				}
			}
			return result;
		}
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.model.dao.ISystemDAO#storeWirelessDevice(com.gadgetworks.codeshelf.model.persist.WirelessDevice)
	 */
	public void storeWirelessDevice(WirelessDevice inWirelessDevice) {
		if (inWirelessDevice.getNetworkDeviceState() == null) {
			LOGGER.error("Device status was null.");
		}

		if (inWirelessDevice.getPersistentId() == null) {
			Ebean.save(inWirelessDevice);
			privateBroadcastAdd(inWirelessDevice);
		} else {
			Ebean.save(inWirelessDevice);
			privateBroadcastUpdate(inWirelessDevice);
		}
		if (mUseDAOCache) {
			if (mWirelessDeviceCacheMap == null) {
				initWirelessDeviceCacheMap();
			}
			mWirelessDeviceCacheMap.put(inWirelessDevice.getPersistentId(), inWirelessDevice);
			mAddressLookupMap.put(inWirelessDevice.getNetAddress(), inWirelessDevice);
		}
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.model.dao.ISystemDAO#deleteWirelessDevice(com.gadgetworks.codeshelf.model.persist.WirelessDevice)
	 */
	public void deleteWirelessDevice(WirelessDevice inWirelessDevice) {
		if (mUseDAOCache) {
			if (mWirelessDeviceCacheMap == null) {
				initWirelessDeviceCacheMap();
			}
			mWirelessDeviceCacheMap.remove(inWirelessDevice.getPersistentId());
			mAddressLookupMap.remove(inWirelessDevice.getNetAddress());
		}
		Ebean.delete(inWirelessDevice);
		privateBroadcastDelete(inWirelessDevice);
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.model.dao.ISystemDAO#getWirelessDevices()
	 */
	public Collection<WirelessDevice> getWirelessDevices() {
		if (!mUseDAOCache) {
			Query<WirelessDevice> query = Ebean.createQuery(WirelessDevice.class);
			query = query.setUseCache(true);
			return query.findList();
		} else {
			if (mWirelessDeviceCacheMap == null) {
				initWirelessDeviceCacheMap();
			}
			// Use the buddies cache.
			return mWirelessDeviceCacheMap.values();
		}
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
	public List<INetworkDevice> getNetworkDevices() {
		return new ArrayList<INetworkDevice>(getWirelessDevices());
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.controller.IDeviceMaintainer#persistChanges(com.gadgetworks.codeshelf.controller.INetworkDevice)
	 */
	public void deviceUpdated(INetworkDevice inNetworkDevice, boolean inPersistentDataChanged) {
		if (inPersistentDataChanged) {
			storeWirelessDevice((WirelessDevice) inNetworkDevice);
		} else {
			pushNonPersistentUpdates((WirelessDevice) inNetworkDevice);
		}
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.model.dao.ISystemDAO#loadCodeShelfNetwork(long)
	 */
	public CodeShelfNetwork loadCodeShelfNetwork(Integer inID) {
		if (!mUseDAOCache) {
			return Ebean.find(CodeShelfNetwork.class, inID);
		} else {
			if (mCodeShelfNetworkCacheMap == null) {
				initCodeShelfNetworkCacheMap();
			}
			return mCodeShelfNetworkCacheMap.get(inID);
		}
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.model.dao.ISystemDAO#findCodeShelfNetwork(java.lang.String)
	 */
	public CodeShelfNetwork findCodeShelfNetwork(NetworkId inNetworkId) {
		if (!mUseDAOCache) {
			Query<CodeShelfNetwork> query = Ebean.createQuery(CodeShelfNetwork.class);
			query.where().eq("mNetworkId", inNetworkId);
			query = query.setUseCache(true);
			return query.findUnique();
		} else {
			CodeShelfNetwork result = null;
			if (mCodeShelfNetworkCacheMap == null) {
				initCodeShelfNetworkCacheMap();
			}
			for (CodeShelfNetwork codeShelfNetwork : mCodeShelfNetworkCacheMap.values()) {
				if (codeShelfNetwork.getId().equals(inNetworkId)) {
					result = codeShelfNetwork;
				}
			}
			return result;
		}

	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.model.dao.ISystemDAO#storeCodeShelfNetwork(com.gadgetworks.codeshelf.model.persist.CodeShelfNetwork)
	 */
	public void storeCodeShelfNetwork(CodeShelfNetwork inCodeShelfNetwork) {

		if (inCodeShelfNetwork.getPersistentId() == null) {
			Ebean.save(inCodeShelfNetwork);
			privateBroadcastAdd(inCodeShelfNetwork);
		} else {
			Ebean.save(inCodeShelfNetwork);
			privateBroadcastUpdate(inCodeShelfNetwork);
		}
		if (mUseDAOCache) {
			if (mCodeShelfNetworkCacheMap == null) {
				initCodeShelfNetworkCacheMap();
			}
			mCodeShelfNetworkCacheMap.put(inCodeShelfNetwork.getPersistentId(), inCodeShelfNetwork);
		}
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.model.dao.ISystemDAO#deleteCodeShelfNetwork(com.gadgetworks.codeshelf.model.persist.CodeShelfNetwork)
	 */
	public void deleteCodeShelfNetwork(CodeShelfNetwork inCodeShelfNetwork) {
		if (mUseDAOCache) {
			if (mCodeShelfNetworkCacheMap == null) {
				initCodeShelfNetworkCacheMap();
			}
			mCodeShelfNetworkCacheMap.remove(inCodeShelfNetwork.getPersistentId());
		}
		Ebean.delete(inCodeShelfNetwork);
		privateBroadcastDelete(inCodeShelfNetwork);
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.model.dao.ISystemDAO#getPersistentProperties()
	 */
	public Collection<CodeShelfNetwork> getCodeShelfNetworks() {
		if (!mUseDAOCache) {
			Query<CodeShelfNetwork> query = Ebean.createQuery(CodeShelfNetwork.class);
			query = query.setUseCache(true);
			return query.findList();
		} else {
			if (mCodeShelfNetworkCacheMap == null) {
				initCodeShelfNetworkCacheMap();
			}
			// Use the accounts cache.
			return mCodeShelfNetworkCacheMap.values();
		}
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.model.dao.ISystemDAO#loadFacility(long)
	 */
	public Facility loadFacility(Integer inID) {
		if (!mUseDAOCache) {
			return Ebean.find(Facility.class, inID);
		} else {
			if (mFacilityCacheMap == null) {
				initFacilityCacheMap();
			}
			return mFacilityCacheMap.get(inID);
		}
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.model.dao.ISystemDAO#findFacility(java.lang.String)
	 */
	public Facility findFacility(String inFacilityId) {
		if (!mUseDAOCache) {
			Query<Facility> query = Ebean.createQuery(Facility.class);
			query.where().eq("mNetworkId", inFacilityId);
			query = query.setUseCache(true);
			return query.findUnique();
		} else {
			Facility result = null;
			if (mFacilityCacheMap == null) {
				initFacilityCacheMap();
			}
			for (Facility facility : mFacilityCacheMap.values()) {
				if (facility.getId().equals(inFacilityId)) {
					result = facility;
				}
			}
			return result;
		}

	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.model.dao.ISystemDAO#storeFacility(com.gadgetworks.codeshelf.model.persist.Facility)
	 */
	public void storeFacility(Facility inFacility) {

		if (inFacility.getPersistentId() == null) {
			Ebean.save(inFacility);
			privateBroadcastAdd(inFacility);
		} else {
			Ebean.save(inFacility);
			privateBroadcastUpdate(inFacility);
		}
		if (mUseDAOCache) {
			if (mFacilityCacheMap == null) {
				initFacilityCacheMap();
			}
			mFacilityCacheMap.put(inFacility.getPersistentId(), inFacility);
		}
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.model.dao.ISystemDAO#deleteFacility(com.gadgetworks.codeshelf.model.persist.Facility)
	 */
	public void deleteFacility(Facility inFacility) {
		if (mUseDAOCache) {
			if (mFacilityCacheMap == null) {
				initFacilityCacheMap();
			}
			mFacilityCacheMap.remove(inFacility.getPersistentId());
		}
		Ebean.delete(inFacility);
		privateBroadcastDelete(inFacility);
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.model.dao.ISystemDAO#getPersistentProperties()
	 */
	public Collection<Facility> getFacilities() {
		if (!mUseDAOCache) {
			Query<Facility> query = Ebean.createQuery(Facility.class);
			query = query.setUseCache(true);
			return query.findList();
		} else {
			if (mFacilityCacheMap == null) {
				initFacilityCacheMap();
			}
			// Use the accounts cache.
			return mFacilityCacheMap.values();
		}
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.model.dao.ISystemDAO#loadAisle(long)
	 */
	public Aisle loadAisle(Integer inID) {
		if (!mUseDAOCache) {
			return Ebean.find(Aisle.class, inID);
		} else {
			if (mAisleCacheMap == null) {
				initAisleCacheMap();
			}
			return mAisleCacheMap.get(inID);
		}
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.model.dao.ISystemDAO#findAisle(java.lang.String)
	 */
	public Aisle findAisle(String inAisleId) {
		if (!mUseDAOCache) {
			Query<Aisle> query = Ebean.createQuery(Aisle.class);
			query.where().eq("mNetworkId", inAisleId);
			query = query.setUseCache(true);
			return query.findUnique();
		} else {
			Aisle result = null;
			if (mAisleCacheMap == null) {
				initAisleCacheMap();
			}
			for (Aisle aisle : mAisleCacheMap.values()) {
				if (aisle.getId().equals(inAisleId)) {
					result = aisle;
				}
			}
			return result;
		}

	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.model.dao.ISystemDAO#storeAisle(com.gadgetworks.codeshelf.model.persist.Aisle)
	 */
	public void storeAisle(Aisle inAisle) {

		if (inAisle.getPersistentId() == null) {
			Ebean.save(inAisle);
			privateBroadcastAdd(inAisle);
		} else {
			Ebean.save(inAisle);
			privateBroadcastUpdate(inAisle);
		}
		if (mUseDAOCache) {
			if (mAisleCacheMap == null) {
				initAisleCacheMap();
			}
			mAisleCacheMap.put(inAisle.getPersistentId(), inAisle);
		}
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.model.dao.ISystemDAO#deleteAisle(com.gadgetworks.codeshelf.model.persist.Aisle)
	 */
	public void deleteAisle(Aisle inAisle) {
		if (mUseDAOCache) {
			if (mAisleCacheMap == null) {
				initAisleCacheMap();
			}
			mAisleCacheMap.remove(inAisle.getPersistentId());
		}
		Ebean.delete(inAisle);
		privateBroadcastDelete(inAisle);
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.model.dao.ISystemDAO#getPersistentProperties()
	 */
	public Collection<Aisle> getAisles() {
		if (!mUseDAOCache) {
			Query<Aisle> query = Ebean.createQuery(Aisle.class);
			query = query.setUseCache(true);
			return query.findList();
		} else {
			if (mAisleCacheMap == null) {
				initAisleCacheMap();
			}
			// Use the accounts cache.
			return mAisleCacheMap.values();
		}
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.model.dao.ISystemDAO#loadControlGroup(long)
	 */
	public ControlGroup loadControlGroup(Integer inID) {
		if (!mUseDAOCache) {
			return Ebean.find(ControlGroup.class, inID);
		} else {
			if (mControlGroupCacheMap == null) {
				initControlGroupCacheMap();
			}
			return mControlGroupCacheMap.get(inID);
		}
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.model.dao.ISystemDAO#findControlGroup(java.lang.String)
	 */
	public ControlGroup findControlGroup(String inPropertyID) {
		if (!mUseDAOCache) {
			Query<ControlGroup> query = Ebean.createQuery(ControlGroup.class);
			query.where().eq("mPropertyID", inPropertyID);
			query = query.setUseCache(true);
			return query.findUnique();
		} else {
			ControlGroup result = null;
			if (mControlGroupCacheMap == null) {
				initControlGroupCacheMap();
			}
			for (ControlGroup property : mControlGroupCacheMap.values()) {
				if (property.getId().equals(inPropertyID)) {
					result = property;
				}
			}
			return result;
		}

	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.model.dao.ISystemDAO#storeControlGroup(com.gadgetworks.codeshelf.model.persist.ControlGroup)
	 */
	public void storeControlGroup(ControlGroup inControlGroup) {

		if (inControlGroup.getPersistentId() == null) {
			Ebean.save(inControlGroup);
			privateBroadcastAdd(inControlGroup);
		} else {
			Ebean.save(inControlGroup);
			privateBroadcastUpdate(inControlGroup);
		}
		if (mUseDAOCache) {
			if (mControlGroupCacheMap == null) {
				initControlGroupCacheMap();
			}
			mControlGroupCacheMap.put(inControlGroup.getPersistentId(), inControlGroup);
		}
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.model.dao.ISystemDAO#deleteControlGroup(com.gadgetworks.codeshelf.model.persist.ControlGroup)
	 */
	public void deleteControlGroup(ControlGroup inControlGroup) {
		if (mUseDAOCache) {
			if (mControlGroupCacheMap == null) {
				initControlGroupCacheMap();
			}
			mControlGroupCacheMap.remove(inControlGroup.getPersistentId());
		}
		Ebean.delete(inControlGroup);
		privateBroadcastDelete(inControlGroup);
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.model.dao.ISystemDAO#getControlGroups()
	 */
	public Collection<ControlGroup> getControlGroups() {
		if (!mUseDAOCache) {
			Query<ControlGroup> query = Ebean.createQuery(ControlGroup.class);
			query = query.setUseCache(true);
			return query.findList();
		} else {
			if (mControlGroupCacheMap == null) {
				initControlGroupCacheMap();
			}
			// Use the accounts cache.
			return mControlGroupCacheMap.values();
		}
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.hbnet.model.dao.ISystemDAO#loadPickTag(long)
	 */
	public PickTag loadPickTag(Integer inID) {
		if (!mUseDAOCache) {
			return Ebean.find(PickTag.class, inID);
		} else {
			if (mWirelessDeviceCacheMap == null) {
				initWirelessDeviceCacheMap();
			}
			// All wirelessDevices must be uniquely numbered across all device types.
			return (PickTag) mWirelessDeviceCacheMap.get(inID);
		}
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.hbnet.model.dao.ISystemDAO#findPickTagByMacAddr(java.lang.String)
	 */
	public PickTag findPickTagByMacAddr(NetMacAddress inMacAddr) {
		return (PickTag) findWirelessDeviceByMacAddr(inMacAddr);
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.model.dao.ISystemDAO#findPickTagByNetAddr(com.gadgetworks.codeshelf.controller.NetAddress)
	 */
	public PickTag findPickTagByNetAddr(NetAddress inNetAddr) {
		PickTag result = null;

		for (PickTag pickTag : this.getPickTags()) {
			if (pickTag.getNetAddress().equals(inNetAddr)) {
				result = pickTag;
			}
		}
		return result;
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.hbnet.model.dao.ISystemDAO#storePickTag(com.gadgetworks.hbnet.model.persist.PickTag)
	 */
	public void storePickTag(PickTag inPickTag) {
		storeWirelessDevice(inPickTag);
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.hbnet.model.dao.ISystemDAO#deletePickTag(com.gadgetworks.hbnet.model.persist.PickTag)
	 */
	public void deletePickTag(PickTag inPickTag) {
		deleteWirelessDevice(inPickTag);
	}

	/* --------------------------------------------------------------------------
	* (non-Javadoc)
	* @see com.gadgetworks.hbnet.model.dao.ISystemDAO#getPickTags()
	*/
	public Collection<PickTag> getPickTags() {
		if (!mUseDAOCache) {
			Query<PickTag> query = Ebean.createQuery(PickTag.class);
			query = query.setUseCache(true);
			return query.findList();
		} else {
			Map<Integer, PickTag> result = new HashMap<Integer, PickTag>();
			Collection<WirelessDevice> devices = getWirelessDevices();
			for (WirelessDevice device : devices) {
				if (device instanceof PickTag) {
					result.put(device.getPersistentId(), (PickTag) device);
				}
			}
			return result.values();
		}
	}
}
