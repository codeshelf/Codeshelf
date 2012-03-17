/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: IWebSessionManager.java,v 1.1 2012/03/17 09:07:02 jeffw Exp $
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
