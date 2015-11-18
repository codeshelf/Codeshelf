package com.codeshelf.metrics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.model.DomainObjectManager;
import com.codeshelf.model.domain.Facility;

public class PicksActivityHealthCheck extends HealthCheckRefreshJob {
	private static final Logger	LOGGER				= LoggerFactory.getLogger(PicksActivityHealthCheck.class);

	int							picksLastOneHour	= 0;
	int							picksLast24Hours	= 0;

	@Override
	public void check(Facility facility) throws Exception {
		picksLastOneHour = 0;
		picksLast24Hours = 0;
		checkFacilityActivity(facility);
		String message = String.format("WorkInstructions completed last 1 hr:  %d; last 24 hrs: %d.", picksLastOneHour, picksLast24Hours);
		saveResults(facility, true, message);
	}

	private void checkFacilityActivity(Facility inFacility) {
		try {
			DomainObjectManager doMananager = new DomainObjectManager(inFacility);
			DomainObjectManager.FacilityPickParameters pickParms = doMananager.getFacilityPickParameters();
			picksLastOneHour += pickParms.getPicksLastOneHour();
			picksLast24Hours += pickParms.getPicksLastTwentyFourHours();
		} catch (Exception e) {
			LOGGER.error("checkFacilityActivity", e);
		}
	}
}
