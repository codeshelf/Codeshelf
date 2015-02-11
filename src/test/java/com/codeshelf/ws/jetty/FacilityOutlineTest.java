package com.codeshelf.ws.jetty;

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

import com.codeshelf.model.PositionTypeEnum;
import com.codeshelf.model.domain.DomainTestABC;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.Point;
import com.codeshelf.service.ServiceFactory;
import com.codeshelf.util.ConverterProvider;
import com.codeshelf.ws.jetty.io.JsonEncoder;
import com.codeshelf.ws.jetty.protocol.command.ArgsClass;
import com.codeshelf.ws.jetty.protocol.request.ObjectMethodRequest;
import com.codeshelf.ws.jetty.protocol.request.ObjectUpdateRequest;
import com.codeshelf.ws.jetty.protocol.response.ObjectMethodResponse;
import com.codeshelf.ws.jetty.protocol.response.ObjectUpdateResponse;
import com.codeshelf.ws.jetty.protocol.response.ResponseABC;
import com.codeshelf.ws.jetty.protocol.response.ResponseStatus;
import com.codeshelf.ws.jetty.server.ServerMessageProcessor;
import com.codeshelf.ws.jetty.server.UserSession;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@RunWith(MockitoJUnitRunner.class)
public class FacilityOutlineTest extends DomainTestABC {

	@Test
	public final void testCreateVertex() {
		this.getTenantPersistenceService().beginTransaction();

		Facility facility = Facility.createFacility(getDefaultTenant(),"F1", "facf1",Point.getZeroPoint());
		this.mFacilityDao.store(facility);
		
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
		
		ServerMessageProcessor processor = new ServerMessageProcessor(Mockito.mock(ServiceFactory.class), new ConverterProvider().get());
		ResponseABC response = processor.handleRequest(Mockito.mock(UserSession.class), request);
		Assert.assertTrue(response instanceof ObjectMethodResponse);
		
		ObjectMethodResponse updateResponse = (ObjectMethodResponse) response;
		Assert.assertEquals(ResponseStatus.Success, updateResponse.getStatus());
		
		this.getTenantPersistenceService().commitTransaction();
	}
	
	@Test
	public void testUpdateVertex() throws JsonParseException, JsonMappingException, IOException {
		this.getTenantPersistenceService().beginTransaction();

		Facility facility = Facility.createFacility(getDefaultTenant(),"F1", "facf1",Point.getZeroPoint());
		
		/*
		var anchorPoint = {'posTypeEnum': 'GPS', 'x': event.latLng.lng(), 'y': event.latLng.lat(), 'z' : 0.0};
		/*
		var data = {
			'className': domainobjects['Facility']['className'],
			'persistentId': facility_['persistentId'],
			'properties': [
				{'name': 'anchorPoint', 'value': anchorPoint, 'classType': 'com.codeshelf.model.domain.Point'}
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
		
		ServerMessageProcessor processor = new ServerMessageProcessor(Mockito.mock(ServiceFactory.class), new ConverterProvider().get());
		ResponseABC response = processor.handleRequest(Mockito.mock(UserSession.class), request);

		Assert.assertTrue(response instanceof ObjectUpdateResponse);
		Assert.assertEquals(ResponseStatus.Success, response.getStatus());
		
		this.getTenantPersistenceService().commitTransaction();
	}
}