package com.codeshelf.ws.protocol.command;

import java.util.UUID;

import com.codeshelf.model.domain.Che;
import com.codeshelf.service.WorkService;
import com.codeshelf.ws.protocol.request.PutWallPlacementRequest;
import com.codeshelf.ws.protocol.response.PutWallPlacementResponce;
import com.codeshelf.ws.protocol.response.ResponseABC;
import com.codeshelf.ws.protocol.response.ResponseStatus;
import com.codeshelf.ws.server.WebSocketConnection;

public class PutWallPlacementCommand extends CommandABC{
	private PutWallPlacementRequest request;
	
	private WorkService	workService;
	
	public PutWallPlacementCommand(WebSocketConnection connection, PutWallPlacementRequest request, WorkService workService) {
		super(connection);
		this.request = request;
		this.workService = workService;
	}

	@Override
	public ResponseABC exec() {
		PutWallPlacementResponce response = new PutWallPlacementResponce();
		String cheId = request.getDeviceId();
		
		Che che = Che.staticGetDao().findByPersistentId(UUID.fromString(cheId));
		if (che != null) {
			boolean success = workService.processPutWallPlacement(che, request.getOrderId(), request.getLocationId());
			response.setStatus(success ? ResponseStatus.Success : ResponseStatus.Fail);
			return response;
		}
		response.setStatusMessage("Can't find CHE with id " + cheId);
		response.setStatus(ResponseStatus.Fail);
		return response;
	}
}
