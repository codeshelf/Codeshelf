/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: IWebSessionCommand.java,v 1.5 2012/02/21 02:45:11 jeffw Exp $
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

	String	SUCCEED		= "SUCCEED";
	String	FAIL		= "FAIL";
	String	NEED_LOGIN	= "NEED_LOGIN";

	WebSessionCommandEnum getCommandEnum();
	
	String getCommandId();
	
	IWebSessionCommand exec();
	
	String serialize();
}
