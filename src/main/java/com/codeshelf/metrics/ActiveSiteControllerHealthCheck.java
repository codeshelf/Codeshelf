package com.codeshelf.metrics;

import java.util.Collection;

import com.codeshelf.ws.jetty.server.SessionManagerService;
import com.codeshelf.ws.jetty.server.UserSession;
import com.codeshelf.ws.jetty.server.UserSession.State;

public class ActiveSiteControllerHealthCheck extends CodeshelfHealthCheck {

	SessionManagerService sessionManager;
	
	public ActiveSiteControllerHealthCheck(SessionManagerService sessionManager) {
		super("Active Site Controllers");
		this.sessionManager = sessionManager;
	}
	
    @Override
    protected Result check() throws Exception {
    	Collection<UserSession> sessions = sessionManager.getSessions();
    	int c=0;
    	for (UserSession session : sessions) {
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