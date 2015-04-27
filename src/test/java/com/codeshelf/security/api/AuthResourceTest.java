package com.codeshelf.security.api;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;

import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.Response;

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
		User testUser = new User();
		
		String testTenantName  = "tenant";
		Tenant testTenant = new Tenant();
		testTenant.setName(testTenantName);
		
		
		TokenSession testSession = mock(TokenSession.class);
		when(testSession.getUser()).thenReturn(testUser);
		when(testSession.getTenant()).thenReturn(testTenant);
		when(testSession.getStatus()).thenReturn(TokenSession.Status.ACTIVE_SESSION);
		
		
		TokenSessionService service = mock(TokenSessionService.class);
		when(service.authenticate(anyString(), anyString())).thenReturn(testSession);
		
		RootAuthResource subject = new RootAuthResource(service);
		Response response = subject.authenticate("any", "any");
		
		JsonNode responseNode = toJsonNode(response.getEntity());
		Assert.assertEquals(testTenantName, responseNode.get("tenant").get("name").asText());
	}

	@Test	
	public void testgetAuthorizedUserReturnsTenantName() throws IOException {
		User testUser = new User();
		
		String testTenantName  = "tenant";
		Tenant testTenant = new Tenant();
		testTenant.setName(testTenantName);
		
		
		TokenSession testSession = mock(TokenSession.class);
		when(testSession.getUser()).thenReturn(testUser);
		when(testSession.getTenant()).thenReturn(testTenant);
		when(testSession.getStatus()).thenReturn(TokenSession.Status.ACTIVE_SESSION);
		
		
		TokenSessionService service = mock(TokenSessionService.class);
		when(service.checkAuthCookie(Mockito.any(Cookie.class))).thenReturn(testSession);
		
		RootAuthResource subject = new RootAuthResource(service);
		Response response = subject.getAuthorizedUser(Mockito.any(Cookie.class));
		
		JsonNode responseNode = toJsonNode(response.getEntity());
		Assert.assertEquals(testTenantName, responseNode.get("tenant").get("name").asText());
	}

	
	private JsonNode toJsonNode(Object o) throws IOException {
		ObjectMapper objectMapper = new ObjectMapper();
		String output = objectMapper.writeValueAsString(o);
		JsonNode node = objectMapper.readTree(output);
		return node;
	}
}