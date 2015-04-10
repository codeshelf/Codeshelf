package com.codeshelf.ws.protocol.command;

import java.util.UUID;

import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.model.domain.Che;
import com.codeshelf.service.WorkService;
import com.codeshelf.validation.MethodArgumentException;
import com.codeshelf.ws.protocol.request.ComputePutWallInstructionRequest;
import com.codeshelf.ws.protocol.response.GetPutWallInstructionResponse;
import com.codeshelf.ws.protocol.response.ResponseABC;
import com.codeshelf.ws.protocol.response.ResponseStatus;
import com.codeshelf.ws.server.WebSocketConnection;

@RequiresPermissions("wi:get")
public class ComputePutWallInstructionCommand extends CommandABC {
	@SuppressWarnings("unused")
	private static final Logger					LOGGER	= LoggerFactory.getLogger(ComputeWorkCommand.class);

	private ComputePutWallInstructionRequest	request;

	private WorkService							workService;

	public ComputePutWallInstructionCommand(WebSocketConnection connection,
		ComputePutWallInstructionRequest request,
		WorkService workService) {
		super(connection);
		this.request = request;
		this.workService = workService;
	}

	@Override
	public ResponseABC exec() {
		GetPutWallInstructionResponse response = null;
		String cheId = request.getDeviceId();
		Che che = Che.staticGetDao().findByPersistentId(UUID.fromString(cheId));
		if (che != null) {
			String networkGuid = che.getDeviceNetGuid().getHexStringNoPrefix();
			// Get the work instructions for this CHE and order detail
			try {
				response = workService.getPutWallInstructionsForItem(che, request.getItemOrUpc(), request.getPutWallName());
			} catch (MethodArgumentException e) {
				LOGGER.error("ComputePutWallInstructionCommand.exec", e);
				throw e;
			}

			response.setNetworkGuid(networkGuid);
			response.setStatus(ResponseStatus.Success);
			return response;
		}
		response = new GetPutWallInstructionResponse();
		response.setStatus(ResponseStatus.Fail);
		return response;
	}
}