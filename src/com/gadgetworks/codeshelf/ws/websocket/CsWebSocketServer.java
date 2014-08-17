/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: CsWebSocketServer.java,v 1.1 2013/03/17 19:19:11 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.ws.websocket;

import java.net.InetSocketAddress;
import java.util.concurrent.CopyOnWriteArraySet;

import org.java_websocket.WebSocket;
import org.java_websocket.WebSocketImpl;
import org.java_websocket.framing.Framedata;
import org.java_websocket.framing.Framedata.Opcode;
import org.java_websocket.framing.FramedataImpl1;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gadgetworks.codeshelf.ws.IWebSessionManager;
import com.google.inject.Inject;
import com.google.inject.name.Named;

public class CsWebSocketServer extends WebSocketServer implements IWebSocketServer {
	public static final int					WEBSOCKET_PING_INTERVAL_MILLIS	= 20000;
	public static final int					WEBSOCKET_CHECK_INTERVAL_MILLIS	= 1000;											// Must divide evenly into WEBSOCKET_PING_INTERVAL_MILLIS

	public static final long				WS_PINGPONG_ROUNDTRIP_DELAY_MS	= 2000;
	public static final int					WS_PINGPONG_WARN_MISSED			= 1;
	public static final long				WS_PINGPONG_WARN_ELAPSED_MS		= (WS_PINGPONG_WARN_MISSED * WEBSOCKET_PING_INTERVAL_MILLIS)
																					+ WS_PINGPONG_ROUNDTRIP_DELAY_MS;
	public static final int					WS_PINGPONG_MAX_MISSED			= 2;
	public static final long				WS_PINGPONG_MAX_ELAPSED_MS		= (WS_PINGPONG_MAX_MISSED * WEBSOCKET_PING_INTERVAL_MILLIS)
																					+ WS_PINGPONG_ROUNDTRIP_DELAY_MS;
	public static final int					WS_CLOSE_CODE_PINGPONG_TIMEOUT	= 1;

	private static final Logger				LOGGER							= LoggerFactory.getLogger(CsWebSocketServer.class);

	private IWebSessionManager				mWebSessionManager;
	private CopyOnWriteArraySet<WebSocket>	mWebSockets;
	private boolean							mIdleKill;

	@Inject
	public CsWebSocketServer(@Named(WEBSOCKET_HOSTNAME_PROPERTY) final String inAddr,
		@Named(WEBSOCKET_PORTNUM_PROPERTY) final int inPort,
		final IWebSessionManager inWebSessionManager,
		final WebSocketServer.WebSocketServerFactory inWebSocketServerFactory) {
		super(new InetSocketAddress(inAddr, inPort), 4);

		WebSocketImpl.DEBUG = Boolean.parseBoolean(System.getProperty("websocket.debug"));
		setWebSocketFactory(inWebSocketServerFactory);

		mIdleKill = Boolean.parseBoolean(System.getProperty("websocket.idle.kill"));
		
		mWebSessionManager = inWebSessionManager;
		mWebSockets = new CopyOnWriteArraySet<WebSocket>();
	}

	@Override
	public final void start() {
		super.start();

		Thread websocketPingThread = new Thread(new Runnable() {
			public void run() {
				int checksPerPing = WEBSOCKET_PING_INTERVAL_MILLIS / WEBSOCKET_CHECK_INTERVAL_MILLIS;
				int cycle = 0;
				while (true) {
					if (++cycle % checksPerPing == 0) {
						LOGGER.trace("WebSocket watchdog: ping process start");
						int activeWebSockets = 0;
						for (WebSocket websocket : mWebSockets) {
							activeWebSockets++;
							LOGGER.debug("WebSocket watchdog: ping " + websocket.getRemoteSocketAddress());
							FramedataImpl1 pingFrame = new FramedataImpl1(Opcode.PING);
							pingFrame.setFin(true);
							websocket.sendFrame(pingFrame);
						}
						if (activeWebSockets == 0) {
							// log at a higher level if 0 clients are connected
							LOGGER.debug("WebSocket watchdog: pinged " + activeWebSockets + " clients");
						} else {
							LOGGER.trace("WebSocket watchdog: pinged " + activeWebSockets + " clients");
						}
					}

					for (WebSocket websocket : mWebSockets) {
						if (!mWebSessionManager.checkLastPongTime(websocket, WS_PINGPONG_WARN_ELAPSED_MS, WS_PINGPONG_MAX_ELAPSED_MS)) {
							if (mIdleKill) {
								websocket.closeConnection(WS_CLOSE_CODE_PINGPONG_TIMEOUT, "PONG timeout expired");
							}
						}
					}

					try {
						Thread.sleep(WEBSOCKET_CHECK_INTERVAL_MILLIS);
					} catch (InterruptedException e) {
						LOGGER.error("", e);
					}
				}
			}

		},
			"WebSocket ping thread");
		websocketPingThread.start();
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
			LOGGER.info("WebSocket close: " + inWebSocket.getRemoteSocketAddress() + " ( code:" + inCode + " reason:" + inReason
					+ " remote:" + inRemote);
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
		if (inWebSocket != null) {
			LOGGER.error("Error: " + inWebSocket.toString(), inException);
		} else {
			LOGGER.error("Error: (CsWebSocketServer.onError - websocket was null)", inException);
		}
	}

	@Override
	public final void onWebsocketPong(WebSocket inWebSocket, Framedata inFrameData) {
		super.onWebsocketPong(inWebSocket, inFrameData); // does nothing

		LOGGER.debug("WebSocket pong from " + inWebSocket.getRemoteSocketAddress());
		mWebSessionManager.handlePong(inWebSocket);
	}
}
