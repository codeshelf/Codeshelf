/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: IDaoListener.java,v 1.3 2012/03/17 09:07:02 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.dao;

// --------------------------------------------------------------------------
/**
 *  @author jeffw
 */
public interface IDaoListener {
	
	void objectAdded(Object inObject);
	
	void objectUpdated(Object inObject);
	
	void objectDeleted(Object inObject);

}
