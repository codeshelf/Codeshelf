package com.codeshelf.ws.protocol.request;

import lombok.Getter;

public class PutWallPlacementRequest extends DeviceRequestABC{
	@Getter
	private String orderId;
	
	@Getter
	private String locationId;
	
	public PutWallPlacementRequest() {}
	
	public PutWallPlacementRequest(String deviceId, String orderId, String locationId) {
		this.orderId = orderId;
		this.locationId = locationId;
		setDeviceId(deviceId);
	}
}
