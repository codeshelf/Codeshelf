/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: HttpServer.java,v 1.4 2012/10/24 01:00:59 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.application;

import java.net.Socket;
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

/**
 * @author jeffw
 *
 */
public class HttpServer implements IHttpServer {

	private static final Log		LOGGER					= LogFactory.getLog(HttpServer.class);

	private static final String		HTTPSERVER_THREADNAME	= "HTTP Server";
	private static final Integer	HTTP_SERVER_PORT_NUM	= 8000;

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
		}, HTTPSERVER_THREADNAME);
		httpThread.start();
	}

	private void doStartServer() {

		mServer = new Server();
		NetworkTrafficSelectChannelConnector connector = new NetworkTrafficSelectChannelConnector(mServer);
		connector.setHost("localhost");
		connector.setPort(HTTP_SERVER_PORT_NUM);
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

		HandlerList handlers = new HandlerList();
		handlers.setHandlers(new Handler[] { resourceHandler, new DefaultHandler() });
		mServer.setHandler(handlers);

		try {
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
