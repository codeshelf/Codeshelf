package com.codeshelf.ws.protocol.request;

import lombok.Getter;

public class PalletizerRemoveOrderRequest extends DeviceRequestABC{
	@Getter
	private String license;

	public PalletizerRemoveOrderRequest() {
	}
	
	public PalletizerRemoveOrderRequest(String cheId, String license) {
		setDeviceId(cheId);
		this.license = license;
	}
}
