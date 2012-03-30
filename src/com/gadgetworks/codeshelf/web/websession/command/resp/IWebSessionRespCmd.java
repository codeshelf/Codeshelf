/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: IWebSessionRespCmd.java,v 1.2 2012/03/30 23:21:35 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.web.websession.command.resp;

import com.gadgetworks.codeshelf.web.websession.command.IWebSessionCmd;


/**
 * @author jeffw
 *
 */
public interface IWebSessionRespCmd extends IWebSessionCmd {

	WebSessionRespCmdEnum getCommandEnum();
	
	void setCommandId(String inCommandId);

	// --------------------------------------------------------------------------
	/**
	 * Call this method to get the JSON encoded response-message for the command.
	 * @return
	 */
	String getResponseMsg();
	
}
