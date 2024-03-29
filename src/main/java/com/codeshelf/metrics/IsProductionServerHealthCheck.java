package com.codeshelf.metrics;

import com.codeshelf.behavior.PropertyBehavior;
import com.codeshelf.model.FacilityPropertyType;
import com.codeshelf.model.domain.Facility;

public class IsProductionServerHealthCheck extends HealthCheckRefreshJob {
	@Override
	protected void check(Facility facility) throws Exception {
		boolean production = PropertyBehavior.getPropertyAsBoolean(facility, FacilityPropertyType.PRODUCTION);
		if (production) {
			// No need to spam the logs
			saveResults(facility, true, CodeshelfHealthCheck.OK);
		} else {
			// No need to spam the logs
			saveResults(facility, false, CodeshelfHealthCheck.OK);
		}
	}
}
