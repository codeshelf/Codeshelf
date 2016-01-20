package com.codeshelf.model.dao;

import java.sql.Timestamp;
import java.util.UUID;

import org.hibernate.Session;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.PutwallTest;
import com.codeshelf.model.domain.Worker;
import com.codeshelf.persistence.SideTransaction;
import com.codeshelf.persistence.TenantPersistenceService;
import com.codeshelf.testframework.HibernateTest;

public class MultipleTransactionsTest extends HibernateTest{
	private static final Logger	LOGGER	= LoggerFactory.getLogger(PutwallTest.class);


	@Test
	public void multipleTransactionsMainToSideTest() throws Exception{
		LOGGER.info("Open main transaction and creat a facility");
		beginTransaction();
		Facility facility = getFacility();
		final UUID facilityId = facility.getPersistentId();
		
		LOGGER.info("Create new session and try and fail to find that facility there");
		facility = new SideTransaction<Facility>() {
			@Override
			public Facility task(Session session) {
				return (Facility)session.get(Facility.class, facilityId);
			}
		}.run();
		Assert.assertNull(facility);
		
		LOGGER.info("Commit the main transaction");
		commitTransaction();
		
		LOGGER.info("Opean another new session, and successfully look up the facility");
		facility = new SideTransaction<Facility>() {
			@Override
			public Facility task(Session session) {
				return (Facility)session.get(Facility.class, facilityId);
			}
		}.run();
		Assert.assertNotNull(facility);
	}
	
	@Test
	public void multipleTransactionsSideToMainTest() throws Exception{
		LOGGER.info("Open main transaction and create a facility. Then, commit");
		beginTransaction();
		Facility facility = getFacility();
		commitTransaction();
		
		LOGGER.info("Open main transaction and a new transaction");
		beginTransaction();
		
		LOGGER.info("Create worker in the new transaction");
		final Worker worker = new Worker();
		worker.setParent(facility);
		worker.setLastName("WorkerLast");
		worker.setDomainId("WorkerId");
		worker.setActive(true);
		worker.setUpdated(new Timestamp(System.currentTimeMillis()));
		new SideTransaction<Void>() {
			@Override
			public Void task(Session session) {
				session.saveOrUpdate(worker);
				return null;
			}
		}.run();
		UUID workerId = worker.getPersistentId();
							
		LOGGER.info("Successfully find worker in the main transaction");
		Worker worker2 = Worker.staticGetDao().findByPersistentId(workerId);
		Assert.assertNotNull(worker2);
		commitTransaction();
	}

	@Test
	public void multipleTransactionsConsistencyTest(){
		TenantPersistenceService persistence = getTenantPersistenceService();
		LOGGER.info("Cycle main transaction and create a facility. Then, commit");
		persistence.beginTransaction();
		persistence.commitTransaction();
		
		LOGGER.info("Retrieve TenantPersistenceService's 'current' session");
		Session sessionMain = persistence.getSession();
		
		LOGGER.info("Create a new sesison");
		Session sessionNew = persistence.openNewSession();
		
		LOGGER.info("Ask for TenantPersistenceService's 'current' session again");
		Session sessionMainRepeat = persistence.getSession();
		
		Assert.assertTrue(sessionMain == sessionMainRepeat);
		Assert.assertTrue(sessionMain != sessionNew);
		
		LOGGER.info("Open main transaction and create a facility. Then, commit");
	}
}
