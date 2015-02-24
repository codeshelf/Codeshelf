package com.codeshelf.ws.jetty.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.ws.jetty.protocol.message.IMessageProcessor;
import com.codeshelf.ws.jetty.protocol.message.MessageABC;
import com.codeshelf.ws.jetty.protocol.request.RequestABC;
import com.codeshelf.ws.jetty.protocol.response.ResponseABC;
import com.codeshelf.ws.jetty.server.UserSession;

public class LogResponseProcessor implements IMessageProcessor {

	private static final Logger	LOGGER = LoggerFactory.getLogger(LogResponseProcessor.class);

	@Override
	public void handleResponse(UserSession session, ResponseABC response) {
		LOGGER.info("Response received:"+response);
	}

	@Override
	public ResponseABC handleRequest(UserSession session, RequestABC request) {
		LOGGER.info("Request received:"+request);
		return null;
	}

	@Override
	public void handleMessage(UserSession session, MessageABC message) {
		LOGGER.info("Message received:"+message);
	}
}
