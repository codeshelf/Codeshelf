package com.codeshelf.ws;

import java.util.LinkedList;
import java.util.List;

import javax.websocket.EncodeException;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import com.codeshelf.service.ServiceFactory;
import com.codeshelf.testframework.MockDaoTest;
import com.codeshelf.util.ConverterProvider;
import com.codeshelf.ws.io.JsonEncoder;
import com.codeshelf.ws.protocol.command.ArgsClass;
import com.codeshelf.ws.protocol.request.ObjectMethodRequest;
import com.codeshelf.ws.protocol.response.ObjectMethodResponse;
import com.codeshelf.ws.protocol.response.ResponseABC;
import com.codeshelf.ws.protocol.response.ResponseStatus;
import com.codeshelf.ws.server.ServerMessageProcessor;

@RunWith(MockitoJUnitRunner.class)
public class ObjectMethodCommandTest extends MockDaoTest {

	@Test
	// this should really be an integration test
	public final void testCreateFacilityOK() {

		getTenantPersistenceService().beginTransaction();
		
		// "ObjectMethodRequest":{"className":"Organization","persistentId":"77fdd850-2245-11e4-822c-48d705ccef0f","methodName":"createFacility",
		// "methodArgs":[{"name":"domainId","value":"F1","classType":"java.lang.String"},
		// {"name":"description","value":"First Facility","classType":"java.lang.String"},
		// {"name":"anchorPoint","value":{"posTypeEnum":"GPS","x":-122.27488799999999,"y":37.8013437,"z":0},"classType":"com.codeshelf.model.domain.Point"}],"messageId":"cid_6"}

		// {"ObjectMethodRequest":{"messageId":"ec34aa40-2250-11e4-9427-48d705ccef0f","className":"Organization","persistentId":"ec343510-2250-11e4-9427-48d705ccef0f","methodName":"createFacility",
		// "methodArgs":[{"name":"domainId","value":"F1","classType":"java.lang.String"},
		// {"name":"description","value":"First Facility","classType":"java.lang.String"},
		// {"name":"anchorPoint","value":{"posTypeEnum":"METERS_FROM_PARENT","x":0.0,"y":0.0,"z":0.0},"classType":"com.codeshelf.model.domain.Point"}]}}

		//Organization.setDao(orgDao);
		/*
		Facility.DAO = facDao;
		DropboxGateway.DAO = dropboxDao;
		IronMqGateway.DAO = ironMqDao;
		//EdiServiceABC.DAO = ediServiceABCDao;
		CodeshelfNetwork.DAO = netDao;
		ContainerKind.DAO = containerKindDao;
		Che.DAO = cheDao;
		SiteController.setDao(siteControllerDao);
		User.setDao(userDao);
		*/

		ObjectMethodRequest request = new ObjectMethodRequest();
		request.setClassName("Organization");
		request.setPersistentId("deprecated"); // organization id is ignored
		request.setMethodName("createFacilityUi");

		List<ArgsClass> methodArgs = new LinkedList<ArgsClass>();
		ArgsClass ac1 = new ArgsClass("domainId", "F1", String.class.getName());
		methodArgs.add(ac1);
		ArgsClass ac2 = new ArgsClass("description", "First Facility", String.class.getName());
		methodArgs.add(ac2);
		ArgsClass ac3 = new ArgsClass("x", 1.23, Double.class.getName());
		methodArgs.add(ac3);
		ArgsClass ac4 = new ArgsClass("y", 2.34, Double.class.getName());
		methodArgs.add(ac4);
		request.setMethodArgs(methodArgs);

		JsonEncoder enc = new JsonEncoder();
		String msg="";
		try {
			msg = enc.encode(request);
			System.out.println(msg);
		} catch (EncodeException e) {
		}

		ServerMessageProcessor processor = new ServerMessageProcessor(Mockito.mock(ServiceFactory.class), new ConverterProvider().get(), this.webSocketManagerService);
		ResponseABC response = processor.handleRequest(this.getMockWsConnection(), request);
		Assert.assertTrue(response instanceof ObjectMethodResponse);

		ObjectMethodResponse updateResponse = (ObjectMethodResponse) response;
		Assert.assertEquals(updateResponse.toString(), ResponseStatus.Success, updateResponse.getStatus());
		
		getTenantPersistenceService().commitTransaction();
	}
}
