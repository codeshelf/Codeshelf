package com.gadgetworks.codeshelf.ws.jetty.protocol.response;

import lombok.Getter;
import lombok.Setter;

import com.gadgetworks.codeshelf.validation.Errors;

public class ServiceMethodResponse extends ResponseABC {
	
	@Getter @Setter
	Object results;
	
	@Getter @Setter
	Errors errors;

}
