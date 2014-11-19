package com.gadgetworks.codeshelf.ws.jetty;


import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import javax.persistence.PostPersist;
import javax.persistence.PostUpdate;
import javax.persistence.PrePersist;
import javax.websocket.Session;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gadgetworks.codeshelf.filter.Listener;
import com.gadgetworks.codeshelf.model.PositionTypeEnum;
import com.gadgetworks.codeshelf.model.dao.ObjectChangeBroadcaster;
import com.gadgetworks.codeshelf.model.domain.DomainTestABC;
import com.gadgetworks.codeshelf.model.domain.Facility;
import com.gadgetworks.codeshelf.model.domain.Path;
import com.gadgetworks.codeshelf.model.domain.PathSegment;
import com.gadgetworks.codeshelf.platform.persistence.PersistenceService;
import com.gadgetworks.codeshelf.service.ServiceFactory;
import com.gadgetworks.codeshelf.ws.jetty.protocol.command.ArgsClass;
import com.gadgetworks.codeshelf.ws.jetty.protocol.request.CreatePathRequest;
import com.gadgetworks.codeshelf.ws.jetty.protocol.request.ObjectMethodRequest;
import com.gadgetworks.codeshelf.ws.jetty.protocol.response.CreatePathResponse;
import com.gadgetworks.codeshelf.ws.jetty.protocol.response.ObjectMethodResponse;
import com.gadgetworks.codeshelf.ws.jetty.protocol.response.ResponseABC;
import com.gadgetworks.codeshelf.ws.jetty.server.ServerMessageProcessor;
import com.gadgetworks.codeshelf.ws.jetty.server.UserSession;

@RunWith(MockitoJUnitRunner.class)
public class CreatePathCommandTest extends DomainTestABC {

	PersistenceService persistenceService = PersistenceService.getInstance();
	
	@Mock
	private ServiceFactory mockServiceFactory;

	@Test
	public void testPojo() throws JsonGenerationException, JsonMappingException, IOException {
		JsonPojo pojo = new JsonPojo();
		ObjectMapper mapper = new ObjectMapper();
		String jsonString = "";
		jsonString = mapper.writeValueAsString(pojo);
		System.out.println(jsonString);
	}
	
	@Test
	public void testCreatePathWithCommand() throws JsonGenerationException, JsonMappingException, IOException {
		this.getPersistenceService().beginTenantTransaction();

		int numberOfSegments = 3;
		String testPathDomainId = "DOMID-2";
		
		Facility testFacility = this.createDefaultFacility(testPathDomainId);

		ObjectChangeBroadcaster objectChangeBroadcaster = this.getPersistenceService().getObjectChangeBroadcaster();
		Session websocketSession = mock(Session.class);
		UserSession viewSession = new UserSession(websocketSession);

		try {
			/* register a filter like the UI does */
			viewSession.registerObjectEventListener(new Listener(PathSegment.class, "ID1"));
			objectChangeBroadcaster.registerDAOListener(viewSession,  PathSegment.class);
			
			
			
			Path noPath = Path.DAO.findByDomainId(testFacility, testPathDomainId);
			Assert.assertNull(noPath);
			
			PathSegment[] segments = createPathSegment(numberOfSegments);
			
			CreatePathRequest request = new CreatePathRequest();
			request.setDomainId(testPathDomainId);
			request.setFacilityId(testFacility.getPersistentId().toString());
			request.setPathSegments(segments);
			
			UserSession requestSession = new UserSession(mock(Session.class));
			requestSession.setSessionId("test-session");
			
			ServerMessageProcessor processor = new ServerMessageProcessor(mockServiceFactory);
			ResponseABC response = processor.handleRequest(requestSession, request);
			Assert.assertTrue(response instanceof CreatePathResponse);
		}
		finally {
			objectChangeBroadcaster.unregisterDAOListener(viewSession);
			this.getPersistenceService().endTenantTransaction();
		}

	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void testCreatePathViaObjectMethod() throws JsonParseException, JsonMappingException, IOException {
		this.getPersistenceService().beginTenantTransaction();

		int numberOfSegments = 3;
		String testPathDomainId = "DOMID";
		
		Facility testFacility = this.createDefaultFacility(testPathDomainId);
	
		Path noPath = Path.DAO.findByDomainId(testFacility, testPathDomainId);
		Assert.assertNull(noPath);
		
		PathSegment[] segments = createPathSegment(numberOfSegments);
		
		List<ArgsClass> args = new LinkedList<ArgsClass>();
		ArgsClass arg = new ArgsClass("pathDomainId", testPathDomainId, String.class.getName());
		args.add(arg);
		ArgsClass arg2 = new ArgsClass("segments", segments, PathSegment[].class.getName());
		args.add(arg2);
		
		ObjectMethodRequest request = new ObjectMethodRequest();
		request.setClassName("Facility");
		request.setPersistentId(testFacility.getPersistentId().toString());
		request.setMethodName("createPath");
		request.setMethodArgs(args);
		
		UserSession session = Mockito.mock(UserSession.class);
		session.setSessionId("test-session");

		
		ServerMessageProcessor processor = new ServerMessageProcessor(mockServiceFactory);

		ResponseABC response = processor.handleRequest(session, request);

		Assert.assertTrue(response instanceof ObjectMethodResponse);
		
		Path createdPath1 = Path.DAO.findByDomainId(testFacility, testPathDomainId);
		Assert.assertNotNull(createdPath1);
		Assert.assertEquals(numberOfSegments, createdPath1.getSegments().size());

		this.getPersistenceService().endTenantTransaction();
	}
	
	private PathSegment[] createPathSegment(int numberOfSegments) {
		PathSegment[] segments = new PathSegment[numberOfSegments];
		for (int i = 0; i < numberOfSegments; i++) {
			double di = i;
			PathSegment segment = new PathSegment();
			segment.setDomainId("P."+i);
			segment.setSegmentOrder(i);
			segment.setPosTypeEnum(PositionTypeEnum.METERS_FROM_PARENT);
			segment.setStartPosX(di);
			segment.setStartPosY(di);
			segment.setStartPosZ(di);
			segment.setEndPosX(di);
			segment.setEndPosY(di);
			segment.setEndPosZ(di);			
			segments[i]=segment;
		}
		return segments;
	}
}