package com.codeshelf.ws.protocol.request;

import com.codeshelf.ws.protocol.message.MessageABC;

//@IndexSubclasses
//@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.WRAPPER_OBJECT)
public abstract class RequestABC extends MessageABC {
	@Override
	public String getDeviceIdentifier() {
		return null;
	}
}
