package com.codeshelf.ws;

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
import com.codeshelf.service.BehaviorFactory;
import com.codeshelf.service.UiUpdateBehavior;
import com.codeshelf.testframework.MockDaoTest;
import com.codeshelf.util.ConverterProvider;
import com.codeshelf.ws.protocol.request.ObjectUpdateRequest;
import com.codeshelf.ws.protocol.response.ObjectUpdateResponse;
import com.codeshelf.ws.protocol.response.ResponseABC;
import com.codeshelf.ws.protocol.response.ResponseStatus;
import com.codeshelf.ws.server.ServerMessageProcessor;

// example che update message:
// "ObjectUpdateRequest":{"className":"Che","persistentId":"66575760-00b8-11e4-ba3a-48d705ccef0f","properties":{"description":"1123"},"messageId":"cid_6"}

public class CreateCheTest extends MockDaoTest {
	private ServerMessageProcessor	processor;

	@Before
	public void doBefore() {
		super.doBefore();
		processor = new ServerMessageProcessor(Mockito.mock(BehaviorFactory.class), new ConverterProvider().get(), this.webSocketManagerService);
	}

	
	@Test
	public final void testCreateChe() {
		this.getTenantPersistenceService().beginTransaction();
		Facility facility = createFacility();
		Facility.staticGetDao().store(facility);		
		
		UiUpdateBehavior service = new UiUpdateBehavior();
		UUID cheid = service.addChe(facility.getPersistentId().toString(), "Test Device", "Updated Description", "orange", "0x00000099", "SETUP_ORDERS", "ORIGINALSERIAL");
		this.getTenantPersistenceService().commitTransaction();

		this.getTenantPersistenceService().beginTransaction();
		Che che = Che.staticGetDao().findByPersistentId(cheid);
		Assert.assertEquals(che.getDomainId(), "Test Device");
		Assert.assertEquals(che.getDescription(), "Updated Description");
		Assert.assertEquals(che.getColor(), ColorEnum.ORANGE);
		Assert.assertEquals(che.getDeviceGuidStr(), "0x00000099");
		Assert.assertEquals(che.getProcessMode(), ProcessMode.SETUP_ORDERS);
		this.getTenantPersistenceService().commitTransaction();
	}

	@Test
	public final void testDeleteChe() {
		this.getTenantPersistenceService().beginTransaction();
		Facility facility = createFacility();
		Facility.staticGetDao().store(facility);		
		
		UiUpdateBehavior service = new UiUpdateBehavior();
		UUID cheid = service.addChe(facility.getPersistentId().toString(), "Test Device", "Updated Description", "orange", "0x00000099", "SETUP_ORDERS", "ORIGINALSERIAL");
		this.getTenantPersistenceService().commitTransaction();

		this.getTenantPersistenceService().beginTransaction();
		service.deleteChe(cheid.toString());

		Che che = Che.staticGetDao().findByPersistentId(cheid);
		Assert.assertNull(che);
		this.getTenantPersistenceService().commitTransaction();
	}

	
	@Test
	// TODO: create proper mock daoProvider / set up injector /?
	public final void testUpdateCheOK() {
		this.getTenantPersistenceService().beginTransaction();

		String description1 = "che description";
		String description2 = "changed che description";
				
		Facility facility = Facility.createFacility("F1", "facilityf1", Point.getZeroPoint());
		Facility.staticGetDao().store(facility);		
		
		CodeshelfNetwork network = facility.getNetworks().get(0);
		
		Che che = new Che();
		che.setDescription(description1);
		che.setParent(network);
		che.setDomainId("C1");
		Che.staticGetDao().store(che);
		
		ObjectUpdateRequest req = new ObjectUpdateRequest();
		req.setClassName("Che");
		req.setPersistentId(che.getPersistentId().toString());
		
		HashMap<String,Object> properties = new HashMap<String,Object>();
		properties.put("description", description2);
		req.setProperties(properties);
		
		ResponseABC response = processor.handleRequest(this.getMockWsConnection(), req);

		Assert.assertTrue(response instanceof ObjectUpdateResponse);
		
		ObjectUpdateResponse updateResponse = (ObjectUpdateResponse) response;
		Assert.assertEquals(ResponseStatus.Success, updateResponse.getStatus());
		
		Object result = updateResponse.getResults();
		Assert.assertTrue(Hibernate.getClass(result).equals(Che.class));
		
		Che changedChe = (Che) result;
		Assert.assertEquals(description2, changedChe.getDescription());
		
		this.getTenantPersistenceService().commitTransaction();

	}
	
