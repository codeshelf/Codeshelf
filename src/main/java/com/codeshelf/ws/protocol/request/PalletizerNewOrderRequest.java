package com.codeshelf.ws.protocol.request;

import lombok.Getter;

public class PalletizerNewOrderRequest extends DeviceRequestABC{
	@Getter
	private String item;
	
	@Getter
	private String location;

	public PalletizerNewOrderRequest() {
	}
	
	public PalletizerNewOrderRequest(String cheId, String item, String location) {
		setDeviceId(cheId);
		this.item = item;
		this.location = location;
	}
}
