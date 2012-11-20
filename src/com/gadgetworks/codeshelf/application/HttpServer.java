/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: HttpServer.java,v 1.9 2012/11/20 04:10:56 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.application;

import java.io.File;
import java.net.Socket;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.Arrays;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jetty.io.NetworkTrafficListener;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.NCSARequestLog;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.RequestLogHandler;
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

	private String				mWebSiteContentPath;
	private String				mWebSiteHostname;
	private int					mWebSitePortNum;

	private String				mWebAppContentPath;
	private String				mWebAppHostname;
	private int					mWebAppPortNum;

	private String				mKeystorePath;
	private String				mKeystoreStorePassword;
	private String				mKeystoreKeyPassword;

	private Server				mWebappServer;
	private Server				mWebsiteServer;

	@Inject
	public HttpServer(@Named(IHttpServer.WEBSITE_CONTENT_PATH_PROPERTY) final String inWebSiteContentPath,
		@Named(IHttpServer.WEBSITE_HOSTNAME_PROPERTY) final String inWebSiteHostname,
		@Named(IHttpServer.WEBSITE_PORTNUM_PROPERTY) final int inWebSitePortNum,
		@Named(IHttpServer.WEBAPP_CONTENT_PATH_PROPERTY) final String inWebAppContentPath,
		@Named(IHttpServer.WEBAPP_HOSTNAME_PROPERTY) final String inWebAppHostname,
		@Named(IHttpServer.WEBAPP_PORTNUM_PROPERTY) final int inWebAppPortNum,
		@Named(KEYSTORE_PATH_PROPERTY) final String inKeystorePath,
		@Named(KEYSTORE_STORE_PASSWORD_PROPERTY) final String inKeystoreStorePassword,
		@Named(KEYSTORE_KEY_PASSWORD_PROPERTY) final String inKeystoreKeyPassword) {

		mWebSiteContentPath = inWebSiteContentPath;
		mWebSiteHostname = inWebSiteHostname;
		mWebSitePortNum = inWebSitePortNum;

		mWebAppContentPath = inWebAppContentPath;
		mWebAppHostname = inWebAppHostname;
		mWebAppPortNum = inWebAppPortNum;

		mKeystorePath = inKeystorePath;
		mKeystoreStorePassword = inKeystoreStorePassword;
		mKeystoreKeyPassword = inKeystoreKeyPassword;
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
			File file = new File(mKeystorePath);
			URL url = file.toURL();
			Resource keyStoreResource = Resource.newResource(url);
			sslContextFactory.setKeyStoreResource(keyStoreResource);
			sslContextFactory.setKeyStorePassword(mKeystoreStorePassword);
			sslContextFactory.setKeyManagerPassword(mKeystoreKeyPassword);

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
			ResourceHandler resourceHandler = new ResourceHandler();
			resourceHandler.setDirectoriesListed(false);
			resourceHandler.setWelcomeFiles(new String[] { inDefaultPage });
			resourceHandler.setResourceBase(inContentPath);

			RequestLogHandler requestLogHandler = new RequestLogHandler();

//	        NCSARequestLog requestLog = new NCSARequestLog();
//	        requestLog.setRetainDays(90);
//	        requestLog.setAppend(true);
//	        requestLog.setExtended(false);
//	        requestLog.setLogTimeZone("GMT");
//	        requestLog.setLogCookies(false);
//	        requestLogHandler.setRequestLog(requestLog);
	        
			HandlerList handlers = new HandlerList();
			//			handlers.setHandlers(new Handler[] { webappResourceHandler, new DefaultHandler() });
			handlers.setHandlers(new Handler[] { resourceHandler, requestLogHandler });
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