	@Test
	public final void testCheNotFound() {
		this.getTenantPersistenceService().beginTransaction();

		String description1 = "che description";
		String description2 = "changed che description";

		//setupDaos();

		Facility facility = Facility.createFacility("F1", "facf1",Point.getZeroPoint());

		Facility.staticGetDao().store(facility);		
		
		CodeshelfNetwork network = facility.getNetworks().get(0);
		
		Che che = new Che();
		che.setDescription(description1);
		che.setParent(network);
		che.setDomainId("C1");
		Che.staticGetDao().store(che);
		
		ObjectUpdateRequest req = new ObjectUpdateRequest();
		req.setClassName("Che");
		req.setPersistentId(UUID.randomUUID().toString());
		
		HashMap<String,Object> properties = new HashMap<String,Object>();
		properties.put("description", description2);
		req.setProperties(properties);
		

		ResponseABC response = processor.handleRequest(this.getMockWsConnection(), req);
		Assert.assertTrue(response instanceof ObjectUpdateResponse);
		
		ObjectUpdateResponse updateResponse = (ObjectUpdateResponse) response;
		Assert.assertEquals(ResponseStatus.Fail, updateResponse.getStatus());
		
		Object result = updateResponse.getResults();
		Assert.assertNull(result);
		
		this.getTenantPersistenceService().commitTransaction();

	}
	
	@Test
	public final void testBogusCheId() {
		this.getTenantPersistenceService().beginTransaction();

		String description1 = "che description";
		String description2 = "changed che description";
		
		//setupDaos();

		Facility facility = Facility.createFacility("F1", "facf1", Point.getZeroPoint());
		Facility.staticGetDao().store(facility);		
		
		CodeshelfNetwork network = facility.getNetworks().get(0);
		
		Che che = new Che();
		che.setDescription(description1);
		che.setParent(network);
		che.setDomainId("C1");
		Che.staticGetDao().store(che);
		
		ObjectUpdateRequest req = new ObjectUpdateRequest();
		req.setClassName("Che");
		req.setPersistentId("bogus-id");
		
		HashMap<String,Object> properties = new HashMap<String,Object>();
		properties.put("description", description2);
		req.setProperties(properties);
		
		ResponseABC response = processor.handleRequest(this.getMockWsConnection(), req);
		Assert.assertTrue(response instanceof ObjectUpdateResponse);
		
		ObjectUpdateResponse updateResponse = (ObjectUpdateResponse) response;
		Assert.assertEquals(ResponseStatus.Fail, updateResponse.getStatus());
		
		Object result = updateResponse.getResults();
		Assert.assertNull(result);		

		this.getTenantPersistenceService().commitTransaction();

	}
	
	
	@Test
	public final void testInvalidClass() {	

		ObjectUpdateRequest req = new ObjectUpdateRequest();
		req.setClassName("Foobar");
		req.setPersistentId("bogus-id");
		
		HashMap<String,Object> properties = new HashMap<String,Object>();
		properties.put("foo", "bar");
		req.setProperties(properties);
		
		ResponseABC response = processor.handleRequest(this.getMockWsConnection(), req);
		Assert.assertTrue(response instanceof ObjectUpdateResponse);
		
		ObjectUpdateResponse updateResponse = (ObjectUpdateResponse) response;
		Assert.assertEquals(ResponseStatus.Fail, updateResponse.getStatus());
		
		Object result = updateResponse.getResults();
		Assert.assertNull(result);		
	}
	
	
	@Test
	public final void testUndefinedCheId() {
		this.getTenantPersistenceService().beginTransaction();

		String description1 = "che description";
		String description2 = "changed che description";

		//setupDaos();

		Facility facility = Facility.createFacility("F1", "facf1", Point.getZeroPoint());
		Facility.staticGetDao().store(facility);		
		
		CodeshelfNetwork network = facility.getNetworks().get(0);
		
		Che che = new Che();
		che.setDescription(description1);
		che.setParent(network);
		che.setDomainId("C1");
		Che.staticGetDao().store(che);
		
		ObjectUpdateRequest req = new ObjectUpdateRequest();
		req.setClassName("Che");
		req.setPersistentId(null);
		
		HashMap<String,Object> properties = new HashMap<String,Object>();
		properties.put("description", description2);
		req.setProperties(properties);
		
		ResponseABC response = processor.handleRequest(this.getMockWsConnection(), req);
		Assert.assertTrue(response instanceof ObjectUpdateResponse);
		
		ObjectUpdateResponse updateResponse = (ObjectUpdateResponse) response;
		Assert.assertEquals(ResponseStatus.Fail, updateResponse.getStatus());
		
		Object result = updateResponse.getResults();
		Assert.assertNull(result);		
		
		this.getTenantPersistenceService().commitTransaction();
	}
	
