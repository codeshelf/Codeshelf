package com.gadgetworks.codeshelf.ws.jetty.io;

import javax.websocket.EncodeException;
import javax.websocket.Encoder;
import javax.websocket.EndpointConfig;

import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gadgetworks.codeshelf.ws.jetty.protocol.message.MessageABC;

public class JsonEncoder implements Encoder.Text<MessageABC> {

	private static final Logger	LOGGER = LoggerFactory.getLogger(JsonEncoder.class);

	@Override
	public void init(EndpointConfig ec) {
	}

	@Override
	public void destroy() {
	}

	@Override
	public String encode(MessageABC message) throws EncodeException {
		try {
			String jsonString = null;
			ObjectMapper mapper = new ObjectMapper();
			jsonString = mapper.writeValueAsString(message);
			LOGGER.debug("Encoding message: "+jsonString);
			return jsonString;
		} 
		catch (Exception e) {
			LOGGER.error("Failed to encode response", e);
			throw new EncodeException(message,"Failed to encode response",e);
		} 
	}
}
