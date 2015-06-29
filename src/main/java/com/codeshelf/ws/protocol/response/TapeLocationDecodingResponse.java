package com.codeshelf.ws.protocol.response;

import lombok.Getter;
import lombok.Setter;

public class TapeLocationDecodingResponse extends DeviceResponseABC{
	@Getter @Setter
	private String decodedLocation;
}
