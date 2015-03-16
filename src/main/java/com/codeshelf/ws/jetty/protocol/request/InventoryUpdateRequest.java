package com.codeshelf.ws.jetty.protocol.request;

import lombok.Getter;

public class InventoryUpdateRequest extends DeviceRequest {

	@Getter
	String gtin;
	
	@Getter
	String location;
	
	public InventoryUpdateRequest() {
	}
	
	public InventoryUpdateRequest(String cheId, String inGtin, String inLocation) {
		this.gtin = inGtin;
		this.location = inLocation;
		setDeviceId(cheId);
	}
}