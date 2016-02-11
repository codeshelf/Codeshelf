package com.codeshelf.ws.protocol.command;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.behavior.InfoBehavior;
import com.codeshelf.behavior.InfoBehavior.InfoPackage;
import com.codeshelf.model.domain.Che;
import com.codeshelf.ws.protocol.request.InfoRequest;
import com.codeshelf.ws.protocol.response.InfoResponse;
import com.codeshelf.ws.protocol.response.ResponseABC;
import com.codeshelf.ws.protocol.response.ResponseStatus;
import com.codeshelf.ws.server.WebSocketConnection;

public class InfoCommand extends CommandABC<InfoRequest>{
	private static final Logger	LOGGER = LoggerFactory.getLogger(InfoCommand.class);
	final private InfoBehavior infoService;
	
	public InfoCommand(WebSocketConnection connection, InfoRequest request, InfoBehavior infoService) {
		super(connection, request);
		this.infoService = infoService;
	}

	@Override
	public ResponseABC exec() {
		String cheId = request.getDeviceId();
		LOGGER.info("Get Info {} for {}", request.getType(), cheId);
		InfoResponse response = new InfoResponse();
		Che che = Che.staticGetDao().findByPersistentId(UUID.fromString(cheId));
		if (che!=null) {
			String networkGuid =  che.getDeviceNetGuid().getHexStringNoPrefix();
			InfoPackage info = infoService.getInfo(che.getFacility(), request, che.getColor());
			response.setInfo(info);
			response.setNetworkGuid(networkGuid);
			response.setStatus(ResponseStatus.Success);
			return response;
		}
		LOGGER.warn("Info request error: Did not find CHE {}", cheId);
		response.setStatus(ResponseStatus.Fail);
		return response;
	}
}
