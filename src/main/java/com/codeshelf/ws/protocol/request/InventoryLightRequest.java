package com.codeshelf.ws.protocol.request;

import lombok.Getter;

public class InventoryLightRequest extends DeviceRequest {

	@Getter
	String gtin;
	
	public InventoryLightRequest() {
	}
	
	public InventoryLightRequest(String cheId, String inGtin) {
		this.gtin = inGtin;
		setDeviceId(cheId);
	}
}