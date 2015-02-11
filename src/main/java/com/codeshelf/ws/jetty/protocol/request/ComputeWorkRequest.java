package com.codeshelf.ws.jetty.protocol.request;

import java.util.LinkedList;

import lombok.Getter;
import lombok.Setter;

public class ComputeWorkRequest extends DeviceRequest {
	
	@Getter @Setter
	LinkedList<String> containerIds;
	
	public ComputeWorkRequest() {
	}
	
	public ComputeWorkRequest(String cheId, LinkedList<String> containerIds) {
		setDeviceId(cheId);
		this.containerIds = containerIds;
	}
}
