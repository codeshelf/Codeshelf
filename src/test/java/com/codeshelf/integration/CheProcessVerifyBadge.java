package com.codeshelf.integration;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.behavior.PropertyBehavior;
import com.codeshelf.device.CheStateEnum;
import com.codeshelf.model.FacilityPropertyType;
import com.codeshelf.model.domain.Che;
import com.codeshelf.model.domain.Che.ProcessMode;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.Worker;
import com.codeshelf.sim.worker.PickSimulator;
import com.codeshelf.testframework.ServerTest;

public class CheProcessVerifyBadge extends ServerTest {
	private static final int	WAIT_TIME	= 4000;
	private static final Logger	LOGGER		= LoggerFactory.getLogger(CheProcessVerifyBadge.class);

	private final static String WORKER1 = "Worker1", WORKER2 = "Worker2";
	
	private void init(boolean badgeAuth) {
		this.getTenantPersistenceService().beginTransaction();
		//Set BADGEAUTH
		PropertyBehavior.setProperty(getFacility(), FacilityPropertyType.BADGEAUTH, badgeAuth ? "true" : "false");
		//Set CHE2 to LINE_SCAN mode
		Che che2 = Che.staticGetDao().findByDomainId(getNetwork(), cheId2);
		che2.setProcessMode(ProcessMode.LINE_SCAN);
		this.getTenantPersistenceService().commitTransaction();
	}
	
	@Test
	public void testNoAuthenticationNewWorker(){
		init(false);
		
		//Login into 2 CHEs with new badges
		this.startSiteController();
		
		PickSimulator picker1 = waitAndGetPickerForProcessType(this, cheGuid1, "CHE_SETUPORDERS");		
		picker1.loginAndSetup(WORKER1); // This goes through the setup_summary state
		
		PickSimulator picker2 = waitAndGetPickerForProcessType(this, cheGuid2, "CHE_LINESCAN");		
		picker2.scanSomething("U%" + WORKER2);
		picker2.waitForCheState(CheStateEnum.READY, WAIT_TIME); // line scan has different process

		//Verify created workers
		this.getTenantPersistenceService().beginTransaction();

		Worker createdWorker1 = Worker.findWorker(getFacility(), WORKER1);
		verifyWorker(createdWorker1, getFacility(), WORKER1, WORKER1);
		
		Worker createdWorker2 = Worker.findWorker(getFacility(), WORKER2);
		verifyWorker(createdWorker2, getFacility(), WORKER2, WORKER2);

		this.getTenantPersistenceService().commitTransaction();
	}

	/**
	 * For DEV-1426.  Show no badge authorization log in for non- U%, and show that ANSI-1252 prefix does not give an automatic loging
	 */
	@Test
	public void testNoAuthDongleEncoding(){
		String worker2 = "JOE123";
		String ansi1252prefix = "\\000023";
		String worker3 = "MARY51";
		init(false);
		
		this.startSiteController();
		
		PickSimulator picker1 = waitAndGetPickerForProcessType(this, cheGuid1, "CHE_SETUPORDERS");		
		LOGGER.info("1: Log in a worker as we normally do ");
		picker1.loginAndSetup(WORKER1);
		
		// CHE 2 is set to line scan process
		PickSimulator picker2 = waitAndGetPickerForProcessType(this, cheGuid2, "CHE_LINESCAN");		
		LOGGER.info("2: Log in a worker using a customer badge that does not have a U%");
		picker2.scanSomething(worker2);
		picker2.waitForCheState(CheStateEnum.READY, WAIT_TIME); 

		PickSimulator picker3 = waitAndGetPickerForProcessType(this, cheGuid3, "CHE_SETUPORDERS");		
		LOGGER.info("3: If the worker just linked CodeCorps scanner to a dongle, this prefix comes through.");
		picker3.scanSomething(ansi1252prefix);
		picker3.waitInSameState(CheStateEnum.IDLE, WAIT_TIME);
				
		LOGGER.info("3b: cannot happen. Introduce a null string scanned");
		picker3.scanSomething(null);
		picker3.waitInSameState(CheStateEnum.IDLE, WAIT_TIME);

		picker3.scanSomething(worker3);
		picker3.waitForCheState(CheStateEnum.SETUP_SUMMARY, WAIT_TIME); // line scan has different process

		//Verify created workers
		this.getTenantPersistenceService().beginTransaction();

		Worker createdWorker1 = Worker.findWorker(getFacility(), WORKER1);
		verifyWorker(createdWorker1, getFacility(), WORKER1, WORKER1);
		
		Worker createdWorker2 = Worker.findWorker(getFacility(), worker2);
		verifyWorker(createdWorker2, getFacility(), worker2, worker2);

		LOGGER.info("3c: Showing that we ate the prefix");
		Worker createdWorker3a = Worker.findWorker(getFacility(), ansi1252prefix);
		Assert.assertNull(createdWorker3a);
	
		Worker createdWorker3b = Worker.findWorker(getFacility(), worker3);
		verifyWorker(createdWorker3b, getFacility(), worker3, worker3);

		this.getTenantPersistenceService().commitTransaction();
	}

	@Test
	public void testAuthenticationNewWorker(){
		init(true);

		//Fail to login into 2 CHEs with new badges
		this.startSiteController();
		
		PickSimulator picker1 = waitAndGetPickerForProcessType(this, cheGuid1, "CHE_SETUPORDERS");		
		picker1.scanSomething("U%" + WORKER1);
		picker1.waitForCheState(CheStateEnum.IDLE, WAIT_TIME);
		Assert.assertEquals("UNKNOWN BADGE", picker1.getLastCheDisplayString(1));
		
		PickSimulator picker2 = waitAndGetPickerForProcessType(this, cheGuid2, "CHE_LINESCAN");		
		picker2.scanSomething("U%" + WORKER2);
		picker2.waitForCheState(CheStateEnum.IDLE, WAIT_TIME);
		Assert.assertEquals("UNKNOWN BADGE", picker2.getLastCheDisplayString(1));
	}

