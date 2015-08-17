package com.codeshelf.ws.protocol.request;

import lombok.Getter;

public class PalletizerRemoveOrderRequest extends DeviceRequestABC{
	@Getter
	private String prefix;
	@Getter
	private String scan;


	public PalletizerRemoveOrderRequest() {
	}
	
	public PalletizerRemoveOrderRequest(String cheId, String prefix, String scan) {
		setDeviceId(cheId);
		this.prefix = prefix;
		this.scan = scan;
	}
}
