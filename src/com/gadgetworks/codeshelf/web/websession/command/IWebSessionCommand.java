/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: IWebSessionCommand.java,v 1.2 2012/02/07 08:17:59 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.web.websession.command;

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
