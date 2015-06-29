package com.codeshelf.ws.protocol.request;

import lombok.Getter;

public class InventoryLightItemRequest extends DeviceRequestABC {

	@Getter
	String gtin;
	
	@Getter
	String location;
	
	public InventoryLightItemRequest() {
	}
	
	public InventoryLightItemRequest(String cheId, String inGtin) {
		this.gtin = inGtin;
		setDeviceId(cheId);
	}
}