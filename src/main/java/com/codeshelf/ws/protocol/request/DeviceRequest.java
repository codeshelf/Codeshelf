package com.codeshelf.ws.protocol.request;

import lombok.Getter;
import lombok.Setter;

public abstract class DeviceRequest extends RequestABC {
	@Getter @Setter
	private String deviceId;

	@Override
	public String getDeviceIdentifier() {
		return getDeviceId();
	}

}
