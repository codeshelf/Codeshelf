package com.codeshelf.ws.protocol.command;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.behavior.PalletizerBehavior;
import com.codeshelf.model.domain.Che;
import com.codeshelf.ws.protocol.request.PalletizerCompleteWiRequest;
import com.codeshelf.ws.protocol.response.GenericDeviceResponse;
import com.codeshelf.ws.protocol.response.ResponseABC;
import com.codeshelf.ws.protocol.response.ResponseStatus;
import com.codeshelf.ws.server.WebSocketConnection;

public class PalletizerCompleteWiCommand extends CommandABC{
	private static final Logger	LOGGER		= LoggerFactory.getLogger(PalletizerCompleteWiCommand.class);

	private PalletizerCompleteWiRequest	request;

	private PalletizerBehavior				palletizerService;

	public PalletizerCompleteWiCommand(WebSocketConnection connection, PalletizerCompleteWiRequest request, PalletizerBehavior palletizerService) {
		super(connection);
		this.request = request;
		this.palletizerService = palletizerService;
	}

	@Override
	public ResponseABC exec() {
		String cheId = request.getDeviceId();
		UUID wiId = request.getWiId();
		Boolean shorted = request.getShorted();
		LOGGER.info("Palletizer Complete Wi with wi {}, shorted = {} on che {} request ", wiId, shorted, cheId);
		GenericDeviceResponse response = new GenericDeviceResponse();
		Che che = Che.staticGetDao().findByPersistentId(UUID.fromString(cheId));
		if (che!=null) {
			String networkGuid = che.getDeviceNetGuid().getHexStringNoPrefix();
			palletizerService.completeWi(wiId, shorted);
			response.setNetworkGuid(networkGuid);
			response.setStatus(ResponseStatus.Success);
			return response;
		}
		LOGGER.warn("Palletizer Complete Wi request error: Did not find CHE {}", cheId);
		response.setStatus(ResponseStatus.Fail);
		return response;
	}

}
