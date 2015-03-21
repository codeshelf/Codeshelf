package com.codeshelf.ws.jetty;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import com.codeshelf.manager.TenantManagerService;
import com.codeshelf.manager.User;
import com.codeshelf.model.domain.UserType;
import com.codeshelf.service.ServiceFactory;
import com.codeshelf.testframework.HibernateTest;
import com.codeshelf.util.ConverterProvider;
import com.codeshelf.ws.jetty.protocol.request.LoginRequest;
import com.codeshelf.ws.jetty.protocol.response.LoginResponse;
import com.codeshelf.ws.jetty.protocol.response.ResponseABC;
import com.codeshelf.ws.jetty.protocol.response.ResponseStatus;
import com.codeshelf.ws.jetty.server.ServerMessageProcessor;
import com.codeshelf.ws.jetty.server.WebSocketConnection;

public class LoginTest extends HibernateTest {

	private ServerMessageProcessor	processor;

	
	
	@Override
	public void doBefore() {
		super.doBefore();
		processor = new ServerMessageProcessor(Mockito.mock(ServiceFactory.class), new ConverterProvider().get(), this.webSocketManagerService);

	}

	@Test
	public final void testLoginSucceed() {
		this.getTenantPersistenceService().beginTransaction(getDefaultTenant());
		
		// Create a user for the organization.
		String password = "password";
		User user = TenantManagerService.getInstance().createUser(getDefaultTenant(), "user1@example.com", password, UserType.APPUSER, null);

		LoginRequest request = new LoginRequest();
		request.setUserId(user.getUsername());
		request.setPassword(password);
		
		ResponseABC response = processor.handleRequest(Mockito.mock(WebSocketConnection.class), request);

		Assert.assertTrue(response instanceof LoginResponse);
		
		LoginResponse loginResponse = (LoginResponse) response;
		Assert.assertEquals(ResponseStatus.Success, loginResponse.getStatus());
		Assert.assertEquals(user.getUsername(), loginResponse.getUser().getUsername());
		
		this.getTenantPersistenceService().commitTransaction(getDefaultTenant());
	}

	@SuppressWarnings("unused")
	@Test
	public final void testUserIdFail() {
		this.getTenantPersistenceService().beginTransaction(getDefaultTenant());

		// Create a user for the organization.
		String password = "password";
		User user = TenantManagerService.getInstance().createUser(getDefaultTenant(), "user1@example.com", password, UserType.APPUSER, null);
		
		LoginRequest request = new LoginRequest();
		request.setUserId("user@invalid.com");
		request.setPassword(password);
		
		ResponseABC response = processor.handleRequest(Mockito.mock(WebSocketConnection.class), request);

		Assert.assertTrue(response instanceof LoginResponse);
		
		LoginResponse loginResponse = (LoginResponse) response;
		Assert.assertEquals(ResponseStatus.Authentication_Failed, loginResponse.getStatus());
		
		this.getTenantPersistenceService().commitTransaction(getDefaultTenant());
	}

	@Test
	public final void testPasswordFail() {
		this.getTenantPersistenceService().beginTransaction(getDefaultTenant());

		// Create a user for the organization.
		String password = "password";
		User user = TenantManagerService.getInstance().createUser(getDefaultTenant(), "user1@example.com", password, UserType.APPUSER, null);
		
		LoginRequest request = new LoginRequest();
		request.setUserId(user.getUsername());
		request.setPassword("invalid");
		
		ResponseABC response = processor.handleRequest(Mockito.mock(WebSocketConnection.class), request);

		Assert.assertTrue(response instanceof LoginResponse);
		
		LoginResponse loginResponse = (LoginResponse) response;
		Assert.assertEquals(ResponseStatus.Authentication_Failed, loginResponse.getStatus());

		this.getTenantPersistenceService().commitTransaction(getDefaultTenant());
	}
}
