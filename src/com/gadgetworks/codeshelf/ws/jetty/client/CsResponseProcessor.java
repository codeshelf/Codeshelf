package com.gadgetworks.codeshelf.ws.jetty.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gadgetworks.codeshelf.ws.jetty.response.EchoResponse;
import com.gadgetworks.codeshelf.ws.jetty.response.LoginResponse;
import com.gadgetworks.codeshelf.ws.jetty.response.ResponseABC;

public class CsResponseProcessor implements ResponseProcessor {
	private static final Logger	LOGGER = LoggerFactory.getLogger(LogResponseProcessor.class);

	@Override
	public void handleResponse(ResponseABC response) {
		LOGGER.info("Response received:"+response);
		// TODO: implement handling responses from server
		if (response instanceof LoginResponse) {
			LoginResponse LoginResponse = (LoginResponse) response;
			// some something with the login response...
		}
		else if (response instanceof EchoResponse) {
			EchoResponse echoResponse = (EchoResponse) response;
			LOGGER.info("Wow - got an echo: "+echoResponse.getMessage());
		}
	}
}
