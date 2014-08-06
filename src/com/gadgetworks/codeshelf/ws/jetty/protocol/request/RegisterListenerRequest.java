package com.gadgetworks.codeshelf.ws.jetty.protocol.request;

import java.util.List;
import java.util.UUID;

import lombok.Getter;
import lombok.Setter;

public class RegisterListenerRequest extends RequestABC {

	@Getter @Setter
	String className;
	
	@Getter @Setter
	List<String> propertyNames;

	@Getter @Setter
	List<UUID> objectIds;
}
