package com.gadgetworks.codeshelf.ws.jetty.server;

import java.util.Collection;

import javax.websocket.CloseReason;
import javax.websocket.CloseReason.CloseCodes;

import lombok.Getter;
import lombok.Setter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gadgetworks.codeshelf.application.ContextLogging;
import com.gadgetworks.codeshelf.util.ThreadUtils;
import com.gadgetworks.codeshelf.ws.jetty.protocol.message.KeepAlive;
import com.gadgetworks.codeshelf.ws.jetty.protocol.request.PingRequest;

public class ServerWatchdogThread extends Thread {
	
	private static final Logger	LOGGER = LoggerFactory.getLogger(ServerWatchdogThread.class);

	private SessionManager sessionManager;
	
	@Getter @Setter
	boolean exit = false;
	
	@Getter @Setter
	int waitTime = 1*1000;
	
	@Getter @Setter
	int initialWaitTime = 2*1000;
	
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
	
	public ServerWatchdogThread(SessionManager sessionManager) {
		this.sessionManager = sessionManager;
		this.setDaemon(true);
	}

	@Override
	public void run() {
		LOGGER.info("Watchdog thread running. SuppressKeepAlive="+this.isSuppressKeepAlive()+" KillIdle="+this.isKillIdle());
		try {
			ThreadUtils.sleep(initialWaitTime);
		} catch (Exception e) {
		}
		while (!exit) {
			try {
				doSessionWatchdog();
				ThreadUtils.sleep(waitTime);
			} catch (Exception e) {
				LOGGER.error("Caught exception in watchdog, continuing.",e);
			}
		}
		LOGGER.info("Watchdog thread exiting by request.");
	}
	
	private void doSessionWatchdog() {
		int pings = 0;
		
		// check status, send keepalive etc on all sessions
		Collection<UserSession> sessions = this.sessionManager.getSessions();
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
}
