package com.codeshelf.model.domain;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.codeshelf.api.ErrorResponse;
import com.codeshelf.api.resources.WorkersResource;
import com.codeshelf.api.resources.subresources.FacilityResource;
import com.codeshelf.api.resources.subresources.WorkerResource;
import com.codeshelf.service.NotificationService;
import com.codeshelf.service.OrderService;
import com.codeshelf.service.UiUpdateService;
import com.codeshelf.service.WorkService;
import com.codeshelf.testframework.HibernateTest;

public class WorkerTest extends HibernateTest {
	private FacilityResource facilityResource;
	private WorkersResource workersResource;

	@Before
	public void init(){
		WorkService workService = generateWorkService();
		OrderService orderService = new OrderService();
		NotificationService notificaitonService = new NotificationService();
		facilityResource = new FacilityResource(workService, 
			orderService, 
			notificaitonService, 
			webSocketManagerService, 
			new UiUpdateService(), 
			createAisleFileImporter(), 
			createLocationAliasImporter(), 
			createInventoryImporter(), 
			createOrderImporter());
		Facility facility = getFacility();
		facilityResource.setFacility(facility);
		workersResource = new WorkersResource();
	}
	
	@Test
	public void testCreateWorker(){
		this.getTenantPersistenceService().beginTransaction();
	
		//Create Worker with all fields set
		Worker workerFull = createWorkerObject(true, "FirstName", "LastName", "MI", "abc123", "GroupName", "hr123");
		Response response = facilityResource.createWorker(workerFull);
		Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatus());
		Worker savedWorkerFull = (Worker)response.getEntity();
		compareWorkers(workerFull, savedWorkerFull);
		
