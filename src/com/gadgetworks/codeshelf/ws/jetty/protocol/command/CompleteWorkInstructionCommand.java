package com.gadgetworks.codeshelf.ws.jetty.protocol.command;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gadgetworks.codeshelf.model.domain.WorkInstruction;
import com.gadgetworks.codeshelf.service.WorkService;
import com.gadgetworks.codeshelf.ws.jetty.protocol.request.CompleteWorkInstructionRequest;
import com.gadgetworks.codeshelf.ws.jetty.protocol.response.CompleteWorkInstructionResponse;
import com.gadgetworks.codeshelf.ws.jetty.protocol.response.ResponseABC;
import com.gadgetworks.codeshelf.ws.jetty.protocol.response.ResponseStatus;
import com.gadgetworks.codeshelf.ws.jetty.server.UserSession;

public class CompleteWorkInstructionCommand extends CommandABC {

	private static final Logger	LOGGER = LoggerFactory.getLogger(CompleteWorkInstructionCommand.class);

	final private CompleteWorkInstructionRequest request;

	final private WorkService workService;
	
	public CompleteWorkInstructionCommand(UserSession session, CompleteWorkInstructionRequest request, WorkService workService) {
		super(session);
		this.request = request;
		this.workService = workService;
	}

	@Override
	public ResponseABC exec() {

		UUID cheId = request.getCheId();
		WorkInstruction incomingWI = request.getWorkInstruction();
		CompleteWorkInstructionResponse response = new CompleteWorkInstructionResponse();
		response.setWorkInstructionId(incomingWI.getPersistentId());
		try {
			workService.completeWorkInstruction(cheId, incomingWI);
			response.setStatus(ResponseStatus.Success);	
		} catch(Exception e) {
			LOGGER.error("Unable to process completed work instruction: " + incomingWI, e);
			response.setStatus(ResponseStatus.Fail);
		}
		return response;
	}

}
