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

import com.codeshelf.manager.Tenant;
import com.codeshelf.model.domain.IDomainObject;

/**
 * @author jeffw
 *
 */
public interface ITypedDao<T> {

	T findByPersistentId(Tenant tenant,UUID inPersistentId);

	T findByPersistentId(Tenant tenant,String inPersistentIdAsString);
	
	T reload(Tenant tenant,T domainObject);

	T findByDomainId(Tenant tenant,IDomainObject inParentObject, String inDomainId);

	List<T> findByPersistentIdList(Tenant tenant,List<UUID> inPersistentIdList);

	List<T> findByFilter(Tenant tenant,List<Criterion> inFilter);

	List<T> findByFilter(Tenant tenant,String criteriaName, Map<String, Object> inFilterArgs);

	List<T> findByFilter(Tenant tenant,String criteriaName, Map<String, Object> inFilterArgs, int maxRecords);

	boolean matchesFilter(Tenant tenant,String criteriaName, Map<String, Object> inFilterArgs,
		UUID persistentId);

	
	// runtime type of object should be checked by implementation:
	void store(Tenant tenant,IDomainObject inDomainObject) throws DaoException; 
	void delete(Tenant tenant,IDomainObject inDomainObject) throws DaoException;

	List<T> getAll(Tenant tenant);

	Class<T> getDaoClass();

	Criteria createCriteria(Tenant tenant);
	List<T> findByCriteriaQuery(Criteria criteria);
	
}
