package com.gadgetworks.codeshelf.ws.jetty.protocol.request;

import java.util.LinkedList;

import lombok.Getter;
import lombok.Setter;

public class ComputeWorkRequest extends RequestABC {

	@Getter @Setter
	String cheId;
	
	@Getter @Setter
	LinkedList<String> containerIds;
	
	public ComputeWorkRequest() {
	}
	
	public ComputeWorkRequest(String cheId, LinkedList<String> containerIds) {
		this.cheId = cheId;
		this.containerIds = containerIds;
	}
}
