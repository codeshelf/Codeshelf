/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: ITypedDao.java,v 1.14 2013/04/11 20:26:44 jeffw Exp $
 *******************************************************************************/
package com.codeshelf.model.dao;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.hibernate.Criteria;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Order;

import com.codeshelf.model.domain.IDomainObject;

/**
 * @author jeffw
 *
 */
public interface ITypedDao<T> {

	T findByPersistentId(UUID inPersistentId);

	T findByPersistentId(String inPersistentIdAsString);
	
	T reload(T domainObject);

	List<T> findByParent(IDomainObject inParentObject);

	T findByDomainId(IDomainObject inParentObject, String inDomainId);

	List<T> findByPersistentIdList(List<UUID> inPersistentIdList);
	List<T> findByParentPersistentIdList(List<UUID> inIdList);

	List<T> findByFilter(List<Criterion> inFilter);
	List<T> findByFilter(List<Criterion> inFilter, List<Order> inOrderBys);

	List<T> findByFilter(String criteriaName, Map<String, Object> inFilterArgs);

	boolean matchesFilter(String criteriaName, Map<String, Object> inFilterArgs,
		UUID persistentId);

	
	// runtime type of object should be checked by implementation:
	void store(IDomainObject inDomainObject) throws DaoException; 
	void delete(IDomainObject inDomainObject) throws DaoException;

	List<T> getAll();

	Class<T> getDaoClass();

	List<T> findByCriteriaQuery(Criteria criteria);
	
	int countByCriteriaQuery(Criteria criteria);
	int countByFilter(List<Criterion> inFilter);

	List<UUID> getUUIDListByCriteriaQuery(Criteria criteria);

	Criteria createCriteria();

	
}
