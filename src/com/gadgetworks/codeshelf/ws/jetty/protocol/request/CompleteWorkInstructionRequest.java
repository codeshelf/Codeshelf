package com.gadgetworks.codeshelf.ws.jetty.protocol.request;

import lombok.Getter;
import lombok.Setter;

import com.gadgetworks.codeshelf.model.domain.WorkInstruction;

public class CompleteWorkInstructionRequest extends DeviceRequest {

	@Getter
	@Setter
	WorkInstruction	workInstruction;

	public CompleteWorkInstructionRequest() {
	}

	public CompleteWorkInstructionRequest(String cheId, WorkInstruction workInstruction) {
		this.setDeviceId(cheId);
		this.workInstruction = workInstruction;
	}

}
