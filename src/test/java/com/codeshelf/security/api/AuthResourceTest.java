package com.codeshelf.security.api;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;

import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import com.codeshelf.manager.Tenant;
import com.codeshelf.manager.User;
import com.codeshelf.security.TokenSession;
import com.codeshelf.security.TokenSessionService;
import com.codeshelf.testframework.MockDaoTest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class AuthResourceTest extends MockDaoTest {

	@Test
	public void failedAuthenticationReturns401() {
		TokenSession testSession = mock(TokenSession.class);
		when(testSession.getStatus()).thenReturn(TokenSession.Status.BAD_CREDENTIALS);
		
		TokenSessionService service = mock(TokenSessionService.class);
		RootAuthResource subject = new RootAuthResource(service);
		
		when(service.authenticate(anyString(), anyString())).thenReturn(testSession);
		Response response = subject.authenticate("any", "any");
		Assert.assertEquals(401, response.getStatus());
	}
	
	@Test	
	public void testAuthenticationReturnsTenantName() throws IOException {

		String testTenantName  = "tenant";

		TokenSession testSession = createMockTokenSession(TokenSession.Status.ACTIVE_SESSION, testTenantName);

		TokenSessionService service = mock(TokenSessionService.class);
		when(service.authenticate(anyString(), anyString())).thenReturn(testSession);
		
		RootAuthResource subject = new RootAuthResource(service);
		Response response = subject.authenticate("any", "any");
		
		JsonNode responseNode = toJsonNode(response.getEntity());
		Assert.assertEquals(testTenantName, responseNode.get("tenant").get("name").asText());
	}

	@Test	
	public void testgetAuthorizedUserReturnsTenantName() throws IOException {
		
		String testTenantName  = "tenant";

		TokenSession testSession = createMockTokenSession(TokenSession.Status.ACTIVE_SESSION, testTenantName);
		
		TokenSessionService service = mock(TokenSessionService.class);
		when(service.checkAuthCookie(Mockito.any(Cookie.class))).thenReturn(testSession);
		
		RootAuthResource subject = new RootAuthResource(service);
		Response response = subject.getAuthorizedUser(Mockito.any(Cookie.class));
		
		JsonNode responseNode = toJsonNode(response.getEntity());
		Assert.assertEquals(testTenantName, responseNode.get("tenant").get("name").asText());
	}

	@Test
	public void testChangePasswordInactiveSession() {
		String testTenantName  = "tenant";

		TokenSession testSession = createMockTokenSession(TokenSession.Status.INVALID_TOKEN, testTenantName);

		TokenSessionService service = mock(TokenSessionService.class);
		when(service.checkAuthCookie(Mockito.any(Cookie.class))).thenReturn(testSession);

		RootAuthResource subject = new RootAuthResource(service);
		Response response = subject.changePassword("good", "newgood", null, mock(Cookie.class));
		Assert.assertEquals(Status.FORBIDDEN.getStatusCode(), response.getStatus());
	}

	@Test
	public void testChangePasswordWrongOldPassword() throws IOException {
		String testTenantName  = "tenant";

		TokenSession testSession = createMockTokenSession(TokenSession.Status.ACTIVE_SESSION, testTenantName);

		TokenSessionService service = mock(TokenSessionService.class);
		when(service.checkAuthCookie(Mockito.any(Cookie.class))).thenReturn(testSession);

		RootAuthResource subject = new RootAuthResource(service);
		Response response = subject.changePassword("bad", "newgood", null, mock(Cookie.class));
		Assert.assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
		
		JsonNode responseNode = toJsonNode(response.getEntity());
		Assert.assertTrue(responseNode.toString(), responseNode.get("fieldErrors").has("old"));
		Assert.assertNotNull(responseNode.get("fieldErrors").get("old").findValue("message"));
		
	}

	
	private TokenSession createMockTokenSession(TokenSession.Status status, String tenantName) {
		Tenant testTenant = new Tenant();
		testTenant.setName(tenantName);
		
		User testUser = new User();
		
		TokenSession testSession = mock(TokenSession.class);
		when(testSession.getUser()).thenReturn(testUser);
		when(testSession.getTenant()).thenReturn(testTenant);
		when(testSession.getStatus()).thenReturn(status);
		return testSession;
	}
	
	private JsonNode toJsonNode(Object o) throws IOException {
		ObjectMapper objectMapper = new ObjectMapper();
		String output = objectMapper.writeValueAsString(o);
		JsonNode node = objectMapper.readTree(output);
		return node;
	}
}
