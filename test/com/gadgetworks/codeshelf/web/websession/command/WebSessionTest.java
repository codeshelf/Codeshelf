package com.gadgetworks.codeshelf.web.websession.command;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.NotYetConnectedException;
import java.util.UUID;

import junit.framework.Assert;
import lombok.Getter;

import org.apache.shiro.realm.Realm;
import org.java_websocket.IWebSocket;
import org.java_websocket.drafts.Draft;
import org.java_websocket.exceptions.InvalidDataException;
import org.java_websocket.exceptions.InvalidHandshakeException;
import org.java_websocket.framing.Framedata;
import org.java_websocket.handshake.ClientHandshakeBuilder;
import org.junit.Test;

import com.eaio.uuid.UUIDGen;
import com.gadgetworks.codeshelf.model.dao.ITypedDao;
import com.gadgetworks.codeshelf.model.dao.MockDao;
import com.gadgetworks.codeshelf.model.domain.Che;
import com.gadgetworks.codeshelf.model.domain.Organization;
import com.gadgetworks.codeshelf.model.domain.User;
import com.gadgetworks.codeshelf.security.CodeshelfRealm;
import com.gadgetworks.codeshelf.web.websession.IWebSession;
import com.gadgetworks.codeshelf.web.websession.WebSession;
import com.gadgetworks.codeshelf.web.websession.command.req.IWebSessionReqCmdFactory;
import com.gadgetworks.codeshelf.web.websession.command.req.WebSessionReqCmdFactory;
import com.gadgetworks.codeshelf.web.websession.command.resp.IWebSessionRespCmd;

public class WebSessionTest {

	private class TestWebSocket implements IWebSocket {

		@Getter
		private String	sendString;

		@Override
		public void close(int code, String message) {
		}

		@Override
		public void close(int code) {
		}

		@Override
		public void send(String text) throws NotYetConnectedException {
		}

		@Override
		public void send(ByteBuffer bytes) throws IllegalArgumentException, NotYetConnectedException {
		}

		@Override
		public void send(byte[] bytes) throws IllegalArgumentException, NotYetConnectedException {
		}

		@Override
		public void sendFrame(Framedata framedata) {
		}

		@Override
		public boolean hasBufferedData() {
			return false;
		}

		@Override
		public void startHandshake(ClientHandshakeBuilder handshakedata) throws InvalidHandshakeException {
		}

		@Override
		public InetSocketAddress getRemoteSocketAddress() {
			return null;
		}

		@Override
		public InetSocketAddress getLocalSocketAddress() {
			return null;
		}

		@Override
		public boolean isConnecting() {
			return false;
		}

		@Override
		public boolean isOpen() {
			return false;
		}

		@Override
		public boolean isClosing() {
			return false;
		}

		@Override
		public boolean isClosed() {
			return false;
		}

		@Override
		public Draft getDraft() {
			return null;
		}

		@Override
		public void close(InvalidDataException arg0) {
			// TODO Auto-generated method stub

		}

		@Override
		public void closeConnection(int arg0, String arg1) {
			// TODO Auto-generated method stub

		}

		@Override
		public READYSTATE getReadyState() {
			// TODO Auto-generated method stub
			return null;
		}
	}

	@Test
	public final void testLoginSucceed() {

		ITypedDao<Organization> organizationDao = new MockDao<Organization>();
		ITypedDao<User> userDao = new MockDao<User>();
		ITypedDao<Che> cheDao = new MockDao<Che>();

		Organization organization = new Organization();
		organization.setPersistentId(new UUID(UUIDGen.newTime(), UUIDGen.getClockSeqAndNode()));
		organization.setDomainId("O1");
		organization.setDescription("TEST");
		organizationDao.store(organization);

		// Create a user for the organization.
		User user = new User();
		user.setParent(organization);
		user.setDomainId("user@example.com");
		user.setEmail("user@example.com");
		user.setPassword("password");
		user.setActive(true);
		userDao.store(user);
		organization.addUser(user);

		TestWebSocket testWebSocket = new TestWebSocket();
		IWebSessionReqCmdFactory factory = new WebSessionReqCmdFactory(organizationDao, cheDao, null);
		Realm realm = new CodeshelfRealm();
		IWebSession webSession = new WebSession(testWebSocket, factory, realm);
		String inMessage = "{\"id\":\"cid_5\",\"type\":\"LOGIN_RQ\",\"data\":{\"organizationId\":\"O1\", \"userId\":\"user@example.com\", \"password\":\"password\"}}";
		IWebSessionRespCmd respCommand = webSession.processMessage(inMessage);

		Assert.assertEquals("{\"id\":\"cid_5\",\"type\":\"LOGIN_RS\",\"data\":{\"LOGIN_RS\":\"SUCCEED\",\"organization\":{\"description\":\"TEST\",\"domainId\":\"O1\",\"persistentId\":\""
				+ organization.getPersistentId() + "\",\"className\":\"Organization\"}}}", respCommand.getResponseMsg());
	}

