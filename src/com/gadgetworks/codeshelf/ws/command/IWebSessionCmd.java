/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: IWebSessionCmd.java,v 1.1 2013/03/17 19:19:13 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.ws.command;

/**
 * @author jeffw
 *
 */
public interface IWebSessionCmd {

	String	COMMAND_ID_ELEMENT		= "id";
	String	COMMAND_TYPE_ELEMENT	= "type";
	String	DATA_ELEMENT			= "data";
	String	STATUS_ELEMENT			= "status";

	
	String getCommandId();

}
