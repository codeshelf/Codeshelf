package com.codeshelf.ws.protocol.command;

import java.util.UUID;

import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.service.InventoryService;
import com.codeshelf.ws.protocol.request.InventoryUpdateRequest;
import com.codeshelf.ws.protocol.response.InventoryUpdateResponse;
import com.codeshelf.ws.protocol.response.ResponseABC;
import com.codeshelf.ws.protocol.response.ResponseStatus;
import com.codeshelf.ws.server.WebSocketConnection;

@RequiresPermissions("che:inventory")		// TODO: ensure cart user has this right
public class InventoryUpdateCommand extends CommandABC {

	private static final Logger	LOGGER	= LoggerFactory.getLogger(ComputeWorkCommand.class);

	private InventoryUpdateRequest request;
	private InventoryService inventoryService;
	
	public InventoryUpdateCommand(WebSocketConnection connection, InventoryUpdateRequest request, InventoryService inventoryService) {
		super(connection);
		this.request = request;
		this.inventoryService = inventoryService;
	}

	@Override
	public ResponseABC exec() {
		
		InventoryUpdateResponse response = null;
		
		UUID cheUUID = UUID.fromString(request.getDeviceId());
		if (cheUUID != null ) {
			response = inventoryService.moveOrCreateInventory(request.getGtin(), request.getLocation(), cheUUID);
		} else {
			LOGGER.error("Unable to process CHE UUID: {}", request.getDeviceId());
			response = new InventoryUpdateResponse();
			response.appendStatusMessage("Unable to process CHE UUID");
			response.setStatus(ResponseStatus.Fail);
		}
		
		return response;
	}
	
}