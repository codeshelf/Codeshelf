/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2013, Jeffrey B. Williams, All rights reserved
 *  $Id: ICsWebSocketClient.java,v 1.2 2013/03/03 02:52:51 jeffw Exp $
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
	
}
