package com.codeshelf.ws.protocol.response;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@ToString(callSuper=true)
public class ObjectUpdateResponse extends ResponseABC {
	@Getter @Setter
	Object results;
}
