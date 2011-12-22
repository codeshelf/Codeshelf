/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: GenericDao.java,v 1.1 2011/12/22 11:46:31 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.dao;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.swt.widgets.Display;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.Query;
import com.gadgetworks.codeshelf.model.persist.Aisle;
import com.gadgetworks.codeshelf.model.persist.PersistABC;

/**
 * @author jeffw
 *
 */
public class GenericDao<T extends PersistABC> implements IGenericDao<T> {

	private static List<IDAOListener>	mListeners		= new ArrayList<IDAOListener>();

	private Boolean						mUseDaoCache	= true;
	private Map<Integer, T>				mCacheMap;

	private Class<T>					mClass;

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
	private static void privateBroadcastUpdate(final Object inObject) {
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
	private static void privateBroadcastDelete(final Object inObject) {
		Display display = Display.getDefault();
		display.asyncExec(new Runnable() {
			public void run() {
				for (IDAOListener daoListener : mListeners) {
					daoListener.objectDeleted(inObject);
				}
			}
		});
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
	 * The cache map holds instances of the object, so that EBean doesn't replace them.
	 * The GUI is not stateless (in some cases), so we can't deal with new instances.
	 * If it were a straight-up webapp this wouldn't be a problem, but a desktop UI contains obj refs.
	 */
	private void initCacheMap() {
		Query<T> query = Ebean.createQuery(mClass);
		query = query.setUseCache(true);
		Collection<T> daoObjects = query.findList();
		mCacheMap = new HashMap<Integer, T>();
		for (T daoObject : daoObjects) {
			mCacheMap.put(daoObject.getPersistentId(), daoObject);
		}
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.model.dao.IGenericDao#loadByPersistentId(java.lang.Integer)
	 */
	public T loadByPersistentId(Integer inID) {
		if (!mUseDaoCache) {
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
		if (!mUseDaoCache) {
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
	public void store(final T inDomainObject) {
		if (inDomainObject.getPersistentId() == null) {
			Ebean.save(inDomainObject);
			privateBroadcastAdd(inDomainObject);
		} else {
			Ebean.save(inDomainObject);
			privateBroadcastUpdate(inDomainObject);
		}
		if (mUseDaoCache) {
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
	public void delete(final T inDomainObject) {
		if (mUseDaoCache) {
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
	public Collection<T> getAll() {
		if (!mUseDaoCache) {
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
