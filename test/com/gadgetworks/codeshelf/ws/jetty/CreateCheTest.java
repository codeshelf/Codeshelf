package com.gadgetworks.codeshelf.ws.jetty;

import java.util.HashMap;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import com.eaio.uuid.UUID;
import com.gadgetworks.codeshelf.model.dao.DAOTestABC;
import com.gadgetworks.codeshelf.model.domain.Aisle;
import com.gadgetworks.codeshelf.model.domain.Che;
import com.gadgetworks.codeshelf.model.domain.CodeshelfNetwork;
import com.gadgetworks.codeshelf.model.domain.Facility;
import com.gadgetworks.codeshelf.model.domain.Organization;
import com.gadgetworks.codeshelf.model.domain.Point;
import com.gadgetworks.codeshelf.model.domain.Che.ProcessMode;
import com.gadgetworks.codeshelf.service.ServiceFactory;
import com.gadgetworks.codeshelf.service.UiUpdateService;
import com.gadgetworks.codeshelf.util.ConverterProvider;
import com.gadgetworks.codeshelf.ws.jetty.protocol.request.ObjectUpdateRequest;
import com.gadgetworks.codeshelf.ws.jetty.protocol.response.ObjectUpdateResponse;
import com.gadgetworks.codeshelf.ws.jetty.protocol.response.ResponseABC;
import com.gadgetworks.codeshelf.ws.jetty.protocol.response.ResponseStatus;
import com.gadgetworks.codeshelf.ws.jetty.server.ServerMessageProcessor;
import com.gadgetworks.codeshelf.ws.jetty.server.UserSession;
import com.gadgetworks.flyweight.command.ColorEnum;
import com.gadgetworks.flyweight.command.NetGuid;

// example che update message:
// "ObjectUpdateRequest":{"className":"Che","persistentId":"66575760-00b8-11e4-ba3a-48d705ccef0f","properties":{"description":"1123"},"messageId":"cid_6"}

public class CreateCheTest extends DAOTestABC {
	UserSession mSession;
	
	private ServerMessageProcessor	processor;


	public void doBefore() {
		processor = new ServerMessageProcessor(Mockito.mock(ServiceFactory.class), new ConverterProvider().get());
	}
	
	@Test
	// TODO: create proper mock daoProvider / set up injector /?
	public final void testUpdateCheOK() {
		this.getPersistenceService().beginTenantTransaction();

		String description1 = "che description";
		String description2 = "changed che description";
		

		Organization organization = new Organization();
		organization.setOrganizationId("CTEST1.O1");
		Organization.DAO.store(organization);
		
		Facility facility = organization.createFacility("F1", "facilityf1", Point.getZeroPoint());
		Facility.DAO.store(facility);		
		
		CodeshelfNetwork network = facility.createNetwork("N1");
		organization.createDefaultSiteControllerUser(network); 
		CodeshelfNetwork.DAO.store(network);
		
		Che che = new Che();
		che.setDescription(description1);
		che.setParent(network);
		che.setDomainId("C1");
		Che.DAO.store(che);
		
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
		Assert.assertTrue(result instanceof Che);
		
		Che changedChe = (Che) result;
		Assert.assertEquals(description2, changedChe.getDescription());
		
		this.getPersistenceService().commitTenantTransaction();

	}
	
	@Test
	public final void testCheNotFound() {
		this.getPersistenceService().beginTenantTransaction();

		String description1 = "che description";
		String description2 = "changed che description";

		//setupDaos();

		Organization organization = new Organization();
		organization.setOrganizationId("CTEST1.O1");
		Organization.DAO.store(organization);

		Facility facility = organization.createFacility("F1", "facf1",Point.getZeroPoint());

		Facility.DAO.store(facility);		
		
		CodeshelfNetwork network = facility.createNetwork("N1");
		CodeshelfNetwork.DAO.store(network);
		organization.createDefaultSiteControllerUser(network); 
		
		Che che = new Che();
		che.setDescription(description1);
		che.setParent(network);
		che.setDomainId("C1");
		Che.DAO.store(che);
		
		ObjectUpdateRequest req = new ObjectUpdateRequest();
		req.setClassName("Che");
		req.setPersistentId(new UUID().toString());
		
		HashMap<String,Object> properties = new HashMap<String,Object>();
		properties.put("description", description2);
		req.setProperties(properties);
		

		ResponseABC response = processor.handleRequest(mSession, req);
		Assert.assertTrue(response instanceof ObjectUpdateResponse);
		
		ObjectUpdateResponse updateResponse = (ObjectUpdateResponse) response;
		Assert.assertEquals(ResponseStatus.Fail, updateResponse.getStatus());
		
		Object result = updateResponse.getResults();
		Assert.assertNull(result);
		
		this.getPersistenceService().commitTenantTransaction();

	}
	
