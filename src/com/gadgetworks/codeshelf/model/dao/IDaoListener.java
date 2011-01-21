/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: IDaoListener.java,v 1.2 2011/01/21 01:12:12 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.dao;

// --------------------------------------------------------------------------
/**
 *  @author jeffw
 */
public interface IDAOListener {
	
	void objectAdded(Object inObject);
	
	void objectUpdated(Object inObject);
	
	void objectDeleted(Object inObject);

}
