/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: IDaoListener.java,v 1.4 2012/03/30 23:21:35 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.dao;

import com.gadgetworks.codeshelf.model.persist.PersistABC;

// --------------------------------------------------------------------------
/**
 *  @author jeffw
 */
public interface IDaoListener {
	
	void objectAdded(PersistABC inObject);
	
	void objectUpdated(PersistABC inObject);
	
	void objectDeleted(PersistABC inObject);

}
