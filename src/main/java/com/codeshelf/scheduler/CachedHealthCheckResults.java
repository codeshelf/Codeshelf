package com.codeshelf.scheduler;

import java.util.HashMap;
import java.util.Iterator;
import java.util.UUID;

import com.codahale.metrics.health.HealthCheck.Result;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.persistence.TenantPersistenceService;

public class CachedHealthCheckResults {
	private static CachedHealthCheckResults instance = null;
	private HashMap<String, HashMap<UUID, FacilityResult>> jobResults = new HashMap<>();
	
	private CachedHealthCheckResults(){}
	
	private static CachedHealthCheckResults getInstance(){
		if (instance == null) {
			instance = new CachedHealthCheckResults();
		}
		return instance;
	}
	
	public static synchronized Result getJobResult(String jobName){
		HashMap<UUID, FacilityResult> facilityResults = getInstance().jobResults.get(jobName);
		if (facilityResults == null) {
			return null;
		}
		Iterator<UUID> facilityIterator = facilityResults.keySet().iterator();
		boolean success = true, nonProductionErrorsEncountered = false, sameMessage = true, singleFacility = facilityResults.size() == 1;
		String repeatingMessage = null;
		StringBuilder combinedMessage = new StringBuilder();
	    while (facilityIterator.hasNext()) {
	    	UUID facilityPersId = facilityIterator.next();
	    	FacilityResult facilityResult = facilityResults.get(facilityPersId);
	    	if (!facilityResult.success) {
	    		if (facilityResult.isProduction){
	    			success = false;
	    		} else {
	    			nonProductionErrorsEncountered = true;
	    		}
	    	}
	    	combinedMessage.append(String.format("Facility %s.%s - %s: %s. ", facilityResult.tenantId, facilityResult.facilityId, facilityResult.success ? "PASS" : "FAIL", facilityResult.message));
	    	if (repeatingMessage == null) {
	    		repeatingMessage = facilityResult.message;
	    	} else if (!repeatingMessage.equals(facilityResult.message)){
	    		sameMessage = false;
	    	}
	    }
	    String combinedMessageStr = combinedMessage.toString();
	    if (!combinedMessageStr.isEmpty()) {
	    	combinedMessageStr = combinedMessageStr.substring(0, combinedMessageStr.length()-1);
	    }
		String errorMessage = (singleFacility || !sameMessage) ? combinedMessageStr : "Multiple Facilities: " + repeatingMessage;
	    if (success) {
	    	if (nonProductionErrorsEncountered) {
	    		return Result.healthy("Non-production error(s) found: " + errorMessage);
	    	} else {
	    		return Result.healthy("Pass");
	    	}
	    } else {
	    	return Result.unhealthy(combinedMessageStr);
	    }
	}
	
	public static synchronized void saveJobResult(String jobName, Facility facility, boolean success, String message){
		HashMap<UUID, FacilityResult> facilityResults = getInstance().jobResults.get(jobName);
		if (facilityResults == null) {
			facilityResults = new HashMap<>();
			getInstance().jobResults.put(jobName, facilityResults);
		}
		String tenantId = TenantPersistenceService.getInstance().getCurrentTenantIdentifier();
		facilityResults.put(facility.getPersistentId(), new FacilityResult(tenantId, facility.getDomainId(), success, facility.isProduction(), message));
	}
	
	private static class FacilityResult{
		private boolean success;
		private boolean isProduction;
		private String tenantId;
		private String facilityId;
		private String message;
		
		public FacilityResult(String tenantId, String facilityId, boolean success, boolean isProduction, String message) {
			this.tenantId = tenantId;
			this.facilityId = facilityId;
			this.success = success;
			this.isProduction = isProduction;
			this.message = message;
		}
	}
}
