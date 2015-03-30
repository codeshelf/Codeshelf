package com.codeshelf.ws.protocol.message;

import lombok.Getter;

import org.atteo.classindex.IndexSubclasses;

import com.eaio.uuid.UUID;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@IndexSubclasses
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.WRAPPER_OBJECT)
public class MessageABC {

	@Getter
	private String messageId = new UUID().toString();
}
