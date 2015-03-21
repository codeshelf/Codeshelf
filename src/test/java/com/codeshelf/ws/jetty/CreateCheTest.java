package com.codeshelf.ws.jetty;

import java.util.HashMap;
import java.util.UUID;

import org.hibernate.Hibernate;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.codeshelf.flyweight.command.ColorEnum;
import com.codeshelf.flyweight.command.NetGuid;
import com.codeshelf.model.domain.Che;
import com.codeshelf.model.domain.Che.ProcessMode;
import com.codeshelf.model.domain.CodeshelfNetwork;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.Point;
import com.codeshelf.service.ServiceFactory;
import com.codeshelf.service.UiUpdateService;
import com.codeshelf.testframework.MockDaoTest;
import com.codeshelf.util.ConverterProvider;
import com.codeshelf.ws.jetty.protocol.request.ObjectUpdateRequest;
import com.codeshelf.ws.jetty.protocol.response.ObjectUpdateResponse;
import com.codeshelf.ws.jetty.protocol.response.ResponseABC;
import com.codeshelf.ws.jetty.protocol.response.ResponseStatus;
import com.codeshelf.ws.jetty.server.ServerMessageProcessor;
import com.codeshelf.ws.jetty.server.WebSocketConnection;

// example che update message:
// "ObjectUpdateRequest":{"className":"Che","persistentId":"66575760-00b8-11e4-ba3a-48d705ccef0f","properties":{"description":"1123"},"messageId":"cid_6"}

public class CreateCheTest extends MockDaoTest {
	WebSocketConnection mSession;
	
	private ServerMessageProcessor	processor;

	@Before
	public void doBefore() {
		super.doBefore();
		processor = new ServerMessageProcessor(Mockito.mock(ServiceFactory.class), new ConverterProvider().get(), this.webSocketManagerService);
	}

	
	@Test
	public final void testCreateChe() {
		this.getTenantPersistenceService().beginTransaction(getDefaultTenant());
		Facility facility = createFacility();
		Facility.staticGetDao().store(getDefaultTenant(),facility);		
		
		UiUpdateService service = new UiUpdateService();
		UUID cheid = service.addChe(getDefaultTenant(),facility.getPersistentId().toString(), "Test Device", "Updated Description", "orange", "0x00000099", "SETUP_ORDERS");
		this.getTenantPersistenceService().commitTransaction(getDefaultTenant());

		this.getTenantPersistenceService().beginTransaction(getDefaultTenant());
		Che che = Che.staticGetDao().findByPersistentId(getDefaultTenant(),cheid);
		Assert.assertEquals(che.getDomainId(), "Test Device");
		Assert.assertEquals(che.getDescription(), "Updated Description");
		Assert.assertEquals(che.getColor(), ColorEnum.ORANGE);
		Assert.assertEquals(che.getDeviceGuidStr(), "0x00000099");
		Assert.assertEquals(che.getProcessMode(), ProcessMode.SETUP_ORDERS);
		this.getTenantPersistenceService().commitTransaction(getDefaultTenant());
	}

	@Test
	public final void testDeleteChe() {
		this.getTenantPersistenceService().beginTransaction(getDefaultTenant());
		Facility facility = createFacility();
		Facility.staticGetDao().store(getDefaultTenant(),facility);		
		
		UiUpdateService service = new UiUpdateService();
		UUID cheid = service.addChe(getDefaultTenant(),facility.getPersistentId().toString(), "Test Device", "Updated Description", "orange", "0x00000099", "SETUP_ORDERS");
		this.getTenantPersistenceService().commitTransaction(getDefaultTenant());

		this.getTenantPersistenceService().beginTransaction(getDefaultTenant());
		service.deleteChe(getDefaultTenant(),cheid.toString());

		Che che = Che.staticGetDao().findByPersistentId(getDefaultTenant(),cheid);
		Assert.assertNull(che);
		this.getTenantPersistenceService().commitTransaction(getDefaultTenant());
	}

	
	@Test
	// TODO: create proper mock daoProvider / set up injector /?
	public final void testUpdateCheOK() {
		this.getTenantPersistenceService().beginTransaction(getDefaultTenant());

		String description1 = "che description";
		String description2 = "changed che description";
				
		Facility facility = Facility.createFacility(getDefaultTenant(),"F1", "facilityf1", Point.getZeroPoint());
		Facility.staticGetDao().store(getDefaultTenant(),facility);		
		
		CodeshelfNetwork network = facility.getNetworks().get(0);
		
		Che che = new Che();
		che.setDescription(description1);
		che.setParent(network);
		che.setDomainId("C1");
		Che.staticGetDao().store(getDefaultTenant(),che);
		
		ObjectUpdateRequest req = new ObjectUpdateRequest();
		req.setClassName("Che");
		req.setPersistentId(che.getPersistentId().toString());
		
		HashMap<String,Object> properties = new HashMap<String,Object>();
		properties.put("description", description2);
		req.setProperties(properties);
		
		ResponseABC response = processor.handleRequest(mSession, req);

		Assert.assertTrue(response instanceof ObjectUpdateResponse);
		
		ObjectUpdateResponse updateResponse = (ObjectUpdateResponse) response;
		Assert.assertEquals(ResponseStatus.Success, updateResponse.getStatus());
		
		Object result = updateResponse.getResults();
		Assert.assertTrue(Hibernate.getClass(result).equals(Che.class));
		
		Che changedChe = (Che) result;
		Assert.assertEquals(description2, changedChe.getDescription());
		
		this.getTenantPersistenceService().commitTransaction(getDefaultTenant());

	}
	
