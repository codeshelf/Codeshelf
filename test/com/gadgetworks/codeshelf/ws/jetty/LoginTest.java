package com.gadgetworks.codeshelf.ws.jetty;

import java.util.UUID;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import com.eaio.uuid.UUIDGen;
import com.gadgetworks.codeshelf.application.Configuration;
import com.gadgetworks.codeshelf.model.domain.DomainTestABC;
import com.gadgetworks.codeshelf.model.domain.Organization;
import com.gadgetworks.codeshelf.model.domain.User;
import com.gadgetworks.codeshelf.model.domain.UserType;
import com.gadgetworks.codeshelf.service.WorkService;
import com.gadgetworks.codeshelf.ws.jetty.protocol.request.LoginRequest;
import com.gadgetworks.codeshelf.ws.jetty.protocol.response.LoginResponse;
import com.gadgetworks.codeshelf.ws.jetty.protocol.response.ResponseABC;
import com.gadgetworks.codeshelf.ws.jetty.protocol.response.ResponseStatus;
import com.gadgetworks.codeshelf.ws.jetty.server.ServerMessageProcessor;
import com.gadgetworks.codeshelf.ws.jetty.server.UserSession;

public class LoginTest extends DomainTestABC {
	
	static {
		Configuration.loadConfig("test");
	}

	@Test
	public final void testLoginSucceed() {
		this.getPersistenceService().beginTenantTransaction();

		Organization organization = new Organization();
		organization.setPersistentId(new UUID(UUIDGen.newTime(), UUIDGen.getClockSeqAndNode()));
		organization.setDomainId("O1");
		organization.setDescription("TEST");
		Organization.DAO.store(organization);

		// Create a user for the organization.
		User user = new User();
		user.setParent(organization);
		user.setDomainId("user1@example.com");
		String password = "password";
		user.setPassword(password);
		user.setActive(true);
		user.setType(UserType.APPUSER);
		User.DAO.store(user);
		organization.addUser(user);

		LoginRequest request = new LoginRequest();
		request.setUserId(user.getDomainId());
		request.setPassword(password);
		
		ServerMessageProcessor processor = new ServerMessageProcessor(Mockito.mock(WorkService.class));
		ResponseABC response = processor.handleRequest(Mockito.mock(UserSession.class), request);

		Assert.assertTrue(response instanceof LoginResponse);
		
		LoginResponse loginResponse = (LoginResponse) response;
		Assert.assertEquals(ResponseStatus.Success, loginResponse.getStatus());
		Assert.assertEquals(user.getDomainId(), loginResponse.getUser().getDomainId());
		
		this.getPersistenceService().endTenantTransaction();
	}

	@Test
	public final void testUserIdFail() {
		this.getPersistenceService().beginTenantTransaction();

		Organization organization = new Organization();
		organization.setPersistentId(new UUID(UUIDGen.newTime(), UUIDGen.getClockSeqAndNode()));
		organization.setDomainId("O2");
		organization.setDescription("TEST");
		Organization.DAO.store(organization);

		// Create a user for the organization.
		User user = new User();
		user.setParent(organization);
		user.setDomainId("user2@example.com");
		user.setType(UserType.APPUSER);
		String password = "password";
		user.setPassword(password);
		user.setActive(true);
		User.DAO.store(user);
		organization.addUser(user);
		
		LoginRequest request = new LoginRequest();
		request.setUserId("user@invalid.com");
		request.setPassword(password);
		
		ServerMessageProcessor processor = new ServerMessageProcessor(Mockito.mock(WorkService.class));
		ResponseABC response = processor.handleRequest(Mockito.mock(UserSession.class), request);

		Assert.assertTrue(response instanceof LoginResponse);
		
		LoginResponse loginResponse = (LoginResponse) response;
		Assert.assertEquals(ResponseStatus.Authentication_Failed, loginResponse.getStatus());
		
		this.getPersistenceService().endTenantTransaction();
	}

	@Test
	public final void testPasswordFail() {
		this.getPersistenceService().beginTenantTransaction();

		Organization organization = new Organization();
		organization.setPersistentId(new UUID(UUIDGen.newTime(), UUIDGen.getClockSeqAndNode()));
		organization.setDomainId("O3");
		organization.setDescription("TEST");
		Organization.DAO.store(organization);

		// Create a user for the organization.
		User user = new User();
		user.setParent(organization);
		user.setDomainId("user3@example.com");
		user.setType(UserType.APPUSER);
		String password = "password";
		user.setPassword(password);
		user.setActive(true);
		User.DAO.store(user);
		organization.addUser(user);

		LoginRequest request = new LoginRequest();
		request.setUserId(user.getDomainId());
		request.setPassword("invalid");
		
		ServerMessageProcessor processor = new ServerMessageProcessor(Mockito.mock(WorkService.class));
		ResponseABC response = processor.handleRequest(Mockito.mock(UserSession.class), request);

		Assert.assertTrue(response instanceof LoginResponse);
		
		LoginResponse loginResponse = (LoginResponse) response;
		Assert.assertEquals(ResponseStatus.Authentication_Failed, loginResponse.getStatus());

		this.getPersistenceService().endTenantTransaction();
	}
}
