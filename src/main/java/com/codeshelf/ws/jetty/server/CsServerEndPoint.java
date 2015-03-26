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

@ServerEndpoint(value="/",encoders={JsonEncoder.class},decoders={JsonDecoder.class},configurator=WebSocketConfigurator.class)
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
		String url = session.getRequestURI().toASCIIString();
		WebSocketConnection conn = webSocketManagerService.getWebSocketConnectionForSession(session);
		if(conn != null) {
			LOGGER.error("onOpen webSocketManager already had a connection for this session");
			User user = conn.getUser();
			if(user != null) {
				throw new RuntimeException("onOpen connection already had a user "+user.getId());
			}
		}
		session.setMaxIdleTimeout(1000*60*idleTimeOut);
		LOGGER.info("WS Session Started: " + session.getId()+", timeout: "+session.getMaxIdleTimeout());
		
		webSocketManagerService.sessionStarted(session); // this will create WebSocketConnection for session
    }

    @OnMessage(maxMessageSize=JsonEncoder.WEBSOCKET_MAX_MESSAGE_SIZE)
    public void onMessage(Session session, MessageABC message) {
    	messageCounter.inc();
    	User setUserContext = null;
    	try{
        	WebSocketConnection csSession = webSocketManagerService.getWebSocketConnectionForSession(session);
        	setUserContext = csSession.getUser();
        	if(setUserContext != null) {
            	CodeshelfSecurityManager.setContext(csSession.getUser(),csSession.getTenant());
        		LOGGER.info("Got message {}", message.getClass().getSimpleName());
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
		} catch (RuntimeException e) {
			LOGGER.error("Unexpected exception during message handling: " + message, e);
		} finally {
    		if(setUserContext != null) {
    			CodeshelfSecurityManager.removeContext();
    		}
		}
    }
    
	@OnClose
    public void onClose(Session session, CloseReason reason) {
		User user = webSocketManagerService.getWebSocketConnectionForSession(session).getUser();
		try {
	    	LOGGER.info(String.format("WS Session %s for user %s closed because of %s", session.getId(), user.toString(), reason));
			webSocketManagerService.sessionEnded(session);		
		} finally {
			CodeshelfSecurityManager.removeContextIfPresent();
		}
    }
    
    @OnError
    public void onError(Session session, Throwable cause) {
    	LOGGER.error("WebSocket error", cause);
    }

    //Injected see ServerMain
	public static void setWebSocketManagerService(WebSocketManagerService instance) {
		if (webSocketManagerService != null) {
			throw new IllegalArgumentException("WebSocketManagerService should only be initialized once");
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