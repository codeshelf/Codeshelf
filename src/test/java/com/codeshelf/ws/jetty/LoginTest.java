package com.codeshelf.ws.jetty;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import com.codeshelf.application.Configuration;
import com.codeshelf.model.domain.DomainTestABC;
import com.codeshelf.model.domain.UserType;
import com.codeshelf.platform.multitenancy.TenantManagerService;
import com.codeshelf.platform.multitenancy.User;
import com.codeshelf.service.ServiceFactory;
import com.codeshelf.util.ConverterProvider;
import com.codeshelf.ws.jetty.protocol.request.LoginRequest;
import com.codeshelf.ws.jetty.protocol.response.LoginResponse;
import com.codeshelf.ws.jetty.protocol.response.ResponseABC;
import com.codeshelf.ws.jetty.protocol.response.ResponseStatus;
import com.codeshelf.ws.jetty.server.ServerMessageProcessor;
import com.codeshelf.ws.jetty.server.UserSession;

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
		this.getTenantPersistenceService().beginTenantTransaction();
		
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
		
		this.getTenantPersistenceService().commitTenantTransaction();
	}

	@SuppressWarnings("unused")
	@Test
	public final void testUserIdFail() {
		this.getTenantPersistenceService().beginTenantTransaction();

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
		
		this.getTenantPersistenceService().commitTenantTransaction();
	}

	@Test
	public final void testPasswordFail() {
		this.getTenantPersistenceService().beginTenantTransaction();

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

		this.getTenantPersistenceService().commitTenantTransaction();
	}
}
