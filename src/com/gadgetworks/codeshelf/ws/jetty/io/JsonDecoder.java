package com.gadgetworks.codeshelf.ws.jetty.io;

import java.io.UnsupportedEncodingException;

import javax.websocket.DecodeException;
import javax.websocket.Decoder;
import javax.websocket.EndpointConfig;

import net.jpountz.lz4.LZ4Factory;

import org.apache.shiro.codec.Base64;
import org.atteo.classindex.ClassIndex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gadgetworks.codeshelf.model.domain.Point;
import com.gadgetworks.codeshelf.model.domain.Vertex;
import com.gadgetworks.codeshelf.ws.jetty.protocol.message.MessageABC;

public class JsonDecoder implements Decoder.Text<MessageABC> {

	private static final Logger	LOGGER = LoggerFactory.getLogger(JsonDecoder.class);
	LZ4Factory lz4Factory = LZ4Factory.safeInstance();
	
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
		//LOGGER.debug("Compressed message: "+rawMessage+ " ("+rawMessage.length()+" bytes)");
		String decompressedMessage;
		if(rawMessage.charAt(0)!='{') {
			// not valid json? compressed/base64
			
			byte[] rawBytes=rawMessage.getBytes();
			byte[] decomBytes = lz4Factory.safeDecompressor().decompress(Base64.decode(rawBytes),JsonEncoder.JSON_COMPRESS_MAXIMUM);
			try {
				decompressedMessage = new String(decomBytes,"UTF-8");
			} catch (UnsupportedEncodingException e1) {
				LOGGER.error("Not UTF-8 request", e1);
				throw new DecodeException("", "Not UTF-8 request", e1);
			}
		} else{
			// regular uncompressed json
			decompressedMessage=rawMessage;
		}
		try {
			LOGGER.debug("Decoding message: "+decompressedMessage);
			ObjectMapper mapper = new ObjectMapper();
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
