package com.gadgetworks.codeshelf.model.dao;

import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gadgetworks.codeshelf.model.domain.IDomainObject;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;
import com.google.inject.Inject;

public class ObjectChangeBroadcaster {

	@SuppressWarnings("unused")
	private static final Logger LOGGER = LoggerFactory.getLogger(ObjectChangeBroadcaster.class);

	private SetMultimap<Class<? extends IDomainObject>, IDaoListener> mListeners = HashMultimap.create();

	@Inject
	public ObjectChangeBroadcaster() {
	}
	// --------------------------------------------------------------------------

	/**
	 * @param inDomainObject
	 */
	public void broadcastAdd(Class<? extends IDomainObject> domainClass, final UUID domainPersistentId) {
		for (final IDaoListener daoListener : mListeners.get(domainClass)) {
			daoListener.objectAdded(domainClass, domainPersistentId);
		}
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inDomainObject
	 */
	public void broadcastUpdate(Class<? extends IDomainObject> domainClass, final UUID domainPersistentId, final Set<String> inChangedProperties) {
		Set<Class<? extends IDomainObject>> keys = mListeners.keySet();
		for (Class<? extends IDomainObject> keyClass : keys) {
			if (keyClass.isAssignableFrom(domainClass)) {
				
				Set<IDaoListener> listenersForClass = mListeners.get(keyClass);
				for (final IDaoListener daoListener : listenersForClass) {
					daoListener.objectUpdated(domainClass, domainPersistentId, inChangedProperties);
				}			}
		}
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inDomainObject
	 */
	public void broadcastDelete(Class<? extends IDomainObject> domainClass, final UUID domainPersistentId) {
		for (final IDaoListener daoListener : mListeners.get(domainClass)) {
			daoListener.objectDeleted(domainClass, domainPersistentId);
		}
	}

	/*
	 * --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * 
	 * @see com.gadgetworks.codeshelf.model.dao.ISystemDAO#registerDAOListener(com.gadgetworks.codeshelf.model.dao.IDAOListener)
	 */
	public final void registerDAOListener(IDaoListener inListener, Class<? extends IDomainObject> daoClass) {
		mListeners.put(daoClass, inListener);
	}

	/*
	 * --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * 
	 * @see com.gadgetworks.codeshelf.model.dao.ISystemDAO#unRegisterDAOListener(com.gadgetworks.codeshelf.model.dao.IDAOListener)
	 */
	public final void unregisterDAOListener(IDaoListener inListener) {
		//Copy on write
		SetMultimap<Class<? extends IDomainObject>, IDaoListener> newMap = HashMultimap.create();
		for (Class<? extends IDomainObject> key : mListeners.keySet()) {
			Set<IDaoListener> newSet = Sets.newHashSet(mListeners.get(key));
			newSet.remove(inListener);
			newMap.putAll(key, newSet);
		}
		mListeners = newMap;
	}

	/*
	 * --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * 
	 * @see com.gadgetworks.codeshelf.model.dao.ISystemDAO#unRegisterDAOListener(com.gadgetworks.codeshelf.model.dao.IDAOListener)
	 */
	public final void removeDAOListeners() {
		mListeners.clear();
	}

}
