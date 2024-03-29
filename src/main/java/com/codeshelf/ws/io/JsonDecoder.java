package com.codeshelf.ws.io;

import javax.websocket.DecodeException;
import javax.websocket.Decoder;
import javax.websocket.EndpointConfig;

import org.atteo.classindex.ClassIndex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.model.domain.Point;
import com.codeshelf.model.domain.Vertex;
import com.codeshelf.ws.protocol.message.MessageABC;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

public class JsonDecoder implements Decoder.Text<MessageABC> {

	private static final Logger	LOGGER = LoggerFactory.getLogger(JsonDecoder.class);
	
	public JsonDecoder() {
	}

	@Override
	public void destroy() {
	}

	@Override
	public void init(EndpointConfig ec) {
	}

	@Override
	public MessageABC decode(String rawMessage) throws DecodeException {
		String decompressedMessage = new CompressedJsonMessage(rawMessage,true).getUncompressed();

		try {
			LOGGER.debug("Decoding message: "+decompressedMessage);
			ObjectMapper mapper = new ObjectMapper(); // TODO: should reuse and share globally per jackson documentation -ic
			mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

			// register classes
			Iterable<Class<? extends MessageABC>> requestClasses = ClassIndex.getSubclasses(MessageABC.class);
			for (Class<? extends MessageABC> requestType : requestClasses) {
				mapper.registerSubtypes(requestType);
			}
			mapper.registerSubtypes(Point.class);
			mapper.registerSubtypes(Vertex.class);
			// decode message
			MessageABC message = mapper.readValue(decompressedMessage, MessageABC.class);
			return message;
		}
		catch (Exception e) {
			LOGGER.error("Failed to decode request: "+decompressedMessage, e);
			throw new DecodeException(decompressedMessage, "Failed to decode request", e);
		}
	}

	@Override
	public boolean willDecode(String arg0) {
        return true;
	}
}
