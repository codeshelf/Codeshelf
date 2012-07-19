/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: IDaoProvider.java,v 1.4 2012/07/19 06:11:33 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.dao;

import java.util.List;

import com.gadgetworks.codeshelf.model.domain.IDomainObject;

/**
 * @author jeffw
 *
 */
public interface IDaoProvider {

	<T extends IDomainObject> ITypedDao<T> getDaoInstance(final Class<T> inDomainObjectClass);

	<T extends IDomainObject> List<ITypedDao<T>> getAllDaos();
	
}
