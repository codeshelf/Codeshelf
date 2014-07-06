package com.gadgetworks.codeshelf.ws.jetty.response;

import lombok.Getter;
import lombok.Setter;

public class EchoResponse extends ResponseABC {

	@Getter @Setter
	String message;
	
	public EchoResponse() {
	}

	public EchoResponse(String message) {
		this.message = message;
	}
}
