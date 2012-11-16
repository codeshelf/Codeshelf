/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: HttpServer.java,v 1.6 2012/11/16 08:05:56 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.application;

import java.io.File;
import java.net.Socket;
import java.net.URL;
import java.nio.ByteBuffer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jetty.io.NetworkTrafficListener;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.server.nio.NetworkTrafficSelectChannelConnector;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.resource.URLResource;
import org.eclipse.jetty.util.ssl.SslContextFactory;

/**
 * @author jeffw
 *
 */
public class HttpServer implements IHttpServer {

	private static final Log		LOGGER					= LogFactory.getLog(HttpServer.class);

	private static final String		HTTP_SERVER_THREADNAME	= "HTTP Server";
	private static final String		HTTP_SERVER_HOSTNAME	= "localhost";
	private static final Integer	HTTP_SERVER_PORTNUM		= 8443;

	private static final String		KEYSTORE				= "codeshelf.keystore";
	private static final String		STOREPASSWORD			= "x2HPbC2avltYQR";
	private static final String		KEYPASSWORD				= "x2HPbC2avltYQR";

	private Server					mServer;

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.application.IHttpServer#startServer()
	 */
	public final void startServer() {

		Thread httpThread = new Thread(new Runnable() {
			public void run() {
				doStartServer();
			}
		}, HTTP_SERVER_THREADNAME);
		httpThread.start();
	}

	private void doStartServer() {

		try {
			mServer = new Server();

			SslContextFactory sslContextFactory = new SslContextFactory();
			String keystorePath = System.getProperty(KEYSTORE);
			File file=new File(keystorePath);
			URL url = file.toURL();
			Resource keyStoreResource = Resource.newResource(url);
			sslContextFactory.setKeyStoreResource(keyStoreResource);
			sslContextFactory.setKeyStorePassword(STOREPASSWORD);
			sslContextFactory.setKeyManagerPassword(KEYPASSWORD);

			NetworkTrafficSelectChannelConnector connector = new NetworkTrafficSelectChannelConnector(mServer, sslContextFactory);
			connector.setHost(HTTP_SERVER_HOSTNAME);
			connector.setPort(HTTP_SERVER_PORTNUM);
			connector.addNetworkTrafficListener(new NetworkTrafficListener() {
				public void outgoing(Socket inSocket, ByteBuffer inByteBuffer) {
				}

				public void opened(Socket inSocket) {
					LOGGER.info("HTTP CONNECTION OPENED: " + inSocket.getInetAddress());
				}

				public void incoming(Socket inSocket, ByteBuffer inByteBuffer) {
				}

				public void closed(Socket inSocket) {
				}
			});

			mServer.addConnector(connector);

			ResourceHandler resourceHandler = new ResourceHandler();
			resourceHandler.setDirectoriesListed(false);
			resourceHandler.setWelcomeFiles(new String[] { "codeshelf.dev.html" });
			resourceHandler.setResourceBase("../CodeshelfUX/");

			//		ContextHandler contextHandler = new ContextHandler();
			//		contextHandler.

			HandlerList handlers = new HandlerList();
			handlers.setHandlers(new Handler[] { resourceHandler, new DefaultHandler() });
			mServer.setHandler(handlers);

			mServer.start();
			mServer.join();
		} catch (InterruptedException e) {
			LOGGER.error("", e);
		} catch (Exception e) {
			LOGGER.error("", e);
		}
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.application.IHttpServer#stopServer()
	 */
	public final void stopServer() {
		if (mServer != null) {
			mServer.setStopAtShutdown(true);
		}
	}
}
