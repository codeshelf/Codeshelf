package com.gadgetworks.codeshelf.web.websession.command;

import java.util.List;
import java.util.Map;

import junit.framework.Assert;
import lombok.Getter;

import org.junit.Test;

import com.avaje.ebean.Query;
import com.gadgetworks.codeshelf.model.dao.DaoException;
import com.gadgetworks.codeshelf.model.dao.IDaoListener;
import com.gadgetworks.codeshelf.model.dao.ITypedDao;
import com.gadgetworks.codeshelf.model.dao.MockDao;
import com.gadgetworks.codeshelf.model.domain.IDomainObject;
import com.gadgetworks.codeshelf.model.domain.Organization;
import com.gadgetworks.codeshelf.web.websession.IWebSession;
import com.gadgetworks.codeshelf.web.websession.WebSession;
import com.gadgetworks.codeshelf.web.websession.command.req.IWebSessionReqCmdFactory;
import com.gadgetworks.codeshelf.web.websession.command.req.WebSessionReqCmdFactory;
import com.gadgetworks.codeshelf.web.websession.command.resp.IWebSessionRespCmd;
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

	@Test
	public final void testLaunchCodeCheckSucceed() {

		MockDao<Organization> organizationDao = new MockDao<Organization>();

		Organization organization = new Organization();
		organization.setPersistentId(1L);
		organization.setShortDomainId("O1");
		organization.setDescription("TEST");
		organizationDao.store(organization);

		TestWebSocket testWebSocket = new TestWebSocket();
		IWebSessionReqCmdFactory factory = new WebSessionReqCmdFactory(organizationDao, null);
		IWebSession webSession = new WebSession(testWebSocket, factory);
		String inMessage = "{\"id\":\"cid_5\",\"type\":\"LAUNCH_CODE_RQ\",\"data\":{\"launchCode\":\"O1\"}}";
		IWebSessionRespCmd respCommand = webSession.processMessage(inMessage);

		Assert.assertEquals("{\"id\":\"cid_5\",\"type\":\"LAUNCH_CODE_RS\",\"data\":{\"LAUNCH_CODE_RS\":\"SUCCEED\",\"organization\":{\"description\":\"TEST\",\"shortDomainId\":\"O1\",\"persistentId\":"
				+ organization.getPersistentId() + ",\"className\":\"Organization\"}}}",
			respCommand.getResponseMsg());
	}

	@Test
	public final void testLaunchCodeCheckFail() {

		MockDao<Organization> organizationDao = new MockDao<Organization>();

		Organization organization = new Organization();
		organization.setPersistentId(1L);
		organization.setShortDomainId("O1");
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
