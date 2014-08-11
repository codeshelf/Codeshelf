package com.gadgetworks.codeshelf.ws.jetty.protocol.response;

import lombok.Getter;
import lombok.Setter;

public class ObjectDeleteResponse extends ResponseABC {
	@Getter @Setter
	Object results;
}
