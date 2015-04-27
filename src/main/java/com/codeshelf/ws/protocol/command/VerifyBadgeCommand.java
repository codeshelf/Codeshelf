package com.codeshelf.ws.protocol.command;

import java.util.UUID;

import org.apache.shiro.authz.annotation.RequiresPermissions;

import com.codeshelf.model.domain.Che;
import com.codeshelf.service.WorkService;
import com.codeshelf.ws.protocol.request.VerifyBadgeRequest;
import com.codeshelf.ws.protocol.response.ResponseABC;
import com.codeshelf.ws.protocol.response.ResponseStatus;
import com.codeshelf.ws.protocol.response.VerifyBadgeResponse;
import com.codeshelf.ws.server.WebSocketConnection;

@RequiresPermissions("che:verifybadge")
public class VerifyBadgeCommand extends CommandABC {

	private VerifyBadgeRequest	request;
	private WorkService			workService;

	public VerifyBadgeCommand(WebSocketConnection connection, VerifyBadgeRequest request, WorkService workService) {
		super(connection);
		this.request = request;
		this.workService = workService;
	}

	@Override
	public ResponseABC exec() {
		VerifyBadgeResponse response = new VerifyBadgeResponse();
		String cheId = request.getDeviceId();
		Che che = Che.staticGetDao().findByPersistentId(UUID.fromString(cheId));
		if (che != null) {
			String networkGuid = che.getDeviceNetGuid().getHexStringNoPrefix();
			boolean verified = workService.badgeVerifiesOK(che, request.getBadge());
			response.setNetworkGuid(networkGuid);
			response.setVerified(verified);
			response.setStatus(ResponseStatus.Success);
			return response;
		}
		response.setStatusMessage("Can't find CHE with id "+cheId);
		response.setStatus(ResponseStatus.Fail);
		return response;
	}
}