package com.gadgetworks.codeshelf.ws.jetty;

import java.util.HashMap;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import com.eaio.uuid.UUID;
import com.gadgetworks.codeshelf.model.dao.DAOTestABC;
import com.gadgetworks.codeshelf.model.dao.ITypedDao;
import com.gadgetworks.codeshelf.model.dao.MockDaoProvider;
import com.gadgetworks.codeshelf.model.domain.Che;
import com.gadgetworks.codeshelf.model.domain.CodeshelfNetwork;
import com.gadgetworks.codeshelf.model.domain.DropboxService;
import com.gadgetworks.codeshelf.model.domain.Facility;
import com.gadgetworks.codeshelf.model.domain.IronMqService;
import com.gadgetworks.codeshelf.model.domain.Organization;
import com.gadgetworks.codeshelf.model.domain.Point;
import com.gadgetworks.codeshelf.model.domain.Vertex;
import com.gadgetworks.codeshelf.ws.jetty.protocol.request.ObjectUpdateRequest;
import com.gadgetworks.codeshelf.ws.jetty.protocol.response.ObjectUpdateResponse;
import com.gadgetworks.codeshelf.ws.jetty.protocol.response.ResponseABC;
import com.gadgetworks.codeshelf.ws.jetty.protocol.response.ResponseStatus;
import com.gadgetworks.codeshelf.ws.jetty.server.ServerMessageProcessor;
import com.gadgetworks.codeshelf.ws.jetty.server.UserSession;

// example che update message:
// "ObjectUpdateRequest":{"className":"Che","persistentId":"66575760-00b8-11e4-ba3a-48d705ccef0f","properties":{"description":"1123"},"messageId":"cid_6"}

@RunWith(MockitoJUnitRunner.class)
public class CreateCheTest extends DAOTestABC {
	MockDaoProvider mDaoProvider;
	UserSession mSession;
	
	@Test
	public final void testUpdateCheOK() {

		String description1 = "che description";
		String description2 = "changed che description";
		
		setupDaos();

		Organization organization = new Organization();
		organization.setOrganizationId("CTEST1.O1");
		Organization.DAO.store(organization);
		
		Facility facility = organization.createFacility("F1", "facilityf1", Point.getZeroPoint());
		Facility.DAO.store(facility);		
		
		CodeshelfNetwork network = facility.createNetwork("N1");
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
		
		ServerMessageProcessor processor = new ServerMessageProcessor(mDaoProvider);
		ResponseABC response = processor.handleRequest(mSession, req);

		Assert.assertTrue(response instanceof ObjectUpdateResponse);
		
		ObjectUpdateResponse updateResponse = (ObjectUpdateResponse) response;
		Assert.assertEquals(ResponseStatus.Success, updateResponse.getStatus());
		
		Object result = updateResponse.getResults();
		Assert.assertTrue(result instanceof Che);
		
		Che changedChe = (Che) result;
		Assert.assertEquals(description2, changedChe.getDescription());
	}
	
	@Test
	public final void testCheNotFound() {

		String description1 = "che description";
		String description2 = "changed che description";

		setupDaos();

		Organization organization = new Organization();
		organization.setOrganizationId("CTEST1.O1");
		Organization.DAO.store(organization);

		Facility facility = organization.createFacility("F1", "facf1",Point.getZeroPoint());

		Facility.DAO.store(facility);		
		
		CodeshelfNetwork network = facility.createNetwork("N1");
		CodeshelfNetwork.DAO.store(network);
		
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
		
		ServerMessageProcessor processor = new ServerMessageProcessor(mDaoProvider);

		ResponseABC response = processor.handleRequest(mSession, req);
		Assert.assertTrue(response instanceof ObjectUpdateResponse);
		
		ObjectUpdateResponse updateResponse = (ObjectUpdateResponse) response;
		Assert.assertEquals(ResponseStatus.Fail, updateResponse.getStatus());
		
		Object result = updateResponse.getResults();
		Assert.assertNull(result);
	}
	
	@Test
	public final void testBogusCheId() {
		String description1 = "che description";
		String description2 = "changed che description";
		
		setupDaos();

		Organization organization = new Organization();
		organization.setOrganizationId("CTEST1.O1");
		Organization.DAO.store(organization);

		Facility facility = organization.createFacility("F1", "facf1", Point.getZeroPoint());
		Facility.DAO.store(facility);		
		
		CodeshelfNetwork network = facility.createNetwork("N1");
		CodeshelfNetwork.DAO.store(network);
		
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
		
		ServerMessageProcessor processor = new ServerMessageProcessor(mDaoProvider);

		ResponseABC response = processor.handleRequest(mSession, req);
		Assert.assertTrue(response instanceof ObjectUpdateResponse);
		
		ObjectUpdateResponse updateResponse = (ObjectUpdateResponse) response;
		Assert.assertEquals(ResponseStatus.Fail, updateResponse.getStatus());
		
		Object result = updateResponse.getResults();
		Assert.assertNull(result);		
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
		
		ServerMessageProcessor processor = new ServerMessageProcessor(mDaoProvider);

		ResponseABC response = processor.handleRequest(session, req);
		Assert.assertTrue(response instanceof ObjectUpdateResponse);
		
		ObjectUpdateResponse updateResponse = (ObjectUpdateResponse) response;
		Assert.assertEquals(ResponseStatus.Fail, updateResponse.getStatus());
		
		Object result = updateResponse.getResults();
		Assert.assertNull(result);		
	}
	
	
	@Test
	public final void testUndefinedCheId() {
		String description1 = "che description";
		String description2 = "changed che description";

		setupDaos();

		Organization organization = new Organization();
		organization.setOrganizationId("CTEST1.O1");
		Organization.DAO.store(organization);

		Facility facility = organization.createFacility("F1", "facf1", Point.getZeroPoint());
		Facility.DAO.store(facility);		
		
		CodeshelfNetwork network = facility.createNetwork("N1");
		CodeshelfNetwork.DAO.store(network);
		
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
		
		ServerMessageProcessor processor = new ServerMessageProcessor(mDaoProvider);

		ResponseABC response = processor.handleRequest(mSession, req);
		Assert.assertTrue(response instanceof ObjectUpdateResponse);
		
		ObjectUpdateResponse updateResponse = (ObjectUpdateResponse) response;
		Assert.assertEquals(ResponseStatus.Fail, updateResponse.getStatus());
		
		Object result = updateResponse.getResults();
		Assert.assertNull(result);		
	}

	private void setupDaos() {
		mDaoProvider = new MockDaoProvider();
		
		mSession = Mockito.mock(UserSession.class);
		mSession.setSessionId("test-session");
		
		Organization.setDao(mDaoProvider.getDaoInstance(Organization.class));
		Facility.setDao(mDaoProvider.getDaoInstance(Facility.class));
		CodeshelfNetwork.setDao(mDaoProvider.getDaoInstance(CodeshelfNetwork.class));
		Che.setDao(mDaoProvider.getDaoInstance(Che.class));
		Vertex.setDao(mDaoProvider.getDaoInstance(Vertex.class));
		DropboxService.setDao(mDaoProvider.getDaoInstance(DropboxService.class));
		IronMqService.setDao(mDaoProvider.getDaoInstance(IronMqService.class));
		CodeshelfNetwork.setDao(mDaoProvider.getDaoInstance(CodeshelfNetwork.class));
	}	

}
