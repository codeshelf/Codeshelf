package com.codeshelf.ws.jetty.server;

import java.util.ArrayList;

import javax.websocket.Session;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.codeshelf.metrics.DummyMetricsService;
import com.codeshelf.model.domain.UserType;
import com.codeshelf.platform.multitenancy.Tenant;
import com.codeshelf.platform.multitenancy.User;
import com.codeshelf.ws.jetty.protocol.message.MessageABC;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.Service;
import com.google.common.util.concurrent.ServiceManager;

public class SessionManagerTest {
	ServiceManager serviceManager;
	SessionManagerService sessionManager;
	
	@Before
	public void doBefore() {	
		sessionManager = new SessionManagerService();

		ArrayList<Service> services = new ArrayList<Service>();
		services.add(sessionManager);
		services.add(new DummyMetricsService());
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
		user.setUserId(userId);
		user.setUsername(username);
		return user;
	}
}
