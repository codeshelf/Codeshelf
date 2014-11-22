/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: ITypedDao.java,v 1.14 2013/04/11 20:26:44 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.dao;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.hibernate.criterion.Criterion;

import com.gadgetworks.codeshelf.model.domain.IDomainObject;

/**
 * @author jeffw
 *
 */
public interface ITypedDao<T> extends IDao {

	T findByPersistentId(UUID inPersistentId);

	T findByPersistentId(String inPersistentIdAsString);

	<P extends IDomainObject> P findByPersistentId(Class<P> inClass, UUID inPersistentId);

	T findByDomainId(IDomainObject inParentObject, String inDomainId);

	List<T> findByPersistentIdList(List<UUID> inPersistentIdList);

	List<T> findByFilter(List<Criterion> inFilter);

	List<T> findByFilterAndClass(String inFilter, Map<String, Object> inFilterParams, Class<T> inClass);

	void store(T inDomainObject) throws DaoException;

	void delete(T inDomainObject) throws DaoException;

	List<T> getAll();

	Class<T> getDaoClass();
	
	Object getNextId(final Class<?> beanType);
	
	// TODO: remove transaction methods from DAO layer
	void beginTransaction();
	void endTransaction();	
}
