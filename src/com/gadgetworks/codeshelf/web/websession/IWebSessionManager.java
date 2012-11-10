/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: IWebSessionManager.java,v 1.3 2012/11/10 03:20:01 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.web.websession;

import org.java_websocket.IWebSocket;

/**
 * @author jeffw
 *
 */
public interface IWebSessionManager {

	void handleSessionOpen(IWebSocket inWebSocket);

	void handleSessionClose(IWebSocket inWebSocket);

	void handleSessionMessage(IWebSocket inWebSocket, String inMessage);
	
}
