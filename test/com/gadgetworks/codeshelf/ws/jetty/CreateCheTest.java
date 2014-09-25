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
import com.gadgetworks.codeshelf.model.domain.Facility;
import com.gadgetworks.codeshelf.model.domain.Organization;
import com.gadgetworks.codeshelf.model.domain.Point;
import com.gadgetworks.codeshelf.model.domain.Vertex;
import com.gadgetworks.codeshelf.ws.jetty.protocol.request.ObjectUpdateRequest;
import com.gadgetworks.codeshelf.ws.jetty.protocol.response.ObjectUpdateResponse;
import com.gadgetworks.codeshelf.ws.jetty.protocol.response.ResponseABC;
import com.gadgetworks.codeshelf.ws.jetty.protocol.response.ResponseStatus;
import com.gadgetworks.codeshelf.ws.jetty.server.UserSession;
import com.gadgetworks.codeshelf.ws.jetty.server.ServerMessageProcessor;

// example che update message:
// "ObjectUpdateRequest":{"className":"Che","persistentId":"66575760-00b8-11e4-ba3a-48d705ccef0f","properties":{"description":"1123"},"messageId":"cid_6"}

@RunWith(MockitoJUnitRunner.class)
public class CreateCheTest extends DAOTestABC {

	@Test
	public final void testUpdateCheOK() {

		String description1 = "che description";
		String description2 = "changed che description";
		
		MockDaoProvider daoProvider = new MockDaoProvider();
		
		UserSession session = Mockito.mock(UserSession.class);
		session.setSessionId("test-session");
	
		ITypedDao<Organization> orgDao = daoProvider.getDaoInstance(Organization.class);
		ITypedDao<Facility> facDao = daoProvider.getDaoInstance(Facility.class);
		ITypedDao<CodeshelfNetwork> netDao = daoProvider.getDaoInstance(CodeshelfNetwork.class);
		ITypedDao<Che> cheDao = daoProvider.getDaoInstance(Che.class);

		Organization organization = new Organization();
		organization.setOrganizationId("CTEST1.O1");
		orgDao.store(organization);

		Facility facility = new Facility(organization, "F1", Point.getZeroPoint());
		facDao.store(facility);		
		
		CodeshelfNetwork network = new CodeshelfNetwork(facility, "N1", "foo");
		netDao.store(network);
		
		Che che = new Che();
		che.setDescription(description1);
		che.setParent(network);
		che.setDomainId("C1");
		cheDao.store(che);
		
		ObjectUpdateRequest req = new ObjectUpdateRequest();
		req.setClassName("Che");
		req.setPersistentId(che.getPersistentId().toString());
		
		HashMap<String,Object> properties = new HashMap<String,Object>();
		properties.put("description", description2);
		req.setProperties(properties);
		
		ServerMessageProcessor processor = new ServerMessageProcessor(daoProvider);
		ResponseABC response = processor.handleRequest(session, req);

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
		
		MockDaoProvider daoProvider = new MockDaoProvider();
		
		UserSession session = Mockito.mock(UserSession.class);
		session.setSessionId("test-session");

	
		ITypedDao<Organization> orgDao = daoProvider.getDaoInstance(Organization.class);
		ITypedDao<Facility> facDao = daoProvider.getDaoInstance(Facility.class);
		ITypedDao<CodeshelfNetwork> netDao = daoProvider.getDaoInstance(CodeshelfNetwork.class);
		ITypedDao<Che> cheDao = daoProvider.getDaoInstance(Che.class);
		ITypedDao<Vertex> vertexDao = daoProvider.getDaoInstance(Vertex.class);

		Vertex.DAO = vertexDao;

		Organization organization = new Organization();
		organization.setOrganizationId("CTEST1.O1");
		orgDao.store(organization);

		Facility facility = new Facility(organization, "F1", Point.getZeroPoint());

		facDao.store(facility);		
		
		CodeshelfNetwork network = new CodeshelfNetwork(facility, "N1", "foo");
		netDao.store(network);
		
		Che che = new Che();
		che.setDescription(description1);
		che.setParent(network);
		che.setDomainId("C1");
		cheDao.store(che);
		
		ObjectUpdateRequest req = new ObjectUpdateRequest();
		req.setClassName("Che");
		req.setPersistentId(new UUID().toString());
		
		HashMap<String,Object> properties = new HashMap<String,Object>();
		properties.put("description", description2);
		req.setProperties(properties);
		
		ServerMessageProcessor processor = new ServerMessageProcessor(daoProvider);

		ResponseABC response = processor.handleRequest(session, req);
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
		
		MockDaoProvider daoProvider = new MockDaoProvider();
		
		UserSession session = Mockito.mock(UserSession.class);
		session.setSessionId("test-session");

	
		ITypedDao<Organization> orgDao = daoProvider.getDaoInstance(Organization.class);
		ITypedDao<Facility> facDao = daoProvider.getDaoInstance(Facility.class);
		ITypedDao<CodeshelfNetwork> netDao = daoProvider.getDaoInstance(CodeshelfNetwork.class);
		ITypedDao<Che> cheDao = daoProvider.getDaoInstance(Che.class);

		Organization organization = new Organization();
		organization.setOrganizationId("CTEST1.O1");
		orgDao.store(organization);

		Facility facility = new Facility(organization, "F1", Point.getZeroPoint());
		facDao.store(facility);		
		
		CodeshelfNetwork network = new CodeshelfNetwork(facility, "N1", "foo");
		netDao.store(network);
		
		Che che = new Che();
		che.setDescription(description1);
		che.setParent(network);
		che.setDomainId("C1");
		cheDao.store(che);
		
		ObjectUpdateRequest req = new ObjectUpdateRequest();
		req.setClassName("Che");
		req.setPersistentId("bogus-id");
		
		HashMap<String,Object> properties = new HashMap<String,Object>();
		properties.put("description", description2);
		req.setProperties(properties);
		
		ServerMessageProcessor processor = new ServerMessageProcessor(daoProvider);

		ResponseABC response = processor.handleRequest(session, req);
		Assert.assertTrue(response instanceof ObjectUpdateResponse);
		
		ObjectUpdateResponse updateResponse = (ObjectUpdateResponse) response;
		Assert.assertEquals(ResponseStatus.Fail, updateResponse.getStatus());
		
		Object result = updateResponse.getResults();
		Assert.assertNull(result);		
	}
	
	
	@Test
	public final void testInvalidClass() {	
		MockDaoProvider daoProvider = new MockDaoProvider();
		
		UserSession session = Mockito.mock(UserSession.class);
		session.setSessionId("test-session");

	
		ObjectUpdateRequest req = new ObjectUpdateRequest();
		req.setClassName("Foobar");
		req.setPersistentId("bogus-id");
		
		HashMap<String,Object> properties = new HashMap<String,Object>();
		properties.put("foo", "bar");
		req.setProperties(properties);
		
		ServerMessageProcessor processor = new ServerMessageProcessor(daoProvider);

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
		
		MockDaoProvider daoProvider = new MockDaoProvider();
		
		UserSession session = Mockito.mock(UserSession.class);
		session.setSessionId("test-session");
	
		ITypedDao<Organization> orgDao = daoProvider.getDaoInstance(Organization.class);
		ITypedDao<Facility> facDao = daoProvider.getDaoInstance(Facility.class);
		ITypedDao<CodeshelfNetwork> netDao = daoProvider.getDaoInstance(CodeshelfNetwork.class);
		ITypedDao<Che> cheDao = daoProvider.getDaoInstance(Che.class);

		Organization organization = new Organization();
		organization.setOrganizationId("CTEST1.O1");
		orgDao.store(organization);

		Facility facility = new Facility(organization, "F1", Point.getZeroPoint());
		facDao.store(facility);		
		
		CodeshelfNetwork network = new CodeshelfNetwork(facility, "N1", "foo");
		netDao.store(network);
		
		Che che = new Che();
		che.setDescription(description1);
		che.setParent(network);
		che.setDomainId("C1");
		cheDao.store(che);
		
		ObjectUpdateRequest req = new ObjectUpdateRequest();
		req.setClassName("Che");
		req.setPersistentId(null);
		
		HashMap<String,Object> properties = new HashMap<String,Object>();
		properties.put("description", description2);
		req.setProperties(properties);
		
		ServerMessageProcessor processor = new ServerMessageProcessor(daoProvider);

		ResponseABC response = processor.handleRequest(session, req);
		Assert.assertTrue(response instanceof ObjectUpdateResponse);
		
		ObjectUpdateResponse updateResponse = (ObjectUpdateResponse) response;
		Assert.assertEquals(ResponseStatus.Fail, updateResponse.getStatus());
		
		Object result = updateResponse.getResults();
		Assert.assertNull(result);		
	}	

}
