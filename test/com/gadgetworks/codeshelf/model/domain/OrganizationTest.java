package com.gadgetworks.codeshelf.model.domain;

import org.junit.Test;

public class OrganizationTest extends DomainTestABC {

	
	@Test
	public void testFacilityCreation() {
		this.getTenantPersistenceService().beginTenantTransaction();

		Facility.createFacility(getDefaultTenant(),"FACILITY NAME", "INDESCRIPTIONS", Point.getZeroPoint());

		this.getTenantPersistenceService().commitTenantTransaction();
	}
}
