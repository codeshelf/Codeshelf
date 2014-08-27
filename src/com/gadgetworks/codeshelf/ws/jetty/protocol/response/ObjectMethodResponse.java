package com.gadgetworks.codeshelf.ws.jetty.protocol.response;

import com.gadgetworks.codeshelf.validation.Errors;

import lombok.Getter;
import lombok.Setter;

public class ObjectMethodResponse extends ResponseABC {
	@Getter @Setter
	Object results;
	
	@Getter @Setter
	Errors errors;
}
