package com.gadgetworks.codeshelf.ws.jetty.io;

import javax.websocket.DecodeException;
import javax.websocket.Decoder;
import javax.websocket.EndpointConfig;

import org.atteo.classindex.ClassIndex;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gadgetworks.codeshelf.ws.jetty.protocol.message.MessageABC;
import com.gadgetworks.codeshelf.ws.jetty.protocol.request.RequestABC;
import com.gadgetworks.codeshelf.ws.jetty.protocol.response.ResponseABC;

public class JsonDecoder implements Decoder.Text<MessageABC> {

	private static final Logger	LOGGER = LoggerFactory.getLogger(JsonDecoder.class);

	public JsonDecoder() {
		LOGGER.debug("Creating "+this.getClass().getSimpleName());
	}
	
	@Override
	public void destroy() {
	}

	@Override
	public void init(EndpointConfig ec) {
	}

	@Override
	public MessageABC decode(String rawMessage) throws DecodeException {
		try {
			LOGGER.debug("Decoding message: "+rawMessage);
			ObjectMapper mapper = new ObjectMapper();
			// register classes
			Iterable<Class<? extends MessageABC>> requestClasses = ClassIndex.getSubclasses(MessageABC.class);
			for (Class<? extends MessageABC> requestType : requestClasses) {
				mapper.registerSubtypes(requestType);
			}
			/*
			Iterable<Class<? extends ResponseABC>> responseClasses = ClassIndex.getSubclasses(ResponseABC.class);
			for (Class<? extends ResponseABC> responseType : responseClasses) {
				mapper.registerSubtypes(responseType);
			}
			*/			
			// decode message
			MessageABC message = mapper.readValue(rawMessage, MessageABC.class);
			return message;
		} 
		catch (Exception e) {
			LOGGER.error("Failed to decode request: "+rawMessage, e);
			throw new DecodeException(rawMessage, "Failed to decode request");
		}
	}

	@Override
	public boolean willDecode(String arg0) {
        return true;
	}
}