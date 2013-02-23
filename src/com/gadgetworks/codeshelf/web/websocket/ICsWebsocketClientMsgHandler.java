/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2013, Jeffrey B. Williams, All rights reserved
 *  $Id: ICsWebsocketClientMsgHandler.java,v 1.1 2013/02/23 05:42:09 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.web.websocket;

import org.java_websocket.client.IWebSocketClient;


/**
 * @author jeffw
 *
 */
public interface ICsWebsocketClientMsgHandler {
	
	void start(IWebSocketClient inWebSocketClient);
	
	void stop();
	
	void handleMessage(final String inMessage);
}
