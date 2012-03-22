/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: IDbFacade.java,v 1.2 2012/03/22 07:35:11 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.dao;

import java.util.Collection;
import java.util.List;

import com.gadgetworks.codeshelf.model.persist.PersistABC;

/**
 * @author jeffw
 *
 */
public interface IDbFacade<T extends PersistABC> {

	T findByPersistentId(Class<T> inClass, long inPersistentId);

	T findByDomainId(Class<T> inClass, String inId);
	
	List<T> findByPersistentIdList(Class<T> inClass, List<Long> inIdList);
	
	Collection<T> getAll(Class<T> inClass);
	
	void save(T inDomainObject);
	
	void delete(T inDomainObject);
	
}
