/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: IGenericDao.java,v 1.3 2012/03/17 09:07:02 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.dao;

import java.util.Collection;
import java.util.UUID;

/**
 * @author jeffw
 *
 */
public interface IGenericDao<T> {

	Boolean	USE_DAO_CACHE	= true;

	boolean isObjectPersisted(T inDomainObject);

	T loadByPersistentId(Long inID);

	T findById(String inId);

	void store(T inDomainObject) throws DaoException;

	void delete(T inDomainObject) throws DaoException;

	Collection<T> getAll();

}
