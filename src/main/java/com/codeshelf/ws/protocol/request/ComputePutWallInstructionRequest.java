package com.codeshelf.ws.protocol.request;

import lombok.Getter;

public class ComputePutWallInstructionRequest extends DeviceRequest {

	@Getter
	String itemOrUpc;
	
	@Getter
	String putWallName;
	
	public ComputePutWallInstructionRequest() {
	}
	
	public ComputePutWallInstructionRequest(String cheId, String itemOrUpc, String putWallName) {
		setDeviceId(cheId);
		this.itemOrUpc = itemOrUpc;
		this.putWallName = putWallName;
	}
}