	@Test
	public final void testBogusCheId() {
		this.getPersistenceService().beginTenantTransaction();

		String description1 = "che description";
		String description2 = "changed che description";
		
		//setupDaos();

		Organization organization = new Organization();
		organization.setOrganizationId("CTEST1.O1");
		Organization.DAO.store(organization);

		Facility facility = organization.createFacility("F1", "facf1", Point.getZeroPoint());
		Facility.DAO.store(facility);		
		
		CodeshelfNetwork network = facility.createNetwork("N1");
		CodeshelfNetwork.DAO.store(network);
		organization.createDefaultSiteControllerUser(network); 
		
		Che che = new Che();
		che.setDescription(description1);
		che.setParent(network);
		che.setDomainId("C1");
		Che.DAO.store(che);
		
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

		this.getPersistenceService().commitTenantTransaction();

	}
	
	
	@Test
	public final void testInvalidClass() {	
		UserSession session = Mockito.mock(UserSession.class);
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
		this.getPersistenceService().beginTenantTransaction();

		String description1 = "che description";
		String description2 = "changed che description";

		//setupDaos();

		Organization organization = new Organization();
		organization.setOrganizationId("CTEST1.O1");
		Organization.DAO.store(organization);

		Facility facility = organization.createFacility("F1", "facf1", Point.getZeroPoint());
		Facility.DAO.store(facility);		
		
		CodeshelfNetwork network = facility.createNetwork("N1");
		CodeshelfNetwork.DAO.store(network);
		organization.createDefaultSiteControllerUser(network); 

		
		Che che = new Che();
		che.setDescription(description1);
		che.setParent(network);
		che.setDomainId("C1");
		Che.DAO.store(che);
		
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
		
		this.getPersistenceService().commitTenantTransaction();
	}
	
	@Test
	public void cheUpdateFromUISuccess() {
		this.getPersistenceService().beginTenantTransaction();
		Che che = createTestChe("0x00000002");
		UiUpdateService service = new UiUpdateService();
		service.updateCheEdits(che.getPersistentId().toString(),"Test Device", "Updated Description", "orange", "0x00000099", "SETUP_ORDERS");
		java.util.UUID cheid = che.getPersistentId();
		this.getPersistenceService().commitTenantTransaction();

		this.getPersistenceService().beginTenantTransaction();
		che = Che.DAO.findByPersistentId(cheid);
		Assert.assertEquals(che.getDomainId(), "Test Device");
		Assert.assertEquals(che.getDescription(), "Updated Description");
		Assert.assertEquals(che.getColor(), ColorEnum.ORANGE);
		Assert.assertEquals(che.getDeviceGuidStr(), "0x00000099");
		Assert.assertEquals(che.getProcessMode(), ProcessMode.SETUP_ORDERS);
		this.getPersistenceService().commitTenantTransaction();
	}
	
	@Test
	public void cheUpdateFromUIFail() {
		this.getPersistenceService().beginTenantTransaction();
		Che che = createTestChe("0x00000003");
		UiUpdateService service = new UiUpdateService();
		String persistentId = che.getPersistentId().toString();
		//Update che successfully
		service.updateCheEdits(persistentId, "Test Device", "Description", "orange", "0x00000099", "SETUP_ORDERS");
		//Fail to update name
		service.updateCheEdits(persistentId, "", "Description", "orange", "0x00000099", "SETUP_ORDERS");
		Assert.assertEquals(che.getDomainId(), "Test Device");
		//Fail to update description
		service.updateCheEdits(persistentId, "Test Device", null, "orange", "0x00000099", "SETUP_ORDERS");
		Assert.assertEquals(che.getDescription(), "Description");
		//Fail to update color
		service.updateCheEdits(persistentId, "Test Device", "Description", "yellow", "0x00000099", "SETUP_ORDERS");
		Assert.assertEquals(che.getColor(), ColorEnum.ORANGE);
		//Fail to update controller id
		service.updateCheEdits(persistentId, "Test Device", "Description", "orange", "0x00000099x", "SETUP_ORDERS");
		Assert.assertEquals(che.getDeviceGuidStr(), "0x00000099");
		//Fail to update process mode
		service.updateCheEdits(persistentId, "Test Device Changed", "Description", "orange", "0x00000099x", "SETUP_ORDERSX");
		Assert.assertEquals(che.getDomainId(), "Test Device");

		this.getPersistenceService().commitTenantTransaction();
	}
	
	@Test
	public void getDefaultProcessMode() {
		this.getPersistenceService().beginTenantTransaction();
		UiUpdateService service = new UiUpdateService();
		Organization organization = new Organization();
		organization.setOrganizationId("CTEST1.O1");
		Organization.DAO.store(organization);
		Facility facility = organization.createFacility("F1", "facf1", Point.getZeroPoint());
		CodeshelfNetwork network = facility.createNetwork("WITEST");
		Che che = network.createChe("0x00000004", new NetGuid("0x00000004"));

		//Get default mode in a facility without aisles
		ProcessMode processMode = service.getDefaultProcessMode(che.getPersistentId().toString());
		Assert.assertEquals("Expected Line_Scan as default process mode in a facility with no aisles", processMode, ProcessMode.LINE_SCAN);
		
		//Get default mode in a facility with aisles
		Aisle aisle = facility.createAisle("A1", Point.getZeroPoint(), Point.getZeroPoint().add(5.0, 0.0));
		Aisle.DAO.store(aisle);
		processMode = service.getDefaultProcessMode(che.getPersistentId().toString());
		Assert.assertEquals("Expected Setup_Orers as default process mode in a facility with aisles", processMode, ProcessMode.SETUP_ORDERS);
		this.getPersistenceService().commitTenantTransaction();
	}
	
	private Che createTestChe(String netGuid){
		Organization organization = new Organization();
		organization.setOrganizationId("CTEST1.O1");
		Organization.DAO.store(organization);
		Facility facility = organization.createFacility("F1", "facf1", Point.getZeroPoint());
		CodeshelfNetwork network = facility.createNetwork("WITEST");
		Che che = network.createChe(netGuid, new NetGuid(netGuid));
		return che;
	}

}
