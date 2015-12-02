package com.codeshelf.integration;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;
import org.junit.Assert;
import org.junit.Test;

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
		verifyWorker(foundWorker1, worker1.getFacility(), worker1.getLastName(), worker1.getBadgeId());
		Worker foundWorker2 = findWorker(getFacility(), WORKER2);
		verifyWorker(foundWorker2, worker2.getFacility(), worker2.getLastName(), worker2.getBadgeId());
		this.getTenantPersistenceService().commitTransaction();
	}
	
	private void verifyWorker(Worker worker, Facility facility, String lastName, String badgeId){
		Assert.assertEquals(facility, worker.getFacility());
		Assert.assertEquals(lastName, worker.getLastName());
		Assert.assertEquals(badgeId, worker.getBadgeId());		
	}
	
	private Worker createWorker(Facility facility, String lastName, String badgeId){
		Worker worker = new Worker();
		worker.setParent(facility);
		worker.setLastName(lastName);
		worker.setBadgeId(badgeId);
		worker.setActive(true);
		worker.setUpdated(new Timestamp(System.currentTimeMillis()));
		worker.generateDomainId();
		return worker;
	}
	
	private Worker findWorker(Facility facility, String badgeId) {
		List<Criterion> filterParams = new ArrayList<Criterion>();
		filterParams.add(Restrictions.eq("facility", facility));
		filterParams.add(Restrictions.eq("badgeId", badgeId));
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
}
