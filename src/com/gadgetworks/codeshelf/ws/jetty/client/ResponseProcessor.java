package com.gadgetworks.codeshelf.ws.jetty.client;

import lombok.Setter;

import com.gadgetworks.codeshelf.ws.jetty.protocol.response.ResponseABC;

public abstract class ResponseProcessor {
	
	@Setter
	MessageCoordinator messageCoordinator;
	
	public abstract void handleResponse(ResponseABC response);
}
