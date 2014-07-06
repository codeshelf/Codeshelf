package com.gadgetworks.codeshelf.ws.jetty.io;

import javax.websocket.DecodeException;
import javax.websocket.Decoder;
import javax.websocket.EndpointConfig;

import org.atteo.classindex.ClassIndex;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gadgetworks.codeshelf.ws.jetty.response.ResponseABC;

public class JsonResponseDecoder implements Decoder.Text<ResponseABC> {

	private static final Logger	LOGGER = LoggerFactory.getLogger(JsonResponseDecoder.class);

	public JsonResponseDecoder() {
		LOGGER.debug("Creating JsonResponseDecoder");
	}
	
	@Override
	public void destroy() {
	}

	@Override
	public void init(EndpointConfig ec) {
	}

	@Override
	public ResponseABC decode(String message) throws DecodeException {
		try {
			// TODO: upgrade to Jackson 2+ and make mapper static
			ObjectMapper mapper = new ObjectMapper();
			Iterable<Class<? extends ResponseABC>> classes = ClassIndex.getSubclasses(ResponseABC.class);
			for (Class<? extends ResponseABC> requestType : classes) {
				mapper.registerSubtypes(requestType);
			}
			ResponseABC response = mapper.readValue(message, ResponseABC.class);
			return response;
		} 
		catch (Exception e) {
			LOGGER.error("Failed to decode request: "+message, e);
			throw new DecodeException(message, "Failed to decode request");
		}
	}

	@Override
	public boolean willDecode(String arg0) {
        return true;
	}
}