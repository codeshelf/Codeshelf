package com.codeshelf.model.domain;

import static org.mockito.Mockito.mock;

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
import com.codeshelf.behavior.NotificationBehavior;
import com.codeshelf.behavior.OrderBehavior;
import com.codeshelf.behavior.UiUpdateBehavior;
import com.codeshelf.behavior.WorkBehavior;
import com.codeshelf.testframework.HibernateTest;
import com.google.inject.Provider;

public class WorkerTest extends HibernateTest {
	private FacilityResource facilityResource;
	private WorkersResource workersResource;

	@Before
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void init(){
		WorkBehavior workService = mock(WorkBehavior.class);
		OrderBehavior orderService = new OrderBehavior();
		NotificationBehavior notificaitonService = new NotificationBehavior();
		
		Provider anyProvider = mock(Provider.class);
		facilityResource = new FacilityResource(workService, 
			orderService, 
			notificaitonService, 
			webSocketManagerService,
			null,
			new UiUpdateBehavior(), 
			anyProvider, 
			anyProvider, 
			anyProvider, 
			anyProvider,
			anyProvider);
		Facility facility = getFacility();
		facilityResource.setFacility(facility);
		workersResource = new WorkersResource();
		workersResource.setFacility(facility);
	}
	
	@Test
	public void testCreateWorker(){
		this.getTenantPersistenceService().beginTransaction();
	
		//Create Worker with all fields set
		Worker workerFull = createWorkerObject(true, "FirstName", "LastName", "MI", "abc123", "GroupName", "hr123");
		Response response = workersResource.createWorker(workerFull);
		Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatus());
		Worker savedWorkerFull = (Worker)response.getEntity();
		compareWorkers(workerFull, savedWorkerFull);
		
		//Create Worker with only required fields set
		Worker workerMinimal = createWorkerObject(null, "FirstName_Min", "LastName_Min", null, "abc456", null, null);
		response = workersResource.createWorker(workerMinimal);
		Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatus());
		Worker savedWorkerMinimal = (Worker)response.getEntity();
		compareWorkers(workerMinimal, savedWorkerMinimal);
		
		this.getTenantPersistenceService().commitTransaction();		
	}
	
	@Test
	public void testCreateWorkerNoData(){
		this.getTenantPersistenceService().beginTransaction();
		
		Worker workerEmpty = new Worker();
		Response response = workersResource.createWorker(workerEmpty);
		Assert.assertEquals(HttpServletResponse.SC_BAD_REQUEST, response.getStatus());
		ErrorResponse errorResponse = (ErrorResponse)response.getEntity();
		ArrayList<String> errors = errorResponse.getErrors();
		Assert.assertEquals("Missing body param 'lastName'", errors.get(0));
		Assert.assertEquals("Missing body param 'badgeId'", errors.get(1));
		
		this.getTenantPersistenceService().commitTransaction();		
	}
	
	@Test
	public void testCreateWorkerOverwriteOld(){
		this.getTenantPersistenceService().beginTransaction();
		//Save a worker
		Worker worker1 = createWorkerObject(true, "FirstName_1", "LastName_1", null, "abc123", null, null);
		Response response = workersResource.createWorker(worker1);
		Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatus());
		
		//Overwrite old worker with a new one with the same badge
		Worker worker2 = createWorkerObject(false, "FirstName_2", "LastName_2", null, "abc123", null, null);
		response = workersResource.createWorker(worker2);
		Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatus());
		List<Worker> workers = Worker.staticGetDao().getAll();
		Assert.assertEquals(1, workers.size());
		compareWorkers(worker2, workers.get(0));
		
		this.getTenantPersistenceService().commitTransaction();
	}
	
	@Test
	public void testUpdateWorker() throws Exception{
		this.getTenantPersistenceService().beginTransaction();
		
		//Save a worker
		Worker workerOiginal = createWorkerObject(true, "FirstName", "LastName", "MI", "abc123", "GroupName", "hr123");
		Response response = workersResource.createWorker(workerOiginal);
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
		Response response = workersResource.createWorker(workerOiginal);
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
		Response response = workersResource.createWorker(worker1);
		Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatus());
		
		Worker worker2 = createWorkerObject(true, "FirstName_2", "LastName_2", null, "def456", null, null);
		response = workersResource.createWorker(worker2);
		Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatus());

		//Try to update second worker with badgeId of the first worker
		Worker worker2UpdateRequest = createWorkerObject(true, "FirstName_2", "LastName_2", null, "abc123", null, null);
		WorkerResource workerResource = getWorkerResource(worker2);
		response = workerResource.updateWorker(worker2UpdateRequest);
		Assert.assertEquals(HttpServletResponse.SC_BAD_REQUEST, response.getStatus());
		ErrorResponse errorResponse = (ErrorResponse)response.getEntity();
		ArrayList<String> errors = errorResponse.getErrors();
		Assert.assertEquals("Another worker with badge abc123 already exists", errors.get(0));
		
		//Confirm that a Worker still can't be saved, even when another worker is inactive
		worker2UpdateRequest.setActive(false);
		response = workerResource.updateWorker(worker2UpdateRequest);
		Assert.assertEquals(HttpServletResponse.SC_BAD_REQUEST, response.getStatus());
		errorResponse = (ErrorResponse)response.getEntity();
		errors = errorResponse.getErrors();
		Assert.assertEquals("Another worker with badge abc123 already exists", errors.get(0));

		this.getTenantPersistenceService().commitTransaction();
	}

	
	@Test
	public void testGetAllWorkers() throws Exception {
		this.getTenantPersistenceService().beginTransaction();
			
		//Save two workers
		Worker worker1 = createWorkerObject(true, "FirstName_1", "LastName_1", null, "abc123", null, null);
		Response response = workersResource.createWorker(worker1);
		Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatus());
		
		Worker worker2 = createWorkerObject(true, "FirstName_2", "LastName_2", null, "def456", null, null);
		response = workersResource.createWorker(worker2);
		Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatus());

		//Retrieve the saved Workers in Facility.
		response = workersResource.getAllWorkers();
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
		WorkerResource workerResource = new WorkerResource(null);
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
		Assert.assertEquals(expected.getActive(), actual.getActive());
		Assert.assertEquals(expected.getFirstName(), actual.getFirstName());
		Assert.assertEquals(expected.getLastName(), actual.getLastName());
		Assert.assertEquals(expected.getMiddleInitial(), actual.getMiddleInitial());
		Assert.assertEquals(expected.getBadgeId(), actual.getBadgeId());
		Assert.assertEquals(expected.getGroupName(), actual.getGroupName());
		Assert.assertEquals(expected.getHrId(), actual.getHrId());
	}
}
