/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: IWebSession.java,v 1.1 2012/03/24 06:49:33 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.web.websession;

import com.gadgetworks.codeshelf.model.dao.IDaoListener;

/**
 * @author jeffw
 *
 */
public interface IWebSession extends IDaoListener {

	void processMessage(String inMessage);
	
}
