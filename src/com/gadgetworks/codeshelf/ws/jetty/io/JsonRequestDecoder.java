package com.gadgetworks.codeshelf.ws.jetty.io;

import javax.websocket.DecodeException;
import javax.websocket.Decoder;
import javax.websocket.EndpointConfig;

import org.atteo.classindex.ClassIndex;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gadgetworks.codeshelf.ws.jetty.protocol.request.RequestABC;

public class JsonRequestDecoder implements Decoder.Text<RequestABC> {

	private static final Logger	LOGGER = LoggerFactory.getLogger(JsonRequestDecoder.class);

	@Override
	public void destroy() {
	}

	@Override
	public void init(EndpointConfig ec) {
	}

	@Override
	public RequestABC decode(String message) throws DecodeException {
		try {
			LOGGER.debug("Decoding request: "+message);
			// TODO: upgrade to Jackson 2+ and make mapper static
			ObjectMapper mapper = new ObjectMapper();
			Iterable<Class<? extends RequestABC>> classes = ClassIndex.getSubclasses(RequestABC.class);
			for (Class<? extends RequestABC> requestType : classes) {
				mapper.registerSubtypes(requestType);
			}
			RequestABC request = mapper.readValue(message, RequestABC.class);
			return request;
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