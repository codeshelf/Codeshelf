package com.gadgetworks.codeshelf.ws.jetty.server;

import java.io.IOException;

import javax.websocket.CloseReason;
import javax.websocket.EncodeException;
import javax.websocket.EndpointConfig;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.MetricRegistry;
import com.gadgetworks.codeshelf.ws.jetty.io.JsonDecoder;
import com.gadgetworks.codeshelf.ws.jetty.io.JsonEncoder;
import com.gadgetworks.codeshelf.ws.jetty.protocol.message.MessageABC;
import com.gadgetworks.codeshelf.ws.jetty.protocol.message.MessageProcessor;
import com.gadgetworks.codeshelf.ws.jetty.protocol.request.RequestABC;
import com.gadgetworks.codeshelf.ws.jetty.protocol.response.ResponseABC;

@ServerEndpoint(value="/",encoders={JsonEncoder.class},decoders={JsonDecoder.class})
public class CsServerEndPoint {

	private static final Logger	LOGGER = LoggerFactory.getLogger(CsServerEndPoint.class);

	MessageProcessor messageProcessor;
	SessionManager sessionManager;
	
	MetricRegistry metricsRegistry;
    
	// time to close session after mins of inactivity
	int idleTimeOut = 60;
	
	public CsServerEndPoint() {	
		sessionManager = SessionManager.getInstance();
		messageProcessor = MessageProcessorFactory.getInstance();
	}
	
	@OnOpen
    public void onOpen(Session session, EndpointConfig ec) {
		session.setMaxIdleTimeout(1000*60*idleTimeOut);
		LOGGER.info("WS Session Started: " + session.getId()+", timeout: "+session.getMaxIdleTimeout());
		sessionManager.sessionStarted(session);
    }

    @OnMessage
    public void onMessage(Session session, MessageABC message) throws IOException, EncodeException {
    	if (message instanceof ResponseABC) {
    		ResponseABC response = (ResponseABC) message;
            LOGGER.debug("Received response on session "+session.getId()+": " + response);
            messageProcessor.handleResponse(session, response);
    	}
    	else if (message instanceof RequestABC) {
    		RequestABC request = (RequestABC) message;
            LOGGER.debug("Received request on session "+session.getId()+": " + request);
            // pass request to processor to execute command
            ResponseABC response = messageProcessor.handleRequest(session,request);
            if (response!=null) {
            	// send response to client
            	LOGGER.debug("Sending response "+response+" for request "+request);
            	session.getBasicRemote().sendObject(response);
            }
            else {
            	LOGGER.warn("No response generated for request "+request);
            }    	
    	}    
    }
    
    @OnClose
    public void onClose(Session session, CloseReason reason) {
    	LOGGER.info(String.format("WS Session %s closed because of %s", session.getId(), reason));
		sessionManager.sessionEnded(session);
    }
    
    @OnError
    public void onError(Session session, Throwable cause) {
    	LOGGER.error("WebSocket error", cause);
    }
}