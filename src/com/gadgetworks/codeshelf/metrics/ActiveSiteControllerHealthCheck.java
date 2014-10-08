package com.gadgetworks.codeshelf.metrics;

import java.util.Collection;

import com.gadgetworks.codeshelf.ws.jetty.server.CsSession;
import com.gadgetworks.codeshelf.ws.jetty.server.CsSession.State;
import com.gadgetworks.codeshelf.ws.jetty.server.SessionManager;
import com.gadgetworks.codeshelf.ws.jetty.server.SessionType;

public class ActiveSiteControllerHealthCheck extends CodeshelfHealthCheck {

	public ActiveSiteControllerHealthCheck() {
		super("Active Site Controllers");
	}
	
    @Override
    protected Result check() throws Exception {
    	Collection<CsSession> sessions = SessionManager.getInstance().getSessions();
    	int c=0;
    	for (CsSession session : sessions) {
    		if (session.getLastState()==State.ACTIVE && session.getType()==SessionType.SiteController) {
    			c++;
    		}
    	}
    	if (c>0) {
            return Result.healthy(c+" active site controller(s) connected");
    	}
    	return Result.unhealthy("No site controller connected");
    }
}