/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: IGenericDao.java,v 1.9 2012/03/24 06:49:33 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.dao;

import java.util.Collection;
import java.util.List;

import com.gadgetworks.codeshelf.model.persist.PersistABC;

/**
 * @author jeffw
 *
 */
public interface IGenericDao<T> extends IDao {

	//	boolean isObjectPersisted(T inDomainObject);

	T findByPersistentId(Long inPersistentId);

	T findByDomainId(PersistABC inParentObject, String inDomainId);

	List<T> findByPersistentIdList(List<Long> inPersistentIdList);

	List<T> findByFilter(String inFilter);

	void store(T inDomainObject) throws DaoException;

	void delete(T inDomainObject) throws DaoException;

	Collection<T> getAll();

	void pushNonPersistentUpdates(T inDomainObject);

}
