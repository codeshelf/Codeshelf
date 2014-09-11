package com.gadgetworks.codeshelf.ws.jetty.io;

import javax.websocket.EncodeException;
import javax.websocket.Encoder;
import javax.websocket.EndpointConfig;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gadgetworks.codeshelf.ws.jetty.protocol.message.MessageABC;

public class JsonEncoder implements Encoder.Text<MessageABC> {
	final public static int WEBSOCKET_MAX_MESSAGE_SIZE = Integer.MAX_VALUE; /// tested as long type with extremely large value (50,000,000,000) , did not cause out-of-memory
	final public static int JSON_COMPRESS_THRESHOLD = WEBSOCKET_MAX_MESSAGE_SIZE - 100;

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
			
			if(jsonString.length() >= JSON_COMPRESS_THRESHOLD ) {
				// Note: When triggered, compression will momentarily use ~3x the size of the message in RAM, 
				// while converting it to bytes, compressing it, and converting it back to a string.
				CompressedJsonMessage compressedMessage = new CompressedJsonMessage(jsonString,false);
				//LOGGER.debug("Compressed "+jsonString.length()+" bytes to "+compressedMessage.getCompressedLength()+" bytes");
				return compressedMessage.getCompressed();
			} else {
				return jsonString;
			}
		} 
		catch (Exception e) {
			LOGGER.error("Failed to encode response", e);
			throw new EncodeException(message,"Failed to encode response",e);
		} 
	}
}
