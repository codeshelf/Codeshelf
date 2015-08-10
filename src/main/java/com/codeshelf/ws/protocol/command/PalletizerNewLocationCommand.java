package com.codeshelf.ws.protocol.command;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.model.domain.Che;
import com.codeshelf.service.WorkService;
import com.codeshelf.service.WorkService.PalletizerInfo;
import com.codeshelf.ws.protocol.request.PalletizerNewLocationRequest;
import com.codeshelf.ws.protocol.response.PalletizerItemResponse;
import com.codeshelf.ws.protocol.response.ResponseABC;
import com.codeshelf.ws.protocol.response.ResponseStatus;
import com.codeshelf.ws.server.WebSocketConnection;

public class PalletizerNewLocationCommand extends CommandABC {
	private static final Logger	LOGGER		= LoggerFactory.getLogger(PalletizerNewLocationCommand.class);

	private PalletizerNewLocationRequest	request;

	private WorkService						workService;

	public PalletizerNewLocationCommand(WebSocketConnection connection, PalletizerNewLocationRequest request, WorkService workService) {
		super(connection);
		this.request = request;
		this.workService = workService;
	}

	@Override
	public ResponseABC exec() {
		String cheId = request.getDeviceId();
		String location = request.getLocation();
		String item = request.getItem();
		LOGGER.info("Palletizer New Location {} for Item {} on che {} request ", item, cheId);
		PalletizerItemResponse response = new PalletizerItemResponse();
		Che che = Che.staticGetDao().findByPersistentId(UUID.fromString(cheId));
		if (che!=null) {
			String networkGuid =  che.getDeviceNetGuid().getHexStringNoPrefix();
			PalletizerInfo info = workService.processPalletizerNewLocationRequest(che, item, location);
			response.setInfo(info);
			response.setNetworkGuid(networkGuid);
			response.setStatus(ResponseStatus.Success);
			return response;
		}
		LOGGER.warn("Palletizer New Location request error: Did not find CHE {}", cheId);
		response.setStatus(ResponseStatus.Fail);
		return response;
	}
}
