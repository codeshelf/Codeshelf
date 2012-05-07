/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: IWebSessionCmd.java,v 1.3 2012/05/07 06:34:27 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.web.websession.command;


/**
 * @author jeffw
 *
 */
public interface IWebSessionCmd {

	String	COMMAND_ID_ELEMENT		= "id";
	String	COMMAND_TYPE_ELEMENT	= "t";
	String	DATA_ELEMENT			= "d";

	String getCommandId();

}
