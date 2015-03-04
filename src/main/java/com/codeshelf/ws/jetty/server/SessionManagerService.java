package com.codeshelf.ws.jetty.server;

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
import com.codeshelf.application.ContextLogging;
import com.codeshelf.metrics.MetricsGroup;
import com.codeshelf.metrics.MetricsService;
import com.codeshelf.platform.multitenancy.User;
import com.codeshelf.ws.jetty.protocol.message.KeepAlive;
import com.codeshelf.ws.jetty.protocol.message.MessageABC;
import com.codeshelf.ws.jetty.protocol.request.PingRequest;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.AbstractScheduledService;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

public class SessionManagerService extends AbstractScheduledService {

	private static final Logger	LOGGER = LoggerFactory.getLogger(SessionManagerService.class);

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
	
	private ConcurrentMap<String,UserSession> activeSessions; 
	private ExecutorService	sharedExecutor;

	private Counter activeSessionsCounter;
	private Counter activeSiteControllerSessionsCounter;
	private Counter totalSessionsCounter;
	
	boolean resetting = false;

	public SessionManagerService() {
	}
/*	
	public void awaitRunningOrThrow() {
		try {
			this.awaitRunning(MAX_INITIALIZE_WAIT_SECONDS, TimeUnit.SECONDS);
		} catch (TimeoutException e) {
			throw new IllegalStateException("timeout initializing "+serviceName(),e);
		}
	}
*/
	public synchronized UserSession sessionStarted(Session session) {
		if(this.activeSessions == null) {
			LOGGER.warn("sessionStarted called while service is uninitialized or resetting for test");
			return null; // this should only happen in tests
		} else if(this.state() != State.RUNNING) {
			LOGGER.warn("sessionStarted called while service state is {}",this.state().toString());
			return null; // called while shutting down - this should only happen in tests or maybe not at all
		}

		String sessionId = session.getId();
		UserSession csSession = activeSessions.get(sessionId);
		if (csSession == null) {
			csSession = new UserSession(session, sharedExecutor);
			csSession.setSessionId(sessionId);
			activeSessions.put(sessionId, csSession);
			LOGGER.info("Session "+session.getId()+" started");
			updateCounters();
			if(totalSessionsCounter != null) {
				totalSessionsCounter.inc();
			}
		}
		else {
			LOGGER.warn("Unable to register session: Session with ID "+sessionId+" already registered");
		}
		return csSession;
	}

	public synchronized void sessionEnded(Session session) {
		if(this.state() != State.RUNNING) {
			LOGGER.warn("sessionEnded called while service state is {}",this.state().toString());
			return; // called while shutting down or resetting - this should only happen in tests
		}
		String sessionId = session.getId();
		UserSession csSession = activeSessions.get(sessionId);
		if (csSession!=null) {
			csSession.disconnect();
			activeSessions.remove(sessionId);
			LOGGER.debug("Session "+session.getId()+" ended"); // reason should be logged by endpoint
			updateCounters();
		}
		else {
			LOGGER.warn("Unable to unregister session: Session with ID "+sessionId+" not found");
		}
	}
	
	public UserSession getSession(Session session) {
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
		return this.activeSessions.get(sessionId);
	}
	
	private UserSession getSession(User user) {
		if(this.state() != State.RUNNING) {
			LOGGER.warn("getSession(User) called while service state is {}",this.state().toString());
			return null; // called while shutting down or resetting - this should only happen in tests
		}
		for (UserSession session : this.getSessions()) {
			if(session.getUser() != null && session.getUser().equals(user)) {
				return session;
			}
		}
		return null;
	}
	
	public final Collection<UserSession> getSessions() {
		if(this.state() != State.RUNNING) {
			LOGGER.warn("getSessions called while service state is {}",this.state().toString());
			return new ArrayList<UserSession>(); // called while shutting down or resetting - this should only happen in tests
		}
		return this.activeSessions.values();
	}

	public void messageReceived(Session session) {
		if(this.state() != State.RUNNING) {
			LOGGER.warn("messageReceived called while service state is {}",this.state().toString());
			return; // called while shutting down or resetting - this should only happen in tests
		}
		String sessionId = session.getId();
		UserSession csSession = activeSessions.get(sessionId);
		if (csSession!=null) {
			csSession.setLastMessageReceived(System.currentTimeMillis());
		}
		else {
			LOGGER.warn("Unable to update message received timestamp: Session with ID "+sessionId+" not found");
		}		
	}

	public void messageSent(Session session) {
		if(this.state() != State.RUNNING) {
			LOGGER.warn("messageSent called while service state is {}",this.state().toString());
			return; // called while shutting down or resetting - this should only happen in tests
		}

		String sessionId = session.getId();
		UserSession csSession = activeSessions.get(sessionId);
		if (csSession!=null) {
			csSession.setLastMessageSent(System.currentTimeMillis());
		}
		else {
			LOGGER.warn("Unable to update message sent timestamp: Session with ID "+sessionId+" not found");
		}		
	}
	
	public void updateCounters() {
		if(this.state() != State.RUNNING) {
			LOGGER.warn("updateCounters called while service state is {}",this.state().toString());
			return; // called while shutting down or resetting - this should only happen in tests
		}

		int activeSiteController = 0, numActiveSessions = 0;
		for (UserSession session : activeSessions.values()) {
			if ((session.getLastState()==UserSession.State.ACTIVE || session.getLastState()==UserSession.State.IDLE_WARNING)) {
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

	public int sendMessage(Set<User> users, MessageABC message) {
		List<User> sent = new ArrayList<User>();
		for (User user : users) {
			if (sendMessage(user, message)) {
				sent.add(user);
			}
		} 
		return sent.size();
	}

	private boolean sendMessage(User user, MessageABC message) {
		UserSession session = getSession(user);
		if (session != null) {
			session.sendMessage(message);
			return true;
		} else {
			return false;
		}
	}

	private void processSession(UserSession session) {
		if(!suppressKeepAlive) {
			// consider sending keepalive
			long timeSinceLastSent = System.currentTimeMillis() - session.getLastMessageSent();
			if (timeSinceLastSent>keepAliveInterval) {
				if (session.getLastState()==UserSession.State.INACTIVE) {
					// don't send keep-alives on inactive sessions
					LOGGER.warn("Session is INACTIVE, not sending keepalives - ",session.getSessionId());
				} else {
					LOGGER.debug("Sending keep-alive on "+session.getSessionId());
					try {
						session.sendMessage(new KeepAlive());
					} catch (Exception e) {
						LOGGER.error("Failed to send Keepalive", e);
					}
				}
			}
		} // else suppressing keepalives

		// regardless of keepalive setting, monitor session activity state
		UserSession.State newSessionState = determineSessionState(session);
		if(newSessionState != session.getLastState()) {
			LOGGER.info("Session state on "+session.getSessionId()+" changed from "+session.getLastState().toString()+" to "+newSessionState.toString());
			session.setLastState(newSessionState);
			
			if(killIdle && newSessionState == UserSession.State.INACTIVE) {
				// kill idle session on state change, if configured to do so
				LOGGER.warn("Connection timed out with "+session.getSessionId()+".  Closing session.");
				session.disconnect(new CloseReason(CloseCodes.GOING_AWAY, "Timeout"));
			}
		}

	}

	private UserSession.State determineSessionState(UserSession session) {
		long timeSinceLastReceived = System.currentTimeMillis() - session.getLastMessageReceived();
	
		if ((session.isSiteController() && timeSinceLastReceived > siteControllerTimeout) 
			|| (session.isAppUser() && timeSinceLastReceived > webAppTimeout)
			|| (timeSinceLastReceived > defaultTimeout)) {
			return UserSession.State.INACTIVE;
		}//else 
		if (timeSinceLastReceived > idleWarningTimeout) {
			return UserSession.State.IDLE_WARNING;
		}//else
		return UserSession.State.ACTIVE;
	}
	
	public void reset() {
		this.resetting = true;
		if(this.state() != State.RUNNING) {
			// bad integration test
			throw new IllegalStateException("reset called while service state is "+this.state().toString());
		}
		try {
			shutDown();
			startUp();
		} catch (Exception e) {
			throw new RuntimeException("failed to reset state of SessionManagerService",e);
		}
		this.resetting = false;
		LOGGER.info("reset state of SessionManagerService");
	}
	
	@Override
	protected synchronized void startUp() throws Exception {
		activeSessionsCounter = MetricsService.getInstance().createCounter(MetricsGroup.WSS,"sessions.active");
		activeSiteControllerSessionsCounter = MetricsService.getInstance().createCounter(MetricsGroup.WSS,"sessions.sitecontrollers");
		totalSessionsCounter = MetricsService.getInstance().createCounter(MetricsGroup.WSS,"sessions.total");
		
		ThreadFactoryBuilder builder = new ThreadFactoryBuilder();
		builder.setNameFormat("UserSession %s");
		this.sharedExecutor = Executors.newCachedThreadPool(builder.build());
		this.activeSessions = Maps.newConcurrentMap();
		
		suppressKeepAlive = Boolean.getBoolean("websocket.idle.suppresskeepalive");
		killIdle = Boolean.getBoolean("websocket.idle.kill");

	}

	@Override
	protected synchronized void shutDown() throws Exception {
		LOGGER.info("shutting down session manager with {} active sessions",this.activeSessions.size());
		Collection<UserSession> sessions = this.activeSessions.values();
		for(UserSession session : sessions) {
			session.getWsSession().close(new CloseReason(CloseCodes.GOING_AWAY,""));
		}
		this.activeSessions = null;
		this.sharedExecutor.shutdown();
		this.sharedExecutor.awaitTermination(30, TimeUnit.SECONDS);
		this.sharedExecutor = null;
		LOGGER.info("session manager stopped");
	}

	@Override
	protected synchronized void runOneIteration() throws Exception {
		
		try {
			if(this.activeSessions != null) { // service is not shutting down or resetting
				int pings = 0;
				// check status, send keepalive etc on all sessions
				Collection<UserSession> sessions = this.getSessions();
				for (UserSession session : sessions) {
					ContextLogging.setSession(session);
					// send ping periodically to measure latency
					if (session.isSiteController() && System.currentTimeMillis()-session.getLastPingSent()>pingInterval) {
						// send ping
						PingRequest request = new PingRequest();
						long now = System.currentTimeMillis();
						session.setLastPingSent(now);
						LOGGER.trace("Sending ping on "+session.getSessionId());
						session.sendMessage(request);
						pings++;
					}
					try {
						processSession(session);
					} finally {
						ContextLogging.clearSession();
					}
					// check if keep alive needs to be sent
				}
				if(pings>0) {
					LOGGER.info("Watchdog sent "+pings+" pings");
				}
			}
		} catch (Exception e) {
			LOGGER.error("SERIOUS ERROR: Unhandled exception in SessionManager",e);
		}
	}

	@Override
	protected Scheduler scheduler() {
		//return Scheduler.newFixedRateSchedule(this.startupDelaySeconds, this.periodSeconds, TimeUnit.SECONDS);
		return Scheduler.newFixedDelaySchedule(this.startupDelaySeconds, this.periodSeconds, TimeUnit.SECONDS);
	}
}
