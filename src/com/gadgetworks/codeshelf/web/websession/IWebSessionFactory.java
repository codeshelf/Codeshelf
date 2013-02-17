/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2013, Jeffrey B. Williams, All rights reserved
 *  $Id: IWebSessionFactory.java,v 1.1 2013/02/17 04:22:21 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.web.websession;

import org.java_websocket.IWebSocket;

import com.gadgetworks.codeshelf.web.websession.command.req.IWebSessionReqCmdFactory;

/**
 * @author jeffw
 *
 */
public interface IWebSessionFactory {
	WebSession create(IWebSocket inWebSocket, IWebSessionReqCmdFactory inWebSessionReqCmdFactory);
}
