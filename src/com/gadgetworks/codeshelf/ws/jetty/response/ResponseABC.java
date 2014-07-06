package com.gadgetworks.codeshelf.ws.jetty.response;

import lombok.Getter;
import lombok.Setter;

import org.atteo.classindex.IndexSubclasses;
import org.codehaus.jackson.annotate.JsonTypeInfo;

import com.eaio.uuid.UUID;

@IndexSubclasses
@JsonTypeInfo(use=JsonTypeInfo.Id.NAME, include=JsonTypeInfo.As.WRAPPER_OBJECT)
public abstract class ResponseABC {

	@Getter @Setter
	ResponseStatus status = ResponseStatus.Undefined;

	@Getter
	String responseId = new UUID().toString();
	
	@Getter @Setter
	String requestID;
}
