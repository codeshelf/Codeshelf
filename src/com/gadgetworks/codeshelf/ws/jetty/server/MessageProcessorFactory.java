package com.gadgetworks.codeshelf.ws.jetty.server;

import com.gadgetworks.codeshelf.ws.jetty.protocol.message.MessageProcessor;
import com.google.inject.Inject;
import com.google.inject.Provider;

public class MessageProcessorFactory {
	
	@Inject 
	static Provider<MessageProcessor> messageProcessorProvider;
	
	public static MessageProcessor getInstance() {
		return messageProcessorProvider.get();
	}
}
