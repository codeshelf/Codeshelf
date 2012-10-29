/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: ITypedDao.java,v 1.9 2012/10/29 02:59:27 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.dao;

import java.util.List;
import java.util.Map;

import com.gadgetworks.codeshelf.model.domain.IDomainObject;

/**
 * @author jeffw
 *
 */
public interface ITypedDao<T> extends IDao {

	//	boolean isObjectPersisted(T inDomainObject);

	//	Query<T> query();

	T findByPersistentId(Long inPersistentId);

	T findByDomainId(IDomainObject inParentObject, String inDomainId);

	List<T> findByPersistentIdList(List<Long> inPersistentIdList);

	List<T> findByFilter(String inFilter, Map<String, Object> inFilterParams);

	<L> List<L> findByFilterAndClass(String inFilter, Map<String, Object> inFilterParams, Class<L> inClass);

	void store(T inDomainObject) throws DaoException;

	void delete(T inDomainObject) throws DaoException;

	List<T> getAll();

	void pushNonPersistentUpdates(T inDomainObject);

	Class<T> getDaoClass();
}
