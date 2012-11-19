/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: ICodeshelfWebSocketServer.java,v 1.3 2012/11/19 10:48:25 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.web.websocket;

import java.io.IOException;

import org.java_websocket.IWebSocket;
import org.java_websocket.handshake.ClientHandshake;

public interface ICodeshelfWebSocketServer {

	String	WEBSOCKET_HOSTNAME_PROPERTY	= "WEBSOCKET_HOSTNAME_PROPERTY";
	String	WEBSOCKET_PORTNUM_PROPERTY	= "WEBSOCKET_PORTNUM_PROPERTY";

	String	WEBSOCKET_DEFAULT_HOSTNAME	= "localhost";
	int		WEBSOCKET_DEFAULT_PORTNUM	= 8444;

	void start();

	void stop() throws IOException, InterruptedException;

	void onOpen(IWebSocket inWebSocket, ClientHandshake inHandshake);

	void onClose(IWebSocket inWebSocket, int inCode, String inReason, boolean inRemote);

	void onMessage(IWebSocket inWebSocket, String inMessage);

	void onError(IWebSocket inWebSocket, Exception inException);

}
