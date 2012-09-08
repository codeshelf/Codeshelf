/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: IDaoProvider.java,v 1.5 2012/09/08 03:03:24 jeffw Exp $
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
