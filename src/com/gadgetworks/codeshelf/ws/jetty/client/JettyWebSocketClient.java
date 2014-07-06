package com.gadgetworks.codeshelf.ws.jetty.client;

import java.io.IOException;
import java.net.URI;

import javax.websocket.CloseReason;
import javax.websocket.CloseReason.CloseCodes;
import javax.websocket.ContainerProvider;
import javax.websocket.DeploymentException;
import javax.websocket.EncodeException;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gadgetworks.codeshelf.ws.jetty.request.RequestABC;

public class JettyWebSocketClient {
	
	private static final Logger	LOGGER = LoggerFactory.getLogger(JettyWebSocketClient.class);
	
	String mConnectionString = null;
	Session mSession;
	ResponseProcessor mResponseProcessor = null;
	
	public JettyWebSocketClient(String connectionString) {
		mConnectionString = connectionString;
	}
	
    public void connect() throws DeploymentException, IOException {
    	
    	LOGGER.info("Connecting to WS server at "+mConnectionString);
        URI uri = URI.create(mConnectionString);
        WebSocketContainer container = ContainerProvider.getWebSocketContainer();
        mSession = container.connectToServer(CsClientEndpoint.class,uri);
        if (mSession.isOpen()) {
        	LOGGER.info("Connected to WS server");
        }
        else {
        	LOGGER.warn("Failed to start session on "+mConnectionString);
        }
    }
    
    public void disconnect() throws IOException {
    	mSession.close(new CloseReason(CloseCodes.NORMAL_CLOSURE, "Connection closed by client"));
    }
    
    public void sendRequest(RequestABC request) throws IOException, EncodeException, DeploymentException {
    	// TODO: add connection life cycle management, handling of messages that can't get delivered 
    	// immediately etc.
    	if (mSession == null || !mSession.isOpen()) {
    		// connect to server, if not already connected
    		connect();
    	}
    	if (mSession.isOpen()) {
    		mSession.getBasicRemote().sendObject(request);
    	}
    	else {
    		LOGGER.error("Unable to deliver message. Can't connect to server.");
    	}
    }

	public void setResponseProcessor(ResponseProcessor responseProcessor) {
		mResponseProcessor = responseProcessor;
	}
}