	@Test
	public final void testCheNotFound() {
		this.getTenantPersistenceService().beginTransaction(getDefaultTenant());

		String description1 = "che description";
		String description2 = "changed che description";

		//setupDaos();

		Facility facility = Facility.createFacility(getDefaultTenant(),"F1", "facf1",Point.getZeroPoint());

		Facility.staticGetDao().store(getDefaultTenant(),facility);		
		
		CodeshelfNetwork network = facility.getNetworks().get(0);
		
		Che che = new Che();
		che.setDescription(description1);
		che.setParent(network);
		che.setDomainId("C1");
		Che.staticGetDao().store(getDefaultTenant(),che);
		
		ObjectUpdateRequest req = new ObjectUpdateRequest();
		req.setClassName("Che");
		req.setPersistentId(UUID.randomUUID().toString());
		
		HashMap<String,Object> properties = new HashMap<String,Object>();
		properties.put("description", description2);
		req.setProperties(properties);
		

		ResponseABC response = processor.handleRequest(mSession, req);
		Assert.assertTrue(response instanceof ObjectUpdateResponse);
		
		ObjectUpdateResponse updateResponse = (ObjectUpdateResponse) response;
		Assert.assertEquals(ResponseStatus.Fail, updateResponse.getStatus());
		
		Object result = updateResponse.getResults();
		Assert.assertNull(result);
		
		this.getTenantPersistenceService().commitTransaction(getDefaultTenant());

	}
	
	@Test
	public final void testBogusCheId() {
		this.getTenantPersistenceService().beginTransaction(getDefaultTenant());

		String description1 = "che description";
		String description2 = "changed che description";
		
		//setupDaos();

		Facility facility = Facility.createFacility(getDefaultTenant(),"F1", "facf1", Point.getZeroPoint());
		Facility.staticGetDao().store(getDefaultTenant(),facility);		
		
		CodeshelfNetwork network = facility.getNetworks().get(0);
		
		Che che = new Che();
		che.setDescription(description1);
		che.setParent(network);
		che.setDomainId("C1");
		Che.staticGetDao().store(getDefaultTenant(),che);
		
		ObjectUpdateRequest req = new ObjectUpdateRequest();
		req.setClassName("Che");
		req.setPersistentId("bogus-id");
		
		HashMap<String,Object> properties = new HashMap<String,Object>();
		properties.put("description", description2);
		req.setProperties(properties);
		
		ResponseABC response = processor.handleRequest(mSession, req);
		Assert.assertTrue(response instanceof ObjectUpdateResponse);
		
		ObjectUpdateResponse updateResponse = (ObjectUpdateResponse) response;
		Assert.assertEquals(ResponseStatus.Fail, updateResponse.getStatus());
		
		Object result = updateResponse.getResults();
		Assert.assertNull(result);		

		this.getTenantPersistenceService().commitTransaction(getDefaultTenant());

	}
	
	
	@Test
	public final void testInvalidClass() {	
		WebSocketConnection session = Mockito.mock(WebSocketConnection.class);
		session.setSessionId("test-session");
	
		ObjectUpdateRequest req = new ObjectUpdateRequest();
		req.setClassName("Foobar");
		req.setPersistentId("bogus-id");
		
		HashMap<String,Object> properties = new HashMap<String,Object>();
		properties.put("foo", "bar");
		req.setProperties(properties);
		
		ResponseABC response = processor.handleRequest(session, req);
		Assert.assertTrue(response instanceof ObjectUpdateResponse);
		
		ObjectUpdateResponse updateResponse = (ObjectUpdateResponse) response;
		Assert.assertEquals(ResponseStatus.Fail, updateResponse.getStatus());
		
		Object result = updateResponse.getResults();
		Assert.assertNull(result);		
	}
	
	
	@Test
	public final void testUndefinedCheId() {
		this.getTenantPersistenceService().beginTransaction(getDefaultTenant());

		String description1 = "che description";
		String description2 = "changed che description";

		//setupDaos();

		Facility facility = Facility.createFacility(getDefaultTenant(),"F1", "facf1", Point.getZeroPoint());
		Facility.staticGetDao().store(getDefaultTenant(),facility);		
		
		CodeshelfNetwork network = facility.getNetworks().get(0);
		
		Che che = new Che();
		che.setDescription(description1);
		che.setParent(network);
		che.setDomainId("C1");
		Che.staticGetDao().store(getDefaultTenant(),che);
		
		ObjectUpdateRequest req = new ObjectUpdateRequest();
		req.setClassName("Che");
		req.setPersistentId(null);
		
		HashMap<String,Object> properties = new HashMap<String,Object>();
		properties.put("description", description2);
		req.setProperties(properties);
		
		ResponseABC response = processor.handleRequest(mSession, req);
		Assert.assertTrue(response instanceof ObjectUpdateResponse);
		
		ObjectUpdateResponse updateResponse = (ObjectUpdateResponse) response;
		Assert.assertEquals(ResponseStatus.Fail, updateResponse.getStatus());
		
		Object result = updateResponse.getResults();
		Assert.assertNull(result);		
		
		this.getTenantPersistenceService().commitTransaction(getDefaultTenant());
	}
	
