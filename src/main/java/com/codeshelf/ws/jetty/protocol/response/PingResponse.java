package com.codeshelf.ws.jetty.protocol.response;

import lombok.Getter;
import lombok.Setter;

public class PingResponse extends ResponseABC {

	@Getter @Setter
	long startTime;
	
	public PingResponse() {
	}
}
