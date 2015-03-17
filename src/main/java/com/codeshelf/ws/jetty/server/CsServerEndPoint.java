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

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Counter;
import com.codeshelf.manager.User;
import com.codeshelf.metrics.MetricsGroup;
import com.codeshelf.metrics.MetricsService;
import com.codeshelf.platform.persistence.ITenantPersistenceService;
import com.codeshelf.platform.persistence.TenantPersistenceService;
import com.codeshelf.security.CodeshelfSecurityManager;
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
	private final ITenantPersistenceService tenantPersistenceService;

	private static Counter messageCounter;

	//These are singletons injected at startup. 
	//  This allows us to avoid trying to hook Guice into the object creation  process of javax.websocket/Jetty
	//     but allow Guice to control object creation for these singletons
	private static IMessageProcessor iMessageProcessor;
	private static WebSocketManagerService webSocketManagerService;
	
	// time to close session after mins of inactivity
	int idleTimeOut = 60;
	
	
	public CsServerEndPoint() {
		tenantPersistenceService = TenantPersistenceService.getInstance();
		messageCounter = MetricsService.getInstance().createCounter(MetricsGroup.WSS,"messages.received");

	}
	
	@OnOpen
    public void onOpen(Session session, EndpointConfig ec) {
		WebSocketConnection conn = webSocketManagerService.getSession(session);
		if(conn != null) {
			User user = conn.getUser();
			if(user != null) {
				throw new RuntimeException("onOpen session already had a user "+user.getId());
			}
		}
		session.setMaxIdleTimeout(1000*60*idleTimeOut);
		LOGGER.info("WS Session Started: " + session.getId()+", timeout: "+session.getMaxIdleTimeout());
		webSocketManagerService.sessionStarted(session);
    }

    @OnMessage(maxMessageSize=JsonEncoder.WEBSOCKET_MAX_MESSAGE_SIZE)
    public void onMessage(Session session, MessageABC message) {
    	messageCounter.inc();
    	boolean setUser = false;
    	this.getTenantPersistenceService().beginTransaction();
    	try{
        	WebSocketConnection csSession = webSocketManagerService.getSession(session);
        	if(csSession.getUser() != null) {
        		String username = "null";
        		if(csSession.getUser() != null) 
        			username = csSession.getUser().getUsername();
        		LOGGER.info("Got message {} and setting user context to {}", message.getClass().getSimpleName(), username);
            	CodeshelfSecurityManager.setCurrentUser(csSession.getUser());
            	Subject subject = SecurityUtils.getSubject();
            	LOGGER.info("Subject is {}",subject == null? "null":subject.getPrincipal().toString());
            	setUser = true;
        	} else {
        		LOGGER.info("Got message {} and csSession is null", message.getClass().getSimpleName());
        	}

        	webSocketManagerService.messageReceived(session);
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
    		if(setUser) {
    			CodeshelfSecurityManager.removeCurrentUser();
    		}
		}
    }
    
	@OnClose
    public void onClose(Session session, CloseReason reason) {
		User user = webSocketManagerService.getSession(session).getUser();
		try {
	    	LOGGER.info(String.format("WS Session %s for user %s closed because of %s", session.getId(), user.toString(), reason));
			webSocketManagerService.sessionEnded(session);		
		} finally {
			CodeshelfSecurityManager.removeCurrentUserIfPresent();
		}
    }
    
    @OnError
    public void onError(Session session, Throwable cause) {
    	User user = webSocketManagerService.getSession(session).getUser();
    	CodeshelfSecurityManager.setCurrentUser(user);
    	try {
        	LOGGER.error("WebSocket error", cause);
    	} finally {
        	CodeshelfSecurityManager.removeCurrentUserIfPresent();
    	}
    }

    //Injected see ServerMain
	public static void setWebSocketManagerService(WebSocketManagerService instance) {
		if (webSocketManagerService != null) {
			throw new IllegalArgumentException("SessionManager should only be initialized once");
		}
		webSocketManagerService = instance;
	}

	//Injected see ServerMain
	public static void setMessageProcessor(ServerMessageProcessor instance) {
		if (iMessageProcessor != null) {
			throw new IllegalArgumentException("MessageProcessor should only be initialized once");
		}
		iMessageProcessor = instance;
	}


}