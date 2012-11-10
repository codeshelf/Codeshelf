/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: GwWebSocketListener.java,v 1.1 2012/11/10 03:20:01 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.web.websocket;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.java_websocket.IWebSocket;
import org.java_websocket.SocketChannelIOHelper;
import org.java_websocket.WebSocketImpl;
import org.java_websocket.WebSocketListener;
import org.java_websocket.drafts.Draft;
import org.java_websocket.drafts.Draft_76;
import org.java_websocket.exceptions.InvalidDataException;
import org.java_websocket.framing.CloseFrame;
import org.java_websocket.framing.Framedata;
import org.java_websocket.framing.Framedata.Opcode;
import org.java_websocket.framing.FramedataImpl1;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.handshake.HandshakeImpl1Server;
import org.java_websocket.handshake.Handshakedata;
import org.java_websocket.handshake.ServerHandshake;
import org.java_websocket.handshake.ServerHandshakeBuilder;
import org.java_websocket.server.WebSocketServer;

import com.gadgetworks.codeshelf.web.websession.IWebSessionManager;
import com.google.inject.Inject;

/**
 * @author jeffw
 *
 */
public final class GwWebSocketListener extends WebSocketServer implements IGwWebSocketListener {

	private static final Log				LOGGER						= LogFactory.getLog(WebSocketListener.class);

	private static final String				INTERFACE_THREAD_NAME		= "WebSocket Listener";
	private static final int				INTERFACE_THREAD_PRIORITY	= Thread.NORM_PRIORITY - 1;

	private static final String				WEBSOCKET_ADDRESS			= "localhost";
	private static final int				WEBSOCKET_PORT				= 8080;

	private Thread							mServerThread;
	private ServerSocketChannel				mServerSocket;
	private InetSocketAddress				mAddress;
	private Draft							mDraft;
	private Selector						mSelector;
	private CopyOnWriteArraySet<IWebSocket>	mWebSockets;
	private IWebSessionManager				mWebSessionManager;
	private boolean							mShouldRun;

	@Inject
	public GwWebSocketListener(final IWebSessionManager inWebSessionManager) {
		mWebSessionManager = inWebSessionManager;
	}

	// --------------------------------------------------------------------------
	/**
	 */
	public void start() {

		try {
			mAddress = new InetSocketAddress(InetAddress.getByName(WEBSOCKET_ADDRESS), WEBSOCKET_PORT);
			mServerSocket = ServerSocketChannel.open();
			mServerSocket.configureBlocking(false);
			mServerSocket.socket().bind(mAddress);
			//InetAddress.getLocalHost()
			mSelector = Selector.open();
			mServerSocket.register(mSelector, mServerSocket.validOps());
			mDraft = new Draft_76();
			mWebSockets = new CopyOnWriteArraySet<IWebSocket>();

		} catch (IOException ex) {
			LOGGER.error("Couldn't create websocket", ex);
			//onError(null, ex);
			return;
		}

		LOGGER.info("Server socket started: " + mServerSocket.toString());
		mShouldRun = true;

		mServerThread = new Thread(new Runnable() {
			public void run() {
				while (mShouldRun) {
					try {
						processSocketEvents();
					} catch (Exception e) {
						LOGGER.debug("Exception during web socket handling", e);
					}
				}
				LOGGER.info("Exited websocket manager");
			}
		}, INTERFACE_THREAD_NAME);
		mServerThread.setPriority(INTERFACE_THREAD_PRIORITY);
		mServerThread.setDaemon(true);
		mServerThread.start();
	}

	// --------------------------------------------------------------------------
	/**
	 */
	public void stop() {
		mShouldRun = false;
		try {
			if (mSelector != null) {
				mSelector.close();
			}
			if (mServerSocket != null) {
				mServerSocket.close();
			}
		} catch (IOException e) {
			LOGGER.error("", e);
		}
		if (mWebSockets != null) {
			for (IWebSocket webSocket : mWebSockets) {
				webSocket.close(CloseFrame.GOING_AWAY);
			}
		}
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inSocket
	 */
	private void processSocketEvents() {

		SelectionKey key = null;
		IWebSocket webSocket = null;
		try {
			mSelector.select();
			Set<SelectionKey> keys = mSelector.selectedKeys();
			Iterator<SelectionKey> keyIterator = keys.iterator();

			while (keyIterator.hasNext()) {
				key = keyIterator.next();

				// Remove the current key
				keyIterator.remove();

				// Check if a client required a connection.
				if (key.isAcceptable()) {
					SocketChannel client = mServerSocket.accept();
					client.configureBlocking(false);
					IWebSocket newWebSocket = new WebSocketImpl(this, Collections.singletonList(mDraft), client.socket());
					client.register(mSelector, SelectionKey.OP_READ, newWebSocket);
				}

				// Check if the server is ready to read.
				if (key.isReadable()) {
					webSocket = (WebSocketImpl) key.attachment();
					webSocket.handleRead();
				}

				// Send waiting data to the client.
				if (key.isValid() && key.isWritable()) {
					webSocket = (WebSocketImpl) key.attachment();

					if (SocketChannelIOHelper.batch((WebSocketImpl) webSocket, (ByteChannel) ((WebSocketImpl) webSocket).channel)) {
						key.channel().register(mSelector, SelectionKey.OP_READ, webSocket);
					}
				}
			}

			Iterator<IWebSocket> webSocketIterator = mWebSockets.iterator();
			while (webSocketIterator.hasNext()) {
				// We have to do this check here, and not in the thread that
				// adds the buffered data to the WebSocket, because the
				// Selector is not thread-safe, and can only be accessed
				// by this thread.
				webSocket = webSocketIterator.next();
				if (webSocket.hasBufferedData()) {
					if (SocketChannelIOHelper.batch((WebSocketImpl) webSocket, (ByteChannel) ((WebSocketImpl) webSocket).channel)) {
						// key.channel().register( selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE, conn );
					}
				}
			}
		} catch (IOException ex) {
			LOGGER.error("Websocket problem", ex);
			if (key != null)
				key.cancel();
			onWebsocketError(webSocket, ex);
			if (webSocket != null) {
				webSocket.close(CloseFrame.PROTOCOL_ERROR);
			}
		} catch (InterruptedException ex) {
			LOGGER.error("Websocket problem", ex);
			if (key != null)
				key.cancel();
			onWebsocketError(webSocket, ex);
			if (webSocket != null) {
				webSocket.close(CloseFrame.PROTOCOL_ERROR);
			}
		}
	}

	@Override
	public void onWebsocketError(IWebSocket inWebSocket, Exception inException) {
		LOGGER.error("WebSocket error", inException);
	}

	@Override
	public void onWebsocketOpen(IWebSocket inWebSocket, Handshakedata inHandshakeData) {
		if (mWebSockets.add(inWebSocket)) {
			LOGGER.info("WebSocket open: " + inWebSocket.getRemoteSocketAddress());
			mWebSessionManager.handleSessionOpen(inWebSocket);
		}
	}

	@Override
	public void onWebsocketClose(IWebSocket inWebSocket, int inCode, String inReason, boolean inRemote) {
		if (mWebSockets.remove(inWebSocket)) {
			LOGGER.info("WebSocket close: " + inWebSocket.getRemoteSocketAddress() + " ( code:" + inCode + " reason:" + inReason + " remote:" + inRemote);
			mWebSessionManager.handleSessionClose(inWebSocket);
		}
	}

	@Override
	public void onWebsocketMessage(IWebSocket inWebSocket, String inMessage) {
		if (mWebSockets.contains(inWebSocket)) {
			LOGGER.info("WebSocket message from addr: " + inWebSocket.getRemoteSocketAddress() + " msg: " + inMessage);
			mWebSessionManager.handleSessionMessage(inWebSocket, inMessage);
		}
	}

	@Override
	public void onWebsocketMessage(IWebSocket inWebSocket, ByteBuffer inByteBuffer) {
		StringBuffer message = new StringBuffer();
		message.append("WebSocket blob message from addr: ");
		message.append(inWebSocket.getRemoteSocketAddress());
		message.append(" msg: '");
		message.append(Arrays.toString(inByteBuffer.array()));
		message.append("'");
		LOGGER.info(message);
	}

	@Override
	public void onWebsocketPing(IWebSocket inWebSocket, Framedata inFrameData) {
		FramedataImpl1 resp = new FramedataImpl1(inFrameData);
		resp.setOptcode(Opcode.PONG);
		inWebSocket.sendFrame(resp);
	}

	@Override
	public void onWebsocketPong(IWebSocket inWebSocket, Framedata inFrameData) {
		// TODO Auto-generated method stub
	}

	@Override
	public String getFlashPolicy(IWebSocket inWebSocket) {
		return "<cross-domain-policy><allow-access-from domain=\"*\" to-ports=\"" + inWebSocket.getLocalSocketAddress().getPort() + "\" /></cross-domain-policy>\0";
	}

	@Override
	public ServerHandshakeBuilder onWebsocketHandshakeReceivedAsServer(IWebSocket inWebSocket, Draft inDraft, ClientHandshake inRequest) throws InvalidDataException {
		return new HandshakeImpl1Server();
	}

	@Override
	public void onWebsocketHandshakeReceivedAsClient(IWebSocket inWebSocket, ClientHandshake inRequest, ServerHandshake inResponse) throws InvalidDataException {

	}

	@Override
	public void onWebsocketHandshakeSentAsClient(IWebSocket inWebSocket, ClientHandshake inRequest) throws InvalidDataException {

	}

	@Override
	public void onWriteDemand(IWebSocket inWebSocket) {
		try {
			if ((inWebSocket != null) && (inWebSocket.hasBufferedData())) {
				if (SocketChannelIOHelper.batch((WebSocketImpl) inWebSocket, (ByteChannel) ((WebSocketImpl) inWebSocket).channel)) {
				}
			}
		} catch (IOException ex) {
			LOGGER.error("Websocket problem", ex);
			onWebsocketError(inWebSocket, ex);
			if (inWebSocket != null) {
				inWebSocket.close(CloseFrame.PROTOCOL_ERROR);
			}
		}
	}
}
