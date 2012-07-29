/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: IDaoListener.java,v 1.6 2012/07/29 09:30:19 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.dao;

import java.util.Set;

import com.gadgetworks.codeshelf.model.domain.IDomainObject;

// --------------------------------------------------------------------------
/**
 *  @author jeffw
 */
public interface IDaoListener {
	
	void objectAdded(IDomainObject inObject);
	
	void objectUpdated(IDomainObject inObject, Set<String> inChangedProperties);
	
	void objectDeleted(IDomainObject inObject);

}
