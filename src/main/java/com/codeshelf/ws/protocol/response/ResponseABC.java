package com.codeshelf.ws.protocol.response;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import com.codeshelf.ws.protocol.message.MessageABC;
import com.fasterxml.jackson.annotation.JsonIgnore;

//@IndexSubclasses
//@JsonTypeInfo(use=JsonTypeInfo.Id.NAME, include=JsonTypeInfo.As.WRAPPER_OBJECT)
@ToString
public abstract class ResponseABC extends MessageABC {

	@Getter @Setter
	ResponseStatus status = ResponseStatus.Undefined;

	@Getter @Setter
	String statusMessage;
	
	@Getter @Setter
	String requestId;
	
	@JsonIgnore
	public boolean isSuccess() {
		return status.equals(ResponseStatus.Success);
	}

}
