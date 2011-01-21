/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: SystemDAO.java,v 1.2 2011/01/21 01:12:12 jeffw Exp $
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

import com.avaje.ebean.AdminLogging.LogLevel;
import com.avaje.ebean.AdminLogging.LogLevelStmt;
import com.avaje.ebean.AdminLogging.LogLevelTxnCommit;
import com.avaje.ebean.BeanState;
import com.avaje.ebean.Ebean;
import com.avaje.ebean.EbeanServer;
import com.avaje.ebean.EbeanServerFactory;
import com.avaje.ebean.Query;
import com.avaje.ebean.config.ServerConfig;
import com.gadgetworks.codeshelf.application.Util;
import com.gadgetworks.codeshelf.controller.INetworkDevice;
import com.gadgetworks.codeshelf.controller.NetAddress;
import com.gadgetworks.codeshelf.model.persist.ControlGroup;
import com.gadgetworks.codeshelf.model.persist.DBProperty;
import com.gadgetworks.codeshelf.model.persist.PersistABC;
import com.gadgetworks.codeshelf.model.persist.PersistentProperty;
import com.gadgetworks.codeshelf.model.persist.PickTag;
import com.gadgetworks.codeshelf.model.persist.SnapNetwork;
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
	private Map<Integer, SnapNetwork>			mSnapNetworkCacheMap;
	private Map<Integer, ControlGroup>			mControlGroupCacheMap;
	private Map<NetAddress, WirelessDevice>		mAddressLookupMap;

	public SystemDAO() {
		mListeners = new ArrayList<IDAOListener>();
		ServerConfig config = new ServerConfig();
		config.setName("h2");
		config.loadFromProperties();
		config.setNamingConvention(new GWEbeanNamingConvention());
		config.setDefaultServer(true);
		config.setDebugSql(false);
		config.setLoggingLevel(LogLevel.NONE);
		config.setLoggingLevelQuery(LogLevelStmt.NONE);
		config.setLoggingLevelSqlQuery(LogLevelStmt.NONE);
		config.setLoggingLevelIud(LogLevelStmt.NONE);
		config.setLoggingLevelTxnCommit(LogLevelTxnCommit.DEBUG);
		config.setLoggingToJavaLogger(true);
		config.setResourceDirectory(Util.getApplicationDataDirPath());
		EbeanServer server = EbeanServerFactory.create(config);
		if (server == null) {
			Util.exitSystem();
		}
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
	private void initSnapNetworkCacheMap() {
		Query<SnapNetwork> query = Ebean.createQuery(SnapNetwork.class);
		query = query.setUseCache(true);
		Collection<SnapNetwork> networks = query.findList();
		mSnapNetworkCacheMap = new HashMap<Integer, SnapNetwork>();
		for (SnapNetwork network : networks) {
			mSnapNetworkCacheMap.put(network.getPersistentId(), network);
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
	 * @see com.gadgetworks.codeshelf.model.dao.ISystemDAO#findWirelessDeviceByGUID(java.lang.String)
	 */
	public WirelessDevice findWirelessDeviceByGUID(String inGUID) {
		if (!mUseDAOCache) {
			Query<WirelessDevice> query = Ebean.createQuery(WirelessDevice.class);
			query.where().eq("mGUID", inGUID);
			query = query.setUseCache(true);
			return query.findUnique();
		} else {
			WirelessDevice result = null;
			if (mWirelessDeviceCacheMap == null) {
				initWirelessDeviceCacheMap();
			}
			for (WirelessDevice wirelessDevice : mWirelessDeviceCacheMap.values()) {
				if ((wirelessDevice.getGUID().equals(inGUID))) {
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
	 * @see com.gadgetworks.codeshelf.controller.IDeviceMaintainer#findNetworkDeviceByGUID(java.lang.String)
	 */
	public INetworkDevice findNetworkDeviceByGUID(String inGUID) {
		return findWirelessDeviceByGUID(inGUID);
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
	 * @see com.gadgetworks.codeshelf.model.dao.ISystemDAO#loadSnapNetwork(long)
	 */
	public SnapNetwork loadSnapNetwork(Integer inID) {
		if (!mUseDAOCache) {
			return Ebean.find(SnapNetwork.class, inID);
		} else {
			if (mSnapNetworkCacheMap == null) {
				initSnapNetworkCacheMap();
			}
			return mSnapNetworkCacheMap.get(inID);
		}
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.model.dao.ISystemDAO#findSnapNetwork(java.lang.String)
	 */
	public SnapNetwork findSnapNetwork(String inPropertyID) {
		if (!mUseDAOCache) {
			Query<SnapNetwork> query = Ebean.createQuery(SnapNetwork.class);
			query.where().eq("mPropertyID", inPropertyID);
			query = query.setUseCache(true);
			return query.findUnique();
		} else {
			SnapNetwork result = null;
			if (mSnapNetworkCacheMap == null) {
				initSnapNetworkCacheMap();
			}
			for (SnapNetwork property : mSnapNetworkCacheMap.values()) {
				if (property.getId().equals(inPropertyID)) {
					result = property;
				}
			}
			return result;
		}

	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.model.dao.ISystemDAO#storeSnapNetwork(com.gadgetworks.codeshelf.model.persist.SnapNetwork)
	 */
	public void storeSnapNetwork(SnapNetwork inSnapNetwork) {

		if (inSnapNetwork.getPersistentId() == null) {
			Ebean.save(inSnapNetwork);
			privateBroadcastAdd(inSnapNetwork);
		} else {
			Ebean.save(inSnapNetwork);
			privateBroadcastUpdate(inSnapNetwork);
		}
		if (mUseDAOCache) {
			if (mSnapNetworkCacheMap == null) {
				initSnapNetworkCacheMap();
			}
			mSnapNetworkCacheMap.put(inSnapNetwork.getPersistentId(), inSnapNetwork);
		}
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.model.dao.ISystemDAO#deleteSnapNetwork(com.gadgetworks.codeshelf.model.persist.SnapNetwork)
	 */
	public void deleteSnapNetwork(SnapNetwork inSnapNetwork) {
		if (mUseDAOCache) {
			if (mSnapNetworkCacheMap == null) {
				initSnapNetworkCacheMap();
			}
			mSnapNetworkCacheMap.remove(inSnapNetwork.getPersistentId());
		}
		Ebean.delete(inSnapNetwork);
		privateBroadcastDelete(inSnapNetwork);
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.model.dao.ISystemDAO#getPersistentProperties()
	 */
	public Collection<SnapNetwork> getSnapNetworks() {
		if (!mUseDAOCache) {
			Query<SnapNetwork> query = Ebean.createQuery(SnapNetwork.class);
			query = query.setUseCache(true);
			return query.findList();
		} else {
			if (mSnapNetworkCacheMap == null) {
				initSnapNetworkCacheMap();
			}
			// Use the accounts cache.
			return mSnapNetworkCacheMap.values();
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
	 * @see com.gadgetworks.hbnet.model.dao.ISystemDAO#findPickTagByGUID(java.lang.String)
	 */
	public PickTag findPickTagByGUID(String inGUID) {
		return (PickTag) findWirelessDeviceByGUID(inGUID);
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
