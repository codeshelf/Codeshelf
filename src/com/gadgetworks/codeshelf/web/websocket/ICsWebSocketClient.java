/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2013, Jeffrey B. Williams, All rights reserved
 *  $Id: ICsWebSocketClient.java,v 1.3 2013/03/04 18:10:25 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.web.websocket;

import org.java_websocket.client.IWebSocketClient;


/**
 * @author jeffw
 *
 */
public interface ICsWebSocketClient extends IWebSocketClient {
	
	void start();

	void stop();
	
	boolean isStarted();
	
}
