package com.gadgetworks.codeshelf.ws.jetty.server;

public class RequestProcessorFactory {
	
	final static RequestProcessor requestProcessor = new CsRequestProcessor();
	
	public static RequestProcessor getInstance() {
		// TODO: inject type
		return requestProcessor;
	}
}
