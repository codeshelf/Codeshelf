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
}
