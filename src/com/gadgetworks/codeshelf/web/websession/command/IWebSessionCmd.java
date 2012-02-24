/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: IWebSessionCmd.java,v 1.1 2012/02/24 07:41:23 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.web.websession.command;

import org.codehaus.jackson.JsonNode;

/**
 * @author jeffw
 *
 */
public interface IWebSessionCmd {

	String	COMMAND_ID_ELEMENT		= "id";
	String	COMMAND_TYPE_ELEMENT	= "type";
	String	DATA_ELEMENT			= "data";

	WebSessionCmdEnum getCommandEnum();
	
	String getCommandId();
	
	void setCommandId(String inCommandId);
	
	IWebSessionCmd receive();
	
	String prepareToSend();
}
