/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: ITypedDao.java,v 1.2 2012/07/19 06:11:33 jeffw Exp $
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

	T findByPersistentId(Long inPersistentId);

	T findByDomainId(IDomainObject inParentObject, String inDomainId);

	List<T> findByPersistentIdList(List<Long> inPersistentIdList);

	List<T> findByFilter(String inFilter, Map<String, Object> inFilterParams);

	void store(T inDomainObject) throws DaoException;

	void delete(T inDomainObject) throws DaoException;

	List<T> getAll();

	void pushNonPersistentUpdates(T inDomainObject);

}
