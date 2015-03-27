package com.codeshelf.ws.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.ws.protocol.message.IMessageProcessor;
import com.codeshelf.ws.protocol.message.MessageABC;
import com.codeshelf.ws.protocol.request.RequestABC;
import com.codeshelf.ws.protocol.response.ResponseABC;
import com.codeshelf.ws.server.WebSocketConnection;

public class LogResponseProcessor implements IMessageProcessor {

	private static final Logger	LOGGER = LoggerFactory.getLogger(LogResponseProcessor.class);

	@Override
	public void handleResponse(WebSocketConnection session, ResponseABC response) {
		LOGGER.info("Response received:"+response);
	}

	@Override
	public ResponseABC handleRequest(WebSocketConnection session, RequestABC request) {
		LOGGER.info("Request received:"+request);
		return null;
	}

	@Override
	public void handleMessage(WebSocketConnection session, MessageABC message) {
		LOGGER.info("Message received:"+message);
	}
}
