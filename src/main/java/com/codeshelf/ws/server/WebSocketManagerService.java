package com.codeshelf.ws.server;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.websocket.CloseReason;
import javax.websocket.CloseReason.CloseCodes;
import javax.websocket.Session;

import lombok.Getter;
import lombok.Setter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Counter;
import com.codeshelf.manager.Tenant;
import com.codeshelf.manager.User;
import com.codeshelf.metrics.MetricsGroup;
import com.codeshelf.metrics.MetricsService;
import com.codeshelf.security.CodeshelfSecurityManager;
import com.codeshelf.service.AbstractCodeshelfScheduledService;
import com.codeshelf.ws.protocol.message.KeepAlive;
import com.codeshelf.ws.protocol.message.MessageABC;
import com.codeshelf.ws.protocol.request.PingRequest;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

public class WebSocketManagerService extends AbstractCodeshelfScheduledService {

	private static final Logger	LOGGER = LoggerFactory.getLogger(WebSocketManagerService.class);

	@Getter @Setter
	int periodSeconds = 1;
	
	@Getter @Setter
	int startupDelaySeconds = 2;

	@Getter @Setter
	boolean suppressKeepAlive = false;
	
	@Getter @Setter
	boolean killIdle = false;
	
	@Getter @Setter
	int siteControllerTimeout = 60*1000; // 1 min
	
	@Getter @Setter
	int webAppTimeout = 5*60*1000; // 5 mins
	
	@Getter @Setter
	int defaultTimeout = 5*60*1000; // 5 mins

	@Getter @Setter
	int keepAliveInterval = 10*1000; // 10 seconds keepalive interval
	
	@Getter @Setter
	int idleWarningTimeout = 15*1000; // 15 seconds warning if no keepalive
	
	@Getter @Setter
	int pingInterval = 60*1000;
	
	private ConcurrentMap<String,WebSocketConnection> activeConnections; 
	private ExecutorService	sharedExecutor;

	private Counter activeSessionsCounter;
	private Counter activeSiteControllerSessionsCounter;
	private Counter totalSessionsCounter;
	
	boolean resetting = false;

	public WebSocketManagerService() {
	}

	public synchronized WebSocketConnection sessionStarted(Session session) {
		if(this.activeConnections == null) {
			LOGGER.warn("sessionStarted called while service is uninitialized or resetting for test");
			return null; // this should only happen in tests
		} else if(this.state() != State.RUNNING) {
			LOGGER.warn("sessionStarted called while service state is {}",this.state().toString());
			return null; // called while shutting down - this should only happen in tests or maybe not at all
		}

		String sessionId = session.getId();
		WebSocketConnection wsConnection = activeConnections.get(sessionId);
		if (wsConnection == null) {
			wsConnection = new WebSocketConnection(session, sharedExecutor);
			wsConnection.setSessionId(sessionId); // apparently just used for logging in context of connection	
			activeConnections.put(sessionId, wsConnection);
			LOGGER.info("Session "+session.getId()+" started");
			updateCounters();
			if(totalSessionsCounter != null) {
				totalSessionsCounter.inc();
			}
		}
		else {
			LOGGER.warn("Unable to create WebSocketConnection for Session with ID "+sessionId+": already registered");
		}
		return wsConnection;
	}

	public synchronized void sessionEnded(Session session) {
		if(this.state() != State.RUNNING) {
			LOGGER.warn("sessionEnded called while service state is {}",this.state().toString());
			return; // called while shutting down or resetting - this should only happen in tests
		}
		String sessionId = session.getId();
		WebSocketConnection wsConnection = activeConnections.get(sessionId);
		if (wsConnection!=null) {
			wsConnection.disconnect();
			activeConnections.remove(sessionId);
			LOGGER.debug("WS Connection "+session.getId()+" ended"); // reason should be logged by endpoint
			updateCounters();
		} 
		else {
			LOGGER.warn("sessionEnded: session id "+sessionId+" not found");
		}
	}
	
