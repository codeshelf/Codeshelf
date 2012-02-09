/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: IWebSessionCommand.java,v 1.3 2012/02/09 07:29:23 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.web.websession.command;

import org.codehaus.jackson.JsonNode;

/**
 * @author jeffw
 *
 */
public interface IWebSessionCommand {

	String	SUCCEED		= "SUCCEED";
	String	FAIL		= "FAIL";
	String	NEED_LOGIN	= "NEED_LOGIN";

	WebSessionCommandEnum getCommandEnum();

	String exec();

}
