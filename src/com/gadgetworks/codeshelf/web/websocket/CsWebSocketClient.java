/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: CsWebSocketClient.java,v 1.1 2013/02/10 08:23:07 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.web.websocket;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.channels.NotYetConnectedException;

import org.java_websocket.WebSocket;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.name.Named;

public class CsWebSocketClient extends WebSocketClient implements IWebSocketClient {

	private static final Logger	LOGGER	= LoggerFactory.getLogger(CsWebSocketClient.class);

	@Inject
	public CsWebSocketClient(@Named(WEBSOCKET_URI_PROPERTY) final String inUriStr, final WebSocketClient.WebSocketClientFactory inWebSocketClientFactory) {
		super(URI.create(inUriStr));

		setWebSocketFactory(inWebSocketClientFactory);
	}

	public final void start() {
		WebSocket.DEBUG = true;
		LOGGER.debug("Websocket start");
		try {
			connectBlocking();

			BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
			while (true) {
				try {
					String line = reader.readLine();
					if (line.equals("close")) {
						close();
					} else {
						send(line);
					}
				} catch (NotYetConnectedException | IOException e) {
					LOGGER.error("", e);
				}
			}
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
	}

	public final void onError(final Exception inException) {

	}
}
