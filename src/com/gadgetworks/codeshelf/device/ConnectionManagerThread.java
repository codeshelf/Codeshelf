package com.gadgetworks.codeshelf.device;

import lombok.Getter;
import lombok.Setter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gadgetworks.codeshelf.util.ThreadUtils;
import com.gadgetworks.codeshelf.ws.jetty.client.JettyWebSocketClient;
import com.gadgetworks.codeshelf.ws.jetty.protocol.message.KeepAlive;
import com.gadgetworks.codeshelf.ws.jetty.server.CsSession;

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
	int siteControllerTimeout = 60*1000;
	
	@Getter @Setter
	int keepAliveInterval = 10*1000;	
	
	@Getter @Setter
	int idleWarningTimeout = 15*1000;	
	
	@Getter @Setter
	CsSession.State lastState = CsSession.State.INACTIVE;
	
	public ConnectionManagerThread(CsDeviceManager deviceManager) {
		this.deviceManager = deviceManager;
		this.setName("WebSocket Maintenance Thread");
		this.setDaemon(true);
	}
	
	@Override
	public void run() {
		ThreadUtils.sleep(initialWaitTime);
		LOGGER.info("WS connection manager thread is starting");
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
							client.sendMessage(new KeepAlive());
						}
					}
					
					CsSession.State newSessionState = determineSessionState(client);
					if(newSessionState != this.getLastState()) {
						LOGGER.info("Session state changed from "+getLastState().toString()+" to "+newSessionState.toString());
						setLastState(newSessionState);
						
						if (deviceManager.isIdleKill() && newSessionState == CsSession.State.INACTIVE) {
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

	private CsSession.State determineSessionState(JettyWebSocketClient client) {
		long timeSinceLastReceived = System.currentTimeMillis() - client.getLastMessageReceived();

		if (timeSinceLastReceived > siteControllerTimeout) {
			return CsSession.State.INACTIVE;
		}//else 
		if (timeSinceLastReceived > idleWarningTimeout) {
			return CsSession.State.IDLE_WARNING;
		}//else
		return CsSession.State.ACTIVE;
	}

}
