/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: IWebSessionManager.java,v 1.1 2013/03/17 19:19:13 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.ws;

import org.java_websocket.WebSocket;

/**
 * @author jeffw
 *
 */
public interface IWebSessionManager {

	void handleSessionOpen(WebSocket inWebSocket);

	void handleSessionClose(WebSocket inWebSocket);

	void handleSessionMessage(WebSocket inWebSocket, String inMessage);

}
