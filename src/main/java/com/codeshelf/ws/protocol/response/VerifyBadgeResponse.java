package com.codeshelf.ws.protocol.response;

import lombok.Getter;
import lombok.Setter;

public class VerifyBadgeResponse extends DeviceResponseABC {
	@Getter @Setter
	private Boolean verified = false;
	
	@Getter @Setter
	private String workerNameUI;
}
