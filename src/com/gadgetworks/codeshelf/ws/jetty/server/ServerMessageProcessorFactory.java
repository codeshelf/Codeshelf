package com.gadgetworks.codeshelf.ws.jetty.server;

import com.gadgetworks.codeshelf.ws.jetty.protocol.message.MessageProcessor;

public class ServerMessageProcessorFactory {
	
	final static MessageProcessor requestProcessor = new ServerMessageProcessor();
	
	public static MessageProcessor getInstance() {
		// TODO: inject type
		return requestProcessor;
	}
}
