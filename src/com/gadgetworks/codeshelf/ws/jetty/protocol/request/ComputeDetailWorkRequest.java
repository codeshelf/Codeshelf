package com.gadgetworks.codeshelf.ws.jetty.protocol.request;

import lombok.Getter;

public class ComputeDetailWorkRequest extends RequestABC {

	@Getter
	String cheId;
	
	@Getter
	String orderDetailId;
	
	public ComputeDetailWorkRequest() {
	}
	
	public ComputeDetailWorkRequest(String cheId, String orderDetailId) {
		this.cheId = cheId;
		this.orderDetailId = orderDetailId;
	}
}
