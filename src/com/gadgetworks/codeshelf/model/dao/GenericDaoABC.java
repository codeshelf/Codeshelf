/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: GenericDaoABC.java,v 1.16 2012/12/22 09:36:38 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.dao;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.persistence.OptimisticLockException;
import javax.persistence.PersistenceException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.Query;
import com.avaje.ebean.bean.EntityBean;
import com.gadgetworks.codeshelf.model.domain.Facility;
import com.gadgetworks.codeshelf.model.domain.IDomainObject;
import com.gadgetworks.codeshelf.model.domain.IDomainObjectTree;
import com.gadgetworks.codeshelf.model.domain.Organization;

/**
 * @author jeffw
 *
 */
public abstract class GenericDaoABC<T extends IDomainObject> implements ITypedDao<T> {

	private static final Log	LOGGER		= LogFactory.getLog(GenericDaoABC.class);

	private List<IDaoListener>	mListeners	= new ArrayList<IDaoListener>();

	public GenericDaoABC() {
	}

	// --------------------------------------------------------------------------

	/**
	 * @param inDomainObject
	 */
	private void privateBroadcastAdd(final IDomainObject inDomainObject) {
		for (IDaoListener daoListener : mListeners) {
			daoListener.objectAdded(inDomainObject);
		}
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inDomainObject
	 */
	private void privateBroadcastUpdate(final IDomainObject inDomainObject, Set<String> inChangedProperties) {
		for (IDaoListener daoListener : mListeners) {
			daoListener.objectUpdated(inDomainObject, inChangedProperties);
		}
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inDomainObject
	 */
	private void privateBroadcastDelete(final IDomainObject inDomainObject) {
		for (IDaoListener daoListener : mListeners) {
			daoListener.objectDeleted(inDomainObject);
		}
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.model.dao.ISystemDAO#pushNonPersistentAccountUpdates(com.gadgetworks.codeshelf.model.domain.Account)
	 */
	public final void pushNonPersistentUpdates(T inPerstitentObject) {
		EntityBean bean = (EntityBean) inPerstitentObject;
		Set<String> changedProps = bean._ebean_getIntercept().getChangedProps();
		privateBroadcastUpdate(inPerstitentObject, changedProps);
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inDomainObject
	 * @return
	 */
	//	public final boolean isObjectPersisted(IDomainObject inDomainObject) {
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

	//	public final Query<T> query() {
	//
	//		Query<T> result = null;
	//
	//		result = Ebean.find(getDaoClass());
	//
	//		return result;
	//	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.model.dao.IGenericDao#loadByPersistentId(java.lang.Integer)
	 */
	public final T findByPersistentId(Long inPersistentId) {
		T result = null;
		try {
			result = Ebean.find(getDaoClass(), inPersistentId);
		} catch (PersistenceException e) {
			LOGGER.error("", e);
		}
		return result;
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.model.dao.IGenericDao#findById(java.lang.String)
	 */
	public final T findByDomainId(final IDomainObject inParentObject, final String inId) {
		T result = null;

		String effectiveId = inId;//.toUpperCase();
		try {
			Query<T> query = Ebean.createQuery(getDaoClass());
			if (inParentObject != null) {
				if (getDaoClass().equals(Facility.class)) {
					// This is a bit odd: the Facility is the top-level Location object, but Ebean doesn't allow us to have a parent field that points to another table.
					// (It *should* be able to do this since Ebean knows the class type at runtime, but it just doesn't.)
					query.where().eq(IDomainObject.ID_PROPERTY, effectiveId).eq(IDomainObjectTree.PARENT_ORG_PROPERTY, inParentObject.getPersistentId());
				} else {
					query.where().eq(IDomainObject.ID_PROPERTY, effectiveId).eq(IDomainObjectTree.PARENT_PROPERTY, inParentObject.getPersistentId());
				}
			} else {
				query.where().eq(IDomainObject.ID_PROPERTY, effectiveId);
			}
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
		Query<T> query = Ebean.find(getDaoClass());
		List<T> methodResultsList = query.where().in("persistentId", inIdList).findList();
		return methodResultsList;
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.model.dao.IGenericDao#findByIdList(java.util.List)
	 */
	public final List<T> findByFilter(String inFilter, Map<String, Object> inFilterParams) {
		if ((inFilter != null) && (inFilter.length() > 0)) {
			// If we have a valid filter then get the filtered objects.
			Query<T> query = Ebean.find(getDaoClass());
			query = query.where(inFilter);
			for (Entry<String, Object> param : inFilterParams.entrySet()) {
				query.setParameter(param.getKey(), param.getValue());
			}
			List<T> methodResultsList = query.findList();
			return methodResultsList;
		} else {
			// Otherwise get everything.
			return getAll();
		}
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.model.dao.IGenericDao#findByIdList(java.util.List)
	 */
	public final <L> List<L> findByFilterAndClass(String inFilter, Map<String, Object> inFilterParams, Class<L> inClass) {
		if ((inFilter != null) && (inFilter.length() > 0)) {
			// If we have a valid filter then get the filtered objects.
			Query<L> query = Ebean.find(inClass);
			query = query.where(inFilter);
			for (Entry<String, Object> param : inFilterParams.entrySet()) {
				query.setParameter(param.getKey(), param.getValue());
			}
			List<L> methodResultsList = query.findList();
			return methodResultsList;
		} else {
			return new ArrayList<L>();
		}
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.model.dao.IGenericDao#store(java.lang.Object)
	 */
	public final void store(final T inDomainObject) throws DaoException {
		try {
			if (inDomainObject.getPersistentId() == null) {
				Ebean.save(inDomainObject);
				privateBroadcastAdd(inDomainObject);
			} else {
				EntityBean bean = (EntityBean) inDomainObject;
				Set<String> changedProps = bean._ebean_getIntercept().getChangedProps();
				Ebean.save(inDomainObject);
				privateBroadcastUpdate(inDomainObject, changedProps);
			}
		} catch (OptimisticLockException e) {
			Ebean.refresh(inDomainObject);
			store(inDomainObject);
			LOGGER.error("", e);
			//throw new DaoException(e.getMessage());
		}
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.model.dao.IGenericDao#delete(java.lang.Object)
	 */
	public final void delete(final T inDomainObject) throws DaoException {
		try {
			Ebean.delete(inDomainObject);
			privateBroadcastDelete(inDomainObject);
		} catch (OptimisticLockException e) {
			LOGGER.error("", e);
			throw new DaoException(e.getMessage());
		}
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.model.dao.IGenericDao#getAll()
	 */
	public final List<T> getAll() {
		Query<T> query = Ebean.createQuery(getDaoClass());
		//query = query.setUseCache(true);
		return query.findList();
	}

	/*
	 * --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * 
	 * @see com.gadgetworks.codeshelf.model.dao.ISystemDAO#registerDAOListener(com.gadgetworks.codeshelf.model.dao.IDAOListener)
	 */
	public final void registerDAOListener(IDaoListener inListener) {
		if (!mListeners.contains(inListener)) {
			mListeners.add(inListener);
		}
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

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.model.dao.ITypedDao#getNextId(java.lang.Class)
	 */
	public final Object getNextId(final Class<?> beanType) {
		// An example of how to do this.
		//		Object nextId = Path.DAO.getNextId(Path.class);
		//		path.setPersistentId(new Long((Integer) nextId));

		return Ebean.nextId(beanType);
	}
}
