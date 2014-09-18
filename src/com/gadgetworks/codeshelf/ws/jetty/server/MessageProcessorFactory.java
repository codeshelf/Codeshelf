package com.gadgetworks.codeshelf.ws.jetty.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gadgetworks.codeshelf.ws.jetty.protocol.message.MessageProcessor;
import com.google.inject.Inject;
import com.google.inject.Provider;

public class MessageProcessorFactory {
	
	private static final Logger	LOGGER = LoggerFactory.getLogger(MessageProcessorFactory.class);

	@Inject
	static Provider<MessageProcessor> messageProcessorProvider;
	
	public static MessageProcessor getInstance() {
		if (messageProcessorProvider==null) {
			LOGGER.error("Unable to get message processor instance. Provider is undefined.");
			return null;
		}
		return messageProcessorProvider.get();
	}
}