	public WebSocketConnection getWebSocketConnectionForSession(Session session) {
		if(this.state() != State.RUNNING) {
			LOGGER.warn("getSession(Session) called while service state is {}",this.state().toString());
			return null; // called while shutting down or resetting - this should only happen in tests
		}
		// TODO: synchronize access in case shutdown or reset is occurring simultaneously
		if(resetting) {
			LOGGER.warn("getSession(Session) called while service is resetting"); 
		}
		String sessionId = session.getId();
		if (sessionId==null) {
			return null;
		}
		return this.activeConnections.get(sessionId);
	}
	
	private List<WebSocketConnection> getWebSocketConnectionForUser(User user) {
		List<WebSocketConnection> result = new ArrayList<WebSocketConnection>();
		if(this.state() != State.RUNNING) {
			LOGGER.warn("getSession(User) called while service state is {}",this.state().toString());
			return null; // called while shutting down or resetting - this should only happen in tests
		}
		for (WebSocketConnection session : this.getWebSocketConnections()) {
			if(session.getCurrentUser() != null && session.getCurrentUser().equals(user)) {
				result.add(session);
			}
		}
		return result;
	}
	
	public final Collection<WebSocketConnection> getWebSocketConnections() {
		if(this.state() != State.RUNNING) {
			LOGGER.warn("getSessions called while service state is {}",this.state().toString());
			return new ArrayList<WebSocketConnection>(); // called while shutting down or resetting - this should only happen in tests
		}
		return this.activeConnections.values();
	}

	public void messageReceived(Session session) {
		if(this.state() != State.RUNNING) {
			LOGGER.warn("messageReceived called while service state is {}",this.state().toString());
			return; // called while shutting down or resetting - this should only happen in tests
		}
		String sessionId = session.getId();
		WebSocketConnection csSession = activeConnections.get(sessionId);
		if (csSession!=null) {
			csSession.setLastMessageReceived(System.currentTimeMillis());
		}
		else {
			LOGGER.warn("Unable to update message received timestamp: WS connection for Session "+sessionId+" not found");
		}		
	}

	public void messageSent(Session session) {
		if(this.state() != State.RUNNING) {
			LOGGER.warn("messageSent called while service state is {}",this.state().toString());
			return; // called while shutting down or resetting - this should only happen in tests
		}

		String sessionId = session.getId();
		WebSocketConnection csSession = activeConnections.get(sessionId);
		if (csSession!=null) {
			csSession.setLastMessageSent(System.currentTimeMillis());
		}
		else {
			LOGGER.warn("Unable to update message sent timestamp: WS connection for Session "+sessionId+" not found");
		}		
	}
	
	public void updateCounters() {
		if(this.state() != State.RUNNING) {
			LOGGER.warn("updateCounters called while service state is {}",this.state().toString());
			return; // called while shutting down or resetting - this should only happen in tests
		}

		int activeSiteController = 0, numActiveSessions = 0;
		for (WebSocketConnection session : activeConnections.values()) {
			if ((session.getLastState()==WebSocketConnection.State.ACTIVE || session.getLastState()==WebSocketConnection.State.IDLE_WARNING)) {
				numActiveSessions++;
				if (session.isSiteController()) {
					activeSiteController++;
				}
			}
		}
		// this is needed, since counters don't have an explicit set method
		if(this.activeSiteControllerSessionsCounter != null && this.activeSessionsCounter != null) {
			long c = activeSiteControllerSessionsCounter.getCount();
			activeSiteControllerSessionsCounter.inc(activeSiteController-c);		
			c = activeSessionsCounter.getCount();
			activeSessionsCounter.inc(numActiveSessions-c);
		}
	}

