/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: CsWebSocketClient.java,v 1.6 2013/03/03 02:52:51 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.web.websocket;

import java.net.URI;

import org.java_websocket.WebSocket;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gadgetworks.codeshelf.application.IUtil;
import com.google.inject.Inject;
import com.google.inject.name.Named;

public class CsWebSocketClient extends WebSocketClient implements ICsWebSocketClient {

	public static final String				WEBSOCKET_URI_PROPERTY	= "WEBSOCKET_URI_PROPERTY";

	public static final String				WEBSOCKET_DEFAULT_URI	= "wss://localhost:8444";

	private static final Logger				LOGGER					= LoggerFactory.getLogger(CsWebSocketClient.class);

	private ICsWebsocketClientMsgHandler	mMessageHandler;

	@Inject
	public CsWebSocketClient(@Named(WEBSOCKET_URI_PROPERTY) final String inUriStr,
		final IUtil inUtil,
		final ICsWebsocketClientMsgHandler inMessageHandler,
		final WebSocketClient.WebSocketClientFactory inWebSocketClientFactory) {

		super(URI.create(inUriStr));
		setWebSocketFactory(inWebSocketClientFactory);

		mMessageHandler = inMessageHandler;
	}

	public final void start() {
		WebSocket.DEBUG = true;
		LOGGER.debug("Starting websocket client");

		try {
			connectBlocking();
		} catch (InterruptedException e) {
			LOGGER.error("", e);
		}
	}

	public final void stop() {
		close();
	}

	public final void onOpen(final ServerHandshake inHandshake) {
		LOGGER.debug("Websocket open");
	}

	public final void onClose(final int inCode, final String inReason, final boolean inRemote) {
		LOGGER.debug("Websocket close");
	}

	public final void onMessage(final String inMessage) {
		LOGGER.debug(inMessage);
		mMessageHandler.handleWebSocketMessage(inMessage);
	}

	public final void onError(final Exception inException) {

	}
}
