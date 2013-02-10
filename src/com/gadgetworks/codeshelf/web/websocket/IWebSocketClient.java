/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: IWebSocketClient.java,v 1.1 2013/02/10 08:23:07 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.web.websocket;

import java.io.IOException;

import org.java_websocket.handshake.ServerHandshake;

public interface IWebSocketClient {

	String	WEBSOCKET_URI_PROPERTY	= "WEBSOCKET_URI_PROPERTY";

	String	WEBSOCKET_DEFAULT_URI	= "wss://localhost:8444";

	void start();

	void stop() throws IOException, InterruptedException;

	void onOpen(ServerHandshake inHandshake);

	void onClose(int inCode, String inReason, boolean inRemote);

	void onMessage(String inMessage);

	void onError(Exception inException);

}