	// returns how many distinct users the message was sent to, not how many sessions
	public int sendMessage(Set<User> users, MessageABC message) {
		List<User> sent = new ArrayList<User>();
		for (User user : users) {
			if (sendMessage(user, message)>0) {
				sent.add(user);
			}
		} 
		return sent.size();
	}

	// returns how many sessions the message was sent to
	private int sendMessage(User user, MessageABC message) {
		List<WebSocketConnection> sessions = getWebSocketConnectionForUser(user);
		int sent=0;
		for(WebSocketConnection session : sessions) {
			if(session.sendMessage(message))
				sent++;
		}
		return sent;
	}

	private void updateWebSocketConnectionState(WebSocketConnection wsConnection) {
		if(!suppressKeepAlive) {
			// consider sending keepalive
			long timeSinceLastSent = System.currentTimeMillis() - wsConnection.getLastMessageSent();
			if (timeSinceLastSent>keepAliveInterval) {
				if (wsConnection.getLastState()==WebSocketConnection.State.INACTIVE) {
					// don't send keep-alives on inactive sessions
					LOGGER.warn("WS connection {} is INACTIVE, not sending keepalives",wsConnection.getSessionId());
				} else {
					LOGGER.debug("Sending keep-alive on WS connection {} ",wsConnection.getSessionId());
					try {
						wsConnection.sendMessage(new KeepAlive());
					} catch (Exception e) {
						LOGGER.error("Failed to send Keepalive", e);
					}
				}
			}
		} // else suppressing keepalives


		// regardless of keepalive setting, monitor session activity state
		WebSocketConnection.State lastState = wsConnection.getLastState();
		if(wsConnection.getSessionId() != null 
				&& !lastState.equals(WebSocketConnection.State.CLOSED)) {
			WebSocketConnection.State newSessionState = determineConnectionState(wsConnection);
			if(newSessionState != lastState) {
				LOGGER.info("WS connection state on "+wsConnection.getSessionId()+" changed from "+wsConnection.getLastState().toString()+" to "+newSessionState.toString());
				wsConnection.setLastState(newSessionState);
				
				if(killIdle && newSessionState == WebSocketConnection.State.INACTIVE) {
					// kill idle session on state change, if configured to do so
					LOGGER.warn("WS connection timed out with "+wsConnection.getSessionId()+".  Closing connection.");
					wsConnection.disconnect(new CloseReason(CloseCodes.GOING_AWAY, "Timeout"));
					
				}
			}
		}

	}

	private WebSocketConnection.State determineConnectionState(WebSocketConnection session) {
		long timeSinceLastReceived = System.currentTimeMillis() - session.getLastMessageReceived();
	
		if ((session.isSiteController() && timeSinceLastReceived > siteControllerTimeout) 
			|| (session.isAppUser() && timeSinceLastReceived > webAppTimeout)
			|| (timeSinceLastReceived > defaultTimeout)) {
			return WebSocketConnection.State.INACTIVE;
		}//else 
		if (timeSinceLastReceived > idleWarningTimeout) {
			return WebSocketConnection.State.IDLE_WARNING;
		}//else
		return WebSocketConnection.State.ACTIVE;
	}
	
	public void reset() {
		this.resetting = true;
		if(this.state() != State.RUNNING) {
			// bad integration test
			throw new IllegalStateException("reset called while service state is "+this.state().toString());
		}
		try {
			teardown();
			initialize();
		} catch (Exception e) {
			throw new RuntimeException("failed to reset state of WS connection manager",e);
		}
		this.resetting = false;
		LOGGER.info("reset state of WS connection manager");
	}
	
	@Override
	protected synchronized void startUp() throws Exception {
		initialize();	
	}

