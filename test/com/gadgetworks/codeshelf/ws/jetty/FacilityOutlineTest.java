package com.gadgetworks.codeshelf.ws.jetty;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.websocket.EncodeException;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gadgetworks.codeshelf.model.PositionTypeEnum;
import com.gadgetworks.codeshelf.model.dao.ITypedDao;
import com.gadgetworks.codeshelf.model.dao.MockDaoProvider;
import com.gadgetworks.codeshelf.model.domain.Che;
import com.gadgetworks.codeshelf.model.domain.CodeshelfNetwork;
import com.gadgetworks.codeshelf.model.domain.ContainerKind;
import com.gadgetworks.codeshelf.model.domain.DropboxService;
import com.gadgetworks.codeshelf.model.domain.Facility;
import com.gadgetworks.codeshelf.model.domain.Organization;
import com.gadgetworks.codeshelf.model.domain.Point;
import com.gadgetworks.codeshelf.model.domain.Vertex;
import com.gadgetworks.codeshelf.ws.command.req.ArgsClass;
import com.gadgetworks.codeshelf.ws.jetty.io.JsonEncoder;
import com.gadgetworks.codeshelf.ws.jetty.protocol.request.ObjectMethodRequest;
import com.gadgetworks.codeshelf.ws.jetty.protocol.request.ObjectUpdateRequest;
import com.gadgetworks.codeshelf.ws.jetty.protocol.response.ObjectMethodResponse;
import com.gadgetworks.codeshelf.ws.jetty.protocol.response.ObjectUpdateResponse;
import com.gadgetworks.codeshelf.ws.jetty.protocol.response.ResponseABC;
import com.gadgetworks.codeshelf.ws.jetty.protocol.response.ResponseStatus;
import com.gadgetworks.codeshelf.ws.jetty.server.CsSession;
import com.gadgetworks.codeshelf.ws.jetty.server.ServerMessageProcessor;

@RunWith(MockitoJUnitRunner.class)
public class FacilityOutlineTest {

	@Test
	public final void testCreateVertex() {
		MockDaoProvider daoProvider = new MockDaoProvider();
		
		ITypedDao<Organization> orgDao = daoProvider.getDaoInstance(Organization.class);
		ITypedDao<Facility> facDao = daoProvider.getDaoInstance(Facility.class);
		ITypedDao<CodeshelfNetwork> netDao = daoProvider.getDaoInstance(CodeshelfNetwork.class);
		ITypedDao<Che> cheDao = daoProvider.getDaoInstance(Che.class);
		ITypedDao<DropboxService> dropboxDao = daoProvider.getDaoInstance(DropboxService.class);
		ITypedDao<ContainerKind> containerKindDao = daoProvider.getDaoInstance(ContainerKind.class);
		ITypedDao<Vertex> vertexDao = daoProvider.getDaoInstance(Vertex.class);
		Facility.DAO = facDao;
		DropboxService.DAO = dropboxDao;
		CodeshelfNetwork.DAO = netDao;
		ContainerKind.DAO = containerKindDao;
		Che.DAO = cheDao;
		Vertex.DAO = vertexDao;

		Organization organization = new Organization();
		organization.setOrganizationId("CTEST1.O1");
		orgDao.store(organization);

		Facility facility = new Facility(organization, "F1", Point.getZeroPoint());
		facDao.store(facility);
		
		ObjectMethodRequest request = new ObjectMethodRequest();
		request.setClassName("Facility");
		request.setPersistentId(facility.getPersistentId().toString());
		request.setMethodName("createVertex");
		
		List<ArgsClass> methodArgs = new LinkedList<ArgsClass>();

		/*
			{'name': 'domainId', 'value': 'V' + vertexNum, 'classType': 'java.lang.String'},
			{'name': 'PosTypeByStr', 'value': 'GPS', 'classType': 'java.lang.String'},
			{'name': 'anchorPosX', 'value': event.latLng.lng(), 'classType': 'java.lang.Double'},
			{'name': 'anchorPosY', 'value': event.latLng.lat(), 'classType': 'java.lang.Double'},
			{'name': 'drawOrder', 'value': vertexNum, 'classType': 'java.lang.Integer'}
		*/
		
		ArgsClass ac1 = new ArgsClass("domainId", "V1", String.class.getName());
		methodArgs.add(ac1);
		ArgsClass ac2 = new ArgsClass("PosTypeByStr", "GPS", String.class.getName());
		methodArgs.add(ac2);
		ArgsClass ac3 = new ArgsClass("anchorPosX", 1.23, Double.class.getName());
		methodArgs.add(ac3);
		ArgsClass ac4 = new ArgsClass("anchorPosY", 2.34, Double.class.getName());
		methodArgs.add(ac4);
		ArgsClass ac5 = new ArgsClass("drawOrder", 1, Integer.class.getName());
		methodArgs.add(ac5);
		request.setMethodArgs(methodArgs);
		
		JsonEncoder enc = new JsonEncoder();
		String msg="";
		try {
			msg = enc.encode(request);
			System.out.println(msg);
		} catch (EncodeException e) {
		}		
		
		ServerMessageProcessor processor = new ServerMessageProcessor(daoProvider);
		ResponseABC response = processor.handleRequest(Mockito.mock(CsSession.class), request);
		Assert.assertTrue(response instanceof ObjectMethodResponse);
		
		ObjectMethodResponse updateResponse = (ObjectMethodResponse) response;
		Assert.assertEquals(ResponseStatus.Success, updateResponse.getStatus());
	}
	
