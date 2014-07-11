/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: CsWebSocketServer.java,v 1.1 2013/03/17 19:19:11 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.ws.websocket;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

import org.java_websocket.WebSocket;
import org.java_websocket.WebSocketImpl;
import org.java_websocket.framing.Framedata;
import org.java_websocket.framing.FramedataImpl1;
import org.java_websocket.framing.Framedata.Opcode;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gadgetworks.codeshelf.ws.IWebSessionManager;
import com.google.inject.Inject;
import com.google.inject.name.Named;

public class CsWebSocketServer extends WebSocketServer implements IWebSocketServer {
	public static final int					WEBSOCKET_PING_INTERVAL_MILLIS	= 20000;
	public static final long				WS_PINGPONG_ROUNDTRIP_DELAY_MS	= 2000;
	public static final long				WS_PINGPONG_WARN_ELAPSED_MS		= WEBSOCKET_PING_INTERVAL_MILLIS
																					+ WS_PINGPONG_ROUNDTRIP_DELAY_MS;
	public static final int					WS_PINGPONG_MAX_MISSED			= 2;
	public static final long				WS_PINGPONG_MAX_ELAPSED_MS		= (WS_PINGPONG_MAX_MISSED * WEBSOCKET_PING_INTERVAL_MILLIS)
																					+ WS_PINGPONG_ROUNDTRIP_DELAY_MS;
	public static final int					WS_CLOSE_CODE_PINGPONG_TIMEOUT	= 1;

	private static final Logger				LOGGER							= LoggerFactory.getLogger(CsWebSocketServer.class);

	private IWebSessionManager				mWebSessionManager;
	private CopyOnWriteArraySet<WebSocket>	mWebSockets;

	private Map<WebSocket, Long>			webSocketLastPingTime			= new ConcurrentHashMap<WebSocket, Long>();
	@Inject
	public CsWebSocketServer(@Named(WEBSOCKET_HOSTNAME_PROPERTY) final String inAddr,
		@Named(WEBSOCKET_PORTNUM_PROPERTY) final int inPort,
		final IWebSessionManager inWebSessionManager,
		final WebSocketServer.WebSocketServerFactory inWebSocketServerFactory) {
		super(new InetSocketAddress(inAddr, inPort), 4);

		WebSocketImpl.DEBUG = Boolean.parseBoolean(System.getProperty("websocket.debug"));
		setWebSocketFactory(inWebSocketServerFactory);

		mWebSessionManager = inWebSessionManager;
		mWebSockets = new CopyOnWriteArraySet<WebSocket>();
	}

	@Override
	public final void start() {
		super.start();

		Thread websocketPingThread = new Thread(new Runnable() {
			public void run() {
				while (true) {
					LOGGER.trace("WebSocket watchdog: ping process start");
					int activeWebSockets = 0;
					for (WebSocket websocket : mWebSockets) {
						activeWebSockets++;
						LOGGER.debug("WebSocket watchdog: ping " + websocket.getRemoteSocketAddress());
						FramedataImpl1 pingFrame = new FramedataImpl1(Opcode.PING);
						pingFrame.setFin(true);
						websocket.sendFrame(pingFrame);

						long elapsed = getPongTimerElapsed(websocket);
						if (elapsed > CsWebSocketServer.WS_PINGPONG_WARN_ELAPSED_MS) {
							LOGGER.info("WebSocket watchdog: warning for missed PONG, last " + elapsed + " ms ago");
						} else if (elapsed > CsWebSocketServer.WS_PINGPONG_MAX_ELAPSED_MS) {
							LOGGER.warn("WebSocket watchdog: dead connection - maximum elapsed PONG time reached (" + elapsed
									+ " ms)");
							websocket.closeConnection(WS_CLOSE_CODE_PINGPONG_TIMEOUT, "PONG timeout expired");
						} else {
							//LOGGER.debug("WebSocket watchdog okay, last ping "+elapsed+" ms ago");
						}
					}
					if (activeWebSockets == 0) {
						// log at a higher level if 0 clients are connected
						LOGGER.debug("WebSocket watchdog: pinged " + activeWebSockets + " clients");
					} else {
						LOGGER.trace("WebSocket watchdog: pinged " + activeWebSockets + " clients");
					}

					try {
						Thread.sleep(WEBSOCKET_PING_INTERVAL_MILLIS);
					} catch (InterruptedException e) {
						LOGGER.error("", e);
					}
				}
			}

		}, "WebSocket ping thread");
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

	public final void resetPongTimer(WebSocket inWebsocket) {
		webSocketLastPingTime.put(inWebsocket, System.currentTimeMillis());
	}

	public final long getPongTimerElapsed(WebSocket inWebsocket) {
		Long lastPong = webSocketLastPingTime.get(inWebsocket);
		if (lastPong == null) {
			// first time called for this websocket
			resetPongTimer(inWebsocket);
			return 0;
		} //else
		return System.currentTimeMillis() - lastPong;
	}

	@Override
	public final void onWebsocketPong(WebSocket inWebSocket, Framedata inFrameData) {
		super.onWebsocketPong(inWebSocket, inFrameData); // does nothing
		LOGGER.debug("WebSocket pong from " + inWebSocket.getRemoteSocketAddress());
		// TODO: report time to ping (watchdog) thread
	}
}
