/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: GenericDao.java,v 1.2 2011/12/29 09:15:35 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.dao;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.eclipse.swt.widgets.Display;

import com.avaje.ebean.BeanState;
import com.avaje.ebean.Ebean;
import com.avaje.ebean.Query;
import com.gadgetworks.codeshelf.model.persist.PersistABC;

/**
 * @author jeffw
 *
 */
public class GenericDao<T extends PersistABC> implements IGenericDao<T> {

	protected Map<Long, T>	mCacheMap;
	protected Class<T>		mClass;

	public GenericDao(final Class<T> inClass) {
		mClass = inClass;
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inObject
	 */
	private static void privateBroadcastAdd(final Object inObject) {
		Display display = Display.getDefault();
		display.asyncExec(new Runnable() {
			public void run() {
				DaoManager.gDaoManager.objectAdded(inObject);
			}
		});

	}

	// --------------------------------------------------------------------------
	/**
	 * @param inObject
	 */
	private static void privateBroadcastUpdate(final Object inObject) {
		Display display = Display.getDefault();
		display.asyncExec(new Runnable() {
			public void run() {
				DaoManager.gDaoManager.objectUpdated(inObject);
			}
		});
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inObject
	 */
	private static void privateBroadcastDelete(final Object inObject) {
		Display display = Display.getDefault();
		display.asyncExec(new Runnable() {
			public void run() {
				DaoManager.gDaoManager.objectDeleted(inObject);
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

	// --------------------------------------------------------------------------
	/**
	 * The cache map holds instances of the object, so that EBean doesn't replace them.
	 * The GUI is not stateless (in some cases), so we can't deal with new instances.
	 * If it were a straight-up webapp this wouldn't be a problem, but a desktop UI contains obj refs.
	 */
	protected void initCacheMap() {
		Query<T> query = Ebean.createQuery(mClass);
		query = query.setUseCache(true);
		Collection<T> daoObjects = query.findList();
		mCacheMap = new HashMap<Long, T>();
		for (T daoObject : daoObjects) {
			mCacheMap.put(daoObject.getPersistentId(), daoObject);
		}
	}

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
	public T loadByPersistentId(Long inID) {
		if (!USE_DAO_CACHE) {
			return Ebean.find(mClass, inID);
		} else {
			if (mCacheMap == null) {
				initCacheMap();
			}
			return mCacheMap.get(inID);
		}
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.model.dao.IGenericDao#findById(java.lang.String)
	 */
	public T findById(final String inId) {
		if (!USE_DAO_CACHE) {
			Query<T> query = Ebean.createQuery(mClass);
			query.where().eq(T.getIdColumnName(), inId);
			query = query.setUseCache(true);
			return query.findUnique();
		} else {
			T result = null;
			if (mCacheMap == null) {
				initCacheMap();
			}
			for (T daoObject : mCacheMap.values()) {
				if (daoObject.getId().equals(inId)) {
					result = daoObject;
				}
			}
			return result;
		}
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.model.dao.IGenericDao#store(java.lang.Object)
	 */
	public final void store(final T inDomainObject) throws DAOException {
		if (inDomainObject.getPersistentId() == null) {
			Ebean.save(inDomainObject);
			privateBroadcastAdd(inDomainObject);
		} else {
			Ebean.save(inDomainObject);
			privateBroadcastUpdate(inDomainObject);
		}
		if (USE_DAO_CACHE) {
			if (mCacheMap == null) {
				initCacheMap();
			}
			mCacheMap.put(inDomainObject.getPersistentId(), inDomainObject);
		}
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.model.dao.IGenericDao#delete(java.lang.Object)
	 */
	public final void delete(final T inDomainObject) throws DAOException {
		if (USE_DAO_CACHE) {
			if (mCacheMap == null) {
				initCacheMap();
			}
			mCacheMap.remove(inDomainObject.getPersistentId());
		}
		Ebean.delete(inDomainObject);
		privateBroadcastDelete(inDomainObject);
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.model.dao.IGenericDao#getAll()
	 */
	public final Collection<T> getAll() {
		if (!USE_DAO_CACHE) {
			Query<T> query = Ebean.createQuery(mClass);
			query = query.setUseCache(true);
			return query.findList();
		} else {
			if (mCacheMap == null) {
				initCacheMap();
			}
			// Use the accounts cache.
			return mCacheMap.values();
		}
	}
}
