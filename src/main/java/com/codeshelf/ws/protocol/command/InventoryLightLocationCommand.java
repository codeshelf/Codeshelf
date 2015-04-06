package com.codeshelf.ws.protocol.command;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.service.InventoryService;
import com.codeshelf.ws.protocol.request.InventoryLightLocationRequest;
import com.codeshelf.ws.protocol.response.InventoryLightLocationResponse;
import com.codeshelf.ws.protocol.response.ResponseABC;
import com.codeshelf.ws.server.WebSocketConnection;

public class InventoryLightLocationCommand extends CommandABC {
	@SuppressWarnings("unused")
	private static final Logger	LOGGER	= LoggerFactory.getLogger(ComputeWorkCommand.class);

	private InventoryLightLocationRequest request;
	private InventoryService inventoryService;
	
	public InventoryLightLocationCommand(WebSocketConnection connection, InventoryLightLocationRequest request, InventoryService inventoryService) {
		super(connection);
		this.request = request;
		this.inventoryService = inventoryService;
	}

	@Override
	public ResponseABC exec() {
		return new InventoryLightLocationResponse();
	}
	
}