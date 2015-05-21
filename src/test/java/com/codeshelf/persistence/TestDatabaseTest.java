package com.codeshelf.persistence;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.sql.Timestamp;
import java.util.List;

import org.hibernate.Query;
import org.hibernate.Session;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.model.domain.Container;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.testframework.HibernateTest;

public class TestDatabaseTest extends HibernateTest {
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

	//Passes if the following line is commented out 
	//	configuration.registerTypeOverride(new UtcTimestampType());
	//@Test
	public void testDateQuery() {
		this.getTenantPersistenceService().beginTransaction();
		Facility facility = createFacility();
		Container container = this.createContainer("id", facility);
		Container.staticGetDao().store(container);
		Timestamp updatedTime = container.getUpdated();
		this.getTenantPersistenceService().commitTransaction();
		
		this.getTenantPersistenceService().beginTransaction();
		
		Session session = this.getTenantPersistenceService().getSession();
		Query  query = session.createQuery("from Container where updated <= :endDateTime");
		query.setTimestamp("endDateTime", updatedTime);
		List<Container> containers = query.list();
		Assert.assertEquals(1, containers.size());
		this.getTenantPersistenceService().commitTransaction();
		
	}
	
	
	private void testDatabaseSequence() {
		
		LOGGER.info("Test Database sequence #"+TestDatabaseTest.sequence_static);
		
		assertFalse(this.getTenantPersistenceService().hasAnyActiveTransactions());
		this.getTenantPersistenceService().beginTransaction();

		if(TestDatabaseTest.sequence_static == 1) {
			// create an org to look for in future steps
			Facility fac = this.createFacility();
			fac.setDomainId("org_create");
			Facility.staticGetDao().store(fac);
			
			// new transaction
			this.getTenantPersistenceService().commitTransaction();
			this.getTenantPersistenceService().beginTransaction();

			// org just created should still exist
			assertNotNull(Facility.staticGetDao().findByDomainId(null,"org_create"));			
			
			// create another org
			fac= this.createFacility();
			fac.setDomainId("org_rollback");
			Facility.staticGetDao().store(fac);			
			
			// rollback and new transaction
			this.getTenantPersistenceService().rollbackTransaction();
			assertFalse(this.getTenantPersistenceService().hasAnyActiveTransactions());
			this.getTenantPersistenceService().beginTransaction();

			// last step rolled back new org, so it should not exist
			assertNull(Facility.staticGetDao().findByDomainId(null,"org_rollback"));

		} else if(TestDatabaseTest.sequence_static == 2) {
			// confirm test object was recreated by JUnit (not reused)
			assertEquals(this.sequence_member,1);
			
			// org created in prior step should not exist
			assertNull(Facility.staticGetDao().findByDomainId(null,"org_create"));			
		}		
		
		this.sequence_member++;
		TestDatabaseTest.sequence_static++;
		
		// ensure tenant persistence service can find and roll back the transaction left open above
		this.tenantPersistenceService.rollbackAnyActiveTransactions();
	}
	
	@Test
	public void testOutOfTransactionUpdate() {
		beginTransaction();
		Facility org = new Facility();
		org.setDomainId("an org");
		org.setDescription("foo");
		Facility.staticGetDao().store(org);					
		commitTransaction();
		
		beginTransaction();
		org.setDescription("bar");
		Facility.staticGetDao().store(org);					
		commitTransaction();		

		beginTransaction();
		Facility org2 = Facility.staticGetDao().findByDomainId(null,"an org");
		assertNotNull(org2);
		assertEquals("bar", org2.getDescription());
		commitTransaction();		
	}
}
