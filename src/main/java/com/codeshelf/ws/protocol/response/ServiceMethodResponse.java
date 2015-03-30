package com.codeshelf.ws.protocol.response;

import lombok.Getter;
import lombok.Setter;

import com.codeshelf.validation.Errors;

public class ServiceMethodResponse extends ResponseABC {
	
	@Getter @Setter
	Object results;
	
	@Getter @Setter
	Errors errors;

}
