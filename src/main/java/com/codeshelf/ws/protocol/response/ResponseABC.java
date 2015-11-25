package com.codeshelf.ws.protocol.response;

import lombok.Getter;
import lombok.Setter;

import com.codeshelf.ws.protocol.message.MessageABC;
import com.fasterxml.jackson.annotation.JsonIgnore;

//@IndexSubclasses
//@JsonTypeInfo(use=JsonTypeInfo.Id.NAME, include=JsonTypeInfo.As.WRAPPER_OBJECT)
public abstract class ResponseABC extends MessageABC {

	@Getter
	@Setter
	ResponseStatus	status	= ResponseStatus.Undefined;

	@Getter
	@Setter
	String			statusMessage;

	@Getter
	@Setter
	String			requestId;

	@JsonIgnore
	public boolean isSuccess() {
		return status.equals(ResponseStatus.Success);
	}

	public String toString() {
		return String.format("%s(requestId=%s, status=%s, statusMessage=%s)", this.getClass().getSimpleName(), requestId, status, statusMessage);
	}
	
	@Override
	public String getDeviceIdentifier() {
		return null;
	}
}
