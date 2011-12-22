/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: IGenericDao.java,v 1.1 2011/12/22 11:46:31 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.dao;

import java.util.Collection;

/**
 * @author jeffw
 *
 */
public interface IGenericDao <T> {
	
	T loadByPersistentId(Integer inID);

	T findById(String inId);

	void store(T domainObject);

	void delete(T domainObject);

	Collection<T> getAll();

}
