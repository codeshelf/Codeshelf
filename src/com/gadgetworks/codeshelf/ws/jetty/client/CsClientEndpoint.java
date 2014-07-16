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

import com.gadgetworks.codeshelf.ws.jetty.io.JsonRequestEncoder;
import com.gadgetworks.codeshelf.ws.jetty.io.JsonResponseDecoder;
import com.gadgetworks.codeshelf.ws.jetty.protocol.response.ResponseABC;

@ClientEndpoint(encoders={JsonRequestEncoder.class},decoders={JsonResponseDecoder.class})
public class CsClientEndpoint {
	
	private static final Logger	LOGGER = LoggerFactory.getLogger(CsClientEndpoint.class);

	@Getter @Setter
	ResponseProcessor responseProcessor;
	
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
    public void onMessage(Session session, ResponseABC response) throws IOException, EncodeException {
    	responseProcessor.handleResponse(response);
		this.messageCoordinator.unregisterRequest(response);
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
