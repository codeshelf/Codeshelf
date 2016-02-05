package com.codeshelf.ws.protocol.request;

import com.codeshelf.behavior.PalletizerBehavior.PalletizerInfo;

import lombok.Getter;

public class PalletizerCompleteItemRequest extends DeviceRequestABC{
	@Getter
	private PalletizerInfo info;
	
	@Getter
	private Boolean shorted;
	
	@Getter
	private String userId;
	
	public PalletizerCompleteItemRequest() {
	}
	
	public PalletizerCompleteItemRequest(String deviceGuid, PalletizerInfo info, Boolean shorted, String userId) {
		setDeviceId(deviceGuid);
		this.info = info;
		this.shorted = shorted;
		this.userId = userId;
	}
}
