package com.gadgetworks.codeshelf.ws.jetty.client;

import java.io.IOException;
import java.net.URI;

import javax.websocket.CloseReason;
import javax.websocket.CloseReason.CloseCodes;
import javax.websocket.ContainerProvider;
import javax.websocket.DeploymentException;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;

import lombok.Getter;
import lombok.Setter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gadgetworks.codeshelf.ws.jetty.protocol.message.MessageProcessor;
import com.gadgetworks.codeshelf.ws.jetty.protocol.request.RequestABC;

public class JettyWebSocketClient {
	
	private static final Logger	LOGGER = LoggerFactory.getLogger(JettyWebSocketClient.class);
	
	String connectionString = null;
	Session session;
	
	MessageProcessor responseProcessor = null;
	
	MessageCoordinator messageCoordinator = null;
	
	CsClientEndpoint endpoint = null;

	private WebSocketEventListener	eventListener;
	
	WebSocketContainer container;
	
	@Getter @Setter
	boolean queueingEnabled = true;
	
	MessageQueue queue = new MessageQueue();
	
	public JettyWebSocketClient(String connectionString, MessageProcessor responseProcessor, WebSocketEventListener eventListener) {
		this.connectionString = connectionString;
		this.eventListener = eventListener;
		this.responseProcessor = responseProcessor;
		
        // create and configure WS endpoint                 
        endpoint = new CsClientEndpoint(this);
        endpoint.setMessageProcessor(responseProcessor);
     
        messageCoordinator = new MessageCoordinator(); 
        endpoint.setMessageCoordinator(messageCoordinator);
        responseProcessor.setMessageCoordinator(messageCoordinator);
        container = ContainerProvider.getWebSocketContainer();
	}
	
    public void connect() throws DeploymentException, IOException {    			
    	LOGGER.info("Connecting to WS server at "+connectionString);
        // connect to the server
        URI uri = URI.create(connectionString);
        
        Session session = container.connectToServer(endpoint,uri);
        if (session.isOpen()) {
        	LOGGER.info("Connected to WS server");
        }
        else {
        	LOGGER.warn("Failed to start session on "+connectionString);
        }
    }
    
    public void disconnect() throws IOException {
    	session.close(new CloseReason(CloseCodes.NORMAL_CLOSURE, "Connection closed by client"));
    }
    
    public boolean sendRequest(RequestABC request) {
    	try {
	    	if (!isConnected()) {
	    		LOGGER.warn("Unable to send request "+request+": Not connected to server");
	    		// TODO: queue message to be sent after reconnect
	    		return false;
	    	}
    		session.getBasicRemote().sendObject(request);
    		this.messageCoordinator.registerRequest(request);
    		return true;
    	}
    	catch (Exception e) {
    		LOGGER.error("Exception while trying to send request #"+request.getMessageId(),e);
    		return false;
    	}
    }
    
    public boolean isConnected() {
    	if (session == null || !session.isOpen()) {
    		return false;
    	}
    	return true;
    }
    
    public void connected(Session session) {
    	// set session object and notify
    	this.session = session;
    	if (this.eventListener!=null) eventListener.connected();
    }

	public void disconnected() {
		this.session = null;
    	if (this.eventListener!=null) eventListener.disconnected();
	}
    
}
