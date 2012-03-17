package com.gadgetworks.codeshelf.web.websession.command;

import java.util.Collection;

import junit.framework.Assert;
import lombok.Getter;

import org.junit.Test;

import com.gadgetworks.codeshelf.model.dao.DaoException;
import com.gadgetworks.codeshelf.model.persist.User;
import com.gadgetworks.codeshelf.model.persist.User.IUserDao;
import com.gadgetworks.codeshelf.web.websession.WebSession;
import com.gadgetworks.codeshelf.web.websocket.IWebSocket;

public class WebSessionTest {
	
	private class TestWebSocket implements IWebSocket {

		@Getter
		private String sendString;
		
		@Override
		public void send(String inSendString) throws InterruptedException {
			sendString = inSendString;
		}
	}
	
	private class TestUserDao implements IUserDao {

		@Override
		public boolean isObjectPersisted(User inDomainObject) {
			return false;
		}

		@Override
		public User loadByPersistentId(Long inID) {
			return null;
		}

		@Override
		public User findById(String inId) {
			return null;
		}

		@Override
		public void store(User inDomainObject) throws DaoException {
		}

		@Override
		public void delete(User inDomainObject) throws DaoException {
		}

		@Override
		public Collection<User> getAll() {
			return null;
		}	
	}

	@Test
	public void testProcessMessageLaunchCodeCheck() {
		TestWebSocket testWebSocket = new TestWebSocket();
		IWebSessionReqCmdFactory factory = new WebSessionReqCmdFactory(new TestUserDao());
		WebSession webSession = new WebSession(testWebSocket, factory);
		String inMessage = "{\"id\":\"cmdid_5\",\"type\":\"LAUNCH_CODE_CHECK\",\"data\":{\"launchCode\":\"12345\"}}";
		
		webSession.processMessage(inMessage);
		
		Assert.assertEquals("{\"id\":\"cmdid_5\",\"type\":\"LAUNCH_CODE_RESP\",\"data\":{\"LAUNCH_CODE_RESP\":\"FAIL\"}}", testWebSocket.getSendString());
	}
}
