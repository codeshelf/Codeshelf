package com.codeshelf.generators;

import com.codeshelf.manager.Tenant;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.Point;

public class FacilityGenerator {
	Tenant tenant;

	public FacilityGenerator(Tenant tenant) {
		this.tenant = tenant;
	}
	
	public Facility generateValid(Tenant tenant) {
		Facility facility = Facility.createFacility(tenant,"F1", "", Point.getZeroPoint());
		return facility;
	}
}
