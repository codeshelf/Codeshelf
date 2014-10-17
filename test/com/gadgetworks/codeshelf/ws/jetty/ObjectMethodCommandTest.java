package com.gadgetworks.codeshelf.ws.jetty;

import java.util.LinkedList;
import java.util.List;

import javax.websocket.EncodeException;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import com.gadgetworks.codeshelf.application.Configuration;
import com.gadgetworks.codeshelf.model.dao.ITypedDao;
import com.gadgetworks.codeshelf.model.dao.MockDaoProvider;
import com.gadgetworks.codeshelf.model.domain.Che;
import com.gadgetworks.codeshelf.model.domain.CodeshelfNetwork;
import com.gadgetworks.codeshelf.model.domain.ContainerKind;
import com.gadgetworks.codeshelf.model.domain.DropboxService;
import com.gadgetworks.codeshelf.model.domain.EdiServiceABC;
import com.gadgetworks.codeshelf.model.domain.Facility;
import com.gadgetworks.codeshelf.model.domain.IronMqService;
import com.gadgetworks.codeshelf.model.domain.Organization;
import com.gadgetworks.codeshelf.model.domain.SiteController;
import com.gadgetworks.codeshelf.model.domain.User;
import com.gadgetworks.codeshelf.service.WorkService;
import com.gadgetworks.codeshelf.ws.jetty.io.JsonEncoder;
import com.gadgetworks.codeshelf.ws.jetty.protocol.command.ArgsClass;
import com.gadgetworks.codeshelf.ws.jetty.protocol.request.ObjectMethodRequest;
import com.gadgetworks.codeshelf.ws.jetty.protocol.response.ObjectMethodResponse;
import com.gadgetworks.codeshelf.ws.jetty.protocol.response.ResponseABC;
import com.gadgetworks.codeshelf.ws.jetty.protocol.response.ResponseStatus;
import com.gadgetworks.codeshelf.ws.jetty.server.CsSession;
import com.gadgetworks.codeshelf.ws.jetty.server.ServerMessageProcessor;

@RunWith(MockitoJUnitRunner.class)
public class ObjectMethodCommandTest {

	static {
		Configuration.loadConfig("server");
	}

	@Test
	// this should really be an integration test
	public final void testCreateFacilityOK() {

		// "ObjectMethodRequest":{"className":"Organization","persistentId":"77fdd850-2245-11e4-822c-48d705ccef0f","methodName":"createFacility",
		// "methodArgs":[{"name":"domainId","value":"F1","classType":"java.lang.String"},
		// {"name":"description","value":"First Facility","classType":"java.lang.String"},
		// {"name":"anchorPoint","value":{"posTypeEnum":"GPS","x":-122.27488799999999,"y":37.8013437,"z":0},"classType":"com.gadgetworks.codeshelf.model.domain.Point"}],"messageId":"cid_6"}

		// {"ObjectMethodRequest":{"messageId":"ec34aa40-2250-11e4-9427-48d705ccef0f","className":"Organization","persistentId":"ec343510-2250-11e4-9427-48d705ccef0f","methodName":"createFacility",
		// "methodArgs":[{"name":"domainId","value":"F1","classType":"java.lang.String"},
		// {"name":"description","value":"First Facility","classType":"java.lang.String"},
		// {"name":"anchorPoint","value":{"posTypeEnum":"METERS_FROM_PARENT","x":0.0,"y":0.0,"z":0.0},"classType":"com.gadgetworks.codeshelf.model.domain.Point"}]}}

		MockDaoProvider daoProvider = new MockDaoProvider();

		ITypedDao<Organization> orgDao = daoProvider.getDaoInstance(Organization.class);
		ITypedDao<Facility> facDao = daoProvider.getDaoInstance(Facility.class);
		ITypedDao<CodeshelfNetwork> netDao = daoProvider.getDaoInstance(CodeshelfNetwork.class);
		ITypedDao<Che> cheDao = daoProvider.getDaoInstance(Che.class);
		ITypedDao<DropboxService> dropboxDao = daoProvider.getDaoInstance(DropboxService.class);
		ITypedDao<IronMqService> ironMqDao = daoProvider.getDaoInstance(IronMqService.class);
		ITypedDao<EdiServiceABC> ediServiceABCDao = daoProvider.getDaoInstance(EdiServiceABC.class);
		ITypedDao<ContainerKind> containerKindDao = daoProvider.getDaoInstance(ContainerKind.class);
		ITypedDao<SiteController> siteControllerDao = daoProvider.getDaoInstance(SiteController.class);
		ITypedDao<User> userDao = daoProvider.getDaoInstance(User.class);
		Organization.setDao(orgDao);
		Facility.DAO = facDao;
		DropboxService.DAO = dropboxDao;
		IronMqService.DAO = ironMqDao;
		EdiServiceABC.DAO = ediServiceABCDao;
		CodeshelfNetwork.DAO = netDao;
		ContainerKind.DAO = containerKindDao;
		Che.DAO = cheDao;
		SiteController.setDao(siteControllerDao);
		User.setDao(userDao);

		Organization organization = new Organization();
		organization.setOrganizationId("CTEST1.O1");
		orgDao.store(organization);

		ObjectMethodRequest request = new ObjectMethodRequest();
		request.setClassName("Organization");
		request.setPersistentId(organization.getPersistentId().toString());
		request.setMethodName("createFacility");

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

		ServerMessageProcessor processor = new ServerMessageProcessor(daoProvider, Mockito.mock(WorkService.class));
		ResponseABC response = processor.handleRequest(Mockito.mock(CsSession.class), request);
		Assert.assertTrue(response instanceof ObjectMethodResponse);

		ObjectMethodResponse updateResponse = (ObjectMethodResponse) response;
		Assert.assertEquals(updateResponse.toString(), ResponseStatus.Success, updateResponse.getStatus());
	}
}
