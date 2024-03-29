package com.codeshelf.ws.protocol.command;

import java.util.UUID;

import com.codeshelf.behavior.WorkBehavior;
import com.codeshelf.model.domain.Che;
import com.codeshelf.ws.protocol.request.LogoutRequest;
import com.codeshelf.ws.protocol.response.GenericDeviceResponse;
import com.codeshelf.ws.protocol.response.ResponseABC;
import com.codeshelf.ws.protocol.response.ResponseStatus;
import com.codeshelf.ws.server.WebSocketConnection;

public class LogoutCommand extends CommandABC<LogoutRequest>{
	private WorkBehavior		workBehavior;

	public LogoutCommand(WebSocketConnection connection, LogoutRequest request, WorkBehavior workBehavior) {
		super(connection, request);
		this.workBehavior = workBehavior;
	}

	@Override
	public ResponseABC exec() {
		GenericDeviceResponse response = new GenericDeviceResponse();
		String cheId = request.getDeviceId();
		Che che = Che.staticGetDao().findByPersistentId(UUID.fromString(cheId));
		if (che != null) {
			String workerId = request.getWorkerId();
			workBehavior.logoutWorkerFromChe(che, workerId);
			String networkGuid = che.getDeviceNetGuid().getHexStringNoPrefix();
			response.setNetworkGuid(networkGuid);
			response.setStatus(ResponseStatus.Success);
			return response;
		}
		response.setStatusMessage("Can't find CHE with id "+cheId);
		response.setStatus(ResponseStatus.Fail);
		return response;
	}

}
