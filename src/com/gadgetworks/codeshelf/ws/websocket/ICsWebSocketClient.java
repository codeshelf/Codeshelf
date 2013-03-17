/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2013, Jeffrey B. Williams, All rights reserved
 *  $Id: ICsWebSocketClient.java,v 1.1 2013/03/17 19:19:12 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.ws.websocket;

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
