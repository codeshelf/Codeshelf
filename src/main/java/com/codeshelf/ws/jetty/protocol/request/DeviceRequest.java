package com.codeshelf.ws.jetty.protocol.request;

import lombok.Getter;
import lombok.Setter;

public class DeviceRequest extends RequestABC {
	@Getter @Setter
	private String deviceId;
}
