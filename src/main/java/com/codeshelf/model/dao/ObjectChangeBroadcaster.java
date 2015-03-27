package com.codeshelf.model.dao;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.model.domain.IDomainObject;
import com.google.common.collect.Sets;
import com.google.inject.Inject;

public class ObjectChangeBroadcaster {

	private static final Logger LOGGER = LoggerFactory.getLogger(ObjectChangeBroadcaster.class);

	Map<String,TenantListenerSet> listenerSets = new ConcurrentHashMap<String,TenantListenerSet>();
	
	@Inject
	public ObjectChangeBroadcaster() {
	}
	// --------------------------------------------------------------------------

	/**
	 * @param inDomainObject
	 */
	public synchronized void broadcastAdd(String tenantIdentifier, Class<? extends IDomainObject> domainClass, final UUID domainPersistentId) {
		TenantListenerSet listenerSet = listenerSets.get(tenantIdentifier);
		if(listenerSet != null) {
			Set<IDaoListener> listeners = listenerSet.get(domainClass);
			if(listeners != null) {
				for (final IDaoListener daoListener : listeners) {
					daoListener.objectAdded(domainClass, domainPersistentId);
				}
			}
		}
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inDomainObject
	 */
	public synchronized void broadcastUpdate(String tenantIdentifier, Class<? extends IDomainObject> domainClass, final UUID domainPersistentId, final Set<String> inChangedProperties) {
		TenantListenerSet listenerSet = listenerSets.get(tenantIdentifier);
		if(listenerSet != null) {
			Set<Class<? extends IDomainObject>> listenClasses = listenerSet.keySet();
			for (Class<? extends IDomainObject> keyClass : listenClasses) {
				if (keyClass.isAssignableFrom(domainClass)) {
					
					Set<IDaoListener> listenersForClass = listenerSet.get(keyClass);
					for (final IDaoListener daoListener : listenersForClass) {
						daoListener.objectUpdated(domainClass, domainPersistentId, inChangedProperties);
					}			
				}
			}
		}
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inDomainObject
	 */
	public synchronized void broadcastDelete(String tenantIdentifier, Class<? extends IDomainObject> domainClass, final UUID domainPersistentId,
				Class<? extends IDomainObject> parentClass, final UUID parentId) {
		TenantListenerSet listenerSet = listenerSets.get(tenantIdentifier);
		if(listenerSet != null) {
			Set<IDaoListener> listeners = listenerSet.get(domainClass);
			if(listeners != null) {
				for (final IDaoListener daoListener : listeners) {
					daoListener.objectDeleted(domainClass, domainPersistentId, parentClass, parentId);
				}
			}
		}
	}

	/*
	 * --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * 
	 * @see com.codeshelf.model.dao.ISystemDAO#registerDAOListener(com.codeshelf.model.dao.IDAOListener)
	 */
	public synchronized void registerDAOListener(String tenantIdentifier,IDaoListener inListener, Class<? extends IDomainObject> daoClass) {
		TenantListenerSet listenerSet = listenerSets.get(tenantIdentifier);
		if(listenerSet == null) {
			listenerSet = new TenantListenerSet();
			listenerSets.put(tenantIdentifier, listenerSet);
		}
		listenerSet.put(daoClass, inListener);
	}

	/*
	 * --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * 
	 * @see com.codeshelf.model.dao.ISystemDAO#unRegisterDAOListener(com.codeshelf.model.dao.IDAOListener)
	 */
	public final synchronized void unregisterDAOListener(String tenantIdentifier,IDaoListener inListener) {
		TenantListenerSet listenerSet = listenerSets.get(tenantIdentifier);
		if(listenerSet != null) {
			TenantListenerSet newMap = new TenantListenerSet();
			Set<Class<? extends IDomainObject>> listenClasses = listenerSet.keySet();
			for (Class<? extends IDomainObject> key : listenClasses) {				
				Set<IDaoListener> newSet = Sets.newHashSet(listenerSet.get(key));
				newSet.remove(inListener);
				newMap.putAll(key, newSet);
			}
			listenerSets.put(tenantIdentifier,newMap);
		} else {
			LOGGER.warn("tried to unregister listener for tenant {}, but no listener set existed",tenantIdentifier);
		}

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
	public final synchronized void removeDAOListeners(String tenantIdentifier) {
		this.listenerSets.remove(tenantIdentifier);
	}

}
