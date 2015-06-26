package com.codeshelf.ws.protocol.response;

import lombok.Getter;
import lombok.Setter;

public class TapeLocationDecodingResponse extends ResponseABC{
	@Getter @Setter
	private String decodedLocation;
}
