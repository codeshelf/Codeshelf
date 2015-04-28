package com.codeshelf.ws;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import com.codeshelf.manager.User;
import com.codeshelf.manager.service.TenantManagerService;
import com.codeshelf.security.CodeshelfSecurityManager;
import com.codeshelf.service.ServiceFactory;
import com.codeshelf.testframework.HibernateTest;
import com.codeshelf.util.ConverterProvider;
import com.codeshelf.ws.protocol.request.LoginRequest;
import com.codeshelf.ws.protocol.response.LoginResponse;
import com.codeshelf.ws.protocol.response.ResponseABC;
import com.codeshelf.ws.protocol.response.ResponseStatus;
import com.codeshelf.ws.server.ServerMessageProcessor;
import com.codeshelf.ws.server.WebSocketConnection;

public class LoginTest extends HibernateTest {

	private ServerMessageProcessor	processor;

	
	
	@Override
	public void doBefore() {
		super.doBefore();
		processor = new ServerMessageProcessor(Mockito.mock(ServiceFactory.class), new ConverterProvider().get(), this.webSocketManagerService);

	}

	@Test
	public final void testLoginSucceed() {
		CodeshelfSecurityManager.removeContextIfPresent();
		
		// Create a user for the organization.
		String password = "password";
		User user = TenantManagerService.getInstance().createUser(getDefaultTenant(), "user1@example.com", password, null);

		LoginRequest request = new LoginRequest();
		request.setUserId(user.getUsername());
		request.setPassword(password);
		
		ResponseABC response = processor.handleRequest(Mockito.mock(WebSocketConnection.class), request);

		Assert.assertTrue(response instanceof LoginResponse);
		
		LoginResponse loginResponse = (LoginResponse) response;
		Assert.assertEquals(ResponseStatus.Success, loginResponse.getStatus());
		Assert.assertEquals(user.getUsername(), loginResponse.getUser().getUsername());
		
	}

	@SuppressWarnings("unused")
	@Test
	public final void testUserIdFail() {
		this.getTenantPersistenceService().beginTransaction();

		// Create a user for the organization.
		String password = "password";
		User user = TenantManagerService.getInstance().createUser(getDefaultTenant(), "user1@example.com", password, null);
		
		LoginRequest request = new LoginRequest();
		request.setUserId("user@invalid.com");
		request.setPassword(password);
		
		ResponseABC response = processor.handleRequest(Mockito.mock(WebSocketConnection.class), request);

		Assert.assertTrue(response instanceof LoginResponse);
		
		LoginResponse loginResponse = (LoginResponse) response;
		Assert.assertEquals(ResponseStatus.Authentication_Failed, loginResponse.getStatus());
		
		this.getTenantPersistenceService().commitTransaction();
	}

	@Test
	public final void testPasswordFail() {
		this.getTenantPersistenceService().beginTransaction();

		// Create a user for the organization.
		String password = "password";
		User user = TenantManagerService.getInstance().createUser(getDefaultTenant(), "user1@example.com", password, null);
		
		LoginRequest request = new LoginRequest();
		request.setUserId(user.getUsername());
		request.setPassword("invalid");
		
		ResponseABC response = processor.handleRequest(Mockito.mock(WebSocketConnection.class), request);

		Assert.assertTrue(response instanceof LoginResponse);
		
		LoginResponse loginResponse = (LoginResponse) response;
		Assert.assertEquals(ResponseStatus.Authentication_Failed, loginResponse.getStatus());

		this.getTenantPersistenceService().commitTransaction();
	}
}
