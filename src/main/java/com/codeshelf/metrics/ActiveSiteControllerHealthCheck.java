package com.codeshelf.metrics;

import java.util.Set;
import java.util.stream.Collectors;

import com.codeshelf.manager.User;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.security.UserContext;
import com.codeshelf.ws.server.WebSocketManagerService;
import com.google.inject.Inject;

public class ActiveSiteControllerHealthCheck extends HealthCheckRefreshJob{
	private static final int	MAX_BAD_VERSION_LOGIN_TRIES	= 40; // about 20 minutes of trying to log in
	
	private final WebSocketManagerService sessionManager;
	
	@Inject
	public ActiveSiteControllerHealthCheck(WebSocketManagerService sessionManager) {
		this.sessionManager = sessionManager;
	}

    @Override
    public void check(Facility facility) throws Exception {
    	FacilityHealthCheckResult checkResult = checkResult(facility);
    	saveResults(checkResult);
    }	

    public FacilityHealthCheckResult checkResult(Facility facility) throws Exception {
    	
    	Set<String> connectedUsernames = sessionManager.getConnectedUsers()
    			.map(UserContext::getUsername)
    			.collect(Collectors.toSet());

    	
    	Set<User> facilitySCUsers = facility.getSiteControllerUsers();
    	
    	int numExpectedSCs = facilitySCUsers.size();
    	long numConnectedSCs = facilitySCUsers.stream()
    			.filter((fc) -> connectedUsernames.contains(fc.getUsername())).count();

    	if (numConnectedSCs != numExpectedSCs) {
    		return new FacilityHealthCheckResult(facility, false, "Only %d of %d %s connected", numConnectedSCs, numExpectedSCs, pluralizer(numExpectedSCs));
    	} // else check for failing upgrades

    	int outOfDate = 0;
    	for(User sitecon : facilitySCUsers) {
    		if(sitecon.getBadVersionLoginTries() > MAX_BAD_VERSION_LOGIN_TRIES) {
    			outOfDate++;
    		}
    	}
    	
    	boolean upToDate = (outOfDate <= 0) ? true : false;
    	return new FacilityHealthCheckResult(facility, upToDate, "%d of %d %s are connected and up to date", numExpectedSCs - outOfDate, numExpectedSCs, pluralizer(numExpectedSCs));
    }
    
    private String pluralizer(int value) {
    	return value == 1 ? "site controller has" : "site controllers have";
    }
}
