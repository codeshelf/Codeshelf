package com.gadgetworks.codeshelf.application;

import java.io.File;
import java.net.URL;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gadgetworks.codeshelf.util.IConfiguration;
import com.gadgetworks.codeshelf.ws.jetty.server.ServerWatchdogThread;
import com.gadgetworks.codeshelf.ws.jetty.server.SessionManager;
import com.google.inject.Inject;
import com.sun.jersey.spi.container.servlet.ServletContainer;

public class ApiServer {
	String	DEFAULT_HOSTNAME = "localhost";
	int		DEFAULT_PORTNUM	= 8089;

	private int mPort = DEFAULT_PORTNUM;
	private String mHost = DEFAULT_HOSTNAME;
    private String	mKeystorePath;
	private String	mKeystoreStorePassword="x2HPbC2avltYQR";
	private String	mKeystoreKeyPassword="x2HPbC2avltYQR";

	private static final Logger	LOGGER	= LoggerFactory.getLogger(ApiServer.class);

    private Server mServer = null;
	
	@Inject
	public ApiServer(IConfiguration configuration) {		
		// fetch properties from configuration files
		this.mHost = configuration.getString("apiserver.hostname", DEFAULT_HOSTNAME);
		this.mPort = configuration.getInt("apiserver.port", DEFAULT_PORTNUM);
		this.mKeystorePath = configuration.getString("keystore.path");
		this.mKeystoreStorePassword = configuration.getString("keystore.store.password");
		this.mKeystoreKeyPassword = configuration.getString("keystore.key.password");
		
	}

	public final void startServer() {
		LOGGER.info("Starting Admin Server");
		
		try {
			
			mServer = new Server();

			SslContextFactory sslContextFactory = new SslContextFactory();
			File file = new File(mKeystorePath);
			URL url = file.toURI().toURL();
			Resource keyStoreResource = Resource.newResource(url);
			sslContextFactory.setKeyStoreResource(keyStoreResource);
			sslContextFactory.setKeyStorePassword(mKeystoreStorePassword);
			sslContextFactory.setKeyManagerPassword(mKeystoreKeyPassword);
			
			ServerConnector sslConnector = new ServerConnector(mServer, sslContextFactory);
			sslConnector.setHost(mHost);
			sslConnector.setPort(mPort);
			
			mServer.setConnectors(new Connector[] {sslConnector});
			
			ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
			context.setContextPath("/*");
			mServer.setHandler(context);
			
			ServletHolder sh = new ServletHolder(ServletContainer.class);
			sh.setInitParameter("com.sun.jersey.config.property.resourceConfigClass", "com.sun.jersey.api.core.PackagesResourceConfig");
	        sh.setInitParameter("com.sun.jersey.config.property.packages", "com.gadgetworks.codeshelf.application.apiresources");
	        sh.setInitParameter("com.sun.jersey.api.json.POJOMappingFeature", "true");
			context.addServlet(sh, "/*");

			mServer.start();
		} catch (Exception e) {
			LOGGER.error("Failed to start api server", e);
		}
	}
	
	public final void stopServer() {
		try {		
			mServer.stop();
		} catch (Exception e) {
			LOGGER.error("Failed to stop api server", e);
		}	
	}
}