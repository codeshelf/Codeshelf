package com.codeshelf.ws.protocol.request;

import lombok.Getter;
import lombok.Setter;

import com.codeshelf.ws.protocol.message.MessageABC;

//@IndexSubclasses
//@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.WRAPPER_OBJECT)
public abstract class RequestABC extends MessageABC {
	@Getter @Setter
	private String deviceId;

	@Override
	public String getDeviceIdentifier() {
		return getDeviceId();
	}
}
