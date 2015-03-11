package com.codeshelf.ws.jetty.server;

import java.util.ArrayList;

import javax.websocket.Session;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.codeshelf.metrics.DummyMetricsService;
import com.codeshelf.metrics.IMetricsService;
import com.codeshelf.metrics.MetricsService;
import com.codeshelf.model.domain.UserType;
import com.codeshelf.platform.multitenancy.Tenant;
import com.codeshelf.platform.multitenancy.User;
import com.codeshelf.testframework.MinimalTest;
import com.codeshelf.ws.jetty.protocol.message.MessageABC;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.Service;
import com.google.common.util.concurrent.ServiceManager;

public class SessionManagerTest extends MinimalTest {
	ServiceManager serviceManager;
	SessionManagerService sessionManager;
	
	@Before
	public void doBefore() {	
		sessionManager = new SessionManagerService();
		IMetricsService metrics = new DummyMetricsService();
		MetricsService.setInstance(metrics);

		ArrayList<Service> services = new ArrayList<Service>();
		services.add(sessionManager);
		services.add(metrics);
		serviceManager = new ServiceManager(services);
		serviceManager.startAsync();
		serviceManager.awaitHealthy(); 
	}

	@After
	public void doAfter() {
		serviceManager.stopAsync();
		serviceManager.awaitStopped();
	}
	
	@Test
	public void findUserSession() {
		Session session = Mockito.mock(Session.class);
		Mockito.when(session.getId()).thenReturn("123");
		
		Tenant tenant = Mockito.mock(Tenant.class);
		Assert.assertEquals(tenant, tenant);

		User user = mockUser(tenant, 1, "testuser");
		UserSession csSession = sessionManager.sessionStarted(session);
		csSession.authenticated(user);

		User userToSend = mockUser(tenant, 1, "testuser");
		Assert.assertEquals(user, userToSend);
		Assert.assertEquals(1, sessionManager.sendMessage(ImmutableSet.of(userToSend), Mockito.mock(MessageABC.class)));
	}

	
	
	private User mockUser(Tenant tenant, int userId, String username) {
		User user = new User();
		user.setType(UserType.SITECON);
		user.setId(userId);
		user.setUsername(username);
		return user;
	}
}