	@Test
	public void testUpdateVertex() throws JsonParseException, JsonMappingException, IOException {

		MockDaoProvider daoProvider = new MockDaoProvider();
		
		ITypedDao<Organization> orgDao = daoProvider.getDaoInstance(Organization.class);
		ITypedDao<Facility> facDao = daoProvider.getDaoInstance(Facility.class);
		ITypedDao<CodeshelfNetwork> netDao = daoProvider.getDaoInstance(CodeshelfNetwork.class);
		ITypedDao<Che> cheDao = daoProvider.getDaoInstance(Che.class);
		ITypedDao<DropboxService> dropboxDao = daoProvider.getDaoInstance(DropboxService.class);
		ITypedDao<ContainerKind> containerKindDao = daoProvider.getDaoInstance(ContainerKind.class);
		ITypedDao<Vertex> vertexDao = daoProvider.getDaoInstance(Vertex.class);
		
		Facility.DAO = facDao;
		DropboxService.DAO = dropboxDao;
		CodeshelfNetwork.DAO = netDao;
		ContainerKind.DAO = containerKindDao;
		Che.DAO = cheDao;
		Vertex.DAO = vertexDao;

		Organization organization = new Organization();
		organization.setOrganizationId("CTEST1.O1");
		orgDao.store(organization);

		Facility facility = new Facility(organization, "F1", Point.getZeroPoint());
		facDao.store(facility);	
		
		/*
		var anchorPoint = {'posTypeEnum': 'GPS', 'x': event.latLng.lng(), 'y': event.latLng.lat(), 'z' : 0.0};
		/*
		var data = {
			'className': domainobjects['Facility']['className'],
			'persistentId': facility_['persistentId'],
			'properties': [
				{'name': 'anchorPoint', 'value': anchorPoint, 'classType': 'com.gadgetworks.codeshelf.model.domain.Point'}
			]
		};
		 */
				
		Map<String,Object> properties = new HashMap<String, Object>();
		Point point = new Point(PositionTypeEnum.GPS,1d,2d,0d);
		properties.put("anchorPoint", point);
		
		ObjectUpdateRequest request = new ObjectUpdateRequest();
		request.setClassName("Facility");
		request.setPersistentId(facility.getPersistentId().toString());		
		request.setProperties(properties);
		
		ObjectMapper mapper = new ObjectMapper();
		String jsonString = "";
		try {
			jsonString = mapper.writeValueAsString(request);
			System.out.println(jsonString);
		} catch (Exception e) {
			Assert.fail("Failed to seriaize request");
		}
				
		ServerMessageProcessor processor = new ServerMessageProcessor(daoProvider);
		ResponseABC response = processor.handleRequest(Mockito.mock(CsSession.class), request);

		Assert.assertTrue(response instanceof ObjectUpdateResponse);
		Assert.assertEquals(ResponseStatus.Success, response.getStatus());
	}
}