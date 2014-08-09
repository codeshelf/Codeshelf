package com.gadgetworks.codeshelf.device;

import lombok.Getter;
import lombok.Setter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gadgetworks.codeshelf.util.ThreadUtils;
import com.gadgetworks.codeshelf.ws.jetty.client.JettyWebSocketClient;
import com.gadgetworks.codeshelf.ws.jetty.protocol.message.KeepAlive;

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
	boolean useKeepAlive = true;
	
	@Getter @Setter
	int siteControllerTimeout = 60*1000;
	
	@Getter @Setter
	int keepAliveInterval = 10*1000;	
	
	public ConnectionManagerThread(CsDeviceManager deviceManager) {
		this.deviceManager = deviceManager;
		this.setName("WebSocket Maintenance Thread");
		this.setDaemon(true);
	}
	
	@Override
	public void run() {
		ThreadUtils.sleep(initialWaitTime);
		while(!exit) {
	    	try {
	    		JettyWebSocketClient client = deviceManager.getClient();
	    		if (!client.isConnected()) {
	    			LOGGER.debug("Not connected to server.  Trying to connect.");
	    			client.connect();
	    		}
	    		else {
					if (useKeepAlive) {
						long timeSinceLastSent = System.currentTimeMillis() - client.getLastMessageSent();
						long timeSinceLastReceived = System.currentTimeMillis() - client.getLastMessageReceived();
						LOGGER.debug("WebSocket: Sent="+timeSinceLastSent+" Received="+timeSinceLastReceived);
						if (timeSinceLastSent>keepAliveInterval) {
							// send keep-alive message
							LOGGER.debug("Sending keep-alive");
							client.sendMessage(new KeepAlive());
						}
						if (timeSinceLastReceived>siteControllerTimeout) {
							LOGGER.info("Server connection timed out.  Restarting session.");
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
	}

}
