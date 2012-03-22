/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: GenericDao.java,v 1.9 2012/03/22 07:35:11 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.dao;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.gadgetworks.codeshelf.model.persist.PersistABC;

/**
 * @author jeffw
 *
 */
public class GenericDao<T extends PersistABC> implements IGenericDao<T> {

	//private static final Log	LOGGER		= LogFactory.getLog(GenericDao.class);

	private Class<T>			mClass;
	private IDbFacade<T>		mDbFacade;

	private List<IDaoListener>	mListeners	= new ArrayList<IDaoListener>();

	public GenericDao(final Class<T> inClass, final IDaoRegistry inDaoRegistry, final IDbFacade<T> inDbFacade) {
		mClass = inClass;
		mDbFacade = inDbFacade;
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
	public final T loadByPersistentId(Long inID) {
		return mDbFacade.findByPersistentId(mClass, inID);
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.model.dao.IGenericDao#findById(java.lang.String)
	 */
	public final T findByDomainId(final String inId) {
		return mDbFacade.findByDomainId(mClass, inId);
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.model.dao.IGenericDao#findByIdList(java.util.List)
	 */
	public List<T> findByPersistentIdList(List<Long> inIdList) {
		return mDbFacade.findByPersistentIdList(mClass, inIdList);
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.model.dao.IGenericDao#store(java.lang.Object)
	 */
	public final void store(final T inDomainObject) throws DaoException {
		if (inDomainObject.getPersistentId() == null) {
			mDbFacade.save(inDomainObject);
			privateBroadcastAdd(inDomainObject);
		} else {
			mDbFacade.save(inDomainObject);
			privateBroadcastUpdate(inDomainObject);
		}
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.model.dao.IGenericDao#delete(java.lang.Object)
	 */
	public final void delete(final T inDomainObject) throws DaoException {
		mDbFacade.delete(inDomainObject);
		privateBroadcastDelete(inDomainObject);
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.model.dao.IGenericDao#getAll()
	 */
	public final Collection<T> getAll() {
		return mDbFacade.getAll(mClass);
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
