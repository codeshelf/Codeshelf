/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: IWebSessionReqCmd.java,v 1.1 2012/03/19 04:05:19 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.web.websession.command.req;

import com.gadgetworks.codeshelf.web.websession.command.IWebSessionCmd;
import com.gadgetworks.codeshelf.web.websession.command.resp.IWebSessionRespCmd;


/**
 * @author jeffw
 *
 */
public interface IWebSessionReqCmd extends IWebSessionCmd {

	WebSessionReqCmdEnum getCommandEnum();
	
	String getCommandId();
	
	void setCommandId(String inCommandId);
	
	IWebSessionRespCmd exec();
	
	boolean doesPersist();
}
