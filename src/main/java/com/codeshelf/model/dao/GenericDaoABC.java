/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: GenericDaoABC.java,v 1.25 2013/07/22 04:30:37 jeffw Exp $
 *******************************************************************************/
package com.codeshelf.model.dao;

import java.sql.Timestamp;
import java.util.Collections;
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
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Property;
import org.hibernate.criterion.Restrictions;
import org.hibernate.criterion.SimpleExpression;
import org.joda.time.Interval;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.IDomainObject;
import com.codeshelf.persistence.TenantPersistenceService;
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

	private static final Logger	LOGGER	= LoggerFactory.getLogger(GenericDaoABC.class);

	//	private static final Integer NO_MAX_RECORDS	= null; // indicates no maximum records

	private ConvertUtilsBean	converter;

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
		if (domainObject == null) {
			return null;
		}
		T reloadedDomainObject = findByPersistentId(domainObject.getPersistentId());
		return reloadedDomainObject;
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.codeshelf.model.dao.IGenericDao#findById(java.lang.String)
	 */
	public List<T> findByParent(final IDomainObject parentObject) {
		try {
			Session session = getCurrentSession();
			Criteria criteria = session.createCriteria(getDaoClass());
			if (parentObject != null) {
				Class<T> clazz = getDaoClass();
				if (clazz.equals(Facility.class)) {
					criteria.add(Restrictions.eq(IDomainObject.PARENT_ORG_PROPERTY, parentObject.getPersistentId()));
				} else {
					criteria.add(Restrictions.eq(IDomainObject.PARENT_PROPERTY, parentObject.getPersistentId()));
				}
			}
			criteria.setCacheable(true);
			List<T> results = criteria.list();
			return results;
		} catch (PersistenceException e) {
			LOGGER.error("Failed to find object by domain ID", e);
			return null;
		}
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
					criteria.add(Restrictions.eq(IDomainObject.ID_PROPERTY, effectiveId))
						.add(Restrictions.eq(IDomainObject.PARENT_ORG_PROPERTY, parentObject.getPersistentId()));
				} else {
					criteria.add(Restrictions.eq(IDomainObject.ID_PROPERTY, effectiveId))
						.add(Restrictions.eq(IDomainObject.PARENT_PROPERTY, parentObject.getPersistentId()));
				}
			} else {
				criteria.add(Restrictions.eq(IDomainObject.ID_PROPERTY, effectiveId));
			}
			criteria.setCacheable(true);
			List<T> results = criteria.list();
			if (results.size() == 0) {
				return null;
			} else if (results.size() == 1) {
				return results.get(0);
			} else {
				LOGGER.error(results.size() + " objects found matching domain ID " + domainId);
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
		if (inIdList != null && inIdList.isEmpty()) {
			return Collections.<T> emptyList(); //empty WHERE X IN () causes syntax issue in postgres
		} else {
			Session session = getCurrentSession();
			Criteria criteria = session.createCriteria(getDaoClass());
			criteria.add(Restrictions.in("persistentId", inIdList)); // empty .in() guard present
			List<T> methodResultsList = (List<T>) criteria.list();
			return methodResultsList;
		}
	}

	/**
	 * This may optimize a common need. Better than fetching an hydrating all then checking parent
	 */
	public final List<T> findByParentPersistentIdList(List<UUID> inIdList) {
		if (inIdList != null && inIdList.isEmpty()) {
			return Collections.<T> emptyList(); //empty WHERE X IN () causes syntax issue in postgres
		} else {
			Session session = getCurrentSession();
			Criteria criteria = session.createCriteria(getDaoClass());
			criteria.add(Restrictions.in("parent.persistentId", inIdList)); // empty .in() guard present
			List<T> methodResultsList = (List<T>) criteria.list();
			return methodResultsList;
		}
	}

	public final List<T> findByFilter(List<? extends Criterion> inFilter) {
		// If we have a valid filter then get the filtered objects.
		Session session = getCurrentSession();
		Criteria criteria = session.createCriteria(getDaoClass());
		for (Criterion expression : inFilter) {
			criteria.add(expression);
		}
		List<T> results = criteria.list();
		return results;
	}

	public final List<T> findByFilter(List<? extends Criterion> inFilter, List<Order> inOrderBys) {
		// If we have a valid filter then get the filtered objects.
		Session session = getCurrentSession();
		Criteria criteria = session.createCriteria(getDaoClass());
		for (Criterion expression : inFilter) {
			criteria.add(expression);
		}
		for (Order order : inOrderBys) {
			criteria.addOrder(order);
		}
		List<T> results = criteria.list();
		return results;
	}

	@Override
	public boolean matchesFilter(String inCriteriaName, Map<String, Object> inArgs, UUID inPersistentId) {
		String parameterName = "persistentIdToMatch";
		TypedCriteria criteria = CriteriaRegistry.getInstance().findByName(inCriteriaName, this.getDaoClass());
		Preconditions.checkNotNull(criteria, "Unable to find filter criteria with name: %s", inCriteriaName);
		TypedCriteria singleObjectCriteria = criteria.addEqualsRestriction("persistentId", parameterName, UUID.class);

		HashMap<String, Object> newArgs = Maps.newHashMap(inArgs);
		newArgs.put(parameterName, inPersistentId);
		return (findByCriteria(singleObjectCriteria, newArgs, criteria.getMaxRecords()).isEmpty() == false);
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.codeshelf.model.dao.IGenericDao#findByIdList(java.util.List)
	 */
	public List<T> findByFilter(String inCriteriaName, Map<String, Object> inArgs) {
		// create criteria using look-up table
		TypedCriteria criteria = CriteriaRegistry.getInstance().findByName(inCriteriaName, this.getDaoClass());
		Preconditions.checkNotNull(criteria, "Unable to find filter criteria with name: %s", inCriteriaName);
		return findByCriteria(criteria, inArgs, criteria.getMaxRecords());
	}

	protected List<T> findByCriteria(TypedCriteria criteria, Map<String, Object> inArgs, Integer maxRecords) {
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
			} catch (QueryParameterException e) {
				throw new QueryParameterException("argument could not be found in query: " + name, criteria.getQuery(), e);
			}
		}
		if (maxRecords != null) {
			;
			List<T> results = query.setMaxResults(maxRecords).list();
			if (results.size() == maxRecords) {
				LOGGER.warn("Filter {} for {} reached max filter records: {}", criteria, inArgs, maxRecords);
			}
			return results;
		} else {
			List<T> results = query.list();
			return results;
		}
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

	@Override
	public final int countByCriteriaQuery(Criteria criteria) {
		criteria.setProjection(Projections.rowCount());
		Number value = (Number) criteria.uniqueResult();
		return value.intValue();
	}

	@Override
	public final int countByFilter(List<Criterion> inFilter) {
		Session session = getCurrentSession();
		Criteria criteria = session.createCriteria(getDaoClass());
		for (Criterion expression : inFilter) {
			criteria.add(expression);
		}
		criteria.setProjection(Projections.rowCount());
		Number value = (Number) criteria.uniqueResult();
		return value.intValue();
	}

	@Override
	public List<UUID> getUUIDListByCriteriaQuery(Criteria criteria) {

		criteria.setProjection(Projections.projectionList().add(Projections.property("persistentId"), "persistentId"));

		List<UUID> ids = criteria.list();
		return ids;
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
		if (!expectedClass.isAssignableFrom(clazz)) {
			throw new DaoException("unexpected class used with DAO - expected " + expectedClass.getSimpleName() + " but got "
					+ clazz.getSimpleName());
		}
	}

	//side effect of changing the query temporarily since cloning isn't really supported
	public static long countCriteria(Criteria criteria) {
		//Turn into a count query
		Criteria countCriteria = criteria.setProjection(Projections.rowCount());
		Long total = (Long) countCriteria.uniqueResult();

		//Turn back into entity query
		criteria.setProjection(null);
		criteria.setResultTransformer(Criteria.ROOT_ENTITY);
		return total;
	}

	public static SimpleExpression createSubstringRestriction(String propertyName, String substring) {
		SimpleExpression property = null;
		if (substring != null && substring.indexOf('*') >= 0) {
			property = Property.forName(propertyName).like(substring.replace('*', '%'));
		} else {
			property = Property.forName(propertyName).eq(substring);
		}
		return property;
	}

	public static Criterion createIntervalRestriction(String propertyName, Interval interval) {
		return Property.forName(propertyName).between(new Timestamp(interval.getStartMillis()),
			new Timestamp(interval.getEndMillis()));
	}
}
