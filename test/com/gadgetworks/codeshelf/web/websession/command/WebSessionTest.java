package com.gadgetworks.codeshelf.web.websession.command;

import java.util.List;

import junit.framework.Assert;
import lombok.Getter;

import org.junit.Test;

import com.gadgetworks.codeshelf.model.dao.DaoException;
import com.gadgetworks.codeshelf.model.dao.IDaoListener;
import com.gadgetworks.codeshelf.model.dao.IGenericDao;
import com.gadgetworks.codeshelf.model.persist.Organization;
import com.gadgetworks.codeshelf.model.persist.PersistABC;
import com.gadgetworks.codeshelf.web.websession.IWebSession;
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

	private class TestOrganizationDao implements IGenericDao<Organization> {

		public Organization findByPersistentId(Long inID) {
			return null;
		}

		public Organization findByDomainId(PersistABC inParentObject, String inId) {
			return null;
		}

		public void store(Organization inDomainObject) throws DaoException {
		}

		public void delete(Organization inDomainObject) throws DaoException {
		}

		public List<Organization> getAll() {
			return null;
		}

		public void pushNonPersistentUpdates(Organization inDomainObject) {
		}

		public void registerDAOListener(IDaoListener inListener) {
		}

		public void unregisterDAOListener(IDaoListener inListener) {
		}

		public void removeDAOListeners() {
		}

		public List<Organization> findByPersistentIdList(List<Long> inIdList) {
			return null;
		}

		public List<Organization> findByFilter(String inFilter) {
			return null;
		}
	}

	@Test
	public void testProcessMessageLaunchCodeCheck() {
		TestWebSocket testWebSocket = new TestWebSocket();
		IWebSessionReqCmdFactory factory = new WebSessionReqCmdFactory(new TestOrganizationDao(), null);
		IWebSession webSession = new WebSession(testWebSocket, factory);
		String inMessage = "{\"id\":\"cmdid_5\",\"type\":\"LAUNCH_CODE_CHECK\",\"data\":{\"launchCode\":\"12345\"}}";

		webSession.processMessage(inMessage);

		Assert.assertEquals("{\"id\":\"cmdid_5\",\"type\":\"LAUNCH_CODE_RESP\",\"data\":{\"LAUNCH_CODE_RESP\":\"FAIL\"}}", testWebSocket.getSendString());
	}
}
