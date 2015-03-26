package com.codeshelf.ws.jetty.protocol.command;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.service.InventoryService;
import com.codeshelf.ws.jetty.protocol.request.InventoryLightRequest;
import com.codeshelf.ws.jetty.protocol.response.InventoryLightResponse;
import com.codeshelf.ws.jetty.protocol.response.ResponseABC;
import com.codeshelf.ws.jetty.protocol.response.ResponseStatus;
import com.codeshelf.ws.jetty.server.WebSocketConnection;

public class InventoryLightCommand extends CommandABC {
	@SuppressWarnings("unused")
	private static final Logger	LOGGER	= LoggerFactory.getLogger(ComputeWorkCommand.class);

	private InventoryLightRequest request;
	private InventoryService inventoryService;
	
	public InventoryLightCommand(WebSocketConnection connection, InventoryLightRequest request, InventoryService inventoryService) {
		super(connection);
		this.request = request;
		this.inventoryService = inventoryService;
	}

	@Override
	public ResponseABC exec() {
		InventoryLightResponse response = null;
		
		UUID cheUUID = UUID.fromString(request.getDeviceId());
		if (cheUUID != null ) {
			response = inventoryService.lightInventoryByGtin(request.getGtin(), cheUUID);
		} else {
			response = new InventoryLightResponse();
			response.setStatus(ResponseStatus.Fail);
		}
		
		return response;
	}
	
}