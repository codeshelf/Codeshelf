package com.codeshelf.device;

import java.io.IOException;
import java.net.ConnectException;
import java.util.concurrent.TimeUnit;

import lombok.Getter;
import lombok.Setter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.service.AbstractCodeshelfScheduledService;
import com.codeshelf.ws.client.CsClientEndpoint;
import com.codeshelf.ws.protocol.message.KeepAlive;
import com.codeshelf.ws.server.WebSocketConnection;

public class ClientConnectionManagerService extends AbstractCodeshelfScheduledService {

	private static final Logger	LOGGER					= LoggerFactory.getLogger(ClientConnectionManagerService.class);

	boolean						requestConnected		= true; // establish/stay connected
	
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
	WebSocketConnection.State			lastState				= WebSocketConnection.State.INACTIVE;

	@Getter
	CsClientEndpoint			clientEndpoint;

	public ClientConnectionManagerService(CsClientEndpoint clientEndpoint) {
		this.clientEndpoint = clientEndpoint;
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
		connect();

		try {
			if (clientEndpoint.isConnected()) {
				if(requestConnected) {
					long timeSinceLastSent = System.currentTimeMillis() - clientEndpoint.getLastMessageSent();
					if (!isSuppressKeepAlive()) {
						if (timeSinceLastSent > keepAliveInterval) {
							// DEV-598 ideally, might log once every 10 or saying each keepalive had been sent.
							// For now, just suppress the normal.
							if (timeSinceLastSent > 2 * keepAliveInterval)
								LOGGER.warn("Late: sending keep alive from the site controller " + timeSinceLastSent
										+ " ms after last send.");
							else
								LOGGER.debug("Sending keep alive from the site controller");
							clientEndpoint.sendMessage(new KeepAlive());
						}
					}

					WebSocketConnection.State newSessionState = determineSessionState();
					if (newSessionState != this.getLastState()) {
						LOGGER.info("Session state changed from " + getLastState().toString() + " to " + newSessionState.toString());
						setLastState(newSessionState);

						if (isIdleKill() && newSessionState == WebSocketConnection.State.INACTIVE) {
							LOGGER.warn("Server connection timed out.  Restarting session.");
							clientEndpoint.disconnect();
						}
					}
				}
			}
		} catch (Exception e) {
			LOGGER.error("Unexpected exception in runOneIteration(); (continuing)", e);
		}
	}

	private void connect() {
		if(!requestConnected)
			return;
		
		try {
			if (!clientEndpoint.isConnected()) {
				LOGGER.warn("Not connected to server.  Trying to connect.");
				clientEndpoint.connect(); // we see this throw an IOException when site controller cannot connect to app server
			}
		} catch (ConnectException e) {
			LOGGER.warn("Failed to connect WebSocket: ", e);
		} catch (IOException e) {
			LOGGER.warn("Failed to connect WebSocket: ", e);
		} catch (Exception e) {
			LOGGER.error("Unexpected exception connecting websocket client", e); 
		}
	}

	@Override
	protected void shutDown() throws Exception {
		clientEndpoint.disconnect();
		LOGGER.info("WS connection manager thread is stopping");
	}

	@Override
	protected Scheduler scheduler() {
		return Scheduler.newFixedDelaySchedule(this.startupDelaySeconds, this.periodSeconds, TimeUnit.SECONDS);
	}
	
	public void setDisconnected() {
		this.requestConnected = false;
		if(this.isRunning()) {
			try {
				clientEndpoint.disconnect();
			} catch (IOException e) {
				LOGGER.warn("error trying to disconnect client", e);
			}
		}
	}
	
	public void setConnected() {
		this.requestConnected = true; // this is the default state
		if(this.isRunning()) {
			connect();
		}
	}

	private WebSocketConnection.State determineSessionState() {
		long timeSinceLastReceived = System.currentTimeMillis() - clientEndpoint.getLastMessageReceived();

		if (timeSinceLastReceived > siteControllerTimeout) {
			return WebSocketConnection.State.INACTIVE;
		}//else 
		if (timeSinceLastReceived > idleWarningTimeout) {
			return WebSocketConnection.State.IDLE_WARNING;
		}//else
		return WebSocketConnection.State.ACTIVE;
	}

}
