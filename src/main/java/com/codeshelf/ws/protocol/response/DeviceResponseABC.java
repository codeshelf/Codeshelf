package com.codeshelf.ws.protocol.response;

import lombok.Getter;
import lombok.Setter;

public abstract class DeviceResponseABC extends ResponseABC{
	@Getter	@Setter
	String			networkGuid;
	
	@Override
	public final String getDeviceIdentifier() {
		return getNetworkGuid();
	}
}
