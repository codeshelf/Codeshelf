package com.codeshelf.ws.protocol.response;

import lombok.Getter;
import lombok.Setter;

import com.codeshelf.ws.protocol.message.MessageABC;
import com.fasterxml.jackson.annotation.JsonIgnore;

//@IndexSubclasses
//@JsonTypeInfo(use=JsonTypeInfo.Id.NAME, include=JsonTypeInfo.As.WRAPPER_OBJECT)
// @ToString
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
		// This part matches the original lomboc @ToString. We might consider removing the requestId
		String returnStr = " status:" + status + " statusMessage:" + statusMessage + " requestId:" + requestId;
		String className = this.getClass().getSimpleName(); // the class name is the useful part. Most of the responses do not have toString() overrides, and shouldn't
		return className + ":" + returnStr;
	}
}
