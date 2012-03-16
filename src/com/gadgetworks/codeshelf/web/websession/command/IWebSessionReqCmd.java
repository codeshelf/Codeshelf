/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: IWebSessionReqCmd.java,v 1.1 2012/03/16 15:59:07 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.web.websession.command;


/**
 * @author jeffw
 *
 */
public interface IWebSessionReqCmd extends IWebSessionCmd {

	WebSessionReqCmdEnum getCommandEnum();
	
	String getCommandId();
	
	void setCommandId(String inCommandId);
	
	IWebSessionRespCmd exec();
}
