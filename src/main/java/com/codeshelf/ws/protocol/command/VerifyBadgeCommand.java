package com.codeshelf.ws.protocol.command;

import java.util.UUID;

import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.integration.PickSimulator;
import com.codeshelf.model.domain.Che;
import com.codeshelf.service.WorkService;
import com.codeshelf.ws.protocol.request.VerifyBadgeRequest;
import com.codeshelf.ws.protocol.response.ResponseABC;
import com.codeshelf.ws.protocol.response.VerifyBadgeResponse;
import com.codeshelf.ws.server.WebSocketConnection;

@RequiresPermissions("che:verifybadge")
public class VerifyBadgeCommand extends CommandABC {

	private static final Logger	LOGGER	= LoggerFactory.getLogger(VerifyBadgeCommand.class);
	private VerifyBadgeRequest	request;
	private WorkService			workService;

	public VerifyBadgeCommand(WebSocketConnection connection, VerifyBadgeRequest request, WorkService workService) {
		super(connection);
		this.request = request;
		this.workService = workService;
	}

	@Override
	public ResponseABC exec() {
		// intermittent test failures. Therefore, instrument a little timer here. The tests allow 4 seconds for log in.
		// Just running locally took about 5 ms.
		final long complaintThreshhold = 2000;
		long startTime = System.currentTimeMillis();

		VerifyBadgeResponse response = new VerifyBadgeResponse();
		String cheId = request.getDeviceId();
		Che che = Che.staticGetDao().findByPersistentId(UUID.fromString(cheId));
		if (che != null) {
			String networkGuid = che.getDeviceNetGuid().getHexStringNoPrefix();
			boolean verified = workService.badgeVerifiesOK(che, request.getBadge());
			response.setNetworkGuid(networkGuid);
			response.setVerified(verified);
		}

		long endTime = System.currentTimeMillis();
		if (endTime - startTime > complaintThreshhold) {
			LOGGER.error("VerifyBadgeCommand.exec took {} ms", endTime - startTime);
		}

		return response;
	}
}
