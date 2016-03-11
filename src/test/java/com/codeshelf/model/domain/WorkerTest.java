package com.codeshelf.model.domain;

import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.beanutils.BeanUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.codeshelf.api.ErrorResponse;
import com.codeshelf.api.ParameterUtils;
import com.codeshelf.api.resources.EventsResource;
import com.codeshelf.api.resources.WorkersResource;
import com.codeshelf.api.resources.subresources.FacilityResource;
import com.codeshelf.behavior.NotificationBehavior;
import com.codeshelf.behavior.OrderBehavior;
import com.codeshelf.behavior.UiUpdateBehavior;
import com.codeshelf.behavior.WorkBehavior;
import com.codeshelf.testframework.HibernateTest;
import com.google.inject.Provider;
import com.sun.jersey.api.representation.Form;

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
			new UiUpdateBehavior(webSocketManagerService), 
			anyProvider, 
			anyProvider, 
			anyProvider, 
			anyProvider,
			anyProvider);
		Facility facility = getFacility();
		facilityResource.setFacility(facility);
		workersResource = new WorkersResource(null, null);
		workersResource.setParent(facility);
	}
	
	private Response createWorkerApi(Worker worker) throws ReflectiveOperationException {
		return workersResource.create(ParameterUtils.fromObject(worker));
	}
	
	@Test
	public void testCreateWorker() throws ReflectiveOperationException{
		this.getTenantPersistenceService().beginTransaction();
	
		//Create Worker with all fields set
		Worker workerFull = createWorkerObject(true, "FirstName", "LastName", "MI", "abc123", "GroupName", "hr123");
		Worker savedWorkerFull = (Worker) createWorkerApi(workerFull).getEntity();
		compareWorkers(workerFull, savedWorkerFull);
		
		//Create Worker with only required fields set
		Worker workerMinimal = createWorkerObject(false, "FirstName_Min", "LastName_Min", null, "abc456", null, null);
		Worker savedWorkerMinimal = (Worker) createWorkerApi(workerMinimal).getEntity();
		compareWorkers(workerMinimal, savedWorkerMinimal);
		
		this.getTenantPersistenceService().commitTransaction();		
	}
	
	@Test
	public void testCreateWorkerNoData() throws ReflectiveOperationException{
		beginTransaction();
		Worker workerEmpty = new Worker();
		Response response = createWorkerApi(workerEmpty);
		assertAllRequired(response, "lastName", "domainId");
		rollbackTransaction();
	}
	
	@Test
	public void testUnableToCreateWithSameDomainId() throws ReflectiveOperationException{
		beginTransaction();
		//Save a worker
		String badgeId = "abc123";
		Worker worker1 = createWorkerObject(true, "FirstName_1", "LastName_1", null, badgeId, null, null);
		Response response = createWorkerApi(worker1);
		Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatus());
		commitTransaction();

		beginTransaction();
		//Attempt create with new worker similar badge
		Worker worker2 = createWorkerObject(false, "FirstName_2", "LastName_2", null, badgeId, null, null);
		response = createWorkerApi(worker2);
		Assert.assertEquals(HttpServletResponse.SC_BAD_REQUEST, response.getStatus());
		commitTransaction();
	}
	
	@Test
	public void testUpdateWorker() throws Exception{
		String badgeId = "abc123";
		beginTransaction();
		
		//Save a worker
		Worker workerOiginal = createWorkerObject(true, "FirstName", "LastName", "MI", badgeId , "GroupName", "hr123");
		Response response = createWorkerApi(workerOiginal);
		Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatus());
		Worker workerSaved = (Worker)response.getEntity();
		compareWorkers(workerOiginal, workerSaved);
		commitTransaction();

		beginTransaction();
		//Update a worker
		Worker workerUpdateRequest = createWorkerObject(true, "FirstName_CH", "LastName_CH", "MI_CH", badgeId, "GroupName_CH", "hr123_CH");
		response = update(workerSaved.getDomainId(), workerUpdateRequest);
		Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatus());
		Worker workerUpdated = (Worker)response.getEntity();
		compareWorkers(workerUpdateRequest, workerUpdated);
		commitTransaction();
	}

	private Response update(String domainId, Map<String,String> params) {
		return workersResource.update(domainId, ParameterUtils.fromMap(params));
	}

	private Response update(String domainId, Worker workerUpdateRequest) {
		try {
			Map<String, String> params = BeanUtils.describe(workerUpdateRequest);
			params.remove("persistentId");
			return update(domainId, params);
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException(e);
		}
	}

	@Test
	public void testUpdateWorkerNoData() throws Exception{
		beginTransaction();
		//Save a worker
		String badgeId = "abc123";
		Worker workerOiginal = createWorkerObject(true, "FirstName", "LastName", null, badgeId, null, null);
		Response response = createWorkerApi(workerOiginal);
		Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatus());
		Worker workerSaved = (Worker)response.getEntity();
		compareWorkers(workerOiginal, workerSaved);
		commitTransaction();
		
		beginTransaction();
		//Try to update a worker with an empty object
		Worker workerUpdateRequest = new Worker();
		workerUpdateRequest.setDomainId(badgeId);
		response = update(workerSaved.getDomainId(), workerUpdateRequest);
		assertAllRequired(response, "lastName");
		rollbackTransaction();
	}

	private void assertAllRequired(Response response, String... required) {
		ErrorResponse errorResponse = (ErrorResponse)response.getEntity();
		Assert.assertEquals(HttpServletResponse.SC_BAD_REQUEST, response.getStatus());
		HashSet<String> errors = new HashSet<>(errorResponse.getErrors());
		for (String field : required) {
			errors.remove(field + " is required");
		}
		Assert.assertEquals(errors.toString(), 0, errors.size());
	}
	
	@Test
	public void testUpdateWorkerDuplicateBadge() throws Exception{
		this.getTenantPersistenceService().beginTransaction();
		
		//Save two workers
		Worker worker1 = createWorkerObject(true, "FirstName_1", "LastName_1", null, "abc123", null, null);
		Response response = createWorkerApi(worker1);
		Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatus());
		
		Worker worker2 = createWorkerObject(true, "FirstName_2", "LastName_2", null, "def456", null, null);
		response = createWorkerApi(worker2);
		Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatus());

		//Try to update second worker with badgeId of the first worker
		Worker worker2UpdateRequest = createWorkerObject(true, "FirstName_2", "LastName_2", null, "abc123", null, null);
		response = update(worker2.getDomainId(), worker2UpdateRequest);
		Assert.assertEquals(HttpServletResponse.SC_BAD_REQUEST, response.getStatus());
		ErrorResponse errorResponse = (ErrorResponse)response.getEntity();
		ArrayList<String> errors = errorResponse.getErrors();
		Assert.assertEquals("Another worker with badge abc123 already exists", errors.get(0));
		
		//Confirm that a Worker still can't be saved, even when another worker is inactive
		worker2UpdateRequest.setActive(false);
		response = update(worker2.getDomainId(), worker2UpdateRequest);
		Assert.assertEquals(HttpServletResponse.SC_BAD_REQUEST, response.getStatus());
		errorResponse = (ErrorResponse)response.getEntity();
		errors = errorResponse.getErrors();
		Assert.assertEquals("Another worker with badge abc123 already exists", errors.get(0));

		this.getTenantPersistenceService().commitTransaction();
	}

	@Test
	public void getWorkerEvents() throws Exception {
		this.getTenantPersistenceService().beginTransaction();
		
		
		
		Worker worker1 = createWorkerObject(true, "FirstName_1", "LastName_1", null, "abc123", null, null);
		Response response = createWorkerApi(worker1);
		Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatus());
		
		
		Form form = new Form();
		form.add("limit", "1");
		EventsResource eventsResource = getEventsResource(worker1);
		UriInfo uriInfo = mock(UriInfo.class);
		Mockito.when(uriInfo.getQueryParameters()).thenReturn(form);
		eventsResource.getPagedEvents(uriInfo);

		
		this.getTenantPersistenceService().commitTransaction();
	}
	
	@Test
	public void testGetAllWorkers() throws Exception {
		this.getTenantPersistenceService().beginTransaction();
			
		//Save two workers
		Worker worker1 = createWorkerObject(true, "FirstName_1", "LastName_1", null, "abc123", null, null);
		Worker worker2 = createWorkerObject(true, "FirstName_2", "LastName_2", null, "def456", null, null);
		saveWorker(worker1);
		saveWorker(worker2);

		ArrayList<Worker> workers = getAllWorkers(null, 20);

		//Note that the order of Workers is defaulted by badgeId
		compareWorkers(worker1, workers.get(0));
		compareWorkers(worker2, workers.get(1));
		

		
		this.getTenantPersistenceService().commitTransaction();
	}

	@Test
	public void searchWorkersCaseInsensitive() {
		beginTransaction();
		
		//Save two workers
		Worker worker1 = createWorkerObject(true, "FirstName_1", "LastName_1", null, "abc123", null, null);
		Worker worker2 = createWorkerObject(true, "FirstName_2", "LastName_2", null, "def456", null, null);
		saveWorker(worker1);
		saveWorker(worker2);
		
		
		ArrayList<Worker> foundWorkers = getAllWorkers("*ABC*", 20);
		Assert.assertEquals(1,  foundWorkers.size());
		compareWorkers(worker1, foundWorkers.get(0));
		commitTransaction();
	}

	private ArrayList<Worker> getAllWorkers(String badgeId, Integer limit) {
		UriInfo uriInfo = mock(UriInfo.class);
		HashMap<String, String> params = new HashMap<String, String>();
		if (badgeId != null) {
			params.put("domainId", badgeId);
		}
		params.put("limit", limit.toString());
		Mockito.when(uriInfo.getQueryParameters()).thenReturn(ParameterUtils.fromMap(params));
		Collection<Worker> results = workersResource.getAll(uriInfo).getResults();
		ArrayList<Worker> foundWorkers = new ArrayList<>(results);
		return foundWorkers;
	}
	
	private void saveWorker(Worker worker) {
		try {
			Response response = createWorkerApi(worker);
			Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatus());
		} catch (ReflectiveOperationException e) {
			Assert.fail(e.getMessage());
		}
		
	}
	
	private EventsResource getEventsResource(Worker worker) throws Exception {
		EventsResource eventsResource = new EventsResource(new NotificationBehavior());
		eventsResource.setWorker(worker);
		return eventsResource;
	}	
	private Worker createWorkerObject(boolean active,
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
		worker.setDomainId(badgeId);
		worker.setGroupName(groupName);
		worker.setHrId(hrId);
		return worker;
	}
	
	private void compareWorkers(Worker expected, Worker actual) {
		Assert.assertEquals(expected.getActive(), actual.getActive());
		Assert.assertEquals(expected.getFirstName(), actual.getFirstName());
		Assert.assertEquals(expected.getLastName(), actual.getLastName());
		Assert.assertEquals(expected.getMiddleInitial(), actual.getMiddleInitial());
		Assert.assertEquals(expected.getDomainId(), actual.getDomainId());
		Assert.assertEquals(expected.getGroupName(), actual.getGroupName());
		Assert.assertEquals(expected.getHrId(), actual.getHrId());
	}
}
