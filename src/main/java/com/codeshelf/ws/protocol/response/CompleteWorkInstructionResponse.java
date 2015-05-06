package com.codeshelf.ws.protocol.response;

import java.util.UUID;

import lombok.Getter;
import lombok.Setter;

public class CompleteWorkInstructionResponse extends ResponseABC {
	@Getter @Setter
	UUID workInstructionId;

	@Override
	public String getDeviceIdentifier() {
		return null;
	}
}
