package com.codeshelf.ws.protocol.command;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.model.domain.Che;
import com.codeshelf.service.PalletizerService;
import com.codeshelf.ws.protocol.request.PalletizerRemoveOrderRequest;
import com.codeshelf.ws.protocol.response.PalletizerRemoveOrderResponse;
import com.codeshelf.ws.protocol.response.ResponseABC;
import com.codeshelf.ws.protocol.response.ResponseStatus;
import com.codeshelf.ws.server.WebSocketConnection;

public class PalletizerRemoveOrderCommand extends CommandABC{
	private static final Logger	LOGGER		= LoggerFactory.getLogger(PalletizerRemoveOrderCommand.class);

	private PalletizerRemoveOrderRequest	request;

	private PalletizerService				palletizerService;

	public PalletizerRemoveOrderCommand(WebSocketConnection connection, PalletizerRemoveOrderRequest request, PalletizerService palletizerService) {
		super(connection);
		this.request = request;
		this.palletizerService = palletizerService;
	}

	@Override
	public ResponseABC exec() {
		String cheId = request.getDeviceId();
		String license = request.getLicense();
		LOGGER.info("Palletizer Remove Order liwth license {} on che {} request ", license, cheId);
		PalletizerRemoveOrderResponse response = new PalletizerRemoveOrderResponse();
		Che che = Che.staticGetDao().findByPersistentId(UUID.fromString(cheId));
		if (che!=null) {
			String networkGuid =  che.getDeviceNetGuid().getHexStringNoPrefix();
			String error = palletizerService.removeOrder(che, license);
			response.setError(error);
			response.setNetworkGuid(networkGuid);
			response.setStatus(ResponseStatus.Success);
			return response;
		}
		LOGGER.warn("Palletizer Remove Order request error: Did not find CHE {}", cheId);
		response.setStatus(ResponseStatus.Fail);
		return response;
	}

}