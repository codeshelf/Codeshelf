/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: IWebSessionReqCmd.java,v 1.3 2012/03/24 18:28:01 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.web.websession.command.req;

import com.gadgetworks.codeshelf.web.websession.IWebSession;
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
	
	IWebSessionRespCmd getResponseCmd();
	
	boolean doesPersist();
	
	void registerSessionWithDaos(IWebSession inWebSession);

	void unregisterSessionWithDaos(IWebSession inWebSession);

}
