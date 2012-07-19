/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: IDaoListener.java,v 1.5 2012/07/19 06:11:33 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.dao;

import com.gadgetworks.codeshelf.model.domain.IDomainObject;

// --------------------------------------------------------------------------
/**
 *  @author jeffw
 */
public interface IDaoListener {
	
	void objectAdded(IDomainObject inObject);
	
	void objectUpdated(IDomainObject inObject);
	
	void objectDeleted(IDomainObject inObject);

}
