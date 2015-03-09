package com.codeshelf.model.dao;

import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.model.domain.IDomainObject;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;
import com.google.inject.Inject;

public class ObjectChangeBroadcaster {

	@SuppressWarnings("unused")
	private static final Logger LOGGER = LoggerFactory.getLogger(ObjectChangeBroadcaster.class);

	private SetMultimap<Class<? extends IDomainObject>, IDaoListener> mListeners
		= HashMultimap.create();
	//	= Multimaps.synchronizedSetMultimap(HashMultimap.<Class<? extends IDomainObject>, IDaoListener>create());

	@Inject
	public ObjectChangeBroadcaster() {
	}
	// --------------------------------------------------------------------------

	/**
	 * @param inDomainObject
	 */
	public synchronized void broadcastAdd(Class<? extends IDomainObject> domainClass, final UUID domainPersistentId) {
		Set<IDaoListener> listeners = mListeners.get(domainClass);
		if(listeners != null) {
			for (final IDaoListener daoListener : listeners) {
				daoListener.objectAdded(domainClass, domainPersistentId);
			}
		}
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inDomainObject
	 */
	public synchronized void broadcastUpdate(Class<? extends IDomainObject> domainClass, final UUID domainPersistentId, final Set<String> inChangedProperties) {
		Set<Class<? extends IDomainObject>> listenClasses = mListeners.keySet();
		for (Class<? extends IDomainObject> keyClass : listenClasses) {
			if (keyClass.isAssignableFrom(domainClass)) {
				
				Set<IDaoListener> listenersForClass = mListeners.get(keyClass);
				for (final IDaoListener daoListener : listenersForClass) {
					daoListener.objectUpdated(domainClass, domainPersistentId, inChangedProperties);
				}			
			}
		}
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inDomainObject
	 */
	public synchronized void broadcastDelete(Class<? extends IDomainObject> domainClass, final UUID domainPersistentId,
				Class<? extends IDomainObject> parentClass, final UUID parentId) {
		Set<IDaoListener> listeners = mListeners.get(domainClass);
		if(listeners != null) {
			for (final IDaoListener daoListener : listeners) {
				daoListener.objectDeleted(domainClass, domainPersistentId, parentClass, parentId);
			}
		}
	}

	/*
	 * --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * 
	 * @see com.codeshelf.model.dao.ISystemDAO#registerDAOListener(com.codeshelf.model.dao.IDAOListener)
	 */
	public synchronized void registerDAOListener(IDaoListener inListener, Class<? extends IDomainObject> daoClass) {
		mListeners.put(daoClass, inListener);
	}

	/*
	 * --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * 
	 * @see com.codeshelf.model.dao.ISystemDAO#unRegisterDAOListener(com.codeshelf.model.dao.IDAOListener)
	 */
	public final synchronized void unregisterDAOListener(IDaoListener inListener) {
		SetMultimap<Class<? extends IDomainObject>, IDaoListener> newMap = HashMultimap.create();
		Set<Class<? extends IDomainObject>> listenClasses = mListeners.keySet();
		for (Class<? extends IDomainObject> key : listenClasses) {				
			Set<IDaoListener> newSet = Sets.newHashSet(mListeners.get(key));
			newSet.remove(inListener);
			newMap.putAll(key, newSet);
		}
		mListeners = newMap;

		/*
		Set<Class<? extends IDomainObject>> listenClasses = mListeners.keySet();
		for (Class<? extends IDomainObject> key : listenClasses) {				
			Set<IDaoListener> listeners = mListeners.get(key);
			listeners.remove(inListener);
		}
		*/
	}

	/*
	 * --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * 
	 * @see com.codeshelf.model.dao.ISystemDAO#unRegisterDAOListener(com.codeshelf.model.dao.IDAOListener)
	 */
	public final synchronized void removeDAOListeners() {
		mListeners.clear();
	}

}
