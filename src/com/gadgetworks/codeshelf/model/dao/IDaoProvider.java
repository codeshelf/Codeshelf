/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: IDaoProvider.java,v 1.2 2012/07/11 07:15:42 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.dao;

import com.gadgetworks.codeshelf.model.persist.PersistABC;

/**
 * @author jeffw
 *
 */
public interface IDaoProvider {

	<T extends PersistABC> ITypedDao<T> getDaoInstance(final Class<T> inDomainObjectClass);
	
}
