package com.codeshelf.ws.protocol.message;

import lombok.Getter;
import lombok.Setter;

public abstract class DeviceMessageABC extends MessageABC{
	@Getter @Setter
	String netGuidStr;
	
	@Override
	public String getDeviceIdentifier() {
		return getNetGuidStr();
	}
}