	private void initialize() {
		activeSessionsCounter = MetricsService.getInstance().createCounter(MetricsGroup.WSS,"sessions.active");
		activeSiteControllerSessionsCounter = MetricsService.getInstance().createCounter(MetricsGroup.WSS,"sessions.sitecontrollers");
		totalSessionsCounter = MetricsService.getInstance().createCounter(MetricsGroup.WSS,"sessions.total");
		
		ThreadFactoryBuilder builder = new ThreadFactoryBuilder();
		builder.setNameFormat("UserSession %s");
		this.sharedExecutor = Executors.newCachedThreadPool(builder.build());
		this.activeConnections = Maps.newConcurrentMap();
		
		suppressKeepAlive = Boolean.getBoolean("websocket.idle.suppresskeepalive");
		killIdle = Boolean.getBoolean("websocket.idle.kill");
	}
	
	@Override
	protected synchronized void shutDown() throws Exception {
		teardown();
	}

	private void teardown() throws Exception {
		LOGGER.info("shutting down WS connection manager with {} active sessions",this.activeConnections.size());
		Collection<WebSocketConnection> sessions = this.activeConnections.values();
		for(WebSocketConnection session : sessions) {
			try {
				session.close();
			}catch(IOException e) {
				LOGGER.warn("Failure when closing session {}", session);
			}
		}
		this.activeConnections = null;
		this.sharedExecutor.shutdown();
		this.sharedExecutor.awaitTermination(30, TimeUnit.SECONDS);
		this.sharedExecutor = null;
		LOGGER.info("WS connection manager stopped");
	}

	@Override
	protected synchronized void runOneIteration() throws Exception {
		
		try {
			if(this.activeConnections != null) { // service is not shutting down or resetting
				int pings = 0;
				
				// remove any closed connections
				List<String> connectionIds = new ArrayList<String>(this.activeConnections.keySet());
				for (String connectionId : connectionIds) {
					WebSocketConnection connection = this.activeConnections.get(connectionId);
					if(connection != null && connection.getLastState().equals(WebSocketConnection.State.CLOSED)) {
						this.activeConnections.remove(connectionId);
						LOGGER.warn("WebSocketManager reaped a closed connection. sessionEnded did not complete for {}",connectionId);
					}
				}
				
				// check status, send keepalive etc on all open connections
				Collection<WebSocketConnection> connections = this.getWebSocketConnections();
				for (WebSocketConnection connection : connections) {
					User contextUser = connection.getCurrentUser();
					Tenant contextTenant = connection.getCurrentTenant();
					if(contextUser != null && contextTenant != null) {
						// the security manager requires only a tenant - but we require both for websockets
				    	CodeshelfSecurityManager.setContext(contextUser,contextTenant);
					} 
					// might not be logged in yet, we will still ping
					try {
						// send ping periodically to measure latency
						if (connection.isSiteController() && System.currentTimeMillis()-connection.getLastPingSent()>pingInterval) {
							// send ping
							PingRequest request = new PingRequest();
							long now = System.currentTimeMillis();
							connection.setLastPingSent(now);
							LOGGER.trace("Sending ping on "+connection.getSessionId());
							connection.sendMessage(request);
							pings++;
						}
						updateWebSocketConnectionState(connection);
					} finally {
						if(contextUser != null || contextTenant != null) 
							// a call to OnClose might have already removed context, it is ok if not present here
							CodeshelfSecurityManager.removeContextIfPresent();
					}
					// check if keep alive needs to be sent
				}
				if(pings>0) {
					LOGGER.info("Watchdog sent "+pings+" pings");
				}
			}
		} catch (Exception e) {
			LOGGER.error("SERIOUS ERROR: Unhandled exception in WS connection manager",e);
		}
	}

	@Override
	protected Scheduler scheduler() {
		//return Scheduler.newFixedRateSchedule(this.startupDelaySeconds, this.periodSeconds, TimeUnit.SECONDS);
		return Scheduler.newFixedDelaySchedule(this.startupDelaySeconds, this.periodSeconds, TimeUnit.SECONDS);
	}

	public boolean hasAnySessions() {
		if(this.activeConnections==null)
			return false;
		return !this.activeConnections.isEmpty();
	}
}
