/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: CodeshelfWebSocketServer.java,v 1.1 2012/11/10 03:20:01 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.web.websocket;

import java.net.InetSocketAddress;
import java.util.Set;
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

public class CodeshelfWebSocketServer extends WebSocketServer implements ICodeshelfWebSocketServer {

	public static final String				WEBSOCKET_ADDRESS	= "localhost";
	public static final int					WEBSOCKET_PORT		= 8080;

	private static final Log				LOGGER				= LogFactory.getLog(CodeshelfWebSocketServer.class);

	private InetSocketAddress				mAddress;
	private IWebSessionManager				mWebSessionManager;
	private CopyOnWriteArraySet<IWebSocket>	mWebSockets;

	@Inject
	public CodeshelfWebSocketServer(final @Named("WEBSOCKET_ADDRESS") String inAddr, final @Named("WEBSOCKET_PORT") int inPort, final IWebSessionManager inWebSessionManager) {
		super(new InetSocketAddress(inAddr, inPort));
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

	/**
	 * Sends <var>text</var> to all currently connected WebSocket clients.
	 * 
	 * @param text
	 *            The String to send across the network.
	 * @throws InterruptedException
	 *             When socket related I/O errors occur.
	 */
	public void sendToAll(String text) {
		Set<WebSocket> con = connections();
		synchronized (con) {
			for (WebSocket c : con) {
				c.send(text);
			}
		}
	}
}
