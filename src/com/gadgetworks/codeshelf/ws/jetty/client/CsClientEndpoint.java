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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gadgetworks.codeshelf.ws.jetty.io.JsonRequestEncoder;
import com.gadgetworks.codeshelf.ws.jetty.io.JsonResponseDecoder;
import com.gadgetworks.codeshelf.ws.jetty.response.ResponseABC;
import com.gadgetworks.codeshelf.ws.jetty.server.RequestProcessor;
import com.gadgetworks.codeshelf.ws.jetty.server.RequestProcessorFactory;

@ClientEndpoint(encoders={JsonRequestEncoder.class},decoders={JsonResponseDecoder.class})
public class CsClientEndpoint {
	
	private static final Logger	LOGGER = LoggerFactory.getLogger(CsClientEndpoint.class);
	
	ResponseProcessor mResponseProcessor;
	
	public CsClientEndpoint() {	
		// needs to use a factory patter unfortunately, since Jetty creates endpoints
		// and does not support user properties to be passed in
		mResponseProcessor = ResponseProcessorFactory.getInstance();
	}
	
    @OnOpen
	public final void onConnect(Session session) {
        System.out.println("WebSocket connected: " + session);
    }
    
    @OnMessage
    public void onMessage(Session session, ResponseABC response) throws IOException, EncodeException {
    	mResponseProcessor.handleResponse(response);
    }
    
    @OnClose
	public final void onDisconnect(CloseReason reason) {
        System.out.println("WebSocket closed: " + reason);
    }
    
    @OnError
	public final void onError(Throwable cause) {
        cause.printStackTrace(System.err);
    }
}
