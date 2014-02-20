package com.gadgetworks.codeshelf.web.websession.command;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.NotYetConnectedException;
import java.util.UUID;

import org.junit.Assert;

import lombok.Getter;

import org.apache.shiro.realm.Realm;
import org.java_websocket.WebSocket;
import org.java_websocket.drafts.Draft;
import org.java_websocket.exceptions.InvalidDataException;
import org.java_websocket.exceptions.InvalidHandshakeException;
import org.java_websocket.framing.Framedata;
import org.java_websocket.framing.Framedata.Opcode;
import org.java_websocket.handshake.ClientHandshakeBuilder;
import org.junit.Test;

import com.eaio.uuid.UUIDGen;
import com.gadgetworks.codeshelf.model.dao.ITypedDao;
import com.gadgetworks.codeshelf.model.dao.MockDao;
import com.gadgetworks.codeshelf.model.domain.Che;
import com.gadgetworks.codeshelf.model.domain.OrderDetail;
import com.gadgetworks.codeshelf.model.domain.OrderHeader;
import com.gadgetworks.codeshelf.model.domain.Organization;
import com.gadgetworks.codeshelf.model.domain.User;
import com.gadgetworks.codeshelf.model.domain.WorkInstruction;
import com.gadgetworks.codeshelf.security.CodeshelfRealm;
import com.gadgetworks.codeshelf.ws.IWebSession;
import com.gadgetworks.codeshelf.ws.WebSession;
import com.gadgetworks.codeshelf.ws.command.req.IWsReqCmdFactory;
import com.gadgetworks.codeshelf.ws.command.req.WsReqCmdFactory;
import com.gadgetworks.codeshelf.ws.command.resp.IWsRespCmd;

public class WebSessionTest {

	private class TestWebSocket implements WebSocket {

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
		public void closeConnection(int arg0, String arg1) {
			// TODO Auto-generated method stub

		}

		@Override
		public READYSTATE getReadyState() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public void close() {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void sendFragmentedFrame(Opcode op, ByteBuffer buffer, boolean fin) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public boolean isFlushAndClose() {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public String getResourceDescriptor() {
			// TODO Auto-generated method stub
			return null;
		}
	}

	@Test
	public final void testLoginSucceed() {

		ITypedDao<Organization> organizationDao = new MockDao<Organization>();
		ITypedDao<User> userDao = new MockDao<User>();
		ITypedDao<Che> cheDao = new MockDao<Che>();
		ITypedDao<WorkInstruction> workInstructionDao = new MockDao<WorkInstruction>();
		ITypedDao<OrderHeader> orderHeaderDao = new MockDao<OrderHeader>();
		ITypedDao<OrderDetail> orderDetailDao = new MockDao<OrderDetail>();

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
		IWsReqCmdFactory factory = new WsReqCmdFactory(organizationDao, cheDao, workInstructionDao, orderHeaderDao, orderDetailDao, null);
		Realm realm = new CodeshelfRealm();
		IWebSession webSession = new WebSession(testWebSocket, factory, realm);
		String inMessage = "{\"id\":\"cid_5\",\"type\":\"LOGIN_RQ\",\"data\":{\"organizationId\":\"O1\", \"userId\":\"user@example.com\", \"password\":\"password\"}}";
		IWsRespCmd respCommand = webSession.processMessage(inMessage);

		Assert.assertEquals("{\"id\":\"cid_5\",\"type\":\"LOGIN_RS\",\"data\":{\"LOGIN_RS\":\"SUCCEED\",\"organization\":{\"description\":\"TEST\",\"domainId\":\"O1\",\"persistentId\":\""
				+ organization.getPersistentId() + "\",\"className\":\"Organization\"}}}", respCommand.getResponseMsg());
	}

	@Test
	public final void testUserIdFail() {

		ITypedDao<Organization> organizationDao = new MockDao<Organization>();
		ITypedDao<User> userDao = new MockDao<User>();
		ITypedDao<Che> cheDao = new MockDao<Che>();
		ITypedDao<WorkInstruction> workInstructionDao = new MockDao<WorkInstruction>();
		ITypedDao<OrderHeader> orderHeaderDao = new MockDao<OrderHeader>();
		ITypedDao<OrderDetail> orderDetailDao = new MockDao<OrderDetail>();

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
		IWsReqCmdFactory factory = new WsReqCmdFactory(organizationDao, cheDao, workInstructionDao, orderHeaderDao, orderDetailDao, null);
		Realm realm = new CodeshelfRealm();
		IWebSession webSession = new WebSession(testWebSocket, factory, realm);
		String inMessage = "{\"id\":\"cid_5\",\"type\":\"LOGIN_RQ\",\"data\":{\"organizationId\":\"O1\", \"userId\":\"XXXXX\", \"password\":\"password\"}}";
		IWsRespCmd respCommand = webSession.processMessage(inMessage);

		Assert.assertEquals("{\"id\":\"cid_5\",\"type\":\"LOGIN_RS\",\"data\":{\"LOGIN_RS\":\"FAIL\"}}", respCommand.getResponseMsg());
	}

	@Test
	public final void testPasswordFail() {

		ITypedDao<Organization> organizationDao = new MockDao<Organization>();
		ITypedDao<User> userDao = new MockDao<User>();
		ITypedDao<Che> cheDao = new MockDao<Che>();
		ITypedDao<WorkInstruction> workInstructionDao = new MockDao<WorkInstruction>();
		ITypedDao<OrderHeader> orderHeaderDao = new MockDao<OrderHeader>();
		ITypedDao<OrderDetail> orderDetailDao = new MockDao<OrderDetail>();

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
		IWsReqCmdFactory factory = new WsReqCmdFactory(organizationDao, cheDao, workInstructionDao, orderHeaderDao, orderDetailDao, null);
		Realm realm = new CodeshelfRealm();
		IWebSession webSession = new WebSession(testWebSocket, factory, realm);
		String inMessage = "{\"id\":\"cid_5\",\"type\":\"LOGIN_RQ\",\"data\":{\"organizationId\":\"O1\", \"userId\":\"user@example.com\", \"password\":\"XXXX\"}}";
		IWsRespCmd respCommand = webSession.processMessage(inMessage);

		Assert.assertEquals("{\"id\":\"cid_5\",\"type\":\"LOGIN_RS\",\"data\":{\"LOGIN_RS\":\"FAIL\"}}", respCommand.getResponseMsg());
	}
}
