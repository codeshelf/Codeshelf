package com.gadgetworks.codeshelf.ws.jetty.server;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.websocket.Session;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SessionManager {

	private static final Logger	LOGGER = LoggerFactory.getLogger(SessionManager.class);

	private final static SessionManager theSessionManager = new SessionManager();
	
	private ConcurrentHashMap<String,CsSession> mActiveSessions = new ConcurrentHashMap<String, CsSession>();
	
	private SessionManager() {
	}
	
	public static SessionManager getInstance() {
		return theSessionManager;
	}

	public synchronized void sessionStarted(Session session) {
		String sessionId = session.getId();
		if (!mActiveSessions.containsKey(sessionId)) {
			CsSession csSession = new CsSession();
			csSession.setSessionId(sessionId);
			mActiveSessions.put(sessionId, csSession);
			LOGGER.info("Session "+session.getId()+" started");
		}
		else {
			LOGGER.warn("Unable to register session: Session with ID "+sessionId+" already registered");
		}
	}

	public synchronized void sessionEnded(Session session) {
		String sessionId = session.getId();
		if (mActiveSessions.containsKey(sessionId)) {
			mActiveSessions.remove(sessionId);
			LOGGER.info("Session "+session.getId()+" ended");
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
		return this.mActiveSessions.get(sessionId);
	}

}
