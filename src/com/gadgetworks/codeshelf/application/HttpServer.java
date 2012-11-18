/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: HttpServer.java,v 1.7 2012/11/18 06:04:30 jeffw Exp $
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
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.server.nio.NetworkTrafficSelectChannelConnector;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.ssl.SslContextFactory;

import com.google.inject.Inject;
import com.google.inject.name.Named;

/**
 * @author jeffw
 *
 */
public class HttpServer implements IHttpServer {

	private static final Log	LOGGER						= LogFactory.getLog(HttpServer.class);

	private static final String	WEBAPP_SERVER_THREADNAME	= "Webapp Server";
	private static final String	WEBSITE_SERVER_THREADNAME	= "Website Server";

	private static final String	KEYSTORE					= "codeshelf.keystore";
	private static final String	STOREPASSWORD				= "x2HPbC2avltYQR";
	private static final String	KEYPASSWORD					= "x2HPbC2avltYQR";

	private String				mWebSiteContentPath;
	private String				mWebSiteHostname;
	private int					mWebSitePortNum;

	private String				mWebAppContentPath;
	private String				mWebAppHostname;
	private int					mWebAppPortNum;

	private Server				mWebappServer;
	private Server				mWebsiteServer;

	@Inject
	public HttpServer(@Named(IHttpServer.WEBSITE_CONTENT_PATH) final String inWebSiteContentPath,
		@Named(IHttpServer.WEBSITE_HOSTNAME) final String inWebSiteHostname,
		@Named(IHttpServer.WEBSITE_PORTNUM) final int inWebSitePortNum,
		@Named(IHttpServer.WEBAPP_CONTENT_PATH) final String inWebAppContentPath,
		@Named(IHttpServer.WEBAPP_HOSTNAME) final String inWebAppHostname,
		@Named(IHttpServer.WEBAPP_PORTNUM) final int inWebAppPortNum) {

		mWebSiteContentPath = inWebSiteContentPath;
		mWebSiteHostname = inWebSiteHostname;
		mWebSitePortNum = inWebSitePortNum;

		mWebAppContentPath = inWebAppContentPath;
		mWebAppHostname = inWebAppHostname;
		mWebAppPortNum = inWebAppPortNum;
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.application.IHttpServer#startServer()
	 */
	public final void startServer() {

		if ((mWebSiteContentPath != null) && (mWebSiteContentPath.length() > 0)) {
			Thread websiteServerThread = new Thread(new Runnable() {
				public void run() {
					mWebsiteServer = doStartServer("home.html", mWebSiteContentPath, mWebSiteHostname, mWebSitePortNum);
				}
			}, WEBSITE_SERVER_THREADNAME);
			websiteServerThread.start();
		}

		if ((mWebAppContentPath != null) && (mWebAppContentPath.length() > 0)) {
			Thread webappServerThread = new Thread(new Runnable() {
				public void run() {
					mWebappServer = doStartServer("codeshelf.html", mWebAppContentPath, mWebAppHostname, mWebAppPortNum);
				}
			}, WEBAPP_SERVER_THREADNAME);
			webappServerThread.start();
		}
	}

	// --------------------------------------------------------------------------
	/**
	 */
	private Server doStartServer(final String inDefaultPage, final String inContentPath, final String inHostname, final int inPortNum) {

		Server result = null;

		try {
			result = new Server();

			SslContextFactory sslContextFactory = new SslContextFactory();
			String keystorePath = System.getProperty(KEYSTORE);
			File file = new File(keystorePath);
			URL url = file.toURL();
			Resource keyStoreResource = Resource.newResource(url);
			sslContextFactory.setKeyStoreResource(keyStoreResource);
			sslContextFactory.setKeyStorePassword(STOREPASSWORD);
			sslContextFactory.setKeyManagerPassword(KEYPASSWORD);

			NetworkTrafficSelectChannelConnector connector = new NetworkTrafficSelectChannelConnector(result, sslContextFactory);
			connector.setHost(inHostname);
			connector.setPort(inPortNum);
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

			result.addConnector(connector);

			// Website.
			ResourceHandler websiteResourceHandler = new ResourceHandler();
			websiteResourceHandler.setDirectoriesListed(false);
			websiteResourceHandler.setWelcomeFiles(new String[] { inDefaultPage });
			websiteResourceHandler.setResourceBase(inContentPath);

			HandlerList handlers = new HandlerList();
			//			handlers.setHandlers(new Handler[] { webappResourceHandler, new DefaultHandler() });
			handlers.setHandlers(new Handler[] { websiteResourceHandler });
			result.setHandler(handlers);

			result.start();
			result.join();
		} catch (InterruptedException e) {
			LOGGER.error("", e);
		} catch (Exception e) {
			LOGGER.error("", e);
		}

		return result;
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.application.IHttpServer#stopServer()
	 */
	public final void stopServer() {
		if (mWebsiteServer != null) {
			mWebsiteServer.setStopAtShutdown(true);
		}
		if (mWebappServer != null) {
			mWebappServer.setStopAtShutdown(true);
		}
	}
}
