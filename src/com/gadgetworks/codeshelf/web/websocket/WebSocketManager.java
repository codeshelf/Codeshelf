/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: WebSocketManager.java,v 1.1 2012/02/05 02:53:21 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.web.websocket;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
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

import com.gadgetworks.codeshelf.web.websession.WebSessionManager;
import com.gadgetworks.codeshelf.web.websocket.Framedata.Opcode;
import com.gadgetworks.codeshelf.web.websocket.drafts.Draft_76;

/**
 * @author jeffw
 *
 */
public final class WebSocketManager implements WebSocketListener {

	private static final Log				LOGGER						= LogFactory.getLog(WebSocketManager.class);

	private static final String				INTERFACE_THREAD_NAME		= "WebSocket Listener";
	private static final int				INTERFACE_THREAD_PRIORITY	= Thread.NORM_PRIORITY - 1;

	private static final String				WEBSOCKET_ADDRESS			= "localhost";
	private static final int				WEBSOCKET_PORT				= 8080;

	private Thread							mServerThread;
	private ServerSocketChannel				mServerSocket;
	private InetSocketAddress				mAddress;
	private Draft							mDraft;
	private Selector						mSelector;
	private CopyOnWriteArraySet<WebSocket>	mWebSockets;
	private WebSessionManager				mWebSessionManager;
	private boolean							mShouldRun;

	public WebSocketManager(WebSessionManager inWebSessionManager) {
		try {
			mAddress = new InetSocketAddress(InetAddress.getByName(WEBSOCKET_ADDRESS), WEBSOCKET_PORT);
			mServerSocket = ServerSocketChannel.open();
			mServerSocket.configureBlocking(false);
			mServerSocket.socket().bind(mAddress);
			//InetAddress.getLocalHost()
			mSelector = Selector.open();
			mServerSocket.register(mSelector, mServerSocket.validOps());
			mDraft = new Draft_76();
			mWebSockets = new CopyOnWriteArraySet<WebSocket>();
			mWebSessionManager = inWebSessionManager;

		} catch (IOException ex) {
			LOGGER.error("Couldn't create websocket", ex);
			//onError(null, ex);
			return;
		}
	}

	// --------------------------------------------------------------------------
	/**
	 */
	public void start() {
		LOGGER.info("Server socket started: " + mServerSocket.toString());
		mShouldRun = true;

		mServerThread = new Thread(new Runnable() {
			public void run() {
				while (mShouldRun) {
					processSocketEvents();
				}
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
		if (mServerSocket != null) {
			try {
				mServerSocket.close();
			} catch (IOException e) {
				LOGGER.error("", e);
			}
		}
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inSocket
	 */
	private void processSocketEvents() {

		SelectionKey key = null;
		WebSocket webSocket = null;
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
					WebSocket newWebSocket = new WebSocket(this, Collections.singletonList(mDraft), client.socket().getChannel());
					client.register(mSelector, SelectionKey.OP_READ, newWebSocket);
				}

				// Check if the server is ready to read.
				if (key.isReadable()) {
					webSocket = (WebSocket) key.attachment();
					webSocket.handleRead();
				}

				// Send waiting data to the client.
				if (key.isValid() && key.isWritable()) {
					webSocket = (WebSocket) key.attachment();
					webSocket.handleWrite();
					key.channel().register(mSelector, SelectionKey.OP_READ, webSocket);
				}
			}

			Iterator<WebSocket> webSocketIterator = mWebSockets.iterator();
			while (webSocketIterator.hasNext()) {
				// We have to do this check here, and not in the thread that
				// adds the buffered data to the WebSocket, because the
				// Selector is not thread-safe, and can only be accessed
				// by this thread.
				webSocket = webSocketIterator.next();
				if (webSocket.hasBufferedData()) {
					webSocket.handleWrite();
					// key.channel().register( selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE, conn );
				}
			}
		} catch (IOException ex) {
			LOGGER.error("Websocket problem", ex);
			if (key != null)
				key.cancel();
			onError(webSocket, ex);
			if (webSocket != null) {
				webSocket.close();
			}
		} catch (InterruptedException ex) {
			LOGGER.error("Websocket problem", ex);
			if (key != null)
				key.cancel();
			onError(webSocket, ex);
			if (webSocket != null) {
				webSocket.close();
			}
		}
	}

	@Override
	public void onError(WebSocket inWebSocket, Exception inException) {
		LOGGER.error("WebSocket error", inException);
	}

	@Override
	public void onOpen(WebSocket inWebSocket) {
		if (mWebSockets.add(inWebSocket)) {
			LOGGER.info("WebSocket open: " + inWebSocket.getRemoteSocketAddress());
			mWebSessionManager.handleSessionOpen(inWebSocket);
		}
	}

	@Override
	public void onClose(WebSocket inWebSocket) {
		if (mWebSockets.remove(inWebSocket)) {
			LOGGER.info("WebSocket close: " + inWebSocket.getRemoteSocketAddress());
			mWebSessionManager.handleSessionClose(inWebSocket);
		}
	}

	@Override
	public void onMessage(WebSocket inWebSocket, String inMessage) {
		if (mWebSockets.contains(inWebSocket)) {
			LOGGER.info("WebSocket message from addr: " + inWebSocket.getRemoteSocketAddress() + " msg: " + inMessage);
			mWebSessionManager.handleSessionMessage(inWebSocket, inMessage);
		}
	}

	@Override
	public void onMessage(WebSocket inWebSocket, byte[] inBlob) {
		StringBuffer message = new StringBuffer();
		message.append("WebSocket blob message from addr: ");
		message.append(inWebSocket.getRemoteSocketAddress());
		message.append(" msg: '");
		message.append(Arrays.toString(inBlob));
		message.append("'");
		LOGGER.info(message);
	}

	@Override
	public void onPing(WebSocket inWebSocket, Framedata inFrameData) {
		FramedataImpl1 resp = new FramedataImpl1(inFrameData);
		resp.setOptcode(Opcode.PONG);
		try {
			inWebSocket.sendFrame(resp);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onPong(WebSocket inWebSocket, Framedata inFrameData) {
		// TODO Auto-generated method stub

	}

	@Override
	public String getFlashPolicy(WebSocket conn) {
		return "<cross-domain-policy><allow-access-from domain=\"*\" to-ports=\"" + conn.getLocalSocketAddress().getPort() + "\" /></cross-domain-policy>\0";
	}

	@Override
	public HandshakeBuilder onHandshakeRecievedAsServer(WebSocket inWebSocket, Draft inDraft, Handshakedata inRequest) throws IOException {
		return new HandshakedataImpl1();
	}

	@Override
	public boolean onHandshakeRecievedAsClient(WebSocket inWebSocket, Handshakedata inRequest, Handshakedata inResponse) throws IOException {
		return true;
	}

	@Override
	public void onWriteDemand(WebSocket inWebSocket) {
		// TODO Auto-generated method stub

	}
}