	@Test
	public void cheUpdateFromUISuccess() {
		this.getTenantPersistenceService().beginTransaction(getDefaultTenant());
		Che che = createTestChe("0x00000002");
		UiUpdateService service = new UiUpdateService();
		service.updateChe(getDefaultTenant(),che.getPersistentId().toString(),"Test Device", "Updated Description", "orange", "0x00000099", "SETUP_ORDERS");
		java.util.UUID cheid = che.getPersistentId();
		this.getTenantPersistenceService().commitTransaction(getDefaultTenant());

		this.getTenantPersistenceService().beginTransaction(getDefaultTenant());
		che = Che.staticGetDao().findByPersistentId(getDefaultTenant(),cheid);
		Assert.assertEquals(che.getDomainId(), "Test Device");
		Assert.assertEquals(che.getDescription(), "Updated Description");
		Assert.assertEquals(che.getColor(), ColorEnum.ORANGE);
		Assert.assertEquals(che.getDeviceGuidStr(), "0x00000099");
		Assert.assertEquals(che.getProcessMode(), ProcessMode.SETUP_ORDERS);
		this.getTenantPersistenceService().commitTransaction(getDefaultTenant());
	}
	
	@Test
	public void cheUpdateFromUIFail() {
		this.getTenantPersistenceService().beginTransaction(getDefaultTenant());
		Che che = createTestChe("0x00000003");
		UiUpdateService service = new UiUpdateService();
		String persistentId = che.getPersistentId().toString();
		//Update che successfully
		service.updateChe(getDefaultTenant(),persistentId, "Test Device", "Description", "orange", "0x00000099", "SETUP_ORDERS");
		//Fail to update name
		service.updateChe(getDefaultTenant(),persistentId, "", "Description", "orange", "0x00000099", "SETUP_ORDERS");
		Assert.assertEquals(che.getDomainId(), "Test Device");
		//Fail to update description
		service.updateChe(getDefaultTenant(),persistentId, "Test Device", null, "orange", "0x00000099", "SETUP_ORDERS");
		Assert.assertEquals(che.getDescription(), "Description");
		//Fail to update color
		service.updateChe(getDefaultTenant(),persistentId, "Test Device", "Description", "yellow", "0x00000099", "SETUP_ORDERS");
		Assert.assertEquals(che.getColor(), ColorEnum.ORANGE);
		//Fail to update controller id
		service.updateChe(getDefaultTenant(),persistentId, "Test Device", "Description", "orange", "0x00000099x", "SETUP_ORDERS");
		Assert.assertEquals(che.getDeviceGuidStr(), "0x00000099");
		//Fail to update process mode
		service.updateChe(getDefaultTenant(),persistentId, "Test Device Changed", "Description", "orange", "0x00000099x", "SETUP_ORDERSX");
		Assert.assertEquals(che.getDomainId(), "Test Device");

		this.getTenantPersistenceService().commitTransaction(getDefaultTenant());
	}
	
	private Che createTestChe(String netGuid){
		Facility facility = Facility.createFacility(getDefaultTenant(),"F1", "facf1", Point.getZeroPoint());
		CodeshelfNetwork network = facility.createNetwork(getDefaultTenant(),"WITEST");
		Che che = network.createChe(getDefaultTenant(),netGuid, new NetGuid(netGuid));
		return che;
	}

}
