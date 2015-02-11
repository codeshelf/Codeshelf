package com.codeshelf.generators;

import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.Point;
import com.codeshelf.platform.multitenancy.Tenant;

public class FacilityGenerator {
	Tenant tenant;

	public FacilityGenerator(Tenant tenant) {
		this.tenant = tenant;
	}
	
	public Facility generateValid() {
		Facility facility = Facility.createFacility(tenant,"F1", "", Point.getZeroPoint());
		return facility;
	}
}
