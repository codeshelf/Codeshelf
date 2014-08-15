package com.gadgetworks.codeshelf.ws.jetty.protocol.response;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import com.gadgetworks.codeshelf.ws.jetty.protocol.message.MessageABC;

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
}
