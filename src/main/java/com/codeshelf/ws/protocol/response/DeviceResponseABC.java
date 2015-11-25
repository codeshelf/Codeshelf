package com.codeshelf.ws.protocol.response;

import lombok.Getter;
import lombok.Setter;

public class DeviceResponseABC extends ResponseABC{
	@Getter	@Setter
	String			networkGuid;
	
	@Override
	public final String getDeviceIdentifier() {
		return getNetworkGuid();
	}
	
	public String toString() {
		return String.format("%s(requestId=%s, deviceId=%s, status=%s, statusMessage=%s)", this.getClass().getSimpleName(), requestId, getDeviceIdentifier(), status, statusMessage);
	}

}
