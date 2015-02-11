package com.codeshelf.metrics;

import com.codeshelf.device.ICsDeviceManager;
import com.codeshelf.ws.jetty.client.JettyWebSocketClient;

public class ConnectedToServerHealthCheck extends CodeshelfHealthCheck {
	
	ICsDeviceManager theDeviceManager;
	
	public ConnectedToServerHealthCheck(ICsDeviceManager deviceManager) {
		super("Server Connected");
		this.theDeviceManager = deviceManager;
	}
	
	@Override
	protected Result check() throws Exception {
		JettyWebSocketClient wsClient = theDeviceManager.getWebSocketCient();
		if (wsClient.isConnected()) {
			return Result.healthy("Site controller is connected to server");
		}
		return Result.unhealthy("Site controller is disconnected from server");
	}
}
