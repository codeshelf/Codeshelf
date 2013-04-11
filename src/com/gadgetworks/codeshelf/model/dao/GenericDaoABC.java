/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: GenericDaoABC.java,v 1.24 2013/04/11 20:26:44 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.dao;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;

import javax.persistence.OptimisticLockException;
import javax.persistence.PersistenceException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.avaje.ebean.BeanState;
import com.avaje.ebean.Ebean;
import com.avaje.ebean.EbeanServer;
import com.avaje.ebean.Query;
import com.avaje.ebean.bean.EntityBean;
import com.gadgetworks.codeshelf.model.domain.Facility;
import com.gadgetworks.codeshelf.model.domain.IDomainObject;
import com.gadgetworks.codeshelf.model.domain.IDomainObjectTree;
import com.google.inject.Inject;

/**
 * @author jeffw
 *
 */
public abstract class GenericDaoABC<T extends IDomainObject> implements ITypedDao<T> {

	private static final Logger					LOGGER		= LoggerFactory.getLogger(GenericDaoABC.class);

	private LinkedBlockingQueue<IDaoListener>	mListeners	= new LinkedBlockingQueue<IDaoListener>();
	private EbeanServer							mServer;

	@Inject
	public GenericDaoABC(final ISchemaManager inSchemaManager) {
		// This should be the only singleton for Ebean.
		// It's a bummer that the singleton exists at all, and that it shows up in a constructor.
		// This causes Guice to not be as good for us.
		// In the future we should work with the Ebean folks to support DI-style programming.  (Or fork Ebean and do it ourselves.)
		mServer = Ebean.getServer(inSchemaManager.getDbSchemaName());
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

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.model.dao.IGenericDao#loadByPersistentId(java.lang.Integer)
	 */
	public final T findByPersistentId(UUID inPersistentId) {
		T result = null;
		try {
			result = mServer.find(getDaoClass(), inPersistentId);
		} catch (PersistenceException e) {
			LOGGER.error("", e);
		}
		return result;
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.model.dao.IGenericDao#loadByPersistentId(java.lang.Integer)
	 */
	public final <P extends IDomainObject> P findByPersistentId(Class<P> inClass, UUID inPersistentId) {
		P result = null;
		try {
			result = mServer.find(inClass, inPersistentId);
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
			Query<T> query = mServer.createQuery(getDaoClass());
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
	public final List<T> findByPersistentIdList(List<UUID> inIdList) {
		Query<T> query = mServer.find(getDaoClass());
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
			Query<T> query = mServer.find(getDaoClass());
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
			Query<L> query = mServer.find(inClass);
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
		EntityBean bean = (EntityBean) inDomainObject;
		Set<String> changedProps = bean._ebean_getIntercept().getChangedProps();
		Map<String, Object> changedValues = new HashMap<String, Object>();
		try {
			if (inDomainObject.getPersistentId() == null) {
				mServer.save(inDomainObject);
				privateBroadcastAdd(inDomainObject);
			} else {
				if (changedProps != null) {
					for (String propName : changedProps) {
						if (!propName.equals("version")) {
							changedValues.put(propName, inDomainObject.getFieldValueByName(propName));
						}
					}
				}
				mServer.save(inDomainObject);
				privateBroadcastUpdate(inDomainObject, changedProps);
			}
		} catch (OptimisticLockException e) {
			// We tried to save the object, but the DB version was later than ours.
			// We saved the old, changed values above, so refresh this object, restore the values and try to save again.
			mServer.refresh(inDomainObject);
			// Restore the changed props into the saved object and re-save it.
			if (changedValues.size() > 0) {
				for (Entry<String, Object> entry : changedValues.entrySet()) {
					inDomainObject.setFieldValueByName(entry.getKey(), entry.getValue());
				}
				try {
					// Now cause the object to seem dirty/stale, so that it gets saved.
					//inDomainObject.setVersion(inDomainObject.getVersion());
					mServer.save(inDomainObject);
				} catch (OptimisticLockException e1) {
					// If there is another error, well, that will just go up to the application to deal with it.
					LOGGER.error("", e1);
					throw new DaoException("Couldn't recover from optimistic lock exception.");
				}
			}
		}
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.model.dao.IGenericDao#delete(java.lang.Object)
	 */
	public final void delete(final T inDomainObject) throws DaoException {
		try {
			mServer.delete(inDomainObject);
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
		Query<T> query = mServer.createQuery(getDaoClass());
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

		return mServer.nextId(beanType);
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.model.dao.ITypedDao#beginTransaction()
	 */
	public final void beginTransaction() {
		mServer.beginTransaction();
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.model.dao.ITypedDao#commitTransaction()
	 */
	public final void commitTransaction() {
		mServer.commitTransaction();
	}
	
	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.model.dao.ITypedDao#endTransaction()
	 */
	public final void endTransaction() {
		mServer.endTransaction();
	}
	
	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.model.dao.ITypedDao#isNewOrDirty(com.gadgetworks.codeshelf.model.domain.IDomainObject)
	 */
	public Boolean isNewOrDirty(IDomainObject inDomainObject) {
		Boolean result = false;
		
		BeanState beanState = mServer.getBeanState(inDomainObject);
		
		if (beanState != null) {
			result = beanState.isNewOrDirty();
		}
		
		return result;
	}
}
