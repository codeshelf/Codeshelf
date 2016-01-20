package com.codeshelf.ws.protocol.response;

import java.util.UUID;

import lombok.Getter;
import lombok.Setter;

public class CompleteWorkInstructionResponse extends DeviceResponseABC {
	@Getter @Setter
	UUID workInstructionId;
}
