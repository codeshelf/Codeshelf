package com.codeshelf.device;

import java.util.concurrent.TimeUnit;

import lombok.Getter;
import lombok.Setter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.ws.jetty.client.JettyWebSocketClient;
import com.codeshelf.ws.jetty.protocol.message.KeepAlive;
import com.codeshelf.ws.jetty.server.UserSession;
import com.google.common.util.concurrent.AbstractScheduledService;

public class ClientConnectionManagerService extends AbstractScheduledService {

	private static final Logger	LOGGER					= LoggerFactory.getLogger(ClientConnectionManagerService.class);

	@Getter
	@Setter
	int							periodSeconds			= 1;

	@Getter
	@Setter
	int							startupDelaySeconds		= 2;

	@Getter
	@Setter
	int							siteControllerTimeout	= 24 * 1000;

	@Getter
	@Setter
	int							keepAliveInterval		= 5 * 1000;

	@Getter
	@Setter
	int							idleWarningTimeout		= 14 * 1000;

	@Getter
	@Setter
	boolean						suppressKeepAlive		= false;

	@Getter
	@Setter
	boolean						idleKill				= false;

	@Getter
	@Setter
	UserSession.State			lastState				= UserSession.State.INACTIVE;

	JettyWebSocketClient		wsClient;

	public ClientConnectionManagerService(JettyWebSocketClient client) {
		this.wsClient = client;
	}

	@Override
	protected void startUp() throws Exception {
		LOGGER.info("WS connection manager thread is starting");
		suppressKeepAlive = Boolean.getBoolean("websocket.idle.suppresskeepalive");
		idleKill = Boolean.getBoolean("websocket.idle.kill");
		LOGGER.info("WS connection manager idleKill: " + isIdleKill() + ", suppressKeepAlive: " + isSuppressKeepAlive());
	}

	@Override
	protected void runOneIteration() throws Exception {
		try {
			if (!wsClient.isConnected()) {
				LOGGER.debug("Not connected to server.  Trying to connect.");
				wsClient.connect();
			}
		} catch (Exception e) {
			LOGGER.debug("exception connecting websocket client (continuing)", e); // session onError should log reason
		}

		try {
			if (wsClient.isConnected()) {
				long timeSinceLastSent = System.currentTimeMillis() - wsClient.getLastMessageSent();
				if (!isSuppressKeepAlive()) {
					if (timeSinceLastSent > keepAliveInterval) {
						// DEV-598 ideally, might log once every 10 or saying each keepalive had been sent.
						// For now, just suppress the normal.
						if (timeSinceLastSent > 2 * keepAliveInterval)
							LOGGER.warn("Late: sending keep alive from the site controller " + timeSinceLastSent
									+ " ms after last send.");
						else
							LOGGER.debug("Sending keep alive from the site controller");
						wsClient.sendMessage(new KeepAlive());
					}
				}

				UserSession.State newSessionState = determineSessionState();
				if (newSessionState != this.getLastState()) {
					LOGGER.info("Session state changed from " + getLastState().toString() + " to " + newSessionState.toString());
					setLastState(newSessionState);

					if (isIdleKill() && newSessionState == UserSession.State.INACTIVE) {
						LOGGER.warn("Server connection timed out.  Restarting session.");
						wsClient.disconnect();
					}
				}
			}
		} catch (Exception e) {
			LOGGER.error("Unexpected exception handling keepalive (continuing)", e);
		}
	}

	@Override
	protected void shutDown() throws Exception {
		wsClient.disconnect();
		LOGGER.info("WS connection manager thread is stopping");
	}

	@Override
	protected Scheduler scheduler() {
		return Scheduler.newFixedDelaySchedule(this.startupDelaySeconds, this.periodSeconds, TimeUnit.SECONDS);
	}

	private UserSession.State determineSessionState() {
		long timeSinceLastReceived = System.currentTimeMillis() - wsClient.getLastMessageReceived();

		if (timeSinceLastReceived > siteControllerTimeout) {
			return UserSession.State.INACTIVE;
		}//else 
		if (timeSinceLastReceived > idleWarningTimeout) {
			return UserSession.State.IDLE_WARNING;
		}//else
		return UserSession.State.ACTIVE;
	}

}
