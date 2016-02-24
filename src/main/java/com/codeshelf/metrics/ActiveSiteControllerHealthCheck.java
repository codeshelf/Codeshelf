package com.codeshelf.metrics;

import java.util.Collection;
import java.util.List;

import com.codeshelf.manager.User;
import com.codeshelf.manager.service.TenantManagerService;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.ws.server.WebSocketConnection;
import com.codeshelf.ws.server.WebSocketManagerService;
import com.google.inject.Inject;
import com.codeshelf.ws.server.WebSocketConnection.State;

public class ActiveSiteControllerHealthCheck extends HealthCheckRefreshJob{
	private static final int	MAX_BAD_VERSION_LOGIN_TRIES	= 40; // about 20 minutes of trying to log in
	
	WebSocketManagerService sessionManager;
	
	@Inject
	public ActiveSiteControllerHealthCheck(WebSocketManagerService sessionManager) {
		this.sessionManager = sessionManager;
	}
	
    @Override
    public void check(Facility facility) throws Exception {
    	Collection<WebSocketConnection> sessions = sessionManager.getWebSocketConnections();
    	int c=0;
    	for (WebSocketConnection session : sessions) {
    		if (session.getLastState()==State.ACTIVE && session.isAuthenticated() && session.getCurrentUserContext().isSiteController()) {
    			c++;
    		}
    	}
    	if (c==0) {
    		saveResults(facility, false, "No site controllers connected");
    		return;
    	} // else check for failing upgrades

    	int badVersions = 0;
    	List<User> sitecons = TenantManagerService.getInstance().getSiteControllerUsers(true);
    	for(User sitecon : sitecons) {
    		if(sitecon.getBadVersionLoginTries() > MAX_BAD_VERSION_LOGIN_TRIES) {
    			badVersions++;
    		}
    	}
    	if(badVersions > 0) {
    		String msgHelper = badVersions == 1 ? "controller has" : "controllers have";
			saveResults(facility, false, badVersions + " site " + msgHelper + "  not been upgraded");
    		return;
    	} // else
    	saveResults(facility, true, badVersions+" site controllers have not been upgraded");
    }
}
