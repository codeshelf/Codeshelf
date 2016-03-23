package com.codeshelf.metrics;

import java.util.Set;
import java.util.stream.Collectors;

import com.codeshelf.manager.User;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.SiteController;
import com.codeshelf.model.domain.SiteController.SiteControllerRole;
import com.codeshelf.security.UserContext;
import com.codeshelf.ws.server.WebSocketManagerService;

public class SiteControllerHealthCheck {
	private static final int	MAX_BAD_VERSION_LOGIN_TRIES	= 40; // about 20 minutes of trying to log in 

    public static FacilityHealthCheckResult checkResult(Facility facility, SiteControllerRole desiredRole) throws Exception {
    	WebSocketManagerService sessionManager = WebSocketManagerService.getInstance();
    	Set<String> connectedUsernames = sessionManager.getConnectedUsers()
    			.map(UserContext::getUsername)
    			.collect(Collectors.toSet());

    	int total = 0, connected = 0;
    	StringBuilder error = new StringBuilder();
    	Set<SiteController> siteControllers = facility.getSiteControllers();
    	for (SiteController siteController : siteControllers) {
    		if (siteController.getRole() != desiredRole){
    			continue;
    		}
    		total++;
    		User scUser = siteController.getUser();
    		if (scUser == null) {
    			error.append(String.format("user %s does not exist, ", siteController.getDomainId()));
    			continue;
    		}
			if (!connectedUsernames.contains(scUser.getUsername())){
				error.append(String.format("%s is not connected, ", siteController.getDomainId()));
				continue;
			}
			if (scUser.getBadVersionLoginTries() > MAX_BAD_VERSION_LOGIN_TRIES) {
				error.append(String.format("%s had too many consecutive bad logins, ", siteController.getDomainId()));
				continue;
			}
			connected++;
    	}
    	
    	if (connected < total){
    		//Trim off the last ", "
    		String errorStr = error.length() > 2 ? error.substring(0, error.length() - 2) : error.toString();
    		return new FacilityHealthCheckResult(facility, false, errorStr);
    	} else {
    		return new FacilityHealthCheckResult(facility, true, "Site Controllers OK");
    	}
    	
     	/*
    	Set<User> facilitySCUsers = facility.getSiteControllerUsers();
    	long numConnectedSCs = facilitySCUsers.stream().filter((fc) -> connectedUsernames.contains(fc.getUsername())).count();
    	*/
    }
    
	public static class ActiveSiteControllerHealthCheck extends HealthCheckRefreshJob{
		@Override
		protected void check(Facility facility) throws Exception {
	    	FacilityHealthCheckResult checkResult = checkResult(facility, SiteControllerRole.NETWORK_PRIMARY);
	    	saveResults(checkResult);			
		}	
	}
	
	public static class StandbySiteControllerHealthCheck extends HealthCheckRefreshJob{
		@Override
		protected void check(Facility facility) throws Exception {
	    	FacilityHealthCheckResult checkResult = checkResult(facility, SiteControllerRole.STANDBY);
	    	saveResults(checkResult);			
		}
	}
}
