package com.gadgetworks.codeshelf.ws.jetty.protocol.request;

import lombok.Getter;
import lombok.Setter;

/*
 * Example Message:
 * 
 */


public class ObjectDeleteRequest extends RequestABC {

	@Getter @Setter
	String className;
	
	@Getter @Setter
	String persistentId;
}
