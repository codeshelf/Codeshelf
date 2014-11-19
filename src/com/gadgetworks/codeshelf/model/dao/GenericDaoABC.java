/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: GenericDaoABC.java,v 1.25 2013/07/22 04:30:37 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.dao;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;

import javax.persistence.OptimisticLockException;
import javax.persistence.PersistenceException;

import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gadgetworks.codeshelf.model.domain.Facility;
import com.gadgetworks.codeshelf.model.domain.IDomainObject;
import com.gadgetworks.codeshelf.platform.persistence.PersistenceService;
import com.google.inject.Inject;

/**
 * @author jeffw
 *
 */
@SuppressWarnings("unchecked")
public abstract class GenericDaoABC<T extends IDomainObject> implements ITypedDao<T> {

	private static final Logger LOGGER = LoggerFactory.getLogger(GenericDaoABC.class);

	private LinkedBlockingQueue<IDaoListener>	mListeners	= new LinkedBlockingQueue<IDaoListener>();
	
	PersistenceService persistenceService;

	@Inject
	public GenericDaoABC(PersistenceService persistenceService) {
		this.persistenceService = persistenceService;
	}
	
	protected Session getCurrentSession() {
		Session session = persistenceService.getCurrentTenantSession(); 
		return session;
	}

	// --------------------------------------------------------------------------

	/**
	 * @param inDomainObject
	 */
	@Override
	public void broadcastAdd(final IDomainObject inDomainObject) {
		for (IDaoListener daoListener : mListeners) {
			daoListener.objectAdded(inDomainObject);
		}
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inDomainObject
	 */
	@Override
	public void broadcastUpdate(final IDomainObject inDomainObject, Set<String> inChangedProperties) {
		for (IDaoListener daoListener : mListeners) {
			daoListener.objectUpdated(inDomainObject, inChangedProperties);
		}
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inDomainObject
	 */
	@Override
	public void broadcastDelete(final IDomainObject inDomainObject) {
		for (IDaoListener daoListener : mListeners) {
			daoListener.objectDeleted(inDomainObject);
		}
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

	public final T findByPersistentId(UUID inPersistentId) {
		T result = null;
		try {
			Session session = getCurrentSession();
			result = (T) session.get(getDaoClass(), inPersistentId);
		} 
		catch (Exception e) {
			LOGGER.error("Failed to find object with persistent ID "+inPersistentId, e);
		}
		return result;
	}

	public final T findByPersistentId(String inPersistentIdString) {
		UUID inPersistentId = UUID.fromString(inPersistentIdString);
		return this.findByPersistentId(inPersistentId);
	}
	
	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.model.dao.IGenericDao#loadByPersistentId(java.lang.Integer)
	 */
	public final <P extends IDomainObject> P findByPersistentId(Class<P> inClass, UUID inPersistentId) {
		P result = null;
		try {
			Session session = getCurrentSession();
			result = (P) session.get(inClass, inPersistentId);
		} catch (PersistenceException e) {
			LOGGER.error("Failed to retrieve object of type "+getDaoClass().getSimpleName()+" with ID "+inPersistentId, e);
		}
		return result;
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.model.dao.IGenericDao#findById(java.lang.String)
	 */
	public final T findByDomainId(final IDomainObject parentObject, final String domainId) {
		String effectiveId = domainId;
		try {
			Session session = getCurrentSession();
			
			// Query<T> query = mServer.createQuery(getDaoClass());
	        Criteria criteria = session.createCriteria(getDaoClass());
			if (parentObject != null) {
				Class<T> clazz = getDaoClass();
				if (clazz.equals(Facility.class)) {
					// This is a bit odd: the Facility is the top-level Location object, but Ebean doesn't allow us to have a parent field that points to another table.
					// (It *should* be able to do this since Ebean knows the class type at runtime, but it just doesn't.)
					criteria
						.add(Restrictions.eq(IDomainObject.ID_PROPERTY,effectiveId))
						.add(Restrictions.eq(IDomainObject.PARENT_ORG_PROPERTY,parentObject.getPersistentId()));
					// query.where().eq(IDomainObject.ID_PROPERTY, effectiveId).eq(IDomainObjectTree.PARENT_ORG_PROPERTY, inParentObject.getPersistentId());
				} 
				else {
					criteria
						.add(Restrictions.eq(IDomainObject.ID_PROPERTY,effectiveId))
						.add(Restrictions.eq(IDomainObject.PARENT_PROPERTY,parentObject.getPersistentId()));
					// query.where().eq(IDomainObject.ID_PROPERTY, effectiveId).eq(IDomainObjectTree.PARENT_PROPERTY, inParentObject.getPersistentId());
				}
			} 
			else {
				criteria.add(Restrictions.eq(IDomainObject.ID_PROPERTY,effectiveId));
				// query.where().eq(IDomainObject.ID_PROPERTY, effectiveId);
			}
			//query = query.setUseCache(true);
			List<T> results = criteria.list();
			if (results.size()==0) {
				return null;
			}
			else if (results.size()==1) {
				return results.get(0);
			}
			else {
				LOGGER.error(results.size()+" objects found matching domain ID "+domainId);
				return null;
			}
		} catch (PersistenceException e) {
			LOGGER.error("Failed to find object by domain ID", e);
			return null;
		}
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.model.dao.IGenericDao#findByIdList(java.util.List)
	 */
	public final List<T> findByPersistentIdList(List<UUID> inIdList) {
		Session session = getCurrentSession();
        Criteria criteria = session.createCriteria(getDaoClass());
        criteria.add(Restrictions.in("persistentId", inIdList));
        List<T> methodResultsList = (List<T>) criteria.list();
		//Query<T> query = mServer.find(getDaoClass());
		//List<T> methodResultsList = query.where().in("persistentId", inIdList).findList();
		return methodResultsList;
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.model.dao.IGenericDao#findByIdList(java.util.List)
	public final List<T> findByFilter(Map<String, Object> inFilterParams) {
		// If we have a valid filter then get the filtered objects.
		Session session = getCurrentSession();
        Criteria criteria = session.createCriteria(getDaoClass());
		for (Entry<String, Object> param : inFilterParams.entrySet()) {
			criteria.add(Restrictions.eq(param.getKey(), param.getValue()));
		}
		List<T> results = criteria.list();
		return results;
	}*/
	public final List<T> findByFilter(List<Criterion> inFilter) {
		// If we have a valid filter then get the filtered objects.
		Session session = getCurrentSession();
        Criteria criteria = session.createCriteria(getDaoClass());
		for (Criterion expression : inFilter) {
			criteria.add(expression);
		}
		List<T> results = criteria.list();
		return results;
	}

	/*
	public final List<T> findByFilter(String clause,Map<String,Object> params) {
		Session session = getCurrentSession();
		String queryString = "from Che where "+clause;
		Query query = session.createQuery(queryString);
		for(Entry<String, Object> entry : params.entrySet()) {
			query.setParameter(entry.getKey(), UUID.fromString(entry.getValue().toString()) );
		}
		//query.setProperties(params);
		
		List<T> results = query.list();
		return results;
	}*/

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.model.dao.IGenericDao#findByIdList(java.util.List)
	 */
	public final List<T> findByFilterAndClass(String filter, Map<String, Object> params, Class<T> clazz) {
		Session session = getCurrentSession();
		String queryString = "from "+clazz.getSimpleName()+" where "+filter;
		Query query = session.createQuery(queryString);
		for(Entry<String, Object> entry : params.entrySet()) {
			query.setParameter(entry.getKey(), UUID.fromString(entry.getValue().toString()) );
		}		
		List<T> results = query.list();
		return results;
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.model.dao.IGenericDao#store(java.lang.Object)
	 */
	public final void store(final T inDomainObject) throws DaoException {
		// TODO: need to add change property intercepter and versioning
		Session session = getCurrentSession();
		session.saveOrUpdate(inDomainObject);
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.model.dao.IGenericDao#delete(java.lang.Object)
	 */
	public final void delete(final T inDomainObject) throws DaoException {
		try {
			Session session = getCurrentSession();
			session.delete(inDomainObject);
			// broadcastDelete(inDomainObject); done now via hibernate interceptors
		} catch (OptimisticLockException e) {
			LOGGER.error("Failed to delete object", e);
			throw new DaoException(e.getMessage());
		}
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.model.dao.IGenericDao#getAll()
	 */
	public List<T> getAll() {
		Session session = getCurrentSession();
        Criteria criteria = session.createCriteria(getDaoClass());
		List<T> results = criteria.list();
		return results;
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
		//return mServer.nextId(beanType);
	    UUID persistentId = UUID.randomUUID();
	    return persistentId;
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.model.dao.ITypedDao#beginTransaction()
	 */
	public final void beginTransaction() {
		this.persistenceService.beginTenantTransaction();
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.model.dao.ITypedDao#commitTransaction()
	 */
	//public final void commitTransaction() {
	//	this.persistenceService.endTenantTransaction();
	//}
	
	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.model.dao.ITypedDao#endTransaction()
	 */
	public final void endTransaction() {
		this.persistenceService.endTenantTransaction();
	}
	
	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.model.dao.ITypedDao#isNewOrDirty(com.gadgetworks.codeshelf.model.domain.IDomainObject)
	 */
	public void clearAllCaches() {
	}
}
