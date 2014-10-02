package com.gadgetworks.codeshelf.ws.jetty.server;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import javax.websocket.Session;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Counter;
import com.gadgetworks.codeshelf.metrics.MetricsGroup;
import com.gadgetworks.codeshelf.metrics.MetricsService;
import com.gadgetworks.codeshelf.model.domain.User;

public class SessionManager {

	private static final Logger	LOGGER = LoggerFactory.getLogger(SessionManager.class);

	private final static SessionManager theSessionManager = new SessionManager();
	
	private HashMap<String,CsSession> activeSessions = new HashMap<String, CsSession>();
	
	private final Counter activeSessionsCounter = MetricsService.addCounter(MetricsGroup.WSS,"sessions.active");
	private final Counter totalSessionsCounter = MetricsService.addCounter(MetricsGroup.WSS,"sessions.total");

	
	private SessionManager() {
	}
	 
	public static SessionManager getInstance() {
		return theSessionManager;
	}

	public synchronized void sessionStarted(Session session) {
		String sessionId = session.getId();
		if (!activeSessions.containsKey(sessionId)) {
			CsSession csSession = new CsSession(session);
			csSession.setSessionId(sessionId);
			activeSessions.put(sessionId, csSession);
			LOGGER.info("Session "+session.getId()+" started");
			activeSessionsCounter.inc();
			totalSessionsCounter.inc();
		}
		else {
			LOGGER.warn("Unable to register session: Session with ID "+sessionId+" already registered");
		}
	}

	public synchronized void sessionEnded(Session session) {
		String sessionId = session.getId();
		CsSession csSession = activeSessions.get(sessionId);
		if (csSession!=null) {
			csSession.close();
			activeSessions.remove(sessionId);
			LOGGER.info("Session "+session.getId()+" ended");
			activeSessionsCounter.dec();
		}
		else {
			LOGGER.warn("Unable to unregister session: Session with ID "+sessionId+" not found");
		}
	}
	
	public CsSession getSession(Session session) {
		String sessionId = session.getId();
		if (sessionId==null) {
			return null;
		}
		return this.activeSessions.get(sessionId);
	}
	
	public CsSession getSession(User user) {
		for (CsSession session : this.getSessions()) {
			if(session.getUser().equals(user)) {
				return session;
			}
		}
		return null;
	}
	
	public Set<CsSession> getSessions(Set<User> users) {
		Set<CsSession> userSessions = new HashSet<CsSession>();
		
		for (CsSession session : this.getSessions()) {
			if(users.contains(session.getUser())) {
				userSessions.add(session);
			}
		}
		return userSessions;
	}
	
	public final Collection<CsSession> getSessions() {
		return this.activeSessions.values();
	}

	public void messageReceived(Session session) {
		String sessionId = session.getId();
		CsSession csSession = activeSessions.get(sessionId);
		if (csSession!=null) {
			csSession.setLastMessageReceived(System.currentTimeMillis());
		}
		else {
			LOGGER.warn("Unable to update message received timestamp: Session with ID "+sessionId+" not found");
		}		
	}

	public void messageSent(Session session) {
		String sessionId = session.getId();
		CsSession csSession = activeSessions.get(sessionId);
		if (csSession!=null) {
			csSession.setLastMessageSent(System.currentTimeMillis());
		}
		else {
			LOGGER.warn("Unable to update message sent timestamp: Session with ID "+sessionId+" not found");
		}		
	}
	
	public void resetSessions() {
		activeSessions = new HashMap<String, CsSession>();
	}
}
