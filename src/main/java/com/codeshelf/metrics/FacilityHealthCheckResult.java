package com.codeshelf.metrics;

import com.codeshelf.model.domain.Facility;

import lombok.Getter;

public class FacilityHealthCheckResult {
	
	@Getter
	private Facility facility;
	
	@Getter
	private boolean healthy;
	
	@Getter
	private String message;

	public FacilityHealthCheckResult(Facility facility, boolean healthy, String messageFormat, Object...messageArgs ) {
		this(facility, healthy, String.format(messageFormat, messageArgs));
	}

	public FacilityHealthCheckResult(Facility facility, boolean healthy, String message) {
		this.facility = facility;
		this.healthy = healthy;
		this.message = message;
	}
	
	/**
	 * Accounts for whether facility is production. 
	 */
	public boolean isPass() {
    	return (facility.isProduction()) ? isHealthy() : true; //only truly fail if production
	}
}