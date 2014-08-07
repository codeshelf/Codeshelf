/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: IWebSocketServer.java,v 1.1 2013/03/17 19:19:12 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.ws.websocket;

import java.io.IOException;

public interface IWebSocketServer {

	static final String	WEBSOCKET_HOSTNAME_PROPERTY	= "WEBSOCKET_HOSTNAME_PROPERTY";
	static final String	WEBSOCKET_PORTNUM_PROPERTY	= "WEBSOCKET_PORTNUM_PROPERTY";
	static final String	KEYSTORE_TYPE_PROPERTY				= "KEYSTORE_TYPE_PROPERTY";
	static final String	KEYSTORE_PATH_PROPERTY				= "KEYSTORE_PATH_PROPERTY";
	static final String	KEYSTORE_STORE_PASSWORD_PROPERTY	= "KEYSTORE_STORE_PASSWORD_PROPERTY";
	static final String	KEYSTORE_KEY_PASSWORD_PROPERTY		= "KEYSTORE_KEY_PASSWORD_PROPERTY";

	void start();

	void stop() throws IOException, InterruptedException;
}
