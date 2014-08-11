package com.gadgetworks.codeshelf.ws.jetty.server;

import java.util.Collection;

import javax.websocket.CloseReason;
import javax.websocket.Session;
import javax.websocket.CloseReason.CloseCodes;

import lombok.Getter;
import lombok.Setter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gadgetworks.codeshelf.util.ThreadUtils;
import com.gadgetworks.codeshelf.ws.jetty.protocol.message.KeepAlive;

public class ServerWatchdogThread extends Thread {

	private static final Logger	LOGGER = LoggerFactory.getLogger(ServerWatchdogThread.class);

	JettyWebSocketServer server;
	
	@Getter @Setter
	boolean exit = false;
	
	@Getter @Setter
	int waitTime = 1*1000;
	
	@Getter @Setter
	int initialWaitTime = 2*1000;
	
	@Getter @Setter
	boolean useKeepAlive = true;
	
	@Getter @Setter
	int siteControllerTimeout = 60*1000; // 1 min
	
	@Getter @Setter
	int webAppTimeout = 5*60*1000; // 5 mins
	
	@Getter @Setter
	int unknownTimeout = 5*60*1000; // 5 mins

	@Getter @Setter
	int keepAliveInterval = 10*1000;
	
	public ServerWatchdogThread(JettyWebSocketServer server) {
		this.server = server;
		this.setDaemon(true);
	}

	@Override
	public void run() {
		ThreadUtils.sleep(initialWaitTime);
		while (!exit) {
			try {
				// send ping on all site controller sessions
				Collection<CsSession> sessions = SessionManager.getInstance().getSessions();
				for (CsSession session : sessions) {
					String sessionId = session.getSessionId();
					Session wsSession = session.getSession();
					if (useKeepAlive) {
						long timeSinceLastSent = System.currentTimeMillis() - session.getLastMessageSent();
						long timeSinceLastReceived = System.currentTimeMillis() - session.getLastMessageReceived();
						LOGGER.debug(session.getSessionId()+": Sent="+timeSinceLastSent+" Received="+timeSinceLastReceived);
						if (session.getType()==SessionType.SiteController && timeSinceLastReceived>siteControllerTimeout) {
							// disconnect timed out site controller
							LOGGER.info("Site Controller connection timed out.  Closing session.");
							session.disconnect(new CloseReason(CloseCodes.GOING_AWAY, "Timeout"));
						}
						else if (session.getType()==SessionType.UserApp && timeSinceLastReceived>webAppTimeout) {
							// disconnect user application
							LOGGER.info("Client application connection timed out.  Closing session.");
							session.disconnect(new CloseReason(CloseCodes.GOING_AWAY, "Timeout"));
						}
						else if (session.getType()==SessionType.Unknown && timeSinceLastReceived>unknownTimeout) {
							// disconnect unknown client
							LOGGER.info("Connection to unkown client timed out.  Closing session.");
							session.disconnect(new CloseReason(CloseCodes.GOING_AWAY, "Timeout"));
						}
						else if (timeSinceLastSent>keepAliveInterval) {
							// send keep-alive message
							LOGGER.debug("Sending keep-alive on "+sessionId);
							session.sendMessage(new KeepAlive());
						}
					}
				}
				ThreadUtils.sleep(waitTime);
			} catch (Exception e) {
				LOGGER.error("Failed to send keep alive", e);
			}
		}
	}
}
