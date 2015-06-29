package com.codeshelf.ws.protocol.request;

import lombok.Getter;

public class InventoryLightLocationRequest extends RequestABC {
	@Getter
	String location;
	
	@Getter
	Boolean isTape;
	
	public InventoryLightLocationRequest() {
	}
	
	public InventoryLightLocationRequest(String cheId, String inLocation, Boolean isTape) {
		this.location = inLocation;
		this.isTape = isTape;
		setDeviceId(cheId);
	}
}