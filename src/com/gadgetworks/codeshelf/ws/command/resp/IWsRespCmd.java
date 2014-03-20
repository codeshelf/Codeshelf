/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: IWsRespCmd.java,v 1.1 2013/03/17 19:19:12 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.ws.command.resp;

import com.gadgetworks.codeshelf.ws.command.IWebSessionCmd;

/**
 * @author jeffw
 *
 */
public interface IWsRespCmd extends IWebSessionCmd {

	WsRespCmdEnum getCommandEnum();

	void setCommandId(String inCommandId);

	boolean isError();
	
	// --------------------------------------------------------------------------
	/**
	 * Call this method to get the JSON encoded response-message for the command.
	 * @return
	 */
	String getResponseMsg();

	
}
