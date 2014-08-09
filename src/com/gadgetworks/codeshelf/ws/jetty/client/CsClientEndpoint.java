package com.gadgetworks.codeshelf.ws.jetty.client;

import java.io.IOException;

import javax.websocket.ClientEndpoint;
import javax.websocket.CloseReason;
import javax.websocket.EncodeException;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;

import lombok.Getter;
import lombok.Setter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gadgetworks.codeshelf.ws.jetty.io.JsonDecoder;
import com.gadgetworks.codeshelf.ws.jetty.io.JsonEncoder;
import com.gadgetworks.codeshelf.ws.jetty.protocol.message.MessageABC;
import com.gadgetworks.codeshelf.ws.jetty.protocol.message.MessageProcessor;
import com.gadgetworks.codeshelf.ws.jetty.protocol.request.RequestABC;
import com.gadgetworks.codeshelf.ws.jetty.protocol.response.ResponseABC;

@ClientEndpoint(encoders={JsonEncoder.class},decoders={JsonDecoder.class})
public class CsClientEndpoint {
	
	private static final Logger	LOGGER = LoggerFactory.getLogger(CsClientEndpoint.class);

	@Getter @Setter
	MessageProcessor messageProcessor;
	
	@Getter @Setter 
	MessageCoordinator messageCoordinator;
	
	JettyWebSocketClient client;

	public CsClientEndpoint(JettyWebSocketClient client) {
		this.client = client;
	}
	
    @OnOpen
	public final void onConnect(Session session) {
        LOGGER.info("Connected to server: " + session);
        client.connected(session);
    }
    
    @OnMessage
    public void onMessage(Session session, MessageABC message) throws IOException, EncodeException {
    	client.messageReceived();
    	if (message instanceof ResponseABC) {
    		ResponseABC response = (ResponseABC) message;
	    	messageProcessor.handleResponse(session, response);
			this.messageCoordinator.unregisterRequest(response);
    	}
    	else if (message instanceof RequestABC) {
    		RequestABC request = (RequestABC) message;
        	LOGGER.debug("Request received: "+request);
            // pass request to processor to execute command
            ResponseABC response = messageProcessor.handleRequest(session, request);
            if (response!=null) {
            	// send response to client
            	LOGGER.debug("Sending response "+response+" for request "+request);
            	client.sendMessage(response);
            }
            else {
            	LOGGER.warn("No response generated for request "+request);
            }    	
    	}    
    }
    
    @OnClose
	public final void onDisconnect(CloseReason reason) {
    	LOGGER.info("Disconnected from server: " + reason);
    	client.disconnected();
    }
    
    @OnError
	public final void onError(Throwable cause) {
    	LOGGER.error("WebSocket: "+cause.getMessage());
    }
}
