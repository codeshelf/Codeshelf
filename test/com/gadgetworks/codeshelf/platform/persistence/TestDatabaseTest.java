package com.gadgetworks.codeshelf.platform.persistence;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gadgetworks.codeshelf.integration.EndToEndIntegrationTest;
import com.gadgetworks.codeshelf.model.domain.DomainTestABC;
import com.gadgetworks.codeshelf.model.domain.Organization;

public class TestDatabaseTest extends DomainTestABC {
	// These tests should be run together, as the tests will not be meaningful if run one at a time.
	
	private static final Logger	LOGGER	= LoggerFactory.getLogger(TestDatabaseTest.class);

	static int sequence_static = 1; // perform tests in order
	int sequence_member = 1; // confirm behavior of JUnit as expected (new object is instantiated rather than reusing existing) 
	
	@Test
	public void TestDatabaseSequence_1() {
		testDatabaseSequence();
	}

	@Test
	public void TestDatabaseSequence_2() {
		testDatabaseSequence();
	}

	private void testDatabaseSequence() {
		
		LOGGER.info("Test Database sequence #"+TestDatabaseTest.sequence_static);
		
		assertFalse(this.getPersistenceService().hasActiveTransaction());
		this.getPersistenceService().beginTenantTransaction();

		if(TestDatabaseTest.sequence_static == 1) {
			// create an org to look for in future steps
			Organization org=new Organization();
			org.setDomainId("org_create");
			Organization.DAO.store(org);
			
			// new transaction
			this.getPersistenceService().endTenantTransaction();
			this.getPersistenceService().beginTenantTransaction();

			// org just created should still exist
			assertNotNull(Organization.DAO.findByDomainId(null,"org_create"));			
			
			// create another org
			org=new Organization();
			org.setDomainId("org_rollback");
			Organization.DAO.store(org);			
			
			// rollback and new transaction
			this.getPersistenceService().rollbackTenantTransaction();
			assertFalse(this.getPersistenceService().hasActiveTransaction());
			this.getPersistenceService().beginTenantTransaction();

			// last step rolled back new org, so it should not exist
			assertNull(Organization.DAO.findByDomainId(null,"org_rollback"));

		} else if(TestDatabaseTest.sequence_static == 2) {
			// confirm test object was recreated by JUnit (not reused)
			assertEquals(this.sequence_member,1);
			
			// org created in prior step should not exist
			assertNull(Organization.DAO.findByDomainId(null,"org_create"));			
		}		
		
		this.sequence_member++;
		TestDatabaseTest.sequence_static++;
		
		// NOT closing transaction at end of test, framework should automatically stop persistence service anyhow.		
	}
}
