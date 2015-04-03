package com.codeshelf.ws.server;

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
import com.codeshelf.manager.Tenant;
import com.codeshelf.manager.User;
import com.codeshelf.metrics.MetricsGroup;
import com.codeshelf.metrics.MetricsService;
import com.codeshelf.persistence.TenantPersistenceService;
import com.codeshelf.security.CodeshelfSecurityManager;
import com.codeshelf.ws.io.JsonDecoder;
import com.codeshelf.ws.io.JsonEncoder;
import com.codeshelf.ws.protocol.message.IMessageProcessor;
import com.codeshelf.ws.protocol.message.MessageABC;
import com.codeshelf.ws.protocol.request.LoginRequest;
import com.codeshelf.ws.protocol.request.RequestABC;
import com.codeshelf.ws.protocol.response.ResponseABC;

@ServerEndpoint(value = "/", encoders = { JsonEncoder.class }, decoders = { JsonDecoder.class }, configurator = WebSocketConfigurator.class)
public class CsServerEndPoint {

	private static final Logger				LOGGER		= LoggerFactory.getLogger(CsServerEndPoint.class);

	@Getter
	private final TenantPersistenceService	tenantPersistenceService;

	private static Counter					messageCounter;

	//These are singletons injected at startup. 
	//  This allows us to avoid trying to hook Guice into the object creation  process of javax.websocket/Jetty
	//     but allow Guice to control object creation for these singletons
	private static IMessageProcessor		iMessageProcessor;
	private static WebSocketManagerService	webSocketManagerService;

	// time to close session after mins of inactivity
	int										idleTimeOut	= 60;

	public CsServerEndPoint() {
		tenantPersistenceService = TenantPersistenceService.getInstance();
		messageCounter = MetricsService.getInstance().createCounter(MetricsGroup.WSS, "messages.received");

	}

	@OnOpen
	public void onOpen(Session session, EndpointConfig ec) {
		//String url = session.getRequestURI().toASCIIString();
		WebSocketConnection conn = webSocketManagerService.getWebSocketConnectionForSession(session);
		if (conn != null) {
			LOGGER.error("onOpen webSocketManager already had a connection for this session");
			User user = conn.getCurrentUser();
			Tenant tenant = conn.getCurrentTenant();
			String lastTenantId = conn.getLastTenantIdentifier();
			if (user != null || tenant != null || lastTenantId != null) {
				throw new RuntimeException("onOpen connection had unexpected authentication information associated:" + " user:"
						+ (user == null ? "null" : user.getUsername()) + " tenant:" + (user == null ? "null" : tenant.getId())
						+ " tenantId:" + (lastTenantId == null ? "null" : lastTenantId));
			}
		}
		session.setMaxIdleTimeout(1000 * 60 * idleTimeOut);
		LOGGER.info("WS Session Started: " + session.getId() + ", timeout: " + session.getMaxIdleTimeout());

		webSocketManagerService.sessionStarted(session); // this will create WebSocketConnection for session
	}

	@OnMessage(maxMessageSize = JsonEncoder.WEBSOCKET_MAX_MESSAGE_SIZE)
	public void onMessage(Session session, MessageABC message) {
		messageCounter.inc();
		User setUserContext = null;
		Tenant setTenantContext = null;
		try {
			WebSocketConnection csSession = webSocketManagerService.getWebSocketConnectionForSession(session);
			setUserContext = csSession.getCurrentUser();
			setTenantContext = csSession.getCurrentTenant();
			String msgName = message.getClass().getSimpleName();
			if (setUserContext == null) {
				LOGGER.info("Got message {} and user is null (tenant exists = {})", msgName, (setTenantContext != null));
			} else if (setTenantContext == null) {
				LOGGER.error("Got message {} from user {} but no tenant context", msgName, setUserContext.getUsername());
			} else {
				// ok. set context, and log. But simple keep alives log only at debug level.
				CodeshelfSecurityManager.setContext(setUserContext, setTenantContext);
				if ("KeepAlive".equals(msgName)) {
					LOGGER.debug("Got message {} from user {} on tenant {}",
						msgName,
						setUserContext.getUsername(),
						setTenantContext.getId());
				} else {
					LOGGER.info("Got message {} from user {} on tenant {}",
						msgName,
						setUserContext.getUsername(),
						setTenantContext.getId());
				}
			}

			webSocketManagerService.messageReceived(session);
			if (message instanceof ResponseABC) {
				ResponseABC response = (ResponseABC) message;
				LOGGER.debug("Received response on session " + csSession + ": " + response);
				iMessageProcessor.handleResponse(csSession, response);
			} else if (message instanceof RequestABC) {
				boolean needTransaction = !(message instanceof LoginRequest);

				if (needTransaction)
					TenantPersistenceService.getInstance().beginTransaction();
				try {
					RequestABC request = (RequestABC) message;
					LOGGER.debug("Received request on session " + csSession + ": " + request);
					// pass request to processor to execute command
					ResponseABC response = iMessageProcessor.handleRequest(csSession, request);
					if (response != null) {
						// send response to client
						LOGGER.debug("Sending response " + response + " for request " + request);
						csSession.sendMessage(response);
					} else {
						LOGGER.warn("No response generated for request " + request);
					}
					if (needTransaction)
						TenantPersistenceService.getInstance().commitTransaction();
					needTransaction = false;
				} finally {
					if (needTransaction)
						TenantPersistenceService.getInstance().rollbackTransaction();
				}

			} else {
				// handle all other messages
				LOGGER.debug("Received message on session " + csSession + ": " + message);
				iMessageProcessor.handleMessage(csSession, message);
			}
		} catch (RuntimeException e) {
			LOGGER.error("Unexpected exception during message handling: " + message, e);
		} finally {
			if (setUserContext != null || setTenantContext != null) {
				CodeshelfSecurityManager.removeContext();
			}
		}
	}

	@OnClose
	public void onClose(Session session, CloseReason reason) {
		User user = webSocketManagerService.getWebSocketConnectionForSession(session).getCurrentUser();
		try {
			LOGGER.info(String.format("WS Session %s for user %s closed because of %s", session.getId(), user, reason));
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