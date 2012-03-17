/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: IGenericDao.java,v 1.4 2012/03/17 23:49:23 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.dao;

import java.util.Collection;

/**
 * @author jeffw
 *
 */
public interface IGenericDao<T> extends IDao {

	boolean isObjectPersisted(T inDomainObject);

	T loadByPersistentId(Long inID);

	T findById(String inId);

	void store(T inDomainObject) throws DaoException;

	void delete(T inDomainObject) throws DaoException;

	Collection<T> getAll();
	
	void pushNonPersistentUpdates(T inDomainObject);

}
