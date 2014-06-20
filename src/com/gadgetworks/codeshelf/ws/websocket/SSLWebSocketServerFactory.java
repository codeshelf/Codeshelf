/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: SSLWebSocketServerFactory.java,v 1.1 2013/03/17 19:19:11 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.ws.websocket;

import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.net.URL;
import java.nio.channels.ByteChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManagerFactory;

import org.java_websocket.SSLSocketChannel2;
import org.java_websocket.WebSocketAdapter;
import org.java_websocket.WebSocketImpl;
import org.java_websocket.drafts.Draft;
import org.java_websocket.drafts.IDraft;
import org.java_websocket.server.WebSocketServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.name.Named;

/**
 * @author jeffw
 *
 */
public class SSLWebSocketServerFactory implements WebSocketServer.WebSocketServerFactory {

	private static final Logger	LOGGER								= LoggerFactory.getLogger(SSLWebSocketServerFactory.class);

	private static final String	KEYSTORE_TYPE_PROPERTY				= "KEYSTORE_TYPE_PROPERTY";
	private static final String	KEYSTORE_PATH_PROPERTY				= "KEYSTORE_PATH_PROPERTY";
	private static final String	KEYSTORE_STORE_PASSWORD_PROPERTY	= "KEYSTORE_STORE_PASSWORD_PROPERTY";
	private static final String	KEYSTORE_KEY_PASSWORD_PROPERTY		= "KEYSTORE_KEY_PASSWORD_PROPERTY";

	private String				mKeystorePath;
	private String				mKeystoreType;
	private String				mKeystoreStorePassword;
	private String				mKeystoreKeyPassword;
	private ExecutorService		mExec;

	@Inject
	public SSLWebSocketServerFactory(@Named(KEYSTORE_PATH_PROPERTY) final String inKeystorePath,
		@Named(KEYSTORE_TYPE_PROPERTY) final String inKeystoreType,
		@Named(KEYSTORE_STORE_PASSWORD_PROPERTY) final String inKeystoreStorePassword,
		@Named(KEYSTORE_KEY_PASSWORD_PROPERTY) final String inKeystoreKeyPassword) {
		mKeystorePath = inKeystorePath;
		mKeystoreType = inKeystoreType;
		mKeystoreStorePassword = inKeystoreStorePassword;
		mKeystoreKeyPassword = inKeystoreKeyPassword;
	}

	@Override
	public final ByteChannel wrapChannel(SocketChannel inSocketChannel, SelectionKey inSelectionKey) throws IOException {
		LOGGER.debug("WebSocket wrap channel: " + inSocketChannel.getRemoteAddress());
		SSLEngine sslEngine = getSslContext().createSSLEngine();
		sslEngine.setUseClientMode(false);
		mExec = Executors.newSingleThreadScheduledExecutor();
		return new SSLSocketChannel2(inSocketChannel, sslEngine, mExec, inSelectionKey);
	}

	@Override
	public final WebSocketImpl createWebSocket(WebSocketAdapter inAdapter, IDraft inDraft, Socket inSocket) {
		LOGGER.debug("WebSocket create: " + inSocket.getRemoteSocketAddress());
		return new WebSocketImpl(inAdapter, inDraft, inSocket);
	}

	@Override
	public final WebSocketImpl createWebSocket(WebSocketAdapter inAdapter, List<IDraft> inDraftList, Socket inSocket) {
		LOGGER.debug("WebSocket create: " + inSocket.getRemoteSocketAddress());
		return new WebSocketImpl(inAdapter, inDraftList, inSocket);
	}

	public final SSLContext getSslContext() {
		SSLContext result = null;

		try {
			KeyStore ks = KeyStore.getInstance(mKeystoreType);
			File file = new File(mKeystorePath);
			URL url = file.toURL();
			ks.load(url.openStream(), mKeystoreStorePassword.toCharArray());

			KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
			kmf.init(ks, mKeystoreKeyPassword.toCharArray());
			TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
			tmf.init(ks);

			result = SSLContext.getInstance("TLSv1.2");
			result.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
		} catch (UnrecoverableKeyException | KeyManagementException | KeyStoreException | NoSuchAlgorithmException | CertificateException | IOException e) {
			LOGGER.error("", e);
		}

		return result;
	}
}
