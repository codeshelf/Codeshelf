package com.codeshelf.ws.protocol.command;

import java.util.UUID;

import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.model.domain.WorkInstruction;
import com.codeshelf.service.WorkService;
import com.codeshelf.ws.protocol.request.CompleteWorkInstructionRequest;
import com.codeshelf.ws.protocol.response.CompleteWorkInstructionResponse;
import com.codeshelf.ws.protocol.response.ResponseABC;
import com.codeshelf.ws.protocol.response.ResponseStatus;
import com.codeshelf.ws.server.WebSocketConnection;

@RequiresPermissions("wi:complete")
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
		try {
			if (incomingWI == null) {
				throw new NullPointerException(String.format("could not find work instruction to complete for Che %s", cheId.toString()));
			}
			// Interesting. incomingWI is not null  in JUnit test DataArchiving.testPurgeActiveJobs(). But then completeWorkInstruction() will throw on the commit.
			response.setWorkInstructionId(incomingWI.getPersistentId());
			workService.completeWorkInstruction(cheId, incomingWI);
			response.setStatus(ResponseStatus.Success);	
		} catch(Exception e) {
			LOGGER.error("Unable to process completed work instruction: " + incomingWI, e);
			response.setStatus(ResponseStatus.Fail);
		}
		return response;
	}

}
