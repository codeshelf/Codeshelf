package com.codeshelf.ws.protocol.command;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.behavior.PalletizerBehavior;
import com.codeshelf.behavior.PalletizerBehavior.PalletizerInfo;
import com.codeshelf.model.domain.Che;
import com.codeshelf.ws.protocol.request.PalletizerNewOrderRequest;
import com.codeshelf.ws.protocol.response.PalletizerItemResponse;
import com.codeshelf.ws.protocol.response.ResponseABC;
import com.codeshelf.ws.protocol.response.ResponseStatus;
import com.codeshelf.ws.server.WebSocketConnection;

public class PalletizerNewOrderCommand extends CommandABC {
	private static final Logger	LOGGER		= LoggerFactory.getLogger(PalletizerNewOrderCommand.class);

	private PalletizerNewOrderRequest	request;

	private PalletizerBehavior			palletizerService;

	public PalletizerNewOrderCommand(WebSocketConnection connection, PalletizerNewOrderRequest request, PalletizerBehavior palletizerService) {
		super(connection);
		this.request = request;
		this.palletizerService = palletizerService;
	}

	@Override
	public ResponseABC exec() {
		String cheId = request.getDeviceId();
		String location = request.getLocation();
		String item = request.getItem();
		String userId = request.getUserId();
		LOGGER.info("Palletizer New Order {} for Item {} on che {} request ", item, cheId);
		PalletizerItemResponse response = new PalletizerItemResponse();
		Che che = Che.staticGetDao().findByPersistentId(UUID.fromString(cheId));
		if (che!=null) {
			String networkGuid =  che.getDeviceNetGuid().getHexStringNoPrefix();
			PalletizerInfo info = palletizerService.processPalletizerNewOrderRequest(che, item, location, userId);
			response.setInfo(info);
			response.setNetworkGuid(networkGuid);
			response.setStatus(ResponseStatus.Success);
			return response;
		}
		LOGGER.warn("Palletizer New Order request error: Did not find CHE {}", cheId);
		response.setStatus(ResponseStatus.Fail);
		return response;
	}
}
