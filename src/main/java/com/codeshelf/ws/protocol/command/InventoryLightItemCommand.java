package com.codeshelf.ws.protocol.command;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.service.InventoryService;
import com.codeshelf.ws.protocol.request.InventoryLightItemRequest;
import com.codeshelf.ws.protocol.response.InventoryLightItemResponse;
import com.codeshelf.ws.protocol.response.ResponseABC;
import com.codeshelf.ws.protocol.response.ResponseStatus;
import com.codeshelf.ws.server.WebSocketConnection;

public class InventoryLightItemCommand extends CommandABC {
	@SuppressWarnings("unused")
	private static final Logger	LOGGER	= LoggerFactory.getLogger(ComputeWorkCommand.class);

	private InventoryLightItemRequest request;
	private InventoryService inventoryService;
	
	public InventoryLightItemCommand(WebSocketConnection connection, InventoryLightItemRequest request, InventoryService inventoryService) {
		super(connection);
		this.request = request;
		this.inventoryService = inventoryService;
	}

	@Override
	public ResponseABC exec() {
		InventoryLightItemResponse response = null;
		
		UUID cheUUID = UUID.fromString(request.getDeviceId());
		if (cheUUID != null ) {
			response = inventoryService.lightInventoryByGtin(request.getGtin(), cheUUID);
		} else {
			response = new InventoryLightItemResponse();
			response.setStatus(ResponseStatus.Fail);
		}
		
		return response;
	}
	
}