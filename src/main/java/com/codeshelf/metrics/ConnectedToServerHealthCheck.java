package com.codeshelf.metrics;

import com.codeshelf.ws.jetty.client.CsClientEndpoint;

public class ConnectedToServerHealthCheck extends CodeshelfHealthCheck {
	
	CsClientEndpoint clientEndpoint;
	
	public ConnectedToServerHealthCheck(CsClientEndpoint clientEndpoint) {
		super("Server Connected");
		this.clientEndpoint = clientEndpoint;
	}
	
	@Override
	protected Result check() throws Exception {
		if (clientEndpoint.isConnected()) {
			return Result.healthy("Site controller is connected to server");
		}
		return Result.unhealthy("Site controller is disconnected from server");
	}
}
