package com.codeshelf.ws.protocol.request;

import lombok.Getter;
import lombok.Setter;

public class DeviceRequestABC extends RequestABC {
	@Getter @Setter
	private String deviceId;

	@Override
	public String getDeviceIdentifier() {
		return getDeviceId();
	}
	
	@Override
	public String toString() {
		return String.format("%s(requestId=%s, deviceUUID=%s)", this.getClass().getSimpleName(), getMessageId(), deviceId);
	}
	
}
