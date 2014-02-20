/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2013, Jeffrey B. Williams, All rights reserved
 *  $Id: IWebSessionFactory.java,v 1.1 2013/03/17 19:19:13 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.ws;

import org.java_websocket.WebSocket;

import com.gadgetworks.codeshelf.ws.command.req.IWsReqCmdFactory;

/**
 * @author jeffw
 *
 */
public interface IWebSessionFactory {
	WebSession create(WebSocket inWebSocket, IWsReqCmdFactory inWebSessionReqCmdFactory);
}
