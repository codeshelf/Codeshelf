package com.gadgetworks.codeshelf.ws.jetty.protocol.request;

import lombok.Getter;

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
