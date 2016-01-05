package com.codeshelf.ws.protocol.request;

import java.util.UUID;

import lombok.Getter;

public class PalletizerCompleteWiRequest extends DeviceRequestABC{
	@Getter
	private UUID wiId;
	
	@Getter
	private Boolean shorted;
	
	public PalletizerCompleteWiRequest() {
	}
	
	public PalletizerCompleteWiRequest(String deviceGuid, UUID wiId, Boolean shorted) {
		setDeviceId(deviceGuid);
		this.wiId = wiId;
		this.shorted = shorted;
	}
}
