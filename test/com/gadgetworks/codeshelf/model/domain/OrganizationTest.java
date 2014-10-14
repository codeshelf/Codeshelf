package com.gadgetworks.codeshelf.model.domain;

import org.junit.Test;

public class OrganizationTest extends DomainTestABC {

	
	@Test
	public void createFacility() {
		this.getPersistenceService().beginTenantTransaction();

		Organization organization = new Organization("ORG");
		mOrganizationDao.store(organization);
		organization.createFacility("FACILITY NAME", "INDESCRIPTIONS", 0.0, 1.0);

		this.getPersistenceService().endTenantTransaction();
	}
}
