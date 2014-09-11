package com.gadgetworks.codeshelf.ws.jetty.io;

import javax.websocket.EncodeException;
import javax.websocket.Encoder;
import javax.websocket.EndpointConfig;

import net.jpountz.lz4.LZ4Factory;

import org.apache.ws.commons.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gadgetworks.codeshelf.ws.jetty.protocol.message.MessageABC;

public class JsonEncoder implements Encoder.Text<MessageABC> {
	final public static int WEBSOCKET_MAX_MESSAGE_SIZE = 500000;
	final public static int JSON_COMPRESS_THRESHOLD = WEBSOCKET_MAX_MESSAGE_SIZE-10000;
	final public static int JSON_COMPRESS_MAXIMUM = 1048576; //adjust to suit

	private static final Logger	LOGGER = LoggerFactory.getLogger(JsonEncoder.class);
	LZ4Factory lz4Factory = LZ4Factory.safeInstance();
	
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
			
			//LOGGER.debug("Encoding message: "+jsonString);
			
			if(jsonString.length() >= JSON_COMPRESS_THRESHOLD ) {
				String compressedJson = Base64.encode(lz4Factory.fastCompressor().compress(jsonString.getBytes("UTF-8")));
				LOGGER.debug("Compressed "+jsonString.length()+" bytes to "+compressedJson.length()+" bytes");
				return compressedJson;
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
