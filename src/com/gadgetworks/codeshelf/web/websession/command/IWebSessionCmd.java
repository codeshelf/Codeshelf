/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: IWebSessionCmd.java,v 1.2 2012/03/16 15:59:07 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.web.websession.command;


/**
 * @author jeffw
 *
 */
public interface IWebSessionCmd {

	String	COMMAND_ID_ELEMENT		= "id";
	String	COMMAND_TYPE_ELEMENT	= "type";
	String	DATA_ELEMENT			= "data";

	String getCommandId();

}
