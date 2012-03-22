/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: IGenericDao.java,v 1.6 2012/03/22 07:35:11 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.dao;

import java.util.Collection;
import java.util.List;

/**
 * @author jeffw
 *
 */
public interface IGenericDao<T> extends IDao {

//	boolean isObjectPersisted(T inDomainObject);

	T loadByPersistentId(Long inID);

	T findByDomainId(String inId);
	
	List<T> findByPersistentIdList(List<Long> inIdList);

	void store(T inDomainObject) throws DaoException;

	void delete(T inDomainObject) throws DaoException;

	Collection<T> getAll();
	
	void pushNonPersistentUpdates(T inDomainObject);

}
