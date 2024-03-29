package com.codeshelf.ws.protocol.command;

import java.util.UUID;

import org.apache.shiro.authz.annotation.RequiresPermissions;

import com.codeshelf.behavior.WorkBehavior;
import com.codeshelf.model.domain.Che;
import com.codeshelf.ws.protocol.request.VerifyBadgeRequest;
import com.codeshelf.ws.protocol.response.ResponseABC;
import com.codeshelf.ws.protocol.response.ResponseStatus;
import com.codeshelf.ws.protocol.response.VerifyBadgeResponse;
import com.codeshelf.ws.server.WebSocketConnection;

@RequiresPermissions("che:verifybadge")
public class VerifyBadgeCommand extends CommandABC<VerifyBadgeRequest> {
	private WorkBehavior			workService;

	public VerifyBadgeCommand(WebSocketConnection connection, VerifyBadgeRequest request, WorkBehavior workService) {
		super(connection, request);
		this.workService = workService;
	}

	@Override
	public ResponseABC exec() {
		VerifyBadgeResponse response = new VerifyBadgeResponse();
		String cheId = request.getDeviceId();
		Che che = Che.staticGetDao().findByPersistentId(UUID.fromString(cheId));
		if (che != null) {
			String networkGuid = che.getDeviceNetGuid().getHexStringNoPrefix();
			String workerId = request.getBadge();
			String workerNameUI = workService.verifyBadgeAndGetWorkerName(che, workerId);
			response.setNetworkGuid(networkGuid);
			response.setWorkerId(workerId);
			response.setWorkerNameUI(workerNameUI);
			response.setVerified(workerNameUI != null);
			response.setCheName(che.getDomainId());
			response.setStatus(ResponseStatus.Success);
			return response;
		}
		response.setStatusMessage("Can't find CHE with id "+cheId);
		response.setStatus(ResponseStatus.Fail);
		return response;
	}
}
