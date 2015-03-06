package com.codeshelf.model.domain;

import org.junit.Test;

import com.codeshelf.testframework.MockDaoTest;

public class OrganizationTest extends MockDaoTest {

	
	@Test
	public void testFacilityCreation() {
		this.getTenantPersistenceService().beginTransaction();

		Facility.createFacility(getDefaultTenant(),"FACILITY NAME", "INDESCRIPTIONS", Point.getZeroPoint());

		this.getTenantPersistenceService().commitTransaction();
	}
}
