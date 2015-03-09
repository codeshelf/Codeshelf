/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: GenericDaoABC.java,v 1.25 2013/07/22 04:30:37 jeffw Exp $
 *******************************************************************************/
package com.codeshelf.model.dao;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import javax.persistence.OptimisticLockException;
import javax.persistence.PersistenceException;

import org.apache.commons.beanutils.ConversionException;
import org.apache.commons.beanutils.ConvertUtilsBean;
import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.QueryParameterException;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.IDomainObject;
import com.codeshelf.platform.persistence.TenantPersistenceService;
import com.codeshelf.util.ConverterProvider;
import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.google.inject.Inject;

/**
 * @author jeffw
 *
 */
@SuppressWarnings("unchecked")
public abstract class GenericDaoABC<T extends IDomainObject> implements ITypedDao<T> {

	private static final Logger LOGGER = LoggerFactory.getLogger(GenericDaoABC.class);

	private ConvertUtilsBean converter;

	@Inject
	public GenericDaoABC() {
		this.converter = new ConverterProvider().get();
	}
	
	protected Session getCurrentSession() {
		Session session = TenantPersistenceService.getInstance().getSession(); 
		return session;
	}

	public final T findByPersistentId(UUID inPersistentId) {
		T result = null;
		Session session = getCurrentSession();
		result = (T) session.get(getDaoClass(), inPersistentId);
		return result;
	}

	public final T findByPersistentId(String inPersistentIdString) {
		UUID inPersistentId = UUID.fromString(inPersistentIdString);
		return this.findByPersistentId(inPersistentId);
	}

	public final T reload(T domainObject) {
		if (domainObject==null) {
			return null;
		}
		T reloadedDomainObject = findByPersistentId(domainObject.getPersistentId());
		return reloadedDomainObject;
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.codeshelf.model.dao.IGenericDao#findById(java.lang.String)
	 */
	public T findByDomainId(final IDomainObject parentObject, final String domainId) {
		String effectiveId = domainId;
		try {
			Session session = getCurrentSession();
	        Criteria criteria = session.createCriteria(getDaoClass());
			if (parentObject != null) {
				Class<T> clazz = getDaoClass();
				if (clazz.equals(Facility.class)) {
					criteria
						.add(Restrictions.eq(IDomainObject.ID_PROPERTY,effectiveId))
						.add(Restrictions.eq(IDomainObject.PARENT_ORG_PROPERTY,parentObject.getPersistentId()));
				} 
				else {
					criteria
						.add(Restrictions.eq(IDomainObject.ID_PROPERTY,effectiveId))
						.add(Restrictions.eq(IDomainObject.PARENT_PROPERTY,parentObject.getPersistentId()));
				}
			} 
			else {
				criteria.add(Restrictions.eq(IDomainObject.ID_PROPERTY,effectiveId));
			}
			criteria.setCacheable(true);
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
	 * @see com.codeshelf.model.dao.IGenericDao#findByIdList(java.util.List)
	 */
	public final List<T> findByPersistentIdList(List<UUID> inIdList) {
		Session session = getCurrentSession();
        Criteria criteria = session.createCriteria(getDaoClass());
        criteria.add(Restrictions.in("persistentId", inIdList));
        List<T> methodResultsList = (List<T>) criteria.list();
		return methodResultsList;
	}

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
	
	@Override
	public boolean matchesFilter(String inCriteriaName, Map<String, Object> inArgs, UUID inPersistentId) {
		String parameterName = "persistentIdToMatch";
		TypedCriteria criteria = CriteriaRegistry.getInstance().findByName(inCriteriaName, this.getDaoClass());
		Preconditions.checkNotNull(criteria, "Unable to find filter criteria with name: %s" , inCriteriaName);
		TypedCriteria singleObjectCriteria = criteria.addEqualsRestriction("persistentId", parameterName, UUID.class);
		
		HashMap<String, Object> newArgs = Maps.newHashMap(inArgs);
		newArgs.put(parameterName, inPersistentId);
		return (findByCriteria(singleObjectCriteria, newArgs).isEmpty() == false);
	}
	
	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.codeshelf.model.dao.IGenericDao#findByIdList(java.util.List)
	 */
	public List<T> findByFilter(String inCriteriaName, Map<String, Object> inArgs) {
		// create criteria using look-up table
		TypedCriteria criteria = CriteriaRegistry.getInstance().findByName(inCriteriaName, this.getDaoClass());
		Preconditions.checkNotNull(criteria, "Unable to find filter criteria with name: %s" , inCriteriaName);
		return findByCriteria(criteria, inArgs);
	}

	protected List<T> findByCriteria(TypedCriteria criteria, Map<String, Object> inArgs) {
		Session session = getCurrentSession();
		Query query = session.createQuery(criteria.getQuery());
		for (Entry<String, Object> argument : inArgs.entrySet()) {
			String name = argument.getKey();
			Class<?> paramType = criteria.getParameterTypes().get(name);
			if (paramType == null) {
				throw new IllegalArgumentException("no parameter named: " + name + " found for query: " + criteria.getQuery());
			}
			Object value = argument.getValue();
			Object convertedValue = converter.convert(value, paramType);
			if (convertedValue != null && !paramType.isAssignableFrom(convertedValue.getClass())) {
				throw new ConversionException("Failed to convert value : " + value + " to type: " + paramType);
			}
			try {
				query.setParameter(name, convertedValue);
			}
			catch(QueryParameterException e) {
				throw new QueryParameterException("argument could not be found in query: " + name, criteria.getQuery(), e);
			}
		}		
		List<T> results = query.list();
		return results;
	}
	
	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.codeshelf.model.dao.IGenericDao#store(java.lang.Object)
	 */
	public final void store(final IDomainObject inDomainObject) throws DaoException {
		validateClass(inDomainObject.getClass());

		Session session = getCurrentSession();
		session.saveOrUpdate(inDomainObject);
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.codeshelf.model.dao.IGenericDao#delete(java.lang.Object)
	 */
	public final void delete(final IDomainObject inDomainObject) throws DaoException {
		validateClass(inDomainObject.getClass());
		try {
			Session session = getCurrentSession();
			session.delete(inDomainObject);
		} catch (OptimisticLockException e) {
			LOGGER.error("Failed to delete object", e);
			throw new DaoException(e.getMessage());
		}
	}

	@Override
	public final Criteria createCriteria() {
		Session session = getCurrentSession();
		Criteria criteria = session.createCriteria(getDaoClass());
		return criteria;
	}
	
	@Override
	public List<T> findByCriteriaQuery(Criteria criteria) {
		List<T> results = criteria.list();
		return results;
	}
	
	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.codeshelf.model.dao.IGenericDao#getAll()
	 */
	public List<T> getAll() {
		Session session = getCurrentSession();
        Criteria criteria = session.createCriteria(getDaoClass());
        criteria.setCacheable(true);
		List<T> results = criteria.list();
		return results;
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.codeshelf.model.dao.ITypedDao#getNextId(java.lang.Class)
	 */
	public final Object getNextId(final Class<?> beanType) {
	    UUID persistentId = UUID.randomUUID();
	    return persistentId;
	}

	
	private void validateClass(Class<? extends IDomainObject> clazz) {
		Class<?> expectedClass = this.getDaoClass();
		if(!expectedClass.isAssignableFrom(clazz)) {
			throw new DaoException("unexpected class used with DAO - expected "
				+expectedClass.getSimpleName()+" but got "+clazz.getSimpleName());
		}
	}
}
