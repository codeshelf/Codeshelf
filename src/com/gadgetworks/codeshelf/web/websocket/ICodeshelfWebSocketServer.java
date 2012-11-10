/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: ICodeshelfWebSocketServer.java,v 1.1 2012/11/10 03:20:01 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.web.websocket;

import java.io.IOException;

import org.java_websocket.IWebSocket;
import org.java_websocket.handshake.ClientHandshake;

public interface ICodeshelfWebSocketServer {

	void start();

	void stop() throws IOException, InterruptedException;

	void onOpen(IWebSocket inWebSocket, ClientHandshake inHandshake);

	void onClose(IWebSocket inWebSocket, int inCode, String inReason, boolean inRemote);

	void onMessage(IWebSocket inWebSocket, String inMessage);

	void onError(IWebSocket inWebSocket, Exception inException);

}
