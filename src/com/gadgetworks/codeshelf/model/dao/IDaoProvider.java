/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: IDaoProvider.java,v 1.1 2012/03/22 06:21:47 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.dao;

import com.gadgetworks.codeshelf.model.persist.PersistABC;

/**
 * @author jeffw
 *
 */
public interface IDaoProvider {

	<T extends PersistABC> IGenericDao<T> getDaoInstance(final Class<T> inDomainObjectClass);
	
}
