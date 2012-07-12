/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: IDaoProvider.java,v 1.3 2012/07/12 08:18:06 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.dao;

import java.util.List;

import com.gadgetworks.codeshelf.model.persist.PersistABC;

/**
 * @author jeffw
 *
 */
public interface IDaoProvider {

	<T extends PersistABC> ITypedDao<T> getDaoInstance(final Class<T> inDomainObjectClass);

	<T extends PersistABC> List<ITypedDao<T>> getAllDaos();
	
}
