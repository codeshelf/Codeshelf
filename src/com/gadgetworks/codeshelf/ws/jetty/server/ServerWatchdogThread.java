package com.gadgetworks.codeshelf.ws.jetty.server;

import java.util.Collection;

import javax.websocket.CloseReason;
import javax.websocket.CloseReason.CloseCodes;

import lombok.Getter;
import lombok.Setter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gadgetworks.codeshelf.util.ThreadUtils;
import com.gadgetworks.codeshelf.ws.ContextLogging;
import com.gadgetworks.codeshelf.ws.jetty.protocol.message.KeepAlive;

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
		// check status, send keepalive etc on all sessions
		Collection<CsSession> sessions = this.sessionManager.getSessions();
		for (CsSession session : sessions) {
			ContextLogging.set(session);
			try {
				doSessionWatchdog(session);
			} finally {
				ContextLogging.clear();
			}
		}
	}
	
	private void doSessionWatchdog(CsSession session) {
		if(!suppressKeepAlive) {
			// consider sending keepalive
			long timeSinceLastSent = System.currentTimeMillis() - session.getLastMessageSent();
			if (timeSinceLastSent>keepAliveInterval) {
				if (session.getLastState()==CsSession.State.INACTIVE) {
					// don't send keep-alives on inactive sessions
					LOGGER.warn("Session is INACTIVE, not sending keepalives - "+session.getType().toString()+" ",session.getSessionId());
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
		CsSession.State newSessionState = determineSessionState(session);
		if(newSessionState != session.getLastState()) {
			LOGGER.info("Session state on "+session.getType().toString()+" changed from "+session.getLastState().toString()+" to "+newSessionState.toString());
			session.setLastState(newSessionState);
			
			if(killIdle && newSessionState == CsSession.State.INACTIVE) {
				// kill idle session on state change, if configured to do so
				LOGGER.warn("Connection timed out with "+session.getType().toString()+".  Closing session.");
				session.disconnect(new CloseReason(CloseCodes.GOING_AWAY, "Timeout"));
			}
		}

	}

	private CsSession.State determineSessionState(CsSession session) {
		long timeSinceLastReceived = System.currentTimeMillis() - session.getLastMessageReceived();

		if ((session.getType()==SessionType.SiteController && timeSinceLastReceived > siteControllerTimeout) 
			|| (session.getType()==SessionType.UserApp && timeSinceLastReceived > webAppTimeout)
			|| (session.getType()==SessionType.Unknown && timeSinceLastReceived > defaultTimeout)) {
			return CsSession.State.INACTIVE;
		}//else 
		if (timeSinceLastReceived > idleWarningTimeout) {
			return CsSession.State.IDLE_WARNING;
		}//else
		return CsSession.State.ACTIVE;
	}
}
