package com.gadgetworks.codeshelf.ws.jetty.server;

public class RequestProcessorFactory {
	
	public static RequestProcessor getInstance() {
		// TODO: inject type
		return new CsRequestProcessor();
	}
}
