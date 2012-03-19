/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: GenericDao.java,v 1.7 2012/03/19 04:05:19 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.dao;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.persistence.PersistenceException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.avaje.ebean.BeanState;
import com.avaje.ebean.Ebean;
import com.avaje.ebean.Query;
import com.gadgetworks.codeshelf.model.persist.PersistABC;

/**
 * @author jeffw
 *
 */
public class GenericDao<T extends PersistABC> implements IGenericDao<T> {

	private static final Log	LOGGER		= LogFactory.getLog(PersistABC.class);

	//	protected Map<Long, T>		mCacheMap;
	private Class<T>			mClass;

	private List<IDaoListener>	mListeners	= new ArrayList<IDaoListener>();

	public GenericDao(final Class<T> inClass, final IDaoRegistry inDaoRegistry) {
		mClass = inClass;
		inDaoRegistry.addDao(this);
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inObject
	 */
	private void privateBroadcastAdd(final Object inObject) {
		for (IDaoListener daoListener : mListeners) {
			daoListener.objectAdded(inObject);
		}
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inObject
	 */
	private void privateBroadcastUpdate(final Object inObject) {
		for (IDaoListener daoListener : mListeners) {
			daoListener.objectUpdated(inObject);
		}
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inObject
	 */
	private void privateBroadcastDelete(final Object inObject) {
		for (IDaoListener daoListener : mListeners) {
			daoListener.objectDeleted(inObject);
		}
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.model.dao.ISystemDAO#pushNonPersistentAccountUpdates(com.gadgetworks.codeshelf.model.persist.Account)
	 */
	public final void pushNonPersistentUpdates(T inPerstitentObject) {
		privateBroadcastUpdate(inPerstitentObject);
	}

	// --------------------------------------------------------------------------
	/**
	 * The cache map holds instances of the object, so that EBean doesn't replace them.
	 * The GUI is not stateless (in some cases), so we can't deal with new instances.
	 * If it were a straight-up webapp this wouldn't be a problem, but a desktop UI contains obj refs.
	 */
	//	protected void initCacheMap() {
	//		Query<T> query = Ebean.createQuery(mClass);
	//		query = query.setUseCache(true);
	//		Collection<T> daoObjects = query.findList();
	//		mCacheMap = new HashMap<Long, T>();
	//		for (T daoObject : daoObjects) {
	//			mCacheMap.put(daoObject.getPersistentId(), daoObject);
	//		}
	//	}

	// --------------------------------------------------------------------------
	/**
	 * @param inDomainObject
	 * @return
	 */
	public final boolean isObjectPersisted(PersistABC inDomainObject) {
		boolean result = false;

		BeanState state = Ebean.getBeanState(inDomainObject);
		// If there is a bean state and it's not new then this object was once persisted.
		if ((state != null) && (!state.isNew())) {
			result = true;
		}

		return result;
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.model.dao.IGenericDao#loadByPersistentId(java.lang.Integer)
	 */
	public final T loadByPersistentId(Long inID) {
		//		if (!USE_DAO_CACHE) {
		T result = null;
		try {
			result = Ebean.find(mClass, inID);
		} catch (PersistenceException e) {
			LOGGER.error("", e);
		}
		return result;
		//		} else {
		//			if (mCacheMap == null) {
		//				initCacheMap();
		//			}
		//			return mCacheMap.get(inID);
		//		}
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.model.dao.IGenericDao#findById(java.lang.String)
	 */
	public final T findById(final String inId) {
		//		if (!USE_DAO_CACHE) {
		T result = null;
		try {
			Query<T> query = Ebean.createQuery(mClass);
			query.where().eq(T.getIdColumnName(), inId);
			//query = query.setUseCache(true);
			result = query.findUnique();
		} catch (PersistenceException e) {
			LOGGER.error("", e);
		}
		return result;
		//		} else {
		//			T result = null;
		//			if (mCacheMap == null) {
		//				initCacheMap();
		//			}
		//			for (T daoObject : mCacheMap.values()) {
		//				if (daoObject.getId().equals(inId)) {
		//					result = daoObject;
		//				}
		//			}
		//			return result;
		//		}
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.model.dao.IGenericDao#store(java.lang.Object)
	 */
	public final void store(final T inDomainObject) throws DaoException {
		if (inDomainObject.getPersistentId() == null) {
			Ebean.save(inDomainObject);
			privateBroadcastAdd(inDomainObject);
		} else {
			Ebean.save(inDomainObject);
			privateBroadcastUpdate(inDomainObject);
		}
		//		if (USE_DAO_CACHE) {
		//			if (mCacheMap == null) {
		//				initCacheMap();
		//			}
		//			mCacheMap.put(inDomainObject.getPersistentId(), inDomainObject);
		//		}
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.model.dao.IGenericDao#delete(java.lang.Object)
	 */
	public final void delete(final T inDomainObject) throws DaoException {
		//		if (USE_DAO_CACHE) {
		//			if (mCacheMap == null) {
		//				initCacheMap();
		//			}
		//			mCacheMap.remove(inDomainObject.getPersistentId());
		//		}
		Ebean.delete(inDomainObject);
		privateBroadcastDelete(inDomainObject);
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.model.dao.IGenericDao#getAll()
	 */
	public final Collection<T> getAll() {
		//		if (!USE_DAO_CACHE) {
		Query<T> query = Ebean.createQuery(mClass);
		query = query.setUseCache(true);
		return query.findList();
		//		} else {
		//			if (mCacheMap == null) {
		//				initCacheMap();
		//			}
		//			// Use the accounts cache.
		//			return mCacheMap.values();
		//		}
	}

	/*
	 * --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * 
	 * @see com.gadgetworks.codeshelf.model.dao.ISystemDAO#registerDAOListener(com.gadgetworks.codeshelf.model.dao.IDAOListener)
	 */
	public final void registerDAOListener(IDaoListener inListener) {
		mListeners.add(inListener);
	}

	/*
	 * --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * 
	 * @see com.gadgetworks.codeshelf.model.dao.ISystemDAO#unRegisterDAOListener(com.gadgetworks.codeshelf.model.dao.IDAOListener)
	 */
	public final void unregisterDAOListener(IDaoListener inListener) {
		mListeners.remove(inListener);
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