	@Test
	public void testAuthenticationInactiveWorker(){
		init(true);
		
		//Add inactive workers to DB
		this.getTenantPersistenceService().beginTransaction();
		Worker worker1 = createWorker(getFacility(), "Last Name 1", WORKER1);
		worker1.setActive(false);
		Worker.staticGetDao().store(worker1);
		Worker worker2 = createWorker(getFacility(), "Last Name 2", WORKER2);
		worker2.setActive(false);
		Worker.staticGetDao().store(worker2);
		this.getTenantPersistenceService().commitTransaction();
		
		//Fail to login into 2 CHEs with inactive
		this.startSiteController();
		
		PickSimulator picker1 = waitAndGetPickerForProcessType(this, cheGuid1, "CHE_SETUPORDERS");		
		picker1.scanSomething("U%" + WORKER1);
		picker1.waitForCheState(CheStateEnum.IDLE, WAIT_TIME);
		Assert.assertEquals("UNKNOWN BADGE", picker1.getLastCheDisplayString(1));
		
		PickSimulator picker2 = waitAndGetPickerForProcessType(this, cheGuid2, "CHE_LINESCAN");		
		picker2.scanSomething("U%" + WORKER2);
		picker2.waitForCheState(CheStateEnum.IDLE, WAIT_TIME);
		Assert.assertEquals("UNKNOWN BADGE", picker2.getLastCheDisplayString(1));

	}

	@Test
	public void testNoAuthenticationExistingWorker(){
		init(false);
		commonExistingWorker();
	}
	
	@Test
	public void testAuthenticationExistingWorker(){
		init(true);
		commonExistingWorker();
	}
	
	private void commonExistingWorker(){
		//Add workers to DB
		this.getTenantPersistenceService().beginTransaction();
		Worker worker1 = createWorker(getFacility(), "Last Name 1", WORKER1);
		Worker.staticGetDao().store(worker1);
		Worker worker2 = createWorker(getFacility(), "Last Name 2", WORKER2);
		Worker.staticGetDao().store(worker2);
		this.getTenantPersistenceService().commitTransaction();
		
		//Log into CHEs with existing badges
		this.startSiteController();
		
		PickSimulator picker1 = waitAndGetPickerForProcessType(this, cheGuid1, "CHE_SETUPORDERS");		
		picker1.loginAndSetup(WORKER1);
		
		PickSimulator picker2 = waitAndGetPickerForProcessType(this, cheGuid2, "CHE_LINESCAN");		
		picker2.scanSomething("U%" + WORKER2);
		picker2.waitForCheState(CheStateEnum.READY, WAIT_TIME);
		
		//Verify that no new workers were created, and the old workers are unchanged
		this.getTenantPersistenceService().beginTransaction();
		Worker foundWorker1 = findWorker(getFacility(), WORKER1);
		verifyWorker(foundWorker1, worker1.getFacility(), worker1.getLastName(), worker1.getDomainId());
		Worker foundWorker2 = findWorker(getFacility(), WORKER2);
		verifyWorker(foundWorker2, worker2.getFacility(), worker2.getLastName(), worker2.getDomainId());
		this.getTenantPersistenceService().commitTransaction();
	}
	
	private void verifyWorker(Worker worker, Facility facility, String lastName, String badgeId){
		Assert.assertEquals(facility, worker.getFacility());
		Assert.assertEquals(lastName, worker.getLastName());
		Assert.assertEquals(badgeId, worker.getDomainId());		
	}
	
	private Worker createWorker(Facility facility, String lastName, String badgeId){
		Worker worker = new Worker();
		worker.setParent(facility);
		worker.setLastName(lastName);
		worker.setDomainId(badgeId);
		worker.setActive(true);
		worker.setUpdated(new Timestamp(System.currentTimeMillis()));
		return worker;
	}
	
	private Worker findWorker(Facility facility, String badgeId) {
		List<Criterion> filterParams = new ArrayList<Criterion>();
		filterParams.add(Restrictions.eq("parent", facility));
		filterParams.add(Restrictions.eq("domainId", badgeId));
		filterParams.add(Restrictions.eq("active", true));
		List<Worker> workers = Worker.staticGetDao().findByFilter(filterParams);
		if (workers == null){
			return null;
		}
		if (workers.size() > 1) {
			Assert.fail("Found " + workers.size() + " workers with badge " + badgeId + " in facility " + facility.getDomainId());
			return null;
		}
		return workers.get(0);
	}
	
	@Test
	public void testNoAuthenticationInactiveWorker(){
		init(false);
		this.getTenantPersistenceService().beginTransaction();
		Worker worker1 = createWorker(getFacility(), "Last Name 1", WORKER1);
		worker1.setActive(false);
		Worker.staticGetDao().store(worker1);
		this.getTenantPersistenceService().commitTransaction();
		
		this.startSiteController();
		
		PickSimulator picker1 = waitAndGetPickerForProcessType(this, cheGuid1, "CHE_SETUPORDERS");		
		picker1.loginAndSetup(WORKER1);
		
		this.getTenantPersistenceService().beginTransaction();
		worker1 = Worker.findWorker(getFacility(), WORKER1);
		Assert.assertTrue(worker1.getActive());
		this.getTenantPersistenceService().commitTransaction();
	}
}
