package com.codeshelf.ws.protocol.response;

import com.codeshelf.behavior.PalletizerBehavior.PalletizerRemoveInfo;

import lombok.Getter;
import lombok.Setter;

public class PalletizerRemoveOrderResponse extends DeviceResponseABC {
	@Getter @Setter
	private PalletizerRemoveInfo info;
}
