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

import lombok.Getter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;
import com.gadgetworks.codeshelf.metrics.MetricsGroup;
import com.gadgetworks.codeshelf.metrics.MetricsService;
import com.gadgetworks.codeshelf.platform.persistence.PersistenceService;
import com.gadgetworks.codeshelf.ws.ContextLogging;
import com.gadgetworks.codeshelf.ws.jetty.io.JsonDecoder;
import com.gadgetworks.codeshelf.ws.jetty.io.JsonEncoder;
import com.gadgetworks.codeshelf.ws.jetty.protocol.message.KeepAlive;
import com.gadgetworks.codeshelf.ws.jetty.protocol.message.MessageABC;
import com.gadgetworks.codeshelf.ws.jetty.protocol.message.MessageProcessor;
import com.gadgetworks.codeshelf.ws.jetty.protocol.request.RequestABC;
import com.gadgetworks.codeshelf.ws.jetty.protocol.response.ResponseABC;

@ServerEndpoint(value="/",encoders={JsonEncoder.class},decoders={JsonDecoder.class})
public class CsServerEndPoint {

	private static final Logger	LOGGER = LoggerFactory.getLogger(CsServerEndPoint.class);

	@Getter
	private final PersistenceService persistenceService;

	private static final Counter messageCounter = MetricsService.addCounter(MetricsGroup.WSS,"messages.received");

	MessageProcessor messageProcessor;
	SessionManager sessionManager;
	
	MetricRegistry metricsRegistry;
    
	// time to close session after mins of inactivity
	int idleTimeOut = 60;
	
	public CsServerEndPoint() {
		persistenceService = PersistenceService.getInstance();
		sessionManager = SessionManager.getInstance();
		messageProcessor = MessageProcessorFactory.getInstance();
		if (messageProcessor==null) {
			LOGGER.error("Unable to get Web Socket message processor from factory");
		}
	}
	
	@OnOpen
    public void onOpen(Session session, EndpointConfig ec) {
		ContextLogging.set(sessionManager.getSession(session));
		try {
			session.setMaxIdleTimeout(1000*60*idleTimeOut);
			LOGGER.info("WS Session Started: " + session.getId()+", timeout: "+session.getMaxIdleTimeout());
			sessionManager.sessionStarted(session);
		} finally {
			ContextLogging.clear();
		}
    }

    @OnMessage(maxMessageSize=JsonEncoder.WEBSOCKET_MAX_MESSAGE_SIZE)
    public void onMessage(Session session, MessageABC message) throws IOException, EncodeException {
    	messageCounter.inc();
    	this.getPersistenceService().beginTenantTransaction();
    	UserSession csSession = sessionManager.getSession(session);
    	
		ContextLogging.set(csSession);
    	try{
    		sessionManager.messageReceived(session);
        	if (message instanceof ResponseABC) {
        		ResponseABC response = (ResponseABC) message;
                LOGGER.debug("Received response on session "+csSession+": " + response);
                messageProcessor.handleResponse(csSession, response);
        	}
        	else if (message instanceof RequestABC) {
        		RequestABC request = (RequestABC) message;
                LOGGER.debug("Received request on session "+csSession+": " + request);
               // pass request to processor to execute command
                ResponseABC response = messageProcessor.handleRequest(csSession, request);
                if (response!=null) {
                	// send response to client
                	LOGGER.debug("Sending response "+response+" for request "+request);
                	csSession.sendMessage(response);
                }
                else {
                	LOGGER.warn("No response generated for request "+request);
                }    	
        	}  else if (!(message instanceof KeepAlive)) {
        		// handle all other messages
            	LOGGER.debug("Received message on session "+csSession+": "+message);
            	messageProcessor.handleOtherMessage(csSession, message);
        	}
    	} finally {
    		ContextLogging.clear();
    	}
    }
    
	@OnClose
    public void onClose(Session session, CloseReason reason) {
		ContextLogging.set(sessionManager.getSession(session));
		try {
	    	LOGGER.info(String.format("WS Session %s closed because of %s", session.getId(), reason));
			sessionManager.sessionEnded(session);		
		} finally {
			ContextLogging.clear();
		}
    }
    
    @OnError
    public void onError(Session session, Throwable cause) {
    	ContextLogging.set(sessionManager.getSession(session));
    	LOGGER.error("WebSocket error", cause);
    	ContextLogging.clear();
    }


}