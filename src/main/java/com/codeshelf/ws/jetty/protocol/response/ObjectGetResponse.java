package com.codeshelf.ws.jetty.protocol.response;

import lombok.Getter;
import lombok.Setter;

public class ObjectGetResponse extends ResponseABC {

	@Getter @Setter
	Object results;
}
