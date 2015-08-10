package com.codeshelf.ws.protocol.request;

import lombok.Getter;

public class PalletizerNewLocationRequest extends DeviceRequestABC{
	@Getter
	private String item;
	
	@Getter
	private String location;

	public PalletizerNewLocationRequest() {
	}
	
	public PalletizerNewLocationRequest(String cheId, String item, String location) {
		setDeviceId(cheId);
		this.item = item;
		this.location = location;
	}
}
