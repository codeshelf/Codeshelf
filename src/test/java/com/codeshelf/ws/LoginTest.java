package com.codeshelf.ws;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import com.codeshelf.manager.User;
import com.codeshelf.manager.service.TenantManagerService;
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
		// Create a user for the organization.
		String password = "password";
		User user = TenantManagerService.getInstance().createUser(getDefaultTenant(), "user1@example.com", password, null);
		Assert.assertNull(user.getLastAuthenticated());

		LoginRequest request = new LoginRequest(user.getUsername(),password);
		
		ResponseABC response = processor.handleRequest(Mockito.mock(WebSocketConnection.class), request);

		Assert.assertTrue(response instanceof LoginResponse);
		
		LoginResponse loginResponse = (LoginResponse) response;
		Assert.assertEquals(ResponseStatus.Success, loginResponse.getStatus());
		Assert.assertEquals(user.getUsername(), loginResponse.getUser().getUsername());
		
		// last authenticated was updated
		user = TenantManagerService.getInstance().getUser(user.getId());
		Assert.assertNotNull(user.getLastAuthenticated());
		// bad version login tries is 0
		Assert.assertEquals(0,user.getBadVersionLoginTries());
	}

	@Test
	public final void testUserIdFail() {
		// Create a user for the organization.
		String password = "password";
		User user = TenantManagerService.getInstance().createUser(getDefaultTenant(), "user1@example.com", password, null);
		
		LoginRequest request = new LoginRequest("user@invalid.com",password);
		
		ResponseABC response = processor.handleRequest(Mockito.mock(WebSocketConnection.class), request);

		Assert.assertTrue(response instanceof LoginResponse);
		
		LoginResponse loginResponse = (LoginResponse) response;
		Assert.assertEquals(ResponseStatus.Authentication_Failed, loginResponse.getStatus());

		// last authenticated was NOT updated
		user = TenantManagerService.getInstance().getUser(user.getId());
		Assert.assertNull(user.getLastAuthenticated());
	}

	@Test
	public final void testPasswordFail() {
		// Create a user for the organization.
		String password = "password";
		User user = TenantManagerService.getInstance().createUser(getDefaultTenant(), "user1@example.com", password, null);
		
		LoginRequest request = new LoginRequest(user.getUsername(),"invalid");
		
		ResponseABC response = processor.handleRequest(Mockito.mock(WebSocketConnection.class), request);

		Assert.assertTrue(response instanceof LoginResponse);
		
		LoginResponse loginResponse = (LoginResponse) response;
		Assert.assertEquals(ResponseStatus.Authentication_Failed, loginResponse.getStatus());

		// last authenticated was NOT updated
		user = TenantManagerService.getInstance().getUser(user.getId());
		Assert.assertNull(user.getLastAuthenticated());
	}

	@Test
	public final void testIncompatibleVersionFail() {
		// Create a user for the organization.
		String password = "password";
		User user = TenantManagerService.getInstance().createUser(getDefaultTenant(), "user1@example.com", password, null);
		Assert.assertNull(user.getLastAuthenticated());

		LoginRequest request = new LoginRequest(user.getUsername(),password);
		request.setClientVersion("0.1"); // incompatible version

		ResponseABC response = processor.handleRequest(Mockito.mock(WebSocketConnection.class), request);

		Assert.assertTrue(response instanceof LoginResponse);

		LoginResponse loginResponse = (LoginResponse) response;
		Assert.assertEquals(ResponseStatus.Authentication_Failed, loginResponse.getStatus());

		// last authenticated was still updated even though login failed
		user = TenantManagerService.getInstance().getUser(user.getId());
		Assert.assertNotNull(user.getLastAuthenticated());

		Assert.assertEquals(1,user.getBadVersionLoginTries(),1);
	}

	@Test
	public final void testNullVersionFail() {
		// Create a user for the organization.
		String password = "password";
		User user = TenantManagerService.getInstance().createUser(getDefaultTenant(), "user1@example.com", password, null);
		Assert.assertNull(user.getLastAuthenticated());

		LoginRequest request = new LoginRequest(user.getUsername(),password);
		request.setClientVersion(null); // no version provided

		ResponseABC response = processor.handleRequest(Mockito.mock(WebSocketConnection.class), request);

		Assert.assertTrue(response instanceof LoginResponse);

		LoginResponse loginResponse = (LoginResponse) response;
		Assert.assertEquals(ResponseStatus.Authentication_Failed, loginResponse.getStatus());

		// last authenticated was still updated even though login failed
		user = TenantManagerService.getInstance().getUser(user.getId());
		Assert.assertNotNull(user.getLastAuthenticated());

		Assert.assertEquals(1,user.getBadVersionLoginTries());
		
		// subsequent successful attempt resets bad version login tries counter
		request = new LoginRequest(user.getUsername(),password);
		response = processor.handleRequest(Mockito.mock(WebSocketConnection.class), request);

		Assert.assertTrue(response instanceof LoginResponse);
		loginResponse = (LoginResponse) response;
		Assert.assertEquals(ResponseStatus.Success, loginResponse.getStatus());
		Assert.assertEquals(user.getUsername(), loginResponse.getUser().getUsername());
		
		// bad version login tries is 0
		user = TenantManagerService.getInstance().getUser(user.getId());
		Assert.assertEquals(0,user.getBadVersionLoginTries());

	}
}
