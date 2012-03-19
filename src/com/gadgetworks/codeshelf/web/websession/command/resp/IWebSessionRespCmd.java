/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: IWebSessionRespCmd.java,v 1.1 2012/03/19 04:05:19 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.web.websession.command.resp;

import com.gadgetworks.codeshelf.web.websession.command.IWebSessionCmd;


/**
 * @author jeffw
 *
 */
public interface IWebSessionRespCmd extends IWebSessionCmd {

	WebSessionRespCmdEnum getCommandEnum();
	
	void setCommandId(String inCommandId);

	String getResponseMsg();

}
