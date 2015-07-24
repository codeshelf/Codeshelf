package com.codeshelf.ws.protocol.response;

import lombok.Getter;
import lombok.Setter;

public class InfoResponse extends DeviceResponseABC{
	@Getter @Setter
	private String[] info;
}
