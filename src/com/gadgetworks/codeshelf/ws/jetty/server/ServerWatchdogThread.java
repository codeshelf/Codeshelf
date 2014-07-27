package com.gadgetworks.codeshelf.ws.jetty.server;

import java.util.Collection;

import javax.websocket.Session;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lombok.Getter;
import lombok.Setter;

import com.gadgetworks.codeshelf.util.ThreadUtils;
import com.gadgetworks.codeshelf.ws.jetty.protocol.request.PingRequest;

public class ServerWatchdogThread extends Thread {

	private static final Logger	LOGGER = LoggerFactory.getLogger(ServerWatchdogThread.class);

	JettyWebSocketServer server;
	boolean exit = false;
	
	@Getter @Setter
	int waitTime = 10;
	
	@Getter @Setter
	int initialWaitTime = 10;
	
	@Getter @Setter
	boolean usePing = true;
	
	public ServerWatchdogThread(JettyWebSocketServer server) {
		this.server = server;
	}

	@Override
	public void run() {
		ThreadUtils.sleep(initialWaitTime*1000);
		while (!exit) {
			try {
				// send ping on all site controller sessions
				Collection<CsSession> sessions = SessionManager.getInstance().getSessions();
				for (CsSession session : sessions) {
					String sessionId = session.getSessionId();
					Session wsSession = session.getSession();
					if (session.getType()==SessionType.SiteController) {
						if (usePing) {
							LOGGER.debug("Sending ping on session "+sessionId);
							PingRequest request = new PingRequest();
							this.server.sendRequest(wsSession, request);
						}
					}
				}
				ThreadUtils.sleep(waitTime*1000);
			} catch (Exception e) {
				LOGGER.error("Failed to send ping", e);
			}
		}
	}
}
