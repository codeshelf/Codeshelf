package com.codeshelf.ws.protocol.response;

import com.codeshelf.behavior.InfoBehavior.InfoPackage;

import lombok.Getter;
import lombok.Setter;

public class InfoResponse extends DeviceResponseABC{
	@Getter @Setter
	private InfoPackage info;
}
