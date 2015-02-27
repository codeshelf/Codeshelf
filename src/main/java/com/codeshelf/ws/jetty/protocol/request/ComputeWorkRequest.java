package com.codeshelf.ws.jetty.protocol.request;

import java.util.LinkedList;

import lombok.Getter;
import lombok.Setter;

public class ComputeWorkRequest extends DeviceRequest {
	
	@Getter @Setter
	LinkedList<String> containerIds;
	
	@Getter @Setter
	Boolean reversePick = false;
	
	public ComputeWorkRequest() {
	}
	
	public ComputeWorkRequest(String cheId, LinkedList<String> containerIds, Boolean reversePick) {
		setDeviceId(cheId);
		this.containerIds = containerIds;
		this.reversePick = reversePick;
	}
}
