package com.codeshelf.ws.protocol.request;

import java.util.UUID;

import lombok.Getter;

public class PalletizerCompleteWiRequest extends DeviceRequestABC{
	@Getter
	private UUID wiId;
	
	@Getter
	private String userId;
	
	@Getter
	private Boolean shorted;
	
	public PalletizerCompleteWiRequest() {
	}
	
	public PalletizerCompleteWiRequest(String deviceGuid, UUID wiId, String userId, Boolean shorted) {
		setDeviceId(deviceGuid);
		this.wiId = wiId;
		this.userId = userId;
		this.shorted = shorted;
	}
}
