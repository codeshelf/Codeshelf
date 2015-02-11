package com.codeshelf.ws.jetty.protocol.request;

import lombok.Getter;
import lombok.Setter;

public class PingRequest extends RequestABC {

	@Getter @Setter
	long startTime;
	
	public PingRequest() {
		startTime = System.currentTimeMillis();
	}
}
