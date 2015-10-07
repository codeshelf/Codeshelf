package com.codeshelf.ws.server;

import java.util.ArrayList;
import java.util.List;

import javax.websocket.RemoteEndpoint.Basic;
import javax.websocket.Session;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import com.codeshelf.behavior.WorkBehavior;
import com.codeshelf.manager.Tenant;
import com.codeshelf.manager.User;
import com.codeshelf.metrics.DummyMetricsService;
import com.codeshelf.metrics.IMetricsService;
import com.codeshelf.metrics.MetricsService;
import com.codeshelf.testframework.MinimalTest;
import com.codeshelf.ws.protocol.message.MessageABC;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.Service;

public class SessionManagerTest extends MinimalTest {
	WebSocketManagerService sessionManager;

	@Override
	public boolean ephemeralServicesShouldStartAutomatically() {
		return true;
	}

	@Override
	protected List<Service> generateEphemeralServices() {
		IMetricsService metrics = new DummyMetricsService();
		MetricsService.setInstance(metrics);
		sessionManager = new WebSocketManagerService(metrics, Mockito.mock(WorkBehavior.class)); //this.webSocketManagerService;
		WebSocketManagerService.setInstance(sessionManager);

		ArrayList<Service> services = new ArrayList<Service>();
		services.add(sessionManager);
		services.add(metrics);
		
		return services;
	}

	@Test
	public void findUserSession() {
		Session session = Mockito.mock(Session.class);
		Mockito.when(session.getId()).thenReturn("123");
		Mockito.when(session.getBasicRemote()).thenReturn(Mockito.mock(Basic.class));
		
		Tenant tenant = Mockito.mock(Tenant.class);
		Assert.assertEquals(tenant, tenant);

		User user = mockUser(tenant, 1, "testuser");
		WebSocketConnection csSession = sessionManager.sessionStarted(session);
		csSession.authenticated(user,tenant);

		User userToSend = mockUser(tenant, 1, "testuser");
		Assert.assertEquals(user, userToSend);
		Assert.assertEquals(1, sessionManager.sendMessage(ImmutableSet.of(userToSend), Mockito.mock(MessageABC.class)));
	}

	
	
	private User mockUser(Tenant tenant, int userId, String username) {
		User user = new User();
		user.setId(userId);
		user.setUsername(username);
		return user;
	}
}
