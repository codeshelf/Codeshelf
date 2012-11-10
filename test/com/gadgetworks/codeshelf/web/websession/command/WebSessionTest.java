package com.gadgetworks.codeshelf.web.websession.command;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.NotYetConnectedException;

import junit.framework.Assert;
import lombok.Getter;

import org.java_websocket.IWebSocket;
import org.java_websocket.drafts.Draft;
import org.java_websocket.exceptions.InvalidHandshakeException;
import org.java_websocket.framing.Framedata;
import org.java_websocket.handshake.ClientHandshakeBuilder;
import org.junit.Test;

import com.gadgetworks.codeshelf.model.dao.MockDao;
import com.gadgetworks.codeshelf.model.domain.Organization;
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
		public int getReadyState() {
			return 0;
		}

	}

	@Test
	public final void testLaunchCodeCheckSucceed() {

		MockDao<Organization> organizationDao = new MockDao<Organization>();

		Organization organization = new Organization();
		organization.setPersistentId(1L);
		organization.setDomainId("O1");
		organization.setDescription("TEST");
		organizationDao.store(organization);

		TestWebSocket testWebSocket = new TestWebSocket();
		IWebSessionReqCmdFactory factory = new WebSessionReqCmdFactory(organizationDao, null);
		IWebSession webSession = new WebSession(testWebSocket, factory);
		String inMessage = "{\"id\":\"cid_5\",\"type\":\"LAUNCH_CODE_RQ\",\"data\":{\"launchCode\":\"O1\"}}";
		IWebSessionRespCmd respCommand = webSession.processMessage(inMessage);

		Assert.assertEquals("{\"id\":\"cid_5\",\"type\":\"LAUNCH_CODE_RS\",\"data\":{\"LAUNCH_CODE_RS\":\"SUCCEED\",\"organization\":{\"description\":\"TEST\",\"domainId\":\"O1\",\"persistentId\":"
				+ organization.getPersistentId() + ",\"className\":\"Organization\"}}}",
			respCommand.getResponseMsg());
	}

	@Test
	public final void testLaunchCodeCheckFail() {

		MockDao<Organization> organizationDao = new MockDao<Organization>();

		Organization organization = new Organization();
		organization.setPersistentId(1L);
		organization.setDomainId("O1");
		organization.setDescription("TEST");
		organizationDao.store(organization);

		TestWebSocket testWebSocket = new TestWebSocket();
		IWebSessionReqCmdFactory factory = new WebSessionReqCmdFactory(organizationDao, null);
		IWebSession webSession = new WebSession(testWebSocket, factory);
		String inMessage = "{\"id\":\"cid_5\",\"type\":\"LAUNCH_CODE_RQ\",\"data\":{\"launchCode\":\"XXX\"}}";
		IWebSessionRespCmd respCommand = webSession.processMessage(inMessage);

		Assert.assertEquals("{\"id\":\"cid_5\",\"type\":\"LAUNCH_CODE_RS\",\"data\":{\"LAUNCH_CODE_RS\":\"FAIL\"}}", respCommand.getResponseMsg());
	}
}
