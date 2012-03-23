/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: GenericDao.java,v 1.11 2012/03/23 06:04:44 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.dao;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.persistence.PersistenceException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.Query;
import com.gadgetworks.codeshelf.model.persist.PersistABC;

/**
 * @author jeffw
 *
 */
public class GenericDao<T extends PersistABC> implements IGenericDao<T> {

	private static final Log	LOGGER		= LogFactory.getLog(GenericDao.class);

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
	 * @param inDomainObject
	 * @return
	 */
	//	public final boolean isObjectPersisted(PersistABC inDomainObject) {
	//		boolean result = false;
	//
	//		BeanState state = Ebean.getBeanState(inDomainObject);
	//		// If there is a bean state and it's not new then this object was once persisted.
	//		if ((state != null) && (!state.isNew())) {
	//			result = true;
	//		}
	//
	//		return result;
	//	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.model.dao.IGenericDao#loadByPersistentId(java.lang.Integer)
	 */
	public final T loadByPersistentId(Long inPersistentId) {
		T result = null;
		try {
			result = Ebean.find(mClass, inPersistentId);
		} catch (PersistenceException e) {
			LOGGER.error("", e);
		}
		return result;
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.model.dao.IGenericDao#findById(java.lang.String)
	 */
	public final T findByDomainId(final PersistABC inParentObject, final String inId) {
		T result = null;
		
		String effectiveId;
		if (inParentObject != null) {
			effectiveId = inParentObject.getId() + "." + inId;
		} else {
			effectiveId = inId;
		}
		
		try {
			Query<T> query = Ebean.createQuery(mClass);
			query.where().eq(T.getIdColumnName(), effectiveId);
			//query = query.setUseCache(true);
			result = query.findUnique();
		} catch (PersistenceException e) {
			LOGGER.error("", e);
		}
		return result;
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.model.dao.IGenericDao#findByIdList(java.util.List)
	 */
	public final List<T> findByPersistentIdList(List<Long> inIdList) {
		Query<T> query = Ebean.find(mClass);
		List<T> methodResultsList = query.where().in("persistentId", inIdList).findList();
		return methodResultsList;
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
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.model.dao.IGenericDao#delete(java.lang.Object)
	 */
	public final void delete(final T inDomainObject) throws DaoException {
		Ebean.delete(inDomainObject);
		privateBroadcastDelete(inDomainObject);
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.model.dao.IGenericDao#getAll()
	 */
	public final Collection<T> getAll() {
		Query<T> query = Ebean.createQuery(mClass);
		query = query.setUseCache(true);
		return query.findList();
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
