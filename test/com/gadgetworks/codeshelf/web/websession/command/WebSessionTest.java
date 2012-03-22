package com.gadgetworks.codeshelf.web.websession.command;

import java.util.Collection;
import java.util.List;

import junit.framework.Assert;
import lombok.Getter;

import org.junit.Test;

import com.gadgetworks.codeshelf.model.dao.DaoException;
import com.gadgetworks.codeshelf.model.dao.IDaoListener;
import com.gadgetworks.codeshelf.model.dao.IGenericDao;
import com.gadgetworks.codeshelf.model.persist.User;
import com.gadgetworks.codeshelf.web.websession.WebSession;
import com.gadgetworks.codeshelf.web.websession.command.req.IWebSessionReqCmdFactory;
import com.gadgetworks.codeshelf.web.websession.command.req.WebSessionReqCmdFactory;
import com.gadgetworks.codeshelf.web.websocket.IWebSocket;

public class WebSessionTest {

	private class TestWebSocket implements IWebSocket {

		@Getter
		private String	sendString;

		@Override
		public void send(String inSendString) throws InterruptedException {
			sendString = inSendString;
		}
	}

	private class TestUserDao implements IGenericDao<User> {

		//		@Override
		//		public boolean isObjectPersisted(User inDomainObject) {
		//			return false;
		//		}

		@Override
		public User loadByPersistentId(Long inID) {
			return null;
		}

		@Override
		public User findByDomainId(String inId) {
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

		@Override
		public void pushNonPersistentUpdates(User inDomainObject) {
		}

		@Override
		public void registerDAOListener(IDaoListener inListener) {
		}

		@Override
		public void unregisterDAOListener(IDaoListener inListener) {
		}

		@Override
		public void removeDAOListeners() {
		}

		@Override
		public List<User> findByPersistentIdList(List<Long> inIdList) {
			// TODO Auto-generated method stub
			return null;
		}
	}

	@Test
	public void testProcessMessageLaunchCodeCheck() {
		TestWebSocket testWebSocket = new TestWebSocket();
		IWebSessionReqCmdFactory factory = new WebSessionReqCmdFactory(new TestUserDao(), null);
		WebSession webSession = new WebSession(testWebSocket, factory);
		String inMessage = "{\"id\":\"cmdid_5\",\"type\":\"LAUNCH_CODE_CHECK\",\"data\":{\"launchCode\":\"12345\"}}";

		webSession.processMessage(inMessage);

		Assert.assertEquals("{\"id\":\"cmdid_5\",\"type\":\"LAUNCH_CODE_RESP\",\"data\":{\"LAUNCH_CODE_RESP\":\"FAIL\"}}", testWebSocket.getSendString());
	}
}
