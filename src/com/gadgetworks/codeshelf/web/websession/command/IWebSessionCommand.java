/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: IWebSessionCommand.java,v 1.6 2012/02/21 08:36:00 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.web.websession.command;

import org.codehaus.jackson.JsonNode;

/**
 * @author jeffw
 *
 */
public interface IWebSessionCommand {

	String	COMMAND_ID_ELEMENT		= "id";
	String	COMMAND_TYPE_ELEMENT	= "type";
	String	DATA_ELEMENT			= "data";

	WebSessionCommandEnum getCommandEnum();
	
	String getCommandId();
	
	void setCommandId(String inCommandId);
	
	IWebSessionCommand receive();
	
	String prepareToSend();
}
