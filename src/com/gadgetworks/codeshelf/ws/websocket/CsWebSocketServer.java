/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: CsWebSocketServer.java,v 1.1 2013/03/17 19:19:11 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.ws.websocket;

import java.net.InetSocketAddress;
import java.util.concurrent.CopyOnWriteArraySet;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gadgetworks.codeshelf.ws.IWebSessionManager;
import com.google.inject.Inject;
import com.google.inject.name.Named;

public class CsWebSocketServer extends WebSocketServer implements IWebSocketServer {

	private static final Logger				LOGGER	= LoggerFactory.getLogger(CsWebSocketServer.class);

	private IWebSessionManager				mWebSessionManager;
	private CopyOnWriteArraySet<WebSocket>	mWebSockets;

	@Inject
	public CsWebSocketServer(@Named(WEBSOCKET_HOSTNAME_PROPERTY) final String inAddr,
		@Named(WEBSOCKET_PORTNUM_PROPERTY) final int inPort,
		final IWebSessionManager inWebSessionManager,
		final WebSocketServer.WebSocketServerFactory inWebSocketServerFactory) {
		super(new InetSocketAddress(inAddr, inPort), 4);

		setWebSocketFactory(inWebSocketServerFactory);

		mWebSessionManager = inWebSessionManager;
		mWebSockets = new CopyOnWriteArraySet<WebSocket>();
	}

	@Override
	public final void start() {
		super.start();
	}

	@Override
	public final void onOpen(final WebSocket inWebSocket, final ClientHandshake inHandshake) {
		if (mWebSockets.add(inWebSocket)) {
			LOGGER.info("WebSocket open: " + inWebSocket.getRemoteSocketAddress());
			mWebSessionManager.handleSessionOpen(inWebSocket);
		}
	}

	@Override
	public final void onClose(final WebSocket inWebSocket, final int inCode, final String inReason, final boolean inRemote) {
		if (mWebSockets.remove(inWebSocket)) {
			LOGGER.info("WebSocket close: " + inWebSocket.getRemoteSocketAddress() + " ( code:" + inCode + " reason:" + inReason + " remote:" + inRemote);
			mWebSessionManager.handleSessionClose(inWebSocket);
		}
	}

	@Override
	public final void onMessage(final WebSocket inWebSocket, final String inMessage) {
		if (mWebSockets.contains(inWebSocket)) {
			LOGGER.info("WebSocket message from addr: " + inWebSocket.getRemoteSocketAddress() + " msg: " + inMessage);
			mWebSessionManager.handleSessionMessage(inWebSocket, inMessage);
		}
	}

	@Override
	public final void onError(final WebSocket inWebSocket, final Exception inException) {
		LOGGER.error("Error: " + inWebSocket.toString(), inException);
	}
}
