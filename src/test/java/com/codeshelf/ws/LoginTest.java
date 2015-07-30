package com.codeshelf.ws;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import com.codeshelf.application.JvmProperties;
import com.codeshelf.manager.User;
import com.codeshelf.manager.service.TenantManagerService;
import com.codeshelf.model.domain.CodeshelfNetwork;
import com.codeshelf.service.ServiceFactory;
import com.codeshelf.testframework.ServerTest;
import com.codeshelf.util.ConverterProvider;
import com.codeshelf.ws.protocol.request.LoginRequest;
import com.codeshelf.ws.protocol.response.LoginResponse;
import com.codeshelf.ws.protocol.response.ResponseABC;
import com.codeshelf.ws.protocol.response.ResponseStatus;
import com.codeshelf.ws.server.ServerMessageProcessor;

public class LoginTest extends ServerTest {

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
		request.setClientVersion(null);
		
		ResponseABC response = processor.handleRequest(getMockWsConnection(), request);

		Assert.assertTrue(response instanceof LoginResponse);
		
		LoginResponse loginResponse = (LoginResponse) response;
		Assert.assertEquals(ResponseStatus.Success, loginResponse.getStatus());
		Assert.assertEquals(user.getUsername(), loginResponse.getUser().getUsername());
		
		// last authenticated was updated
		user = TenantManagerService.getInstance().getUser(user.getId());
		Assert.assertNotNull(user.getLastAuthenticated());
		// bad version login tries is 0
		Assert.assertEquals(0,user.getBadVersionLoginTries());
		
		
		// still succeeds if client version not present
		request = new LoginRequest(user.getUsername(),password);
		request.setClientVersion(null);
		
		response = processor.handleRequest(getMockWsConnection(), request);

		Assert.assertTrue(response instanceof LoginResponse);
		
		loginResponse = (LoginResponse) response;
		Assert.assertEquals(ResponseStatus.Success, loginResponse.getStatus());
		

	}

	@Test
	public final void testUserIdFail() {
		// Create a user for the organization.
		String password = "password";
		User user = TenantManagerService.getInstance().createUser(getDefaultTenant(), "user1@example.com", password, null);
		
		LoginRequest request = new LoginRequest("user@invalid.com",password);
		
		ResponseABC response = processor.handleRequest(getMockWsConnection(), request);

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
		
		ResponseABC response = processor.handleRequest(getMockWsConnection(), request);

		Assert.assertTrue(response instanceof LoginResponse);
		
		LoginResponse loginResponse = (LoginResponse) response;
		Assert.assertEquals(ResponseStatus.Authentication_Failed, loginResponse.getStatus());

		// last authenticated was NOT updated
		user = TenantManagerService.getInstance().getUser(user.getId());
		Assert.assertNull(user.getLastAuthenticated());
	}

	@Test
	public final void testIncompatibleVersionFail() {
		// use site controller user (reset last authenticated before starting)
		User user = getFacility().getSiteControllerUsers().iterator().next();
		user.setLastAuthenticated(null);
		TenantManagerService.getInstance().updateUser(user);
		user = TenantManagerService.getInstance().getUser(user.getId());
		String password = CodeshelfNetwork.DEFAULT_SITECON_PASS;
		Assert.assertNull(user.getLastAuthenticated());

		LoginRequest request = new LoginRequest(user.getUsername(),password);
		request.setClientVersion("0.1"); // incompatible version

		ResponseABC response = processor.handleRequest(getMockWsConnection(), request);

		Assert.assertTrue(response instanceof LoginResponse);

		LoginResponse loginResponse = (LoginResponse) response;
		Assert.assertEquals(ResponseStatus.Authentication_Failed, loginResponse.getStatus());

		// last authenticated was still updated even though login failed
		user = TenantManagerService.getInstance().getUser(user.getId());
		Assert.assertNotNull(user.getLastAuthenticated());

		Assert.assertEquals(1,user.getBadVersionLoginTries(),1);
		Assert.assertEquals("0.1",user.getClientVersion());
		
		// subsequent successful attempt resets bad version login tries counter
		request = new LoginRequest(user.getUsername(),password);
		response = processor.handleRequest(getMockWsConnection(), request);

		Assert.assertTrue(response instanceof LoginResponse);
		loginResponse = (LoginResponse) response;
		Assert.assertEquals(ResponseStatus.Success, loginResponse.getStatus());
		Assert.assertEquals(user.getUsername(), loginResponse.getUser().getUsername());
		
		// bad version login tries is 0
		user = TenantManagerService.getInstance().getUser(user.getId());
		Assert.assertEquals(0,user.getBadVersionLoginTries());

		// version recorded
		Assert.assertEquals(JvmProperties.getVersionStringShort(), request.getClientVersion());
		Assert.assertEquals(JvmProperties.getVersionStringShort(),user.getClientVersion());

	}

	@Test
	public final void testNullVersionSucceed() {
		// use site controller user (reset last authenticated before starting)
		User user = getFacility().getSiteControllerUsers().iterator().next();
		user.setLastAuthenticated(null);
		TenantManagerService.getInstance().updateUser(user);
		user = TenantManagerService.getInstance().getUser(user.getId());
		String password = CodeshelfNetwork.DEFAULT_SITECON_PASS;
		Assert.assertNull(user.getLastAuthenticated());

		LoginRequest request = new LoginRequest(user.getUsername(),password);
		request.setClientVersion(null); // no version provided

		ResponseABC response = processor.handleRequest(getMockWsConnection(), request);

		Assert.assertTrue(response instanceof LoginResponse);
		LoginResponse loginResponse = (LoginResponse) response;
		Assert.assertEquals(ResponseStatus.Success, loginResponse.getStatus());
		Assert.assertEquals(user.getUsername(), loginResponse.getUser().getUsername());

		// last authenticated was updated 
		user = TenantManagerService.getInstance().getUser(user.getId());
		Assert.assertNotNull(user.getLastAuthenticated());
		Assert.assertNull(user.getClientVersion());

		// no bad version
		Assert.assertEquals(0,user.getBadVersionLoginTries());
		


	}
}
