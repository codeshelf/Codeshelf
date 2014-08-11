package com.gadgetworks.codeshelf.ws.jetty.protocol.request;

import java.util.UUID;

import lombok.Getter;
import lombok.Setter;

import com.gadgetworks.codeshelf.model.domain.WorkInstruction;

public class CompleteWorkInstructionRequest extends RequestABC {

	@Getter @Setter
	WorkInstruction workInstruction;

	@Getter @Setter
	private UUID cheId;
	
	public CompleteWorkInstructionRequest(UUID cheId, WorkInstruction workInstruction) {
		this.cheId = cheId;
		this.workInstruction = workInstruction;
	}

}
