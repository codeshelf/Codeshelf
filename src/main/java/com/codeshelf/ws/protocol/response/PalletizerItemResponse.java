package com.codeshelf.ws.protocol.response;

import com.codeshelf.behavior.PalletizerBehavior.PalletizerInfo;

import lombok.Getter;
import lombok.Setter;

public class PalletizerItemResponse extends DeviceResponseABC{
	@Getter @Setter
	private PalletizerInfo info;	
}
