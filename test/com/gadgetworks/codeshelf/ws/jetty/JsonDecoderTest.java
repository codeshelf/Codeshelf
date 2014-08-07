package com.gadgetworks.codeshelf.ws.jetty;

import javax.websocket.DecodeException;

import org.junit.Assert;
import org.junit.Test;

import com.gadgetworks.codeshelf.application.Util;
import com.gadgetworks.codeshelf.ws.jetty.io.JsonDecoder;
import com.gadgetworks.codeshelf.ws.jetty.protocol.message.MessageABC;
import com.gadgetworks.codeshelf.ws.jetty.protocol.request.LoginRequest;

public class JsonDecoderTest  {

	static {
		Util.initLogging();
	}
	
	/**
	 * Test sinple decoding without exception
	 */
	@Test
	public void testDecoding() throws DecodeException {
		JsonDecoder decoder = new JsonDecoder();
		String rawMessage = "{'LoginRequest':{'organizationId':'DEMO1','userId':'a@example.com','password':'testme','messageId':'cid_4'}}".replace('\'', '"');
		MessageABC messageABC = decoder.decode(rawMessage);
		Assert.assertTrue(messageABC instanceof LoginRequest);
	}
}
