package com.codeshelf.ws.protocol.command;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.model.domain.Che;
import com.codeshelf.service.WorkService;
import com.codeshelf.service.WorkService.PalletizerInfo;
import com.codeshelf.ws.protocol.request.PalletizerItemRequest;
import com.codeshelf.ws.protocol.response.PalletizerItemResponse;
import com.codeshelf.ws.protocol.response.ResponseABC;
import com.codeshelf.ws.protocol.response.ResponseStatus;
import com.codeshelf.ws.server.WebSocketConnection;

public class PalletizerItemCommand extends CommandABC{
	private static final Logger	LOGGER	= LoggerFactory.getLogger(PalletizerItemCommand.class);

	private PalletizerItemRequest	request;

	private WorkService				workService;

	public PalletizerItemCommand(WebSocketConnection connection, PalletizerItemRequest request, WorkService workService) {
		super(connection);
		this.request = request;
		this.workService = workService;
	}

	@Override
	public ResponseABC exec() {
		String cheId = request.getDeviceId();
		String item = request.getItem();
		LOGGER.info("Palletizer Item {} request for {}", item, cheId);
		PalletizerItemResponse response = new PalletizerItemResponse();
		Che che = Che.staticGetDao().findByPersistentId(UUID.fromString(cheId));
		if (che!=null) {
			String networkGuid =  che.getDeviceNetGuid().getHexStringNoPrefix();
			PalletizerInfo info = workService.processPalletizerItemRequest(che, item);
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
