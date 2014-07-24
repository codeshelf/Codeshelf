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

import javax.websocket.Session;

import org.eclipse.jetty.websocket.jsr356.server.deploy.WebSocketServerContainerInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gadgetworks.codeshelf.ws.jetty.protocol.request.RequestABC;
import com.google.inject.Inject;

@ServerEndpoint(value = "/")
public class JettyWebSocketServer {

	private static final Logger	LOGGER = LoggerFactory.getLogger(JettyWebSocketServer.class);
	
	private int mPort = 8444;
    private Server mServer = null;
    //private RequestProcessor mRequestProcessor;
	//private CsServerEndPoint endpoint;

    // TODO: needs to be passed in
	private String	mKeystoreStorePassword="x2HPbC2avltYQR";
	private String	mKeystoreKeyPassword="x2HPbC2avltYQR";
	
	private final ServerWatchdogThread watchdog = new ServerWatchdogThread(this);
	
	private String	mKeystorePath="/etc/codeshelf.keystore";

	@Inject
	public JettyWebSocketServer() {
		this.watchdog.start();
	}
		
	public final void start() {
		LOGGER.info("Starting Jetty WebSocket Server on port "+mPort+"...");
		
	    try {
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
			connector.setPort(mPort);
		    mServer.addConnector(connector);
		
		    // Setup the basic application "context" for this application at "/"
		    ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
		    mServer.setHandler(context);
	        // Initialize javax.websocket layer
	        ServerContainer wscontainer = WebSocketServerContainerInitializer.configureContext(context);
	        wscontainer.addEndpoint(CsServerEndPoint.class);
	        
	        // start server
	        mServer.start();
	        //mServer.join();
	        
			LOGGER.info("Jetty WebSocket Server started");
	    } catch (Exception e) {
	    	LOGGER.error("Failed to start WebWocket Server", e);
	    }	

	}

	public void stop() throws IOException, InterruptedException {
		try {
			mServer.stop();
		} catch (Exception e) {
			LOGGER.error("Failed to stop Jetty WebSocket server", e);
		}
	}
	
    public boolean sendRequest(Session session, RequestABC request) {
    	try {
	    	if (session==null || !session.isOpen()) {
	    		LOGGER.warn("Unable to send request "+request+": Not connected");
	    		return false;
	    	}
    		session.getBasicRemote().sendObject(request);
    		return true;
    	}
    	catch (Exception e) {
    		LOGGER.error("Exception while trying to send request #"+request.getMessageId(),e);
    		return false;
    	}
    }
}
