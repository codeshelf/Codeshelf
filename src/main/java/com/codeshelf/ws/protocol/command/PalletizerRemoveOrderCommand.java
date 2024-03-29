package com.codeshelf.ws.protocol.command;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.behavior.PalletizerBehavior;
import com.codeshelf.behavior.PalletizerBehavior.PalletizerRemoveInfo;
import com.codeshelf.model.domain.Che;
import com.codeshelf.ws.protocol.request.PalletizerRemoveOrderRequest;
import com.codeshelf.ws.protocol.response.PalletizerRemoveOrderResponse;
import com.codeshelf.ws.protocol.response.ResponseABC;
import com.codeshelf.ws.protocol.response.ResponseStatus;
import com.codeshelf.ws.server.WebSocketConnection;

public class PalletizerRemoveOrderCommand extends CommandABC<PalletizerRemoveOrderRequest>{
	private static final Logger	LOGGER		= LoggerFactory.getLogger(PalletizerRemoveOrderCommand.class);

	private PalletizerBehavior				palletizerService;

	public PalletizerRemoveOrderCommand(WebSocketConnection connection, PalletizerRemoveOrderRequest request, PalletizerBehavior palletizerService) {
		super(connection, request);
		this.palletizerService = palletizerService;
	}

	@Override
	public ResponseABC exec() {
		String cheId = request.getDeviceId();
		String prefix = request.getPrefix();
		String scan = request.getScan();
		LOGGER.info("Palletizer Remove Order with scan {}{} on che {} request ", prefix, scan, cheId);
		PalletizerRemoveOrderResponse response = new PalletizerRemoveOrderResponse();
		Che che = Che.staticGetDao().findByPersistentId(UUID.fromString(cheId));
		if (che!=null) {
			String networkGuid = che.getDeviceNetGuid().getHexStringNoPrefix();
			PalletizerRemoveInfo info = palletizerService.removeOrder(che, prefix, scan);
			response.setInfo(info);
			response.setNetworkGuid(networkGuid);
			response.setStatus(ResponseStatus.Success);
			return response;
		}
		LOGGER.warn("Palletizer Remove Order request error: Did not find CHE {}", cheId);
		response.setStatus(ResponseStatus.Fail);
		return response;
	}

}
