package com.codeshelf.ws.jetty.server;

import javax.websocket.Session;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import com.codeshelf.model.domain.UserType;
import com.codeshelf.platform.multitenancy.Tenant;
import com.codeshelf.platform.multitenancy.User;
import com.codeshelf.ws.jetty.protocol.message.MessageABC;
import com.google.common.collect.ImmutableSet;

public class SessionManagerTest {

	@Test
	public void findUserSession() {
		SessionManager manager = SessionManager.getInstance();
		Session session = Mockito.mock(Session.class);
		Tenant tenant = Mockito.mock(Tenant.class);
		Assert.assertEquals(tenant, tenant);

		User user = mockUser(tenant, 1, "testuser");
		UserSession csSession = manager.sessionStarted(session);
		csSession.authenticated(user);

		User userToSend = mockUser(tenant, 1, "testuser");
		Assert.assertEquals(user, userToSend);
		Assert.assertEquals(1, manager.sendMessage(ImmutableSet.of(userToSend), Mockito.mock(MessageABC.class)));
	}

	
	
	private User mockUser(Tenant tenant, int userId, String username) {
		User user = new User();
		user.setType(UserType.SITECON);
		user.setUserId(userId);
		user.setUsername(username);
		return user;
	}
}
