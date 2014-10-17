package com.gadgetworks.codeshelf.ws.jetty;

import java.util.UUID;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import com.eaio.uuid.UUIDGen;
import com.gadgetworks.codeshelf.application.Configuration;
import com.gadgetworks.codeshelf.model.dao.ITypedDao;
import com.gadgetworks.codeshelf.model.dao.MockDaoProvider;
import com.gadgetworks.codeshelf.model.domain.Organization;
import com.gadgetworks.codeshelf.model.domain.User;
import com.gadgetworks.codeshelf.service.WorkService;
import com.gadgetworks.codeshelf.ws.jetty.protocol.request.LoginRequest;
import com.gadgetworks.codeshelf.ws.jetty.protocol.response.LoginResponse;
import com.gadgetworks.codeshelf.ws.jetty.protocol.response.ResponseABC;
import com.gadgetworks.codeshelf.ws.jetty.protocol.response.ResponseStatus;
import com.gadgetworks.codeshelf.ws.jetty.server.CsSession;
import com.gadgetworks.codeshelf.ws.jetty.server.ServerMessageProcessor;

public class LoginTest {
	
	static {
		Configuration.loadConfig("test");
	}

	@Test
	public final void testLoginSucceed() {
		MockDaoProvider daoProvider = new MockDaoProvider();

		ITypedDao<Organization> organizationDao = daoProvider.getDaoInstance(Organization.class);
		ITypedDao<User> userDao = daoProvider.getDaoInstance(User.class);
		
		Organization.setDao(organizationDao);
		User.setDao(userDao);
			
		Organization organization = new Organization();
		organization.setPersistentId(new UUID(UUIDGen.newTime(), UUIDGen.getClockSeqAndNode()));
		organization.setDomainId("O1");
		organization.setDescription("TEST");
		organizationDao.store(organization);

		// Create a user for the organization.
		User user = new User();
		user.setParent(organization);
		user.setDomainId("user@example.com");
		String password = "password";
		user.setPassword(password);
		user.setActive(true);
		userDao.store(user);
		organization.addUser(user);

		LoginRequest request = new LoginRequest();
		request.setOrganizationId(organization.getDomainId());
		request.setUserId(user.getDomainId());
		request.setPassword(password);
		
		ServerMessageProcessor processor = new ServerMessageProcessor(daoProvider, Mockito.mock(WorkService.class));
		ResponseABC response = processor.handleRequest(Mockito.mock(CsSession.class), request);

		Assert.assertTrue(response instanceof LoginResponse);
		
		LoginResponse loginResponse = (LoginResponse) response;
		Assert.assertEquals(ResponseStatus.Success, loginResponse.getStatus());
		Assert.assertEquals(user.getDomainId(), loginResponse.getUser().getDomainId());
	}

	@Test
	public final void testUserIdFail() {
		MockDaoProvider daoProvider = new MockDaoProvider();
		
		ITypedDao<Organization> organizationDao = daoProvider.getDaoInstance(Organization.class);
		ITypedDao<User> userDao = daoProvider.getDaoInstance(User.class);
		
		Organization organization = new Organization();
		organization.setPersistentId(new UUID(UUIDGen.newTime(), UUIDGen.getClockSeqAndNode()));
		organization.setDomainId("O1");
		organization.setDescription("TEST");
		organizationDao.store(organization);

		// Create a user for the organization.
		User user = new User();
		user.setParent(organization);
		user.setDomainId("user@example.com");
		String password = "password";
		user.setPassword(password);
		user.setActive(true);
		userDao.store(user);
		organization.addUser(user);
		
		LoginRequest request = new LoginRequest();
		request.setOrganizationId(organization.getDomainId());
		request.setUserId("user@invalid.com");
		request.setPassword(password);
		
		ServerMessageProcessor processor = new ServerMessageProcessor(daoProvider, Mockito.mock(WorkService.class));
		ResponseABC response = processor.handleRequest(Mockito.mock(CsSession.class), request);

		Assert.assertTrue(response instanceof LoginResponse);
		
		LoginResponse loginResponse = (LoginResponse) response;
		Assert.assertEquals(ResponseStatus.Authentication_Failed, loginResponse.getStatus());
	}

	@Test
	public final void testPasswordFail() {
		MockDaoProvider daoProvider = new MockDaoProvider();
		
		ITypedDao<Organization> organizationDao = daoProvider.getDaoInstance(Organization.class);
		ITypedDao<User> userDao = daoProvider.getDaoInstance(User.class);
		
		Organization organization = new Organization();
		organization.setPersistentId(new UUID(UUIDGen.newTime(), UUIDGen.getClockSeqAndNode()));
		organization.setDomainId("O1");
		organization.setDescription("TEST");
		organizationDao.store(organization);

		// Create a user for the organization.
		User user = new User();
		user.setParent(organization);
		user.setDomainId("user@example.com");
		String password = "password";
		user.setPassword(password);
		user.setActive(true);
		userDao.store(user);
		organization.addUser(user);

		LoginRequest request = new LoginRequest();
		request.setOrganizationId(organization.getDomainId());
		request.setUserId(user.getDomainId());
		request.setPassword("invalid");
		
		ServerMessageProcessor processor = new ServerMessageProcessor(daoProvider, Mockito.mock(WorkService.class));
		ResponseABC response = processor.handleRequest(Mockito.mock(CsSession.class), request);

		Assert.assertTrue(response instanceof LoginResponse);
		
		LoginResponse loginResponse = (LoginResponse) response;
		Assert.assertEquals(ResponseStatus.Authentication_Failed, loginResponse.getStatus());

	}
}
