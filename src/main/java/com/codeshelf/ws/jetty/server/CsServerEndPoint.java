package com.codeshelf.ws.jetty.server;

import javax.websocket.CloseReason;
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
import com.codeshelf.application.ContextLogging;
import com.codeshelf.metrics.MetricsGroup;
import com.codeshelf.metrics.MetricsService;
import com.codeshelf.platform.multitenancy.Tenant;
import com.codeshelf.platform.persistence.PersistenceService;
import com.codeshelf.platform.persistence.TenantPersistenceService;
import com.codeshelf.ws.jetty.io.JsonDecoder;
import com.codeshelf.ws.jetty.io.JsonEncoder;
import com.codeshelf.ws.jetty.protocol.message.IMessageProcessor;
import com.codeshelf.ws.jetty.protocol.message.MessageABC;
import com.codeshelf.ws.jetty.protocol.request.RequestABC;
import com.codeshelf.ws.jetty.protocol.response.ResponseABC;

@ServerEndpoint(value="/",encoders={JsonEncoder.class},decoders={JsonDecoder.class})
public class CsServerEndPoint {

	private static final Logger	LOGGER = LoggerFactory.getLogger(CsServerEndPoint.class);

	@Getter
	private final PersistenceService<Tenant> tenantPersistenceService;

	private static Counter messageCounter;

	//These are singletons injected at startup. 
	//  This allows us to avoid trying to hook Guice into the object creation  process of javax.websocket/Jetty
	//     but allow Guice to control object creation for these singletons
	private static IMessageProcessor iMessageProcessor;
	private static SessionManagerService sessionManagerService;
	
	// time to close session after mins of inactivity
	int idleTimeOut = 60;
	
	
	public CsServerEndPoint() {
		tenantPersistenceService = TenantPersistenceService.getInstance();
		messageCounter = MetricsService.getInstance().createCounter(MetricsGroup.WSS,"messages.received");

	}
	
	@OnOpen
    public void onOpen(Session session, EndpointConfig ec) {
		ContextLogging.setSession(sessionManagerService.getSession(session));
		try {
			session.setMaxIdleTimeout(1000*60*idleTimeOut);
			LOGGER.info("WS Session Started: " + session.getId()+", timeout: "+session.getMaxIdleTimeout());
			sessionManagerService.sessionStarted(session);
		} finally {
			ContextLogging.clearSession();
		}
    }

    @OnMessage(maxMessageSize=JsonEncoder.WEBSOCKET_MAX_MESSAGE_SIZE)
    public void onMessage(Session session, MessageABC message) {
    	messageCounter.inc();
    	try{
        	this.getTenantPersistenceService().beginTransaction();
        	UserSession csSession = sessionManagerService.getSession(session);
    		ContextLogging.setSession(csSession);
    		sessionManagerService.messageReceived(session);
        	if (message instanceof ResponseABC) {
        		ResponseABC response = (ResponseABC) message;
                LOGGER.debug("Received response on session "+csSession+": " + response);
                iMessageProcessor.handleResponse(csSession, response);
        	}
        	else if (message instanceof RequestABC) {
        		RequestABC request = (RequestABC) message;
                LOGGER.debug("Received request on session "+csSession+": " + request);
               // pass request to processor to execute command
                ResponseABC response = iMessageProcessor.handleRequest(csSession, request);
                if (response!=null) {
                	// send response to client
                	LOGGER.debug("Sending response "+response+" for request "+request);
                	csSession.sendMessage(response);
                }
                else {
                	LOGGER.warn("No response generated for request "+request);
                }    	
        	}  
        	else {
        		// handle all other messages
            	LOGGER.debug("Received message on session "+csSession+": "+message);
            	iMessageProcessor.handleMessage(csSession, message);
        	}
			this.getTenantPersistenceService().commitTransaction();
		} catch (RuntimeException e) {
			this.getTenantPersistenceService().rollbackTransaction();
			LOGGER.error("Unable to persist during message handling: " + message, e);
		}
    	finally {
    		ContextLogging.clearSession();
		}
    }
    
	@OnClose
    public void onClose(Session session, CloseReason reason) {
		ContextLogging.setSession(sessionManagerService.getSession(session));
		try {
	    	LOGGER.info(String.format("WS Session %s closed because of %s", session.getId(), reason));
			sessionManagerService.sessionEnded(session);		
		} finally {
			ContextLogging.clearSession();
		}
    }
    
    @OnError
    public void onError(Session session, Throwable cause) {
    	ContextLogging.setSession(sessionManagerService.getSession(session));
    	LOGGER.error("WebSocket error", cause);
    	ContextLogging.clearSession();
    }

    //Injected see ServerMain
	public static void setSessionManagerService(SessionManagerService instance) {
		if (sessionManagerService != null) {
			throw new IllegalArgumentException("SessionManager should only be initialized once");
		}
		sessionManagerService = instance;
	}

	//Injected see ServerMain
	public static void setMessageProcessor(ServerMessageProcessor instance) {
		if (iMessageProcessor != null) {
			throw new IllegalArgumentException("MessageProcessor should only be initialized once");
		}
		iMessageProcessor = instance;
	}


}