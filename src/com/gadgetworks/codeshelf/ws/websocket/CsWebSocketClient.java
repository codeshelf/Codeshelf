/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: CsWebSocketClient.java,v 1.1 2013/03/17 19:19:11 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.ws.websocket;

import java.io.IOException;
import java.net.URI;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;

import org.java_websocket.WebSocket;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gadgetworks.codeshelf.application.IUtil;
import com.google.inject.name.Named;

public class CsWebSocketClient extends WebSocketClient implements ICsWebSocketClient {

	public static final String				WEBSOCKET_URI_PROPERTY	= "WEBSOCKET_URI_PROPERTY";

	public static final String				WEBSOCKET_DEFAULT_URI	= "wss://localhost:8444";

	private static final Logger				LOGGER					= LoggerFactory.getLogger(CsWebSocketClient.class);

	private ICsWebsocketClientMsgHandler	mMessageHandler;
	private IWebSocketSslContextFactory		mWebSocketSslContextFactory;

	//@Inject
	// Can't really inject - the Java_WebSocket libs are not really built for re-use or re-entrance.  BLerg.
	// If the server connection breaks we can't reuse the websocket and have to create a new one are runtime.
	// See CsDeviceManager where we cache these values on inject and reuse them to create new sockets.
	public CsWebSocketClient(@Named(WEBSOCKET_URI_PROPERTY) final String inUriStr,
		final IUtil inUtil,
		final ICsWebsocketClientMsgHandler inMessageHandler,
		final IWebSocketSslContextFactory inWebSocketSslContextFactory) {

		super(URI.create(inUriStr));

		//WebSocketImpl.DEBUG = true;
		mMessageHandler = inMessageHandler;
		mWebSocketSslContextFactory = inWebSocketSslContextFactory;
	}

	public final void start() {
		LOGGER.debug("Starting websocket client");

		try {
			// Create the SSL context and use that to create the socket.
			SSLContext sslContext = mWebSocketSslContextFactory.getSslContext();
			SSLSocketFactory factory = sslContext.getSocketFactory();
			setSocket(factory.createSocket());
			connectBlocking();
		} catch (InterruptedException e) {
			LOGGER.error("", e);
		} catch (IOException e) {
			LOGGER.error("", e);
		}
	}

	public final void stop() {
		if (isStarted()) {
			close();
		}
	}

	public final boolean isStarted() {
		boolean result = false;
		WebSocket socket = getConnection();
		if (socket != null) {
			result = socket.isOpen();
		}
		return result;
	}

	public final void onOpen(final ServerHandshake inHandshake) {
		LOGGER.debug("Websocket open");
	}

	public final void onClose(final int inCode, final String inReason, final boolean inRemote) {
		LOGGER.debug("Websocket close");
		mMessageHandler.handleWebSocketClosed();
	}

	public final void onMessage(final String inMessage) {
		LOGGER.debug(inMessage);
		mMessageHandler.handleWebSocketMessage(inMessage);
	}

	public final void onError(final Exception inException) {
		LOGGER.debug("Websocket error");
	}
}
