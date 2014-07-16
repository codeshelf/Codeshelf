package com.gadgetworks.codeshelf.device;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gadgetworks.codeshelf.util.ThreadUtils;
import com.gadgetworks.codeshelf.ws.jetty.client.JettyWebSocketClient;

public class ConnectionManagerThread extends Thread {

	private static final Logger	LOGGER = LoggerFactory.getLogger(ConnectionManagerThread.class);

	CsDeviceManager deviceManager;
	boolean exit = false;
	
	public ConnectionManagerThread(CsDeviceManager deviceManager) {
		this.deviceManager = deviceManager;
		this.setName("WebSocket Maintenance Thread");
	}
	
	@Override
	public void run() {
		while(!exit) {
	    	try {
	    		JettyWebSocketClient client = deviceManager.getClient();
	    		if (!client.isConnected()) {
	    			LOGGER.debug("Not connected to server.  Trying to connect.");
	    			client.connect();
	    		}
			} 
	    	catch (Exception e) {
				LOGGER.error("Failed to connect to WS server", e);
			}
    		ThreadUtils.sleep(2000);
		}
	}

}
