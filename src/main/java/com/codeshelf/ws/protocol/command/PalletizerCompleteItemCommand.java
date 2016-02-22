package com.codeshelf.ws.protocol.command;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.behavior.PalletizerBehavior;
import com.codeshelf.behavior.PalletizerBehavior.PalletizerInfo;
import com.codeshelf.model.domain.Che;
import com.codeshelf.ws.protocol.request.PalletizerCompleteItemRequest;
import com.codeshelf.ws.protocol.response.GenericDeviceResponse;
import com.codeshelf.ws.protocol.response.ResponseABC;
import com.codeshelf.ws.protocol.response.ResponseStatus;
import com.codeshelf.ws.server.WebSocketConnection;

public class PalletizerCompleteItemCommand extends CommandABC<PalletizerCompleteItemRequest>{
	private static final Logger	LOGGER		= LoggerFactory.getLogger(PalletizerCompleteItemCommand.class);

	private PalletizerBehavior				palletizerBehavior;

	public PalletizerCompleteItemCommand(WebSocketConnection connection, PalletizerCompleteItemRequest request, PalletizerBehavior palletizerBehavior) {
		super(connection, request);
		this.palletizerBehavior = palletizerBehavior;
	}

	@Override
	public ResponseABC exec() {
		String cheId = request.getDeviceId();
		PalletizerInfo info = request.getInfo();
		Boolean shorted = request.getShorted();
		String userId = request.getUserId();
		GenericDeviceResponse response = new GenericDeviceResponse();
		Che che = Che.staticGetDao().findByPersistentId(UUID.fromString(cheId));
		if (che!=null) {
			LOGGER.info("Palletizer Complete Item {}, shorted = {} on che {} by {}", info.getItem(), shorted, che.getDeviceGuidStr(), userId);
			String networkGuid = che.getDeviceNetGuid().getHexStringNoPrefix();
			palletizerBehavior.completeItem(che, info, shorted, userId);
			response.setNetworkGuid(networkGuid);
			response.setStatus(ResponseStatus.Success);
			return response;
		}
		LOGGER.warn("Palletizer Complete Wi request error: Did not find CHE {}", cheId);
		response.setStatus(ResponseStatus.Fail);
		return response;
	}

}
