package com.gadgetworks.codeshelf.ws.jetty.client;

public class ResponseProcessorFactory {
	
	public static ResponseProcessor getInstance() {
		// TODO: inject type
		return new CsResponseProcessor();
	}
}
