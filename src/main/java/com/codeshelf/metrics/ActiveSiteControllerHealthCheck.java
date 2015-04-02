package com.codeshelf.metrics;

import java.util.Collection;

import com.codeshelf.ws.server.WebSocketConnection;
import com.codeshelf.ws.server.WebSocketConnection.State;
import com.codeshelf.ws.server.WebSocketManagerService;

public class ActiveSiteControllerHealthCheck extends CodeshelfHealthCheck {

	WebSocketManagerService sessionManager;
	
	public ActiveSiteControllerHealthCheck(WebSocketManagerService sessionManager) {
		super("Active Site Controllers");
		this.sessionManager = sessionManager;
	}
	
    @Override
    protected Result check() throws Exception {
    	Collection<WebSocketConnection> sessions = sessionManager.getWebSocketConnections();
    	int c=0;
    	for (WebSocketConnection session : sessions) {
    		if (session.getLastState()==State.ACTIVE && session.isSiteController()) {
    			c++;
    		}
    	}
    	if (c>1) {
            return Result.healthy(c+" active site controllers connected");
    	}
    	else if (c==1) {
            return Result.healthy(c+" active site controller connected");    		
    	}
    	return Result.unhealthy("No site controller connected");
    }
}