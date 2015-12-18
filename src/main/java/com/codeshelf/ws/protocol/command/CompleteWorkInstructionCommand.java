package com.codeshelf.ws.protocol.command;

import java.util.UUID;

import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.behavior.WorkBehavior;
import com.codeshelf.model.domain.Che;
import com.codeshelf.model.domain.WorkInstruction;
import com.codeshelf.ws.protocol.request.CompleteWorkInstructionRequest;
import com.codeshelf.ws.protocol.response.CompleteWorkInstructionResponse;
import com.codeshelf.ws.protocol.response.ResponseABC;
import com.codeshelf.ws.protocol.response.ResponseStatus;
import com.codeshelf.ws.server.WebSocketConnection;

@RequiresPermissions("wi:complete")
public class CompleteWorkInstructionCommand extends CommandABC {

	private static final Logger						LOGGER	= LoggerFactory.getLogger(CompleteWorkInstructionCommand.class);

	final private CompleteWorkInstructionRequest	request;

	final private WorkBehavior						workBehavior;

	public CompleteWorkInstructionCommand(WebSocketConnection connection,
		CompleteWorkInstructionRequest request,
		WorkBehavior workService) {
		super(connection);
		this.request = request;
		this.workBehavior = workService;
	}

	@Override
	public ResponseABC exec() {
		UUID cheId = UUID.fromString(request.getDeviceId());
		WorkInstruction incomingWI = request.getWorkInstruction();
		CompleteWorkInstructionResponse response = new CompleteWorkInstructionResponse();
		Che che = Che.staticGetDao().findByPersistentId(cheId);
		String networkGuid = che.getDeviceNetGuid().getHexStringNoPrefix();
		response.setNetworkGuid(networkGuid);

		try {
			if (incomingWI == null) {
				throw new NullPointerException(String.format("could not find work instruction to complete for Che %s",
					cheId.toString()));
			}
			// incomingWI will never be null since site controller had it as it serialized the response.
			// However, server may no longer have it as it searches by persistent ID.  WorkBehavior.completeWorkInstruction() will throw an IllegalArgumentException.
			response.setWorkInstructionId(incomingWI.getPersistentId());
			workBehavior.completeWorkInstruction(cheId, incomingWI);
			response.setStatus(ResponseStatus.Success);
		} catch (IllegalArgumentException e2) {
			// no need to log, was logged sufficiently in WorkBehavior.completeWorkInstruction();
			response.setStatus(ResponseStatus.Fail);
		}
		catch (Exception e) {
			LOGGER.error("Unable to process completed work instruction: {}", incomingWI, e);
			response.setStatus(ResponseStatus.Fail);
		}
		return response;
	}

}
