package com.gadgetworks.codeshelf.ws.jetty.io;

import javax.websocket.EncodeException;
import javax.websocket.Encoder;
import javax.websocket.EndpointConfig;

import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gadgetworks.codeshelf.ws.jetty.request.RequestABC;

public class JsonRequestEncoder implements Encoder.Text<RequestABC> {

	private static final Logger	LOGGER = LoggerFactory.getLogger(JsonRequestEncoder.class);

	@Override
	public void init(EndpointConfig ec) {
	}

	@Override
	public void destroy() {
	}

	@Override
	public String encode(RequestABC request) throws EncodeException {
		try {
			String jsonString = null;
			ObjectMapper mapper = new ObjectMapper();
			jsonString = mapper.writeValueAsString(request);
			LOGGER.info(jsonString);
			return jsonString;
		} 
		catch (Exception e) {
			LOGGER.error("Failed to encode request", e);
			throw new EncodeException(request,"Failed to encode request",e);
		} 
	}
}
