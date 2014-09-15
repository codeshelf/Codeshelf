package com.gadgetworks.codeshelf.ws.jetty.server;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import javax.websocket.server.ServerContainer;
import javax.websocket.server.ServerEndpoint;

import org.eclipse.jetty.server.NetworkTrafficServerConnector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.websocket.jsr356.server.deploy.WebSocketServerContainerInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gadgetworks.codeshelf.util.PropertyUtils;

@ServerEndpoint(value = "/")
public class JettyWebSocketServer {
	
	String	DEFAULT_HOSTNAME = "localhost";
	int		DEFAULT_PORTNUM	= 8444;
	
	private static final Logger	LOGGER = LoggerFactory.getLogger(JettyWebSocketServer.class);
	
	private int mPort = 8444;
	private String mHost = "localhost";
    private Server mServer = null;

	private String	mKeystoreStorePassword="x2HPbC2avltYQR";
	private String	mKeystoreKeyPassword="x2HPbC2avltYQR";
	
	private final ServerWatchdogThread watchdog;
	
	private String	mKeystorePath="/etc/codeshelf.keystore";

	public JettyWebSocketServer() {		
		// fetch properties from configuration files
		this.mHost = PropertyUtils.getString("websocket.hostname", DEFAULT_HOSTNAME);
		this.mPort = PropertyUtils.getInt("websocket.port", DEFAULT_PORTNUM);
		this.mKeystorePath = PropertyUtils.getString("keystore.path");
		this.mKeystoreStorePassword = PropertyUtils.getString("keystore.store.password");
		this.mKeystoreKeyPassword = PropertyUtils.getString("keystore.key.password");	

		boolean suppressKeepAlive = PropertyUtils.getBoolean("websocket.idle.suppresskeepalive");
		boolean killIdle = PropertyUtils.getBoolean("websocket.idle.kill");

		this.watchdog = new ServerWatchdogThread(SessionManager.getInstance());
		this.watchdog.setSuppressKeepAlive(suppressKeepAlive);
		this.watchdog.setKillIdle(killIdle);
		this.watchdog.start();
	}
		
	public final void start() throws Exception {
		LOGGER.info("Starting Jetty WebSocket Server on port "+mPort+"...");
		
		// create server and connector
		mServer = new Server();
		// create SSL context factory
		SslContextFactory sslContextFactory = new SslContextFactory();
		File file = new File(mKeystorePath);
		URL url = file.toURL();
		Resource keyStoreResource = Resource.newResource(url);
		sslContextFactory.setKeyStoreResource(keyStoreResource);
		sslContextFactory.setKeyStorePassword(mKeystoreStorePassword);
		sslContextFactory.setKeyManagerPassword(mKeystoreKeyPassword);
		
	    //ServerConnector connector = new ServerConnector(mServer);
		NetworkTrafficServerConnector connector = new NetworkTrafficServerConnector(mServer,sslContextFactory);
		connector.setHost(mHost);
		connector.setPort(mPort);
	    mServer.addConnector(connector);
	
	    // Setup the basic application "context" for this application at "/"
	    ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
	    mServer.setHandler(context);
        // Initialize javax.websocket layer
        ServerContainer wscontainer = WebSocketServerContainerInitializer.configureContext(context);
        wscontainer.addEndpoint(CsServerEndPoint.class);

        
        LOGGER.info("Jetty WebSocket Server starting");
        // start server
        mServer.start();
        LOGGER.info("Jetty WebSocket Server started");
	}

	public void stop() throws IOException, InterruptedException {
		try {
			mServer.stop();
		} catch (Exception e) {
			LOGGER.error("Failed to stop Jetty WebSocket server", e);
		}
	}

}
