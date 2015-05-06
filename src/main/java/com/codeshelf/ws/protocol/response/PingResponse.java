package com.codeshelf.ws.protocol.response;

import lombok.Getter;
import lombok.Setter;

public class PingResponse extends ResponseABC {

	@Getter @Setter
	long startTime;
	
	public PingResponse() {
	}

	@Override
	public String getDeviceIdentifier() {
		return null;
	}
}
