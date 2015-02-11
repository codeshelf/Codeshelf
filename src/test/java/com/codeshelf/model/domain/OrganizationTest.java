package com.codeshelf.model.domain;

import org.junit.Test;

public class OrganizationTest extends DomainTestABC {

	
	@Test
	public void testFacilityCreation() {
		this.getTenantPersistenceService().beginTransaction();

		Facility.createFacility(getDefaultTenant(),"FACILITY NAME", "INDESCRIPTIONS", Point.getZeroPoint());

		this.getTenantPersistenceService().commitTransaction();
	}
}