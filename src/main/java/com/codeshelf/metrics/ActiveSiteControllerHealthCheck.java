package com.codeshelf.metrics;

import java.util.Collection;
import java.util.List;

import com.codeshelf.manager.User;
import com.codeshelf.manager.service.TenantManagerService;
import com.codeshelf.ws.server.WebSocketConnection;
import com.codeshelf.ws.server.WebSocketConnection.State;
import com.codeshelf.ws.server.WebSocketManagerService;

/**
 * health check fails if there are NO site controllers connected
 * OR (new) if there is any site controller, connected or not, that seems to be failing upgrade attempts. 
 * (i.e. it needs upgrade + has multiple login attempts failing due to bad version) 
 * 
 * @author ivan
 *
 */
public class ActiveSiteControllerHealthCheck extends CodeshelfHealthCheck {

	private static final int	MAX_BAD_VERSION_LOGIN_TRIES	= 40; // about 20 minutes of trying to log in
	
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
    		if (session.getLastState()==State.ACTIVE && session.isAuthenticated() && session.getCurrentUserContext().isSiteController()) {
    			c++;
    		}
    	}
    	if (c==0) {
        	return Result.unhealthy("No site controllers connected");
    	} // else check for failing upgrades

    	int badVersions = 0;
    	List<User> sitecons = TenantManagerService.getInstance().getSiteControllerUsers(true);
    	for(User sitecon : sitecons) {
    		if(sitecon.getBadVersionLoginTries() > MAX_BAD_VERSION_LOGIN_TRIES) {
    			badVersions++;
    		}
    	}
    	if(badVersions > 0) {
    		return Result.unhealthy(badVersions+" site controllers have not been upgraded");
    	} // else
    	
    	return Result.healthy(c+" site controllers connected");
    	
    }
}