package com.gadgetworks.codeshelf.ws.jetty.protocol.response;

import lombok.Getter;
import lombok.Setter;

public class ComputeWorkResponse extends ResponseABC {

	@Getter @Setter
	String networkGuid;
	
	@Getter @Setter
	Integer workInstructionCount = null;
}
