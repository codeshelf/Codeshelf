package com.codeshelf.device;

import lombok.Getter;
import lombok.Setter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.util.ThreadUtils;
import com.codeshelf.ws.jetty.client.JettyWebSocketClient;
import com.codeshelf.ws.jetty.protocol.message.KeepAlive;
import com.codeshelf.ws.jetty.server.UserSession;

public class ConnectionManagerThread extends Thread {

	private static final Logger	LOGGER = LoggerFactory.getLogger(ConnectionManagerThread.class);

	CsDeviceManager deviceManager;
	
	@Getter @Setter
	boolean exit = false;
	
	@Getter @Setter
	int waitTime = 1*1000;
	
	@Getter @Setter
	int initialWaitTime = 2*1000;
	
	@Getter @Setter
	int siteControllerTimeout = 24*1000;
	
	@Getter @Setter
	int keepAliveInterval = 5*1000;	
	
	@Getter @Setter
	int idleWarningTimeout = 14*1000;	
	
	@Getter @Setter
	UserSession.State lastState = UserSession.State.INACTIVE;
	
	public ConnectionManagerThread(CsDeviceManager deviceManager) {
		this.deviceManager = deviceManager;
		this.setName("WS Client Maintenance Thread");
		this.setDaemon(true);
	}
	
	@Override
	public void run() {
		ThreadUtils.sleep(initialWaitTime);
		LOGGER.info("WS connection manager thread is starting");
		LOGGER.info("WS connection manager idleKill: " + deviceManager.isIdleKill() + ", suppressKeepAlive: " + deviceManager.isSuppressKeepAlive());
		while(!exit) {
	    	try {
	    		JettyWebSocketClient client = deviceManager.getClient();
	    		if (!client.isConnected()) {
	    			LOGGER.debug("Not connected to server.  Trying to connect.");
	    			client.connect();
	    		}
	    		else {
					long timeSinceLastSent = System.currentTimeMillis() - client.getLastMessageSent();
					if (!deviceManager.isSuppressKeepAlive()) {
						if (timeSinceLastSent>keepAliveInterval) {
							// DEV-598 ideally, might log once every 10 or saying each keepalive had been sent.
							// For now, just suppress the normal.
							if (timeSinceLastSent> 2 * keepAliveInterval) 
								LOGGER.warn("Late: sending keep alive from the site controller " + timeSinceLastSent + " ms after last send.");
							else
								LOGGER.debug("Sending keep alive from the site controller");
							client.sendMessage(new KeepAlive());
						}
					}
					
					UserSession.State newSessionState = determineSessionState(client);
					if(newSessionState != this.getLastState()) {
						LOGGER.info("Session state changed from "+getLastState().toString()+" to "+newSessionState.toString());
						setLastState(newSessionState);
						
						if (deviceManager.isIdleKill() && newSessionState == UserSession.State.INACTIVE) {
							LOGGER.warn("Server connection timed out.  Restarting session.");
							client.disconnect();
						}
					}
	    		}
			} 
	    	catch (Exception e) {
				LOGGER.error("Failed to connect to WS server", e);
			}
    		ThreadUtils.sleep(waitTime);
		}
		LOGGER.info("WS connection manager thread is terminating");
	}

	private UserSession.State determineSessionState(JettyWebSocketClient client) {
		long timeSinceLastReceived = System.currentTimeMillis() - client.getLastMessageReceived();

		if (timeSinceLastReceived > siteControllerTimeout) {
			return UserSession.State.INACTIVE;
		}//else 
		if (timeSinceLastReceived > idleWarningTimeout) {
			return UserSession.State.IDLE_WARNING;
		}//else
		return UserSession.State.ACTIVE;
	}

}
