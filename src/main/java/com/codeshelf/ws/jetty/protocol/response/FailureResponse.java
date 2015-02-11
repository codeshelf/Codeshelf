package com.codeshelf.ws.jetty.protocol.response;

import lombok.Getter;
import lombok.Setter;

public class FailureResponse extends ResponseABC{
	@Getter @Setter
	private String cheId;
	
	public FailureResponse() { 
	}
	
	public FailureResponse(String message) {
		setStatus(ResponseStatus.Fail);
		setStatusMessage(message);
	}
}
