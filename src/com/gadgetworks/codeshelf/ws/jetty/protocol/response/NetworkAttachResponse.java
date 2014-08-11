package com.gadgetworks.codeshelf.ws.jetty.protocol.response;

import java.util.UUID;

import lombok.Getter;
import lombok.Setter;

public class NetworkAttachResponse extends ResponseABC {

	@Getter @Setter
	String className;
	
	@Getter @Setter
	UUID networkId;
	
	@Getter @Setter
	String domainId;
	
	@Getter @Setter
	String description;
	
	public NetworkAttachResponse() {
	}
}
