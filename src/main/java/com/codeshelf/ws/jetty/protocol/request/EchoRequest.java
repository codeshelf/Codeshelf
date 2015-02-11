package com.codeshelf.ws.jetty.protocol.request;

import lombok.Getter;
import lombok.Setter;

public class EchoRequest extends RequestABC {

	@Getter @Setter
	String message;
	
	public EchoRequest() {
	}
	
	public EchoRequest(String message) {
		this.message = message;
	}
}
