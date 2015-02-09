package com.gadgetworks.codeshelf.ws.jetty;

import java.util.UUID;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import com.eaio.uuid.UUIDGen;
import com.gadgetworks.codeshelf.application.Configuration;
import com.gadgetworks.codeshelf.model.domain.DomainTestABC;
import com.gadgetworks.codeshelf.model.domain.UserType;
import com.gadgetworks.codeshelf.platform.multitenancy.TenantManagerService;
import com.gadgetworks.codeshelf.platform.multitenancy.User;
import com.gadgetworks.codeshelf.service.ServiceFactory;
import com.gadgetworks.codeshelf.util.ConverterProvider;
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

	private ServerMessageProcessor	processor;

	
	
	@Override
	public void doBefore() throws Exception {
		// TODO Auto-generated method stub
		super.doBefore();
		processor = new ServerMessageProcessor(Mockito.mock(ServiceFactory.class), new ConverterProvider().get());

	}

	@Test
	public final void testLoginSucceed() {
		this.getPersistenceService().beginTenantTransaction();
		
		// Create a user for the organization.
		String password = "password";
		User user = TenantManagerService.getInstance().createUser(getDefaultTenant(), "user1@example.com", password, UserType.APPUSER);

		LoginRequest request = new LoginRequest();
		request.setUserId(user.getUsername());
		request.setPassword(password);
		
		ResponseABC response = processor.handleRequest(Mockito.mock(UserSession.class), request);

		Assert.assertTrue(response instanceof LoginResponse);
		
		LoginResponse loginResponse = (LoginResponse) response;
		Assert.assertEquals(ResponseStatus.Success, loginResponse.getStatus());
		Assert.assertEquals(user.getUsername(), loginResponse.getUser().getUsername());
		
		this.getPersistenceService().commitTenantTransaction();
	}

	@Test
	public final void testUserIdFail() {
		this.getPersistenceService().beginTenantTransaction();

		// Create a user for the organization.
		String password = "password";
		User user = TenantManagerService.getInstance().createUser(getDefaultTenant(), "user1@example.com", password, UserType.APPUSER);
		
		LoginRequest request = new LoginRequest();
		request.setUserId("user@invalid.com");
		request.setPassword(password);
		
		ResponseABC response = processor.handleRequest(Mockito.mock(UserSession.class), request);

		Assert.assertTrue(response instanceof LoginResponse);
		
		LoginResponse loginResponse = (LoginResponse) response;
		Assert.assertEquals(ResponseStatus.Authentication_Failed, loginResponse.getStatus());
		
		this.getPersistenceService().commitTenantTransaction();
	}

	@Test
	public final void testPasswordFail() {
		this.getPersistenceService().beginTenantTransaction();

		// Create a user for the organization.
		String password = "password";
		User user = TenantManagerService.getInstance().createUser(getDefaultTenant(), "user1@example.com", password, UserType.APPUSER);
		
		LoginRequest request = new LoginRequest();
		request.setUserId(user.getUsername());
		request.setPassword("invalid");
		
		ResponseABC response = processor.handleRequest(Mockito.mock(UserSession.class), request);

		Assert.assertTrue(response instanceof LoginResponse);
		
		LoginResponse loginResponse = (LoginResponse) response;
		Assert.assertEquals(ResponseStatus.Authentication_Failed, loginResponse.getStatus());

		this.getPersistenceService().commitTenantTransaction();
	}
}
