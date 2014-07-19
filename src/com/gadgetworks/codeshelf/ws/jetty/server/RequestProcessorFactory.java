package com.gadgetworks.codeshelf.ws.jetty.server;

public class RequestProcessorFactory {
	
	final static RequestProcessor requestProcessor = new ServerRequestProcessor();
	
	public static RequestProcessor getInstance() {
		// TODO: inject type
		return requestProcessor;
	}
}
