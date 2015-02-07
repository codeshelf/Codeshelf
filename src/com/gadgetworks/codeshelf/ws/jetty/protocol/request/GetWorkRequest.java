package com.gadgetworks.codeshelf.ws.jetty.protocol.request;

import lombok.Getter;

public class GetWorkRequest extends DeviceRequest {

	@Getter
	String locationId;
	
	public GetWorkRequest() {
	}
	
	public GetWorkRequest(String cheId, String locationId) {
		setDeviceId(cheId);
		this.locationId = locationId;
	}

}
