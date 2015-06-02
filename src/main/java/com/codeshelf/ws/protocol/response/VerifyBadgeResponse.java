package com.codeshelf.ws.protocol.response;

import lombok.Getter;
import lombok.Setter;

public class VerifyBadgeResponse extends ResponseABC {
	
	@Getter @Setter
	private String networkGuid;

	@Getter @Setter
	private Boolean verified = false;
	
	@Getter @Setter
	private String workerNameUI;
	
	@Override
	public String getDeviceIdentifier() {
		return getNetworkGuid();
	}
}
