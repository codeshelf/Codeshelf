package com.gadgetworks.codeshelf.ws.jetty.server;

import java.util.Collection;

import javax.websocket.CloseReason;
import javax.websocket.CloseReason.CloseCodes;

import lombok.Getter;
import lombok.Setter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gadgetworks.codeshelf.util.ThreadUtils;
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
		ThreadUtils.sleep(initialWaitTime);
		while (!exit) {
			// send ping on all site controller sessions
			Collection<UserSession> sessions = this.sessionManager.getSessions();
			for (UserSession session : sessions) {
				String sessionId = session.getSessionId();
				if (session.getLastState()==UserSession.State.INACTIVE) {
					// don't send keep-alives on inactive sessions
					continue;
				}
				// don't send keepalive if suppressed
				// don't send keepalive to unauthenticated site controller
				if (!suppressKeepAlive && session.isAuthenticated()) {
					long timeSinceLastSent = System.currentTimeMillis() - session.getLastMessageSent();
					if (timeSinceLastSent>keepAliveInterval) {
						LOGGER.debug("Sending keep-alive on "+sessionId);
						try {
							session.sendMessage(new KeepAlive());
						} catch (Exception e) {
							LOGGER.error("Failed to send Keepalive", e);
						}
					}
				}

				UserSession.State newSessionState = determineSessionState(session);
				if(newSessionState != session.getLastState()) {
					LOGGER.info("Session state changed from "+session.getLastState().toString()+" to "+newSessionState.toString());
					session.setLastState(newSessionState);
					
					if(killIdle && newSessionState == UserSession.State.INACTIVE) {
						LOGGER.warn("Connection timed out, Closing session.");
						session.disconnect(new CloseReason(CloseCodes.GOING_AWAY, "Timeout"));
					}
				}
			}
			ThreadUtils.sleep(waitTime);
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
