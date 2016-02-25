package com.codeshelf.ws;


import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Executors;

import javax.websocket.Session;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.codeshelf.behavior.BehaviorFactory;
import com.codeshelf.filter.Filter;
import com.codeshelf.model.PositionTypeEnum;
import com.codeshelf.model.dao.ObjectChangeBroadcaster;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.Path;
import com.codeshelf.model.domain.PathSegment;
import com.codeshelf.persistence.TenantPersistenceService;
import com.codeshelf.testframework.HibernateTest;
import com.codeshelf.util.ConverterProvider;
import com.codeshelf.ws.protocol.command.ArgsClass;
import com.codeshelf.ws.protocol.request.CreatePathRequest;
import com.codeshelf.ws.protocol.request.ObjectMethodRequest;
import com.codeshelf.ws.protocol.response.CreatePathResponse;
import com.codeshelf.ws.protocol.response.ObjectMethodResponse;
import com.codeshelf.ws.protocol.response.ResponseABC;
import com.codeshelf.ws.server.ServerMessageProcessor;
import com.codeshelf.ws.server.WebSocketConnection;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@RunWith(MockitoJUnitRunner.class)
public class CreatePathCommandTest extends HibernateTest {

	@Mock
	private BehaviorFactory mockServiceFactory;

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
		this.getTenantPersistenceService().beginTransaction();

		int numberOfSegments = 3;
		String testPathDomainId = "DOMID-2";
		
		Facility testFacility = this.createFacility();

		ObjectChangeBroadcaster objectChangeBroadcaster = this.getTenantPersistenceService().getEventListenerIntegrator().getChangeBroadcaster();
		Session websocketSession = mock(Session.class);
		WebSocketConnection viewSession = new WebSocketConnection(websocketSession, Executors.newSingleThreadExecutor(),null);

		/* register a filter like the UI does */
		viewSession.registerObjectEventListener(new Filter(TenantPersistenceService.getInstance().getDao(PathSegment.class), PathSegment.class, "ID1"));
		objectChangeBroadcaster.registerDAOListener(getDefaultTenantId(),viewSession,  PathSegment.class);
		
		Path noPath = Path.staticGetDao().findByDomainId(testFacility, testPathDomainId);
		Assert.assertNull(noPath);
		
		PathSegment[] segments = createPathSegment(numberOfSegments);
		
		CreatePathRequest request = new CreatePathRequest();
		request.setDomainId(testPathDomainId);
		request.setFacilityId(testFacility.getPersistentId().toString());
		request.setPathSegments(segments);
		
		ServerMessageProcessor processor = new ServerMessageProcessor(mockServiceFactory, new ConverterProvider().get(), this.webSocketManagerService);
		this.getTenantPersistenceService().commitTransaction();

		ResponseABC response = processor.handleRequest(this.getMockWsConnection(), request);
		Assert.assertTrue(response instanceof CreatePathResponse);
		objectChangeBroadcaster.unregisterDAOListener(getDefaultTenantId(),viewSession);

	}
	
	@Test
	public void testCreatePathViaObjectMethod() throws JsonParseException, JsonMappingException, IOException {
		this.getTenantPersistenceService().beginTransaction();

		int numberOfSegments = 3;
		String testPathDomainId = "DOMID";
		
		Facility testFacility = this.createFacility();
	
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
		
		ServerMessageProcessor processor = new ServerMessageProcessor(mockServiceFactory, new ConverterProvider().get(), this.webSocketManagerService);

		ResponseABC response = processor.handleRequest(this.getMockWsConnection(), request);
		Assert.assertTrue(response instanceof ObjectMethodResponse);
		testFacility = testFacility.reload();
		List<Path> pathList = testFacility.getPaths();
		Path createdPath1 = pathList.get(0);
		// Why is facility F1? Passed in DOMID above.
		Assert.assertEquals(testFacility.getDomainId()+".1", createdPath1.getDomainId());	
		
		Assert.assertEquals(numberOfSegments, createdPath1.getSegments().size());
		this.getTenantPersistenceService().commitTransaction();

	}
	
	private PathSegment[] createPathSegment(int numberOfSegments) {
		PathSegment[] segments = new PathSegment[numberOfSegments];
		for (int i = 0; i < numberOfSegments; i++) {
			double di = i;
			PathSegment segment = new PathSegment();
			segment.setDomainId("P."+i);
			segment.setSegmentOrder(i);
			segment.setPosType(PositionTypeEnum.METERS_FROM_PARENT);
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