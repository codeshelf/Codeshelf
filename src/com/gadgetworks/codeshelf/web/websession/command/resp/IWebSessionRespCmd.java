/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: IWebSessionRespCmd.java,v 1.3 2012/09/08 03:03:23 jeffw Exp $
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
