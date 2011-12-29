/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: IGenericDao.java,v 1.2 2011/12/29 09:15:35 jeffw Exp $
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

	void store(T inDomainObject) throws DAOException;

	void delete(T inDomainObject) throws DAOException;

	Collection<T> getAll();

}
