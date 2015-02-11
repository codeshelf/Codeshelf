package com.codeshelf.ws.jetty.protocol.response;

import java.util.UUID;

import lombok.Getter;
import lombok.Setter;

public class CompleteWorkInstructionResponse extends ResponseABC {
	@Getter @Setter
	UUID workInstructionId;
}
