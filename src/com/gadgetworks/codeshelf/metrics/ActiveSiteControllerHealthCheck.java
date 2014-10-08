package com.gadgetworks.codeshelf.metrics;

import java.util.Collection;

import com.gadgetworks.codeshelf.ws.jetty.server.UserSession;
import com.gadgetworks.codeshelf.ws.jetty.server.UserSession.State;
import com.gadgetworks.codeshelf.ws.jetty.server.SessionManager;

public class ActiveSiteControllerHealthCheck extends CodeshelfHealthCheck {

	public ActiveSiteControllerHealthCheck() {
		super("Active Site Controllers");
	}
	
    @Override
    protected Result check() throws Exception {
    	Collection<UserSession> sessions = SessionManager.getInstance().getSessions();
    	int c=0;
    	for (UserSession session : sessions) {
    		if (session.getLastState()==State.ACTIVE && session.isSiteController()) {
    			c++;
    		}
    	}
    	if (c>0) {
            return Result.healthy(c+" active site controller(s) connected");
    	}
    	return Result.unhealthy("No site controller connected");
    }
}