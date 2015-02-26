package com.codeshelf.ws.jetty.protocol.request;

import lombok.Getter;

public class GetWorkRequest extends DeviceRequest {

	@Getter
	String locationId;
	
	@Getter 
	Boolean reverseOrderFromLastTime = false;
	
	@Getter 
	Boolean reversePickOrder = false;
	
	public GetWorkRequest() {
	}
	
	public GetWorkRequest(String cheId, String locationId, Boolean reversePickOrder, Boolean reverseOrderFromLastTime) {
		setDeviceId(cheId);
		this.locationId = locationId;
		this.reversePickOrder = reversePickOrder;
		this.reverseOrderFromLastTime = reverseOrderFromLastTime;
	}

}
