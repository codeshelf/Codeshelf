package com.codeshelf.ws.jetty.protocol.request;

import lombok.Getter;

public class InventoryScanRequest extends DeviceRequest {

	@Getter
	String gtin;
	
	@Getter
	String location;
	
	public InventoryScanRequest() {
	}
	
	public InventoryScanRequest(String cheId, String inGtin, String inLocation) {
		this.gtin = inGtin;
		this.location = inLocation;
		setDeviceId(cheId);
	}
}