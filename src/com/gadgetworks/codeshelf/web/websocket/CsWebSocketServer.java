/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: CsWebSocketServer.java,v 1.1 2013/02/10 08:23:07 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.web.websocket;

import java.net.InetSocketAddress;
import java.util.concurrent.CopyOnWriteArraySet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.java_websocket.IWebSocket;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import com.gadgetworks.codeshelf.web.websession.IWebSessionManager;
import com.google.inject.Inject;
import com.google.inject.name.Named;

public class CsWebSocketServer extends WebSocketServer implements IWebSocketServer {

	private static final Log				LOGGER	= LogFactory.getLog(CsWebSocketServer.class);

	private IWebSessionManager				mWebSessionManager;
	private CopyOnWriteArraySet<IWebSocket>	mWebSockets;

	@Inject
	public CsWebSocketServer(@Named(WEBSOCKET_HOSTNAME_PROPERTY) final String inAddr,
		@Named(WEBSOCKET_PORTNUM_PROPERTY) final int inPort,
		final IWebSessionManager inWebSessionManager,
		final WebSocketServer.WebSocketServerFactory inWebSocketServerFactory) {
		super(new InetSocketAddress(inAddr, inPort), 4);

		setWebSocketFactory(inWebSocketServerFactory);

		mWebSessionManager = inWebSessionManager;
		mWebSockets = new CopyOnWriteArraySet<IWebSocket>();
	}

	@Override
	public final void start() {
		WebSocket.DEBUG = true;
		super.start();
	}

	@Override
	public final void onOpen(final IWebSocket inWebSocket, final ClientHandshake inHandshake) {
		if (mWebSockets.add(inWebSocket)) {
			LOGGER.info("WebSocket open: " + inWebSocket.getRemoteSocketAddress());
			mWebSessionManager.handleSessionOpen(inWebSocket);
		}
	}

	@Override
	public final void onClose(final IWebSocket inWebSocket, final int inCode, final String inReason, final boolean inRemote) {
		if (mWebSockets.remove(inWebSocket)) {
			LOGGER.info("WebSocket close: " + inWebSocket.getRemoteSocketAddress() + " ( code:" + inCode + " reason:" + inReason + " remote:" + inRemote);
			mWebSessionManager.handleSessionClose(inWebSocket);
		}
	}

	@Override
	public final void onMessage(final IWebSocket inWebSocket, final String inMessage) {
		if (mWebSockets.contains(inWebSocket)) {
			LOGGER.info("WebSocket message from addr: " + inWebSocket.getRemoteSocketAddress() + " msg: " + inMessage);
			mWebSessionManager.handleSessionMessage(inWebSocket, inMessage);
		}
	}

	@Override
	public final void onError(final IWebSocket inWebSocket, final Exception inException) {
		LOGGER.error("Error: " + inWebSocket.toString(), inException);
	}
}
