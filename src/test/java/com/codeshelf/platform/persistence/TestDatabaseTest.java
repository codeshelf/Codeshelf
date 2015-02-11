package com.codeshelf.platform.persistence;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.model.domain.DomainTestABC;
import com.codeshelf.model.domain.Facility;

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
		
		assertFalse(this.getTenantPersistenceService().hasAnyActiveTransaction());
		this.getTenantPersistenceService().beginTenantTransaction();

		if(TestDatabaseTest.sequence_static == 1) {
			// create an org to look for in future steps
			Facility fac = this.createFacility();
			fac.setDomainId("org_create");
			Facility.DAO.store(fac);
			
			// new transaction
			this.getTenantPersistenceService().commitTenantTransaction();
			this.getTenantPersistenceService().beginTenantTransaction();

			// org just created should still exist
			assertNotNull(Facility.DAO.findByDomainId(null,"org_create"));			
			
			// create another org
			fac= this.createFacility();
			fac.setDomainId("org_rollback");
			Facility.DAO.store(fac);			
			
			// rollback and new transaction
			this.getTenantPersistenceService().rollbackTenantTransaction();
			assertFalse(this.getTenantPersistenceService().hasAnyActiveTransaction());
			this.getTenantPersistenceService().beginTenantTransaction();

			// last step rolled back new org, so it should not exist
			assertNull(Facility.DAO.findByDomainId(null,"org_rollback"));

		} else if(TestDatabaseTest.sequence_static == 2) {
			// confirm test object was recreated by JUnit (not reused)
			assertEquals(this.sequence_member,1);
			
			// org created in prior step should not exist
			assertNull(Facility.DAO.findByDomainId(null,"org_create"));			
		}		
		
		this.sequence_member++;
		TestDatabaseTest.sequence_static++;
		
		// NOT closing transaction at end of test, framework should automatically stop persistence service anyhow.		
	}
	
	@Test
	public void testOutOfTransactionUpdate() {
		beginTenantTransaction();
		Facility org = new Facility();
		org.setDomainId("an org");
		org.setDescription("foo");
		Facility.DAO.store(org);					
		commitTenantTransaction();
		
		beginTenantTransaction();
		org.setDescription("bar");
		Facility.DAO.store(org);					
		commitTenantTransaction();		

		beginTenantTransaction();
		Facility org2 = Facility.DAO.findByDomainId(null,"an org");
		assertNotNull(org2);
		assertEquals("bar", org2.getDescription());
		commitTenantTransaction();		
	}
}
