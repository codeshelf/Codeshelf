package com.gadgetworks.codeshelf.ws.jetty.server;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.websocket.Session;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Counter;
import com.gadgetworks.codeshelf.metrics.MetricsGroup;
import com.gadgetworks.codeshelf.metrics.MetricsService;
import com.gadgetworks.codeshelf.model.domain.User;
import com.gadgetworks.codeshelf.ws.jetty.protocol.message.MessageABC;
import com.gadgetworks.codeshelf.ws.jetty.server.UserSession.State;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

public class SessionManager {

	private static final Logger	LOGGER = LoggerFactory.getLogger(SessionManager.class);

	private final static SessionManager theSessionManager = new SessionManager();
	
	private HashMap<String,UserSession> activeSessions; 
	private ExecutorService	sharedExecutor;

	private final Counter activeSessionsCounter = MetricsService.addCounter(MetricsGroup.WSS,"sessions.active");
	private final Counter activeSiteControlerSessionsCounter = MetricsService.addCounter(MetricsGroup.WSS,"sessions.sitecontrollers");
	private final Counter totalSessionsCounter = MetricsService.addCounter(MetricsGroup.WSS,"sessions.total");

	private SessionManager() {
		ThreadFactoryBuilder builder = new ThreadFactoryBuilder();
		builder.setNameFormat("UserSession %s");
		this.sharedExecutor = Executors.newCachedThreadPool(builder.build());
		this.activeSessions = Maps.newHashMap();
	}
	 
	public static SessionManager getInstance() {
		return theSessionManager;
	}

	public synchronized void sessionStarted(Session session) {
		String sessionId = session.getId();
		if (!activeSessions.containsKey(sessionId)) {
			UserSession csSession = new UserSession(session, sharedExecutor);
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
		UserSession csSession = activeSessions.get(sessionId);
		if (csSession!=null) {
			csSession.disconnect();
			activeSessions.remove(sessionId);
			LOGGER.info("Session "+session.getId()+" ended");
			updateCounters();
		}
		else {
			LOGGER.warn("Unable to unregister session: Session with ID "+sessionId+" not found");
		}
	}
	
	public UserSession getSession(Session session) {
		String sessionId = session.getId();
		if (sessionId==null) {
			return null;
		}
		return this.activeSessions.get(sessionId);
	}
	
	public UserSession getSession(User user) {
		for (UserSession session : this.getSessions()) {
			if(session.getUser().equals(user)) {
				return session;
			}
		}
		return null;
	}
	
	public Set<UserSession> getSessions(Set<User> users) {
		Set<UserSession> userSessions = new HashSet<UserSession>();
		for (UserSession session : this.getSessions()) {
			if(users.contains(session.getUser())) {
				userSessions.add(session);
			}
		}
		return userSessions;
	}

	public final Collection<UserSession> getSessions() {
		return this.activeSessions.values();
	}

	public void messageReceived(Session session) {
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
		String sessionId = session.getId();
		UserSession csSession = activeSessions.get(sessionId);
		if (csSession!=null) {
			csSession.setLastMessageSent(System.currentTimeMillis());
		}
		else {
			LOGGER.warn("Unable to update message sent timestamp: Session with ID "+sessionId+" not found");
		}		
	}
	
	public void resetSessions() {
		activeSessions.clear();
	}
	
	public void updateCounters() {
		int activeSiteController = 0, numActiveSessions = 0;
		for (UserSession session : activeSessions.values()) {
			if ((session.getLastState()==State.ACTIVE || session.getLastState()==State.IDLE_WARNING)) {
				numActiveSessions++;
				if (session.isSiteController()) {
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
		UserSession session = getSession(user);
		if (session != null) {
			session.sendMessage(message);
			return true;
		} else {
			return false;
		}
	}
}
