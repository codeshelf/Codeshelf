package com.gadgetworks.codeshelf.ws.jetty.protocol.response;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

import com.gadgetworks.codeshelf.model.domain.WorkInstruction;

public class GetWorkResponse extends ResponseABC {

	@Getter @Setter
	String networkGuid;

	@Getter @Setter
	List<WorkInstruction> workInstructions;
}
