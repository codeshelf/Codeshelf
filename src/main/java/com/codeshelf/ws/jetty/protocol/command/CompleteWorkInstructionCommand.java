package com.codeshelf.ws.jetty.protocol.command;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.model.domain.WorkInstruction;
import com.codeshelf.service.WorkService;
import com.codeshelf.ws.jetty.protocol.request.CompleteWorkInstructionRequest;
import com.codeshelf.ws.jetty.protocol.response.CompleteWorkInstructionResponse;
import com.codeshelf.ws.jetty.protocol.response.ResponseABC;
import com.codeshelf.ws.jetty.protocol.response.ResponseStatus;
import com.codeshelf.ws.jetty.server.WebSocketConnection;

public class CompleteWorkInstructionCommand extends CommandABC {

	private static final Logger	LOGGER = LoggerFactory.getLogger(CompleteWorkInstructionCommand.class);

	final private CompleteWorkInstructionRequest request;

	final private WorkService workService;
	
	public CompleteWorkInstructionCommand(WebSocketConnection connection, CompleteWorkInstructionRequest request, WorkService workService) {
		super(connection);
		this.request = request;
		this.workService = workService;
	}

	@Override
	public ResponseABC exec() {

		UUID cheId = UUID.fromString(request.getDeviceId());
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