	@Test
	public final void testUserIdFail() {

		ITypedDao<Organization> organizationDao = new MockDao<Organization>();
		ITypedDao<User> userDao = new MockDao<User>();
		ITypedDao<Che> cheDao = new MockDao<Che>();

		Organization organization = new Organization();
		organization.setPersistentId(new UUID(UUIDGen.newTime(), UUIDGen.getClockSeqAndNode()));
		organization.setDomainId("O1");
		organization.setDescription("TEST");
		organizationDao.store(organization);

		// Create a user for the organization.
		User user = new User();
		user.setParent(organization);
		user.setDomainId("user@example.com");
		user.setEmail("user@example.com");
		user.setPassword("password");
		user.setActive(true);
		userDao.store(user);
		organization.addUser(user);

		TestWebSocket testWebSocket = new TestWebSocket();
		IWebSessionReqCmdFactory factory = new WebSessionReqCmdFactory(organizationDao, cheDao, null);
		Realm realm = new CodeshelfRealm();
		IWebSession webSession = new WebSession(testWebSocket, factory, realm);
		String inMessage = "{\"id\":\"cid_5\",\"type\":\"LOGIN_RQ\",\"data\":{\"organizationId\":\"O1\", \"userId\":\"XXXXX\", \"password\":\"password\"}}";
		IWebSessionRespCmd respCommand = webSession.processMessage(inMessage);

		Assert.assertEquals("{\"id\":\"cid_5\",\"type\":\"LOGIN_RS\",\"data\":{\"LOGIN_RS\":\"FAIL\"}}", respCommand.getResponseMsg());
	}

	@Test
	public final void testPasswordFail() {

		ITypedDao<Organization> organizationDao = new MockDao<Organization>();
		ITypedDao<User> userDao = new MockDao<User>();
		ITypedDao<Che> cheDao = new MockDao<Che>();

		Organization organization = new Organization();
		organization.setPersistentId(new UUID(UUIDGen.newTime(), UUIDGen.getClockSeqAndNode()));
		organization.setDomainId("O1");
		organization.setDescription("TEST");
		organizationDao.store(organization);

		// Create a user for the organization.
		User user = new User();
		user.setParent(organization);
		user.setDomainId("user@example.com");
		user.setEmail("user@example.com");
		user.setPassword("password");
		user.setActive(true);
		userDao.store(user);
		organization.addUser(user);

		TestWebSocket testWebSocket = new TestWebSocket();
		IWebSessionReqCmdFactory factory = new WebSessionReqCmdFactory(organizationDao, cheDao, null);
		Realm realm = new CodeshelfRealm();
		IWebSession webSession = new WebSession(testWebSocket, factory, realm);
		String inMessage = "{\"id\":\"cid_5\",\"type\":\"LOGIN_RQ\",\"data\":{\"organizationId\":\"O1\", \"userId\":\"user@example.com\", \"password\":\"XXXX\"}}";
		IWebSessionRespCmd respCommand = webSession.processMessage(inMessage);

		Assert.assertEquals("{\"id\":\"cid_5\",\"type\":\"LOGIN_RS\",\"data\":{\"LOGIN_RS\":\"FAIL\"}}", respCommand.getResponseMsg());
	}
}
