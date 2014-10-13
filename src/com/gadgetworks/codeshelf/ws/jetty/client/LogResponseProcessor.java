package com.gadgetworks.codeshelf.ws.jetty.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gadgetworks.codeshelf.ws.jetty.protocol.message.MessageABC;
import com.gadgetworks.codeshelf.ws.jetty.protocol.message.MessageProcessor;
import com.gadgetworks.codeshelf.ws.jetty.protocol.request.RequestABC;
import com.gadgetworks.codeshelf.ws.jetty.protocol.response.ResponseABC;
import com.gadgetworks.codeshelf.ws.jetty.server.CsSession;

public class LogResponseProcessor extends MessageProcessor {

	private static final Logger	LOGGER = LoggerFactory.getLogger(LogResponseProcessor.class);

	@Override
	public void handleResponse(CsSession session, ResponseABC response) {
		LOGGER.info("Response received:"+response);
	}

	@Override
	public ResponseABC handleRequest(CsSession session, RequestABC request) {
		LOGGER.info("Request received:"+request);
		return null;
	}

	@Override
	public void handleMessage(CsSession session, MessageABC message) {
		LOGGER.info("Message received:"+message);
	}
}
