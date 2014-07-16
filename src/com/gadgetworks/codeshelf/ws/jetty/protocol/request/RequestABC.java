package com.gadgetworks.codeshelf.ws.jetty.protocol.request;

import lombok.Getter;

import org.atteo.classindex.IndexSubclasses;
import org.codehaus.jackson.annotate.JsonTypeInfo;

import com.eaio.uuid.UUID;

@IndexSubclasses
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.WRAPPER_OBJECT)
public abstract class RequestABC {

	@Getter
	private String requestId = new UUID().toString();
}
