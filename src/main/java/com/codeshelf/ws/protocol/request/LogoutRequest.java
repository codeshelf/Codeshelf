package com.codeshelf.ws.protocol.request;

import lombok.Getter;
import lombok.Setter;

public class LogoutRequest extends DeviceRequestABC{
	@Getter @Setter
	private String workerId;

	public LogoutRequest() {
	}
	
	public LogoutRequest(String workerId) {
		this.workerId = workerId;
	}
}
