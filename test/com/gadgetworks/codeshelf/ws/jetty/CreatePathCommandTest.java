package com.gadgetworks.codeshelf.ws.jetty;

import static com.natpryce.makeiteasy.MakeItEasy.a;
import static com.natpryce.makeiteasy.MakeItEasy.make;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

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
import com.gadgetworks.codeshelf.model.PositionTypeEnum;
import com.gadgetworks.codeshelf.model.dao.DAOTestABC;
import com.gadgetworks.codeshelf.model.dao.IDaoProvider;
import com.gadgetworks.codeshelf.model.domain.DAOMaker;
import com.gadgetworks.codeshelf.model.domain.Facility;
import com.gadgetworks.codeshelf.model.domain.Path;
import com.gadgetworks.codeshelf.model.domain.Path.PathDao;
import com.gadgetworks.codeshelf.model.domain.PathSegment;
import com.gadgetworks.codeshelf.model.domain.PathSegment.PathSegmentDao;
import com.gadgetworks.codeshelf.model.domain.WorkArea;
import com.gadgetworks.codeshelf.model.domain.WorkArea.WorkAreaDao;
import com.gadgetworks.codeshelf.platform.persistence.PersistenceService;
import com.gadgetworks.codeshelf.ws.jetty.protocol.command.ArgsClass;
import com.gadgetworks.codeshelf.ws.jetty.protocol.request.CreatePathRequest;
import com.gadgetworks.codeshelf.ws.jetty.protocol.request.ObjectMethodRequest;
import com.gadgetworks.codeshelf.ws.jetty.protocol.response.CreatePathResponse;
import com.gadgetworks.codeshelf.ws.jetty.protocol.response.ObjectMethodResponse;
import com.gadgetworks.codeshelf.ws.jetty.protocol.response.ResponseABC;
import com.gadgetworks.codeshelf.ws.jetty.server.ServerMessageProcessor;
import com.gadgetworks.codeshelf.ws.jetty.server.UserSession;
import com.natpryce.makeiteasy.Maker;

@RunWith(MockitoJUnitRunner.class)
public class CreatePathCommandTest extends DAOTestABC {

	PersistenceService persistenceService = new PersistenceService();

	@Mock
	private IDaoProvider mockDaoProvider;

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
		int numberOfSegments = 3;
		String testPathDomainId = "DOMID-2";
		
		DAOMaker maker = new DAOMaker(persistenceService);
		
		@SuppressWarnings("unchecked")
		Maker<Facility> fm = a(maker.TestFacility);
		Facility testFacility = make(fm);
		
		Path.DAO = new PathDao(persistenceService);
		PathSegment.DAO = new PathSegmentDao(persistenceService);
		WorkArea.DAO = new WorkAreaDao(persistenceService);
	
		Path noPath = Path.DAO.findByDomainId(testFacility, testPathDomainId);
		Assert.assertNull(noPath);
		
		when(mockDaoProvider.getDaoInstance(Facility.class)).thenReturn(Facility.DAO);		
		
		PathSegment[] segments = createPathSegment(numberOfSegments);
		
		CreatePathRequest request = new CreatePathRequest();
		request.setDomainId(testPathDomainId);
		request.setFacilityId(testFacility.getPersistentId().toString());
		request.setPathSegments(segments);
		
		ObjectMapper mapper = new ObjectMapper();
		String jsonString = "";
		jsonString = mapper.writeValueAsString(request);
		System.out.println(jsonString);
		
		UserSession session = Mockito.mock(UserSession.class);
		session.setSessionId("test-session");
		
		ServerMessageProcessor processor = new ServerMessageProcessor(mockDaoProvider);
		ResponseABC response = processor.handleRequest(session, request);

		Assert.assertTrue(response instanceof CreatePathResponse);
	}
	
	@Test
	public void testCreatePathViaObjectMethod() throws JsonParseException, JsonMappingException, IOException {
		int numberOfSegments = 3;
		String testPathDomainId = "DOMID";
		
		DAOMaker maker = new DAOMaker(persistenceService);
		Facility testFacility = make(a(maker.TestFacility));
		Path.DAO = new PathDao(persistenceService);
		PathSegment.DAO = new PathSegmentDao(persistenceService);
		WorkArea.DAO = new WorkAreaDao(persistenceService);
	
		Path noPath = Path.DAO.findByDomainId(testFacility, testPathDomainId);
		Assert.assertNull(noPath);
		
		when(mockDaoProvider.getDaoInstance(Facility.class)).thenReturn(Facility.DAO);		
		
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

		
		ServerMessageProcessor processor = new ServerMessageProcessor(mockDaoProvider);

		ResponseABC response = processor.handleRequest(session, request);

		Assert.assertTrue(response instanceof ObjectMethodResponse);
		
		Path createdPath1 = Path.DAO.findByDomainId(testFacility, testPathDomainId);
		Assert.assertNotNull(createdPath1);
		Assert.assertEquals(numberOfSegments, createdPath1.getSegments().size());
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