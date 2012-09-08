/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: IWebSessionManager.java,v 1.2 2012/09/08 03:03:22 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.web.websession;

import com.gadgetworks.codeshelf.web.websocket.WebSocket;

/**
 * @author jeffw
 *
 */
public interface IWebSessionManager {

	void handleSessionOpen(WebSocket inWebSocket);

	void handleSessionClose(WebSocket inWebSocket);

	void handleSessionMessage(WebSocket inWebSocket, String inMessage);
	
}
