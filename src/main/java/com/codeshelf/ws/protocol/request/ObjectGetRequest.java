package com.codeshelf.ws.protocol.request;

import lombok.Getter;
import lombok.Setter;

public class ObjectGetRequest extends RequestABC {

	@Getter @Setter
	String className;
	
	@Getter @Setter
	String persistentId;
	
	@Getter @Setter
	String getterMethod;
}
