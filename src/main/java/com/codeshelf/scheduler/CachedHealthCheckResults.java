package com.codeshelf.scheduler;

import java.util.HashMap;
import java.util.Iterator;

import com.codahale.metrics.health.HealthCheck.Result;
import com.codeshelf.model.domain.Facility;

public class CachedHealthCheckResults {
	private static CachedHealthCheckResults instance = null;
	private HashMap<String, HashMap<String, FacilityResult>> jobResults = new HashMap<>();
	
	private CachedHealthCheckResults(){}
	
	private static CachedHealthCheckResults getInstance(){
		if (instance == null) {
			instance = new CachedHealthCheckResults();
		}
		return instance;
	}
	
	public static synchronized Result getJobResult(String jobName){
		HashMap<String, FacilityResult> facilityResults = getInstance().jobResults.get(jobName);
		if (facilityResults == null) {
			return null;
		}
		Iterator<String> facilityIterator = facilityResults.keySet().iterator();
		boolean success = true;
		StringBuilder combinedMessage = new StringBuilder();
	    while (facilityIterator.hasNext()) {
	    	String facilityId = facilityIterator.next();
	    	FacilityResult facilityResult = facilityResults.get(facilityId);
	    	if (!facilityResult.success) {
	    		success = false;
	    	}
	    	combinedMessage.append(String.format("Facility %s - %s: %s. ", facilityId, facilityResult.success ? "PASS" : "FAIL", facilityResult.message));
	    }
	    String combinedMessageStr = combinedMessage.toString();
	    if (!combinedMessageStr.isEmpty()) {
	    	combinedMessageStr = combinedMessageStr.substring(0, combinedMessageStr.length()-1);
	    }
	    if (success) {
	    	return Result.healthy(combinedMessageStr);
	    } else {
	    	return Result.unhealthy(combinedMessageStr);
	    }
	}
	
	public static synchronized void saveJobResult(String jobName, Facility facility, boolean success, String message){
		HashMap<String, FacilityResult> facilityResults = getInstance().jobResults.get(jobName);
		if (facilityResults == null) {
			facilityResults = new HashMap<>();
			getInstance().jobResults.put(jobName, facilityResults);
		}
		facilityResults.put(facility.getDomainId(), new FacilityResult(success, message));
	}
	
	private static class FacilityResult{
		private boolean success;
		private String message;
		
		public FacilityResult(boolean success, String message) {
			this.success = success;
			this.message = message;
		}
	}
}