		//Create Worker with only required fields set
		Worker workerMinimal = createWorkerObject(null, "FirstName_Min", "LastName_Min", null, "abc456", null, null);
		response = facilityResource.createWorker(workerMinimal);
		Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatus());
		Worker savedWorkerMinimal = (Worker)response.getEntity();
		compareWorkers(workerMinimal, savedWorkerMinimal);
		
		this.getTenantPersistenceService().commitTransaction();		
	}
	
	@Test
	public void testCreateWorkerNoData(){
		this.getTenantPersistenceService().beginTransaction();
		
		Worker workerEmpty = new Worker();
		Response response = facilityResource.createWorker(workerEmpty);
		Assert.assertEquals(HttpServletResponse.SC_BAD_REQUEST, response.getStatus());
		ErrorResponse errorResponse = (ErrorResponse)response.getEntity();
		ArrayList<String> errors = errorResponse.getErrors();
		Assert.assertEquals("Missing body param 'lastName'", errors.get(0));
		Assert.assertEquals("Missing body param 'badgeId'", errors.get(1));
		
		this.getTenantPersistenceService().commitTransaction();		
	}
	
	@Test
	public void testCreateWorkerDuplicateBadge(){
		this.getTenantPersistenceService().beginTransaction();
		//Save a worker
		Worker worker1 = createWorkerObject(true, "FirstName_1", "LastName_1", null, "abc123", null, null);
		Response response = facilityResource.createWorker(worker1);
		Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatus());
		
		//Allow saving an inactive worker with the same badge
		Worker worker2 = createWorkerObject(false, "FirstName_2", "LastName_2", null, "abc123", null, null);
		response = facilityResource.createWorker(worker2);
		Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatus());
		
		//Don't allow saving an active worker with the same badge
		Worker worker3 = createWorkerObject(true, "FirstName_3", "LastName_3", null, "abc123", null, null);
		response = facilityResource.createWorker(worker3);
		Assert.assertEquals(HttpServletResponse.SC_BAD_REQUEST, response.getStatus());
		ErrorResponse errorResponse = (ErrorResponse)response.getEntity();
		ArrayList<String> errors = errorResponse.getErrors();
		Assert.assertEquals("Active worker with badge abc123 already exists", errors.get(0));
		
		this.getTenantPersistenceService().commitTransaction();
	}
	
	@Test
	public void testUpdateWorker() throws Exception{
		this.getTenantPersistenceService().beginTransaction();
		
		//Save a worker
		Worker workerOiginal = createWorkerObject(true, "FirstName", "LastName", "MI", "abc123", "GroupName", "hr123");
		Response response = facilityResource.createWorker(workerOiginal);
		Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatus());
		Worker workerSaved = (Worker)response.getEntity();
		compareWorkers(workerOiginal, workerSaved);
		
		//Update a worker
		WorkerResource workerResource = getWorkerResource(workerSaved);
		Worker workerUpdateRequest = createWorkerObject(true, "FirstName_CH", "LastName_CH", "MI_CH", "abc123_CH", "GroupName_CH", "hr123_CH");
		response = workerResource.updateWorker(workerUpdateRequest);
		Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatus());
		Worker workerUpdated = (Worker)response.getEntity();
		compareWorkers(workerUpdateRequest, workerUpdated);

		this.getTenantPersistenceService().commitTransaction();
	}

	@Test
	public void testUpdateWorkerNoData() throws Exception{
		this.getTenantPersistenceService().beginTransaction();
		
		//Save a worker
		Worker workerOiginal = createWorkerObject(true, "FirstName", "LastName", null, "abc123", null, null);
		Response response = facilityResource.createWorker(workerOiginal);
		Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatus());
		Worker workerSaved = (Worker)response.getEntity();
		compareWorkers(workerOiginal, workerSaved);
		
		//Try to update a worker with an empty object
		WorkerResource workerResource = getWorkerResource(workerSaved);
		Worker workerUpdateRequest = new Worker();
		response = workerResource.updateWorker(workerUpdateRequest);
		Assert.assertEquals(HttpServletResponse.SC_BAD_REQUEST, response.getStatus());
		ErrorResponse errorResponse = (ErrorResponse)response.getEntity();
		ArrayList<String> errors = errorResponse.getErrors();
		Assert.assertEquals("Missing body param 'lastName'", errors.get(0));
		Assert.assertEquals("Missing body param 'badgeId'", errors.get(1));

		this.getTenantPersistenceService().commitTransaction();
	}

	
	@Test
	public void testUpdateWorkerDuplicateBadge() throws Exception{
		this.getTenantPersistenceService().beginTransaction();
		
		//Save two workers
		Worker worker1 = createWorkerObject(true, "FirstName_1", "LastName_1", null, "abc123", null, null);
		Response response = facilityResource.createWorker(worker1);
		Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatus());
		
		Worker worker2 = createWorkerObject(true, "FirstName_2", "LastName_2", null, "def456", null, null);
		response = facilityResource.createWorker(worker2);
		Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatus());

		//Try to update second worker with badgeId of the first worker
		Worker worker2UpdateRequest = createWorkerObject(true, "FirstName_2", "LastName_2", null, "abc123", null, null);
		WorkerResource workerResource = getWorkerResource(worker2);
		response = workerResource.updateWorker(worker2UpdateRequest);
		Assert.assertEquals(HttpServletResponse.SC_BAD_REQUEST, response.getStatus());
		ErrorResponse errorResponse = (ErrorResponse)response.getEntity();
		ArrayList<String> errors = errorResponse.getErrors();
		Assert.assertEquals("Active worker with badge abc123 already exists", errors.get(0));
		
		//Confirm that a Worker can be saved with a duplicate badge while inactive
		worker2UpdateRequest.setActive(false);
		response = workerResource.updateWorker(worker2UpdateRequest);
		Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatus());
		Worker worker2Updated = (Worker)response.getEntity();
		compareWorkers(worker2UpdateRequest, worker2Updated);

		//Confirm that an inactive Worker can't be activated if it had a duplicate badge
		worker2UpdateRequest.setActive(true);
		response = workerResource.updateWorker(worker2UpdateRequest);
		Assert.assertEquals(HttpServletResponse.SC_BAD_REQUEST, response.getStatus());
		errorResponse = (ErrorResponse)response.getEntity();
		errors = errorResponse.getErrors();
		Assert.assertEquals("Active worker with badge abc123 already exists", errors.get(0));
		
		this.getTenantPersistenceService().commitTransaction();
	}

	
	@Test
	public void testGetAllWorkers() {
		this.getTenantPersistenceService().beginTransaction();
			
		//Save two workers
		Worker worker1 = createWorkerObject(true, "FirstName_1", "LastName_1", null, "abc123", null, null);
		Response response = facilityResource.createWorker(worker1);
		Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatus());
		
		Worker worker2 = createWorkerObject(true, "FirstName_2", "LastName_2", null, "def456", null, null);
		response = facilityResource.createWorker(worker2);
		Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatus());

		//Retrieve the saved Workers in Facility.
		response = facilityResource.getAllWorkersInFacility();
		Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatus());
		@SuppressWarnings("unchecked")
		List<Worker> workers = (List<Worker>)response.getEntity();
		//Note that the order of Workers in the list isn't enforced. Could break test if something changes in the back.
		compareWorkers(worker1, workers.get(0));
		compareWorkers(worker2, workers.get(1));
		
		//Retrieve all saved Workers in DB.
		response = workersResource.getAllWorkers();
		Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatus());
		@SuppressWarnings("unchecked")
		List<Worker> workersDB = (List<Worker>)response.getEntity();
		//Note that the order of Workers in the list isn't enforced. Could break test if something changes in the back.
		compareWorkers(worker1, workersDB.get(0));
		compareWorkers(worker2, workersDB.get(1));

		this.getTenantPersistenceService().commitTransaction();
	}

	private WorkerResource getWorkerResource(Worker worker) throws Exception {
		WorkerResource workerResource = new WorkerResource();
		workerResource.setWorker(worker);
		return workerResource;
	}
	
	private Worker createWorkerObject(Boolean active,
		String firstName,
		String lastName,
		String middleInitial,
		String badgeId,
		String groupName,
		String hrId) {
		Worker worker = new Worker();
		worker.setActive(active);
		worker.setFirstName(firstName);
		worker.setLastName(lastName);
		worker.setMiddleInitial(middleInitial);
		worker.setBadgeId(badgeId);
		worker.setGroupName(groupName);
		worker.setHrId(hrId);
		return worker;
	}
	
	private void compareWorkers(Worker expected, Worker actual) {
		Assert.assertEquals(expected.getFacility(), actual.getFacility());
		Assert.assertEquals(expected.getActive(), actual.getActive());
		Assert.assertEquals(expected.getFirstName(), actual.getFirstName());
		Assert.assertEquals(expected.getLastName(), actual.getLastName());
		Assert.assertEquals(expected.getMiddleInitial(), actual.getMiddleInitial());
		Assert.assertEquals(expected.getBadgeId(), actual.getBadgeId());
		Assert.assertEquals(expected.getGroupName(), actual.getGroupName());
		Assert.assertEquals(expected.getHrId(), actual.getHrId());
	}
}
