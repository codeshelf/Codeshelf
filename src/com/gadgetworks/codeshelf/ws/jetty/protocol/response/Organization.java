package com.gadgetworks.codeshelf.ws.jetty.protocol.response;

import java.util.UUID;

import lombok.Getter;

public class Organization {
	@Getter
	UUID persistentId = UUID.randomUUID();
	
	
}
