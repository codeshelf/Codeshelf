package com.codeshelf.ws.protocol.request;

import lombok.Getter;

public class PalletizerItemRequest extends DeviceRequestABC{
	@Getter
	private String item;

	public PalletizerItemRequest() {
	}
	
	public PalletizerItemRequest(String cheId, String item) {
		setDeviceId(cheId);
		this.item = item;
	}
}
