package com.codeshelf.ws.protocol.request;

import lombok.Getter;

public class InventoryUpdateRequest extends DeviceRequestABC {

	@Getter
	String gtin;
	
	@Getter
	String location;
	
	@Getter
	String skuWallName;
	
	public InventoryUpdateRequest() {
	}
	
	public InventoryUpdateRequest(String cheId, String inGtin, String inLocation, String skuWallName) {
		this.gtin = inGtin;
		this.location = inLocation;
		this.skuWallName = skuWallName;
		setDeviceId(cheId);
	}
}