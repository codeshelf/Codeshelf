package com.gadgetworks.codeshelf.ws.jetty.protocol.response;

import lombok.Getter;
import lombok.Setter;

public class ObjectMethodResponse extends ResponseABC {
	@Getter @Setter
	Object results;
}
