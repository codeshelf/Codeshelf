/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: HttpServer.java,v 1.1 2012/10/05 21:01:41 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.application;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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

	private static final Integer	HTTP_SERVER_PORT_NUM	= 8000;

	private Server					mServer;

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.application.IHttpServer#startServer()
	 */
	public final void startServer() {
		mServer = new Server();
		NetworkTrafficSelectChannelConnector connector = new NetworkTrafficSelectChannelConnector(mServer);
		connector.setPort(HTTP_SERVER_PORT_NUM);
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
