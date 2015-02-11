package com.codeshelf.ws.jetty.client;

import java.io.IOException;
import java.net.URI;

import javax.websocket.CloseReason;
import javax.websocket.CloseReason.CloseCodes;
import javax.websocket.DeploymentException;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;

import lombok.Getter;
import lombok.Setter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.ws.jetty.protocol.message.MessageABC;
import com.codeshelf.ws.jetty.protocol.message.MessageProcessor;
import com.codeshelf.ws.jetty.protocol.request.RequestABC;
import com.codeshelf.ws.jetty.server.SessionManager;

public class JettyWebSocketClient {
	
	private static final Logger	LOGGER = LoggerFactory.getLogger(JettyWebSocketClient.class);
	
	private final URI uri;
	
	Session session;
	
	MessageProcessor responseProcessor = null;
	
	MessageCoordinator messageCoordinator = null;
	
	CsClientEndpoint endpoint = null;

	private WebSocketEventListener	eventListener;
	
	WebSocketContainer container;
	
	@Getter @Setter
	boolean queueingEnabled = true;
	
	MessageQueue queue = new MessageQueue();
	
	@Getter @Setter
	long lastMessageSent = 0;
	
	@Getter @Setter
	long lastMessageReceived = 0;
	
	public JettyWebSocketClient(WebSocketContainer webSocketContainer, URI uri, MessageProcessor responseProcessor, WebSocketEventListener eventListener) {
		this.uri = uri;
		this.eventListener = eventListener;
		this.responseProcessor = responseProcessor;
		
        // create and configure WS endpoint                 
        endpoint = new CsClientEndpoint(this);
        endpoint.setSessionManager(SessionManager.getInstance());
        endpoint.setMessageProcessor(responseProcessor);
     
        messageCoordinator = new MessageCoordinator(); 
        endpoint.setMessageCoordinator(messageCoordinator);
        responseProcessor.setMessageCoordinator(messageCoordinator);
        container = webSocketContainer;
	}
	
    public void connect() throws DeploymentException, IOException {    			
    	LOGGER.info("Connecting to WS server at "+ uri);
        // connect to the server
        Session session = container.connectToServer(endpoint,uri);
        if (session.isOpen()) {
        	LOGGER.info("Connected to WS server");
        }
        else {
        	LOGGER.warn("Failed to start session on "+ uri);
        }
    }
    
    public void disconnect() throws IOException {
    	if(session!=null) {
    		LOGGER.debug("closing session");
    		//this does not notify the endpoint callback until the closure messages reaches client, which 
    		// when disconnected may take up to session.getMaxIdleTimeout
        	session.close(new CloseReason(CloseCodes.NORMAL_CLOSURE, "Connection closed by client"));
        	//fire disconnected to listeners
        	disconnected(session);
    	} else {
    		LOGGER.warn("disconnecting client requested, but there is no session to close");
    	}
    }
    
    public boolean sendMessage(MessageABC message) {
    	try {
	    	if (!isConnected()) {
	    		if (!this.queueingEnabled) {
		    		// unable to send message...
		    		LOGGER.error("Unable to send message "+message+": Not connected to server");
		    		return false;
	    		}
	    		else {
	    			// attempt to queue message
	    			if (this.queue.addMessage(message)) {
			    		LOGGER.warn("Not connected to server. Message "+message+" queued.");
			    		return true;
	    			}
	    			else {
			    		LOGGER.error("Unable to send message "+message+": Not connected to server and queueing failed");
			    		return false;
	    			}
	    		}
	    	}
    		session.getBasicRemote().sendObject(message);
			this.messageSent();
    		if (message instanceof RequestABC) {
    			// keep track of request
    			this.messageCoordinator.registerRequest((RequestABC)message);
    		}
    		return true;
    	}
    	catch (Exception e) {
    		LOGGER.error("Exception while trying to send message #"+message.getMessageId(),e);
    		try {
    			this.disconnect();
    		}
    		catch (IOException ioe) {
    			LOGGER.debug("IOException during disconnect", ioe);
    		}
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
    	
    	// send queued messages
    	while (this.queue.getQueueLength()>0) {
    		MessageABC message = this.queue.peek();
    		if (this.sendMessage(message)) {
    			// remove from queue if sent
    			this.queue.remove(message);
    		}
    		else {
        		LOGGER.warn("Failed to send queued message #"+message.getMessageId());
    		}
    	}
    }

	public void disconnected(Session session) {
		if (session.equals(this.session)) {
			this.session = null;
	    	if (this.eventListener!=null) eventListener.disconnected();
		}
		else {
			LOGGER.debug("Session being closed is no longer current: " + session);
		}
	}

	public void messageReceived() {
		this.lastMessageReceived = System.currentTimeMillis();
	}

	public void messageSent() {
		this.lastMessageSent = System.currentTimeMillis();
	}
}
