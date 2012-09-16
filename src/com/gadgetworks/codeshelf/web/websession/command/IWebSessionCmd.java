/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: IWebSessionCmd.java,v 1.5 2012/09/16 00:12:44 jeffw Exp $
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
