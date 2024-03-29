package com.codeshelf.ws.server;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.websocket.CloseReason;
import javax.websocket.EndpointConfig;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Counter;
import com.codeshelf.manager.Tenant;
import com.codeshelf.metrics.MetricsGroup;
import com.codeshelf.metrics.MetricsService;
import com.codeshelf.persistence.TenantPersistenceService;
import com.codeshelf.security.CodeshelfSecurityManager;
import com.codeshelf.security.UserContext;
import com.codeshelf.ws.io.JsonDecoder;
import com.codeshelf.ws.io.JsonEncoder;
import com.codeshelf.ws.protocol.message.IMessageProcessor;
import com.codeshelf.ws.protocol.message.MessageABC;
import com.codeshelf.ws.protocol.message.NotificationMessage;
import com.codeshelf.ws.protocol.request.DeviceRequestABC;
import com.codeshelf.ws.protocol.request.LoginRequest;
import com.codeshelf.ws.protocol.request.RequestABC;
import com.codeshelf.ws.protocol.response.ResponseABC;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

import lombok.Getter;

@ServerEndpoint(value = "/", encoders = { JsonEncoder.class }, decoders = { JsonDecoder.class }, configurator = WebSocketConfigurator.class)
public class CsServerEndPoint {

	public class AuthenticationException extends Exception {

		private static final long serialVersionUID = 1L;

	}

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

	private static HashMap<String, ExecutorService> devicePools = new HashMap<>();

	public CsServerEndPoint() {
		tenantPersistenceService = TenantPersistenceService.getInstance();
		messageCounter = MetricsService.getInstance().createCounter(MetricsGroup.WSS, "messages.received");
	}

	@OnOpen
	public void onOpen(Session session, EndpointConfig ec) {
		//String url = session.getRequestURI().toASCIIString();
		WebSocketConnection conn = webSocketManagerService.getWebSocketConnectionForSession(session);
		UserContext user = null;
		if (conn != null) {
			LOGGER.error("onOpen webSocketManager already had a connection for this session");
			user = conn.getCurrentUserContext();
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
		// Look at login command processing to get a warn on site controller connect

		webSocketManagerService.sessionStarted(session); // this will create WebSocketConnection for session
	}

	@OnMessage(maxMessageSize = JsonEncoder.WEBSOCKET_MAX_MESSAGE_SIZE)
	public void onMessage(Session session, final MessageABC message) {
		messageCounter.inc();
		UserContext setUserContext = null;
		Tenant setTenantContext = null;
		try {
			final WebSocketConnection csSession = webSocketManagerService.getWebSocketConnectionForSession(session);
			setUserContext = csSession.getCurrentUserContext();
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
				} else if (NotificationMessage.class.isAssignableFrom(message.getClass())) {
					NotificationMessage noteMessage = (NotificationMessage) message;
					// So far, only site controller producing notification messages. Taking the user name as that.
					LOGGER.info("Got {} message. Device={}; siteController={}", noteMessage.getEventType(), noteMessage.getDevicePersistentId(),setUserContext.getUsername());
				}
				else {
					LOGGER.debug("Got message {} from user {} on tenant {}",
						message,
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
				Runnable messageHandlingTask = new Runnable() {

					@Override
					public void run() {
						UserContext setUserContext = csSession.getCurrentUserContext();
						Tenant setTenantContext = csSession.getCurrentTenant();
						String msgName = message.getClass().getSimpleName();
						if (setUserContext == null) {
							LOGGER.info("Got message {} and user is null (tenant exists = {})", msgName, (setTenantContext != null));
						} else if (setTenantContext == null) {
							LOGGER.error("Got message {} from user {} but no tenant context", msgName, setUserContext.getUsername());
						} else {
							// ok. set context, and log. But simple keep alives log only at debug level.
							CodeshelfSecurityManager.setContext(setUserContext, setTenantContext);
						}
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
							if (needTransaction) {
								TenantPersistenceService.getInstance().commitTransaction();
								LOGGER.debug("Committed transaction for request " + request);
							}
							;
							needTransaction = false;
						} catch (RuntimeException e) {
							LOGGER.error("Unexpected exception during message handling: " + message, e);
						} finally {
							if (needTransaction) {
								TenantPersistenceService.getInstance().rollbackTransaction();
							}
							CodeshelfSecurityManager.removeContextIfPresent();
						}
					}
				};

				if (message instanceof DeviceRequestABC) {
					String deviceIdentifier = ((DeviceRequestABC) message).getDeviceIdentifier();
					if (deviceIdentifier != null) {
						synchronized(devicePools) {
							ExecutorService devicePool = devicePools.get(deviceIdentifier);
							if (devicePool == null) {
								devicePool = createPool(deviceIdentifier);
								devicePools.put(deviceIdentifier, devicePool);
							}
							devicePool.submit(messageHandlingTask);
						}
					}
				} else {
					messageHandlingTask.run();
				}
			} else {
				// handle all other messages
				LOGGER.debug("Received message on session " + csSession + ": " + message);
				iMessageProcessor.handleMessage(csSession, message);
			}
		} catch (RuntimeException e) {
			LOGGER.error("Unexpected exception during message handling: " + message, e);
		} finally {
			CodeshelfSecurityManager.removeContextIfPresent();
		}
	}

	private ExecutorService createPool(String deviceIdentifier) {
		// TODO Auto-generated method stub
		ThreadFactoryBuilder builder = new ThreadFactoryBuilder()
				.setNameFormat("wss-message-" + deviceIdentifier + "-%d")
				.setThreadFactory(Executors.defaultThreadFactory());
		return new ThreadPoolExecutor(0, 1, 1, TimeUnit.MINUTES, new LinkedBlockingQueue<Runnable>(), builder.build(), new ThreadPoolExecutor.CallerRunsPolicy());
	}

	@OnClose
	public void onClose(Session session, CloseReason reason) {
		try {
			webSocketManagerService.sessionEnded(session, reason);
		} finally {
			CodeshelfSecurityManager.removeContextIfPresent();
		}
	}

	@OnError
	public void onError(Session session, Throwable cause) {
		LOGGER.error("WebSocket error for session {}", session, cause);
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

	public static Map<String, ExecutorService> getDevicePools() {
		return devicePools;
	}

	public static void shutdownDevicePools() {
		for (Entry<String, ExecutorService> entry : devicePools.entrySet()) {
			ExecutorService pool = entry.getValue();
			pool.shutdownNow();
			try {
				pool.awaitTermination(5, TimeUnit.SECONDS);
			} catch (InterruptedException e) {
				LOGGER.error("Time out waiting for shutdown of device pool: {} {}", entry.getKey(), pool);
			}
		}// TODO Auto-generated method stub
		devicePools.clear();
	}

}
