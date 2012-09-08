/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: IWebSession.java,v 1.3 2012/09/08 03:03:22 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.web.websession;

import com.gadgetworks.codeshelf.model.dao.IDaoListener;

/**
 * @author jeffw
 *
 */
public interface IWebSession extends IDaoListener {

	void processMessage(String inMessage);
	
	void endSession();
	
}
