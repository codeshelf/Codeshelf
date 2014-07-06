package com.gadgetworks.codeshelf.ws.jetty.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gadgetworks.codeshelf.ws.jetty.response.ResponseABC;

public class LogResponseProcessor implements ResponseProcessor {

	private static final Logger	LOGGER = LoggerFactory.getLogger(LogResponseProcessor.class);

	@Override
	public void handleResponse(ResponseABC response) {
		LOGGER.info("Response received:"+response);
	}
}
