/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: IDaoListener.java,v 1.7 2012/09/08 03:03:24 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.dao;

import java.util.Set;
import java.util.UUID;

import com.gadgetworks.codeshelf.model.domain.IDomainObject;

// --------------------------------------------------------------------------
/**
 *  @author jeffw
 */
public interface IDaoListener {
	
	void objectAdded(Class<? extends IDomainObject> domainClass, final UUID domainPersistentId);
	
	void objectUpdated(Class<? extends IDomainObject> domainClass, final UUID domainPersistentId, Set<String> inChangedProperties);
	
	void objectDeleted(Class<? extends IDomainObject> domainClass, final UUID domainPersistentId);

}
