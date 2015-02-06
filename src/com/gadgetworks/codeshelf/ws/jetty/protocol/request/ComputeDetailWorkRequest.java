package com.gadgetworks.codeshelf.ws.jetty.protocol.request;

import lombok.Getter;
import lombok.Setter;

public class ComputeDetailWorkRequest extends DeviceRequest {

	@Getter
	String orderDetailId;
	
	public ComputeDetailWorkRequest() {
	}
	
	public ComputeDetailWorkRequest(String cheId, String orderDetailId) {
		setDeviceId(cheId);
		this.orderDetailId = orderDetailId;
	}
}
