package com.codeshelf.ws.protocol.command;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.behavior.PalletizerBehavior;
import com.codeshelf.behavior.PalletizerBehavior.PalletizerInfo;
import com.codeshelf.model.domain.Che;
import com.codeshelf.ws.protocol.request.PalletizerItemRequest;
import com.codeshelf.ws.protocol.response.PalletizerItemResponse;
import com.codeshelf.ws.protocol.response.ResponseABC;
import com.codeshelf.ws.protocol.response.ResponseStatus;
import com.codeshelf.ws.server.WebSocketConnection;

public class PalletizerItemCommand extends CommandABC{
	private static final Logger	LOGGER	= LoggerFactory.getLogger(PalletizerItemCommand.class);

	private PalletizerItemRequest	request;

	private PalletizerBehavior		palletizerService;

	public PalletizerItemCommand(WebSocketConnection connection, PalletizerItemRequest request, PalletizerBehavior palletizerService) {
		super(connection);
		this.request = request;
		this.palletizerService = palletizerService;
	}

	@Override
	public ResponseABC exec() {
		String cheId = request.getDeviceId();
		String item = request.getItem();
		String userId = request.getUserId();
		LOGGER.info("Palletizer Item {} request for {}", item, cheId);
		PalletizerItemResponse response = new PalletizerItemResponse();
		Che che = Che.staticGetDao().findByPersistentId(UUID.fromString(cheId));
		if (che!=null) {
			String networkGuid =  che.getDeviceNetGuid().getHexStringNoPrefix();
			PalletizerInfo info = palletizerService.processPalletizerItemRequest(che, item, userId);
			response.setInfo(info);
			response.setNetworkGuid(networkGuid);
			response.setStatus(ResponseStatus.Success);
			return response;
		}
		LOGGER.warn("Palletizer Item request error: Did not find CHE {}", cheId);
		
		response.setStatus(ResponseStatus.Fail);
		return response;
	}
}
