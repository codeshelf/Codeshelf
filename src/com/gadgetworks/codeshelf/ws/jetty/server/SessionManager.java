package com.gadgetworks.codeshelf.ws.jetty.server;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import javax.websocket.Session;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Counter;
import com.gadgetworks.codeshelf.metrics.MetricsGroup;
import com.gadgetworks.codeshelf.metrics.MetricsService;
import com.gadgetworks.codeshelf.model.domain.User;
import com.gadgetworks.codeshelf.ws.jetty.protocol.message.MessageABC;
import com.gadgetworks.codeshelf.ws.jetty.server.CsSession.State;

public class SessionManager {

	private static final Logger	LOGGER = LoggerFactory.getLogger(SessionManager.class);

	private final static SessionManager theSessionManager = new SessionManager();
	
	private HashMap<String,CsSession> activeSessions = new HashMap<String, CsSession>();
	
	private final Counter activeSessionsCounter = MetricsService.addCounter(MetricsGroup.WSS,"sessions.active");
	private final Counter activeSiteControlerSessionsCounter = MetricsService.addCounter(MetricsGroup.WSS,"sessions.sitecontrollers");
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
			updateCounters();
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
			updateCounters();
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
	
	private CsSession getSession(User user) {
		for (CsSession session : this.getSessions()) {
			if(session.getUser().equals(user)) {
				return session;
			}
		}
		return null;
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
	
	public void updateCounters() {
		int activeSiteController = 0, numActiveSessions = 0;
		for (CsSession session : activeSessions.values()) {
			if ((session.getLastState()==State.ACTIVE || session.getLastState()==State.IDLE_WARNING)) {
				numActiveSessions++;
				if (session.getType()==SessionType.SiteController) {
					activeSiteController++;
				}
			}
		}
		// this is needed, since counters don't have an explicit set method
		long c = activeSiteControlerSessionsCounter.getCount();
		activeSiteControlerSessionsCounter.inc(activeSiteController-c);		
		c = activeSessionsCounter.getCount();
		activeSessionsCounter.inc(numActiveSessions-c);
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
		CsSession session = getSession(user);// TODO Auto-generated method stub
		if (session != null) {
			session.sendMessage(message);
			return true;
		} else {
			return false;
		}
	}
}