	@Test
	public void cheUpdateFromUISuccess() {
		this.getTenantPersistenceService().beginTransaction();
		Che che = createTestChe("0x00000002");
		UiUpdateBehavior service = new UiUpdateBehavior();
		service.updateChe(che.getPersistentId().toString(),"Test Device", "Updated Description", "orange", "0x00000099", "SETUP_ORDERS", "ORIGINALSERIAL");
		java.util.UUID cheid = che.getPersistentId();
		this.getTenantPersistenceService().commitTransaction();

		this.getTenantPersistenceService().beginTransaction();
		che = Che.staticGetDao().findByPersistentId(cheid);
		Assert.assertEquals(che.getDomainId(), "Test Device");
		Assert.assertEquals(che.getDescription(), "Updated Description");
		Assert.assertEquals(che.getColor(), ColorEnum.ORANGE);
		Assert.assertEquals(che.getDeviceGuidStr(), "0x00000099");
		Assert.assertEquals(che.getProcessMode(), ProcessMode.SETUP_ORDERS);
		this.getTenantPersistenceService().commitTransaction();
	}
	
	@Test
	public void cheUpdateFromUIFail() {
		this.getTenantPersistenceService().beginTransaction();
		Che che = createTestChe("0x00000003");
		UiUpdateBehavior service = new UiUpdateBehavior();
		String persistentId = che.getPersistentId().toString();
		//Update che successfully
		service.updateChe(persistentId, "Test Device", "Description", "orange", "0x00000099", "SETUP_ORDERS", "ORIGINALSERIAL");
		//Fail to update name
		service.updateChe(persistentId, "", "Description", "orange", "0x00000099", "SETUP_ORDERS", "ORIGINALSERIAL");
		Assert.assertEquals(che.getDomainId(), "Test Device");
		//Fail to update description
		service.updateChe(persistentId, "Test Device", null, "orange", "0x00000099", "SETUP_ORDERS", "ORIGINALSERIAL");
		Assert.assertEquals(che.getDescription(), "Description");
		//Fail to update color
		service.updateChe(persistentId, "Test Device", "Description", "yellow", "0x00000099", "SETUP_ORDERS", "ORIGINALSERIAL");
		Assert.assertEquals(che.getColor(), ColorEnum.ORANGE);
		//Fail to update controller id
		service.updateChe(persistentId, "Test Device", "Description", "orange", "0x00000099x", "SETUP_ORDERS", "ORIGINALSERIAL");
		Assert.assertEquals(che.getDeviceGuidStr(), "0x00000099");
		//Fail to update process mode
		service.updateChe(persistentId, "Test Device Changed", "Description", "orange", "0x00000099x", "SETUP_ORDERSX", "ORIGINALSERIAL");
		Assert.assertEquals(che.getDomainId(), "Test Device");
		//Fail to update scanner type
		service.updateChe(persistentId, "Test Device Changed", "Description", "orange", "0x00000099x", "SETUP_ORDERS", "ORIGINALSERIALX");
		Assert.assertEquals(che.getDomainId(), "Test Device");

		this.getTenantPersistenceService().commitTransaction();
	}
	
	private Che createTestChe(String netGuid){
		Facility facility = Facility.createFacility("F1", "facf1", Point.getZeroPoint());
		CodeshelfNetwork network = facility.createNetwork("WITEST");
		Che che = network.createChe(netGuid, new NetGuid(netGuid));
		return che;
	}

}
