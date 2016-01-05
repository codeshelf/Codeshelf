package com.codeshelf.ws.protocol.request;

import lombok.Getter;

public class PalletizerItemRequest extends DeviceRequestABC{
	@Getter
	private String item;
	
	@Getter
	private String userId;

	public PalletizerItemRequest() {
	}
	
	public PalletizerItemRequest(String cheId, String item, String userId) {
		setDeviceId(cheId);
		this.item = item;
		this.userId = userId;
	}
}
