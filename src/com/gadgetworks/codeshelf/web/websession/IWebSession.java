/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: IWebSession.java,v 1.4 2012/10/10 22:15:19 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.web.websession;

import com.gadgetworks.codeshelf.model.dao.IDaoListener;
import com.gadgetworks.codeshelf.web.websession.command.resp.IWebSessionRespCmd;

/**
 * @author jeffw
 *
 */
public interface IWebSession extends IDaoListener {

	IWebSessionRespCmd processMessage(String inMessage);
	
	void sendCommand(IWebSessionRespCmd inCommand);
	
	void endSession();
	
}
