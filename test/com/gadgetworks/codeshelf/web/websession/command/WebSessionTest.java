package com.gadgetworks.codeshelf.web.websession.command;

import junit.framework.Assert;
import lombok.Getter;

import org.junit.Test;

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

	@Test
	public void testProcessMessageLaunchCodeCheck() {
		TestWebSocket testWebSocket = new TestWebSocket();
		WebSession webSession = new WebSession(testWebSocket);
		String inMessage = "{\"id\":\"cmdid_5\",\"type\":\"LAUNCH_CODE_CHECK\",\"data\":{\"launchCode\":\"12345\"}}";
		webSession.processMessage(inMessage);
		
		Assert.assertNull(testWebSocket.getSendString());
	}
}
