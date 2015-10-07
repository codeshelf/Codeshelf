package com.codeshelf.ws.protocol.command;

import java.util.UUID;

import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.behavior.InventoryBehavior;
import com.codeshelf.behavior.WorkBehavior;
import com.codeshelf.ws.protocol.request.ComputePutWallInstructionRequest;
import com.codeshelf.ws.protocol.request.InventoryUpdateRequest;
import com.codeshelf.ws.protocol.response.InventoryUpdateResponse;
import com.codeshelf.ws.protocol.response.ResponseABC;
import com.codeshelf.ws.protocol.response.ResponseStatus;
import com.codeshelf.ws.server.WebSocketConnection;

@RequiresPermissions("che:inventory")		// TODO: ensure cart user has this right
public class InventoryUpdateCommand extends CommandABC {

	private static final Logger	LOGGER	= LoggerFactory.getLogger(ComputeWorkCommand.class);

	private InventoryUpdateRequest request;
	private InventoryBehavior inventoryService;
	private WorkBehavior workService;
	
	public InventoryUpdateCommand(WebSocketConnection connection, InventoryUpdateRequest request, InventoryBehavior inventoryService, WorkBehavior workService) {
		super(connection);
		this.request = request;
		this.inventoryService = inventoryService;
		this.workService = workService;
	}

	@Override
	public ResponseABC exec() {
		
		InventoryUpdateResponse response = null;
		String cheId = request.getDeviceId();
		UUID cheUUID = UUID.fromString(cheId);
		if (cheUUID != null ) {
			// At this point, the getLocation() string may be a location name (usually alias), or if a tape Id, it still has the % prefix
			String skuWallName = request.getSkuWallName();
			response = inventoryService.moveOrCreateInventory(request.getGtin(), request.getLocation(), cheUUID, skuWallName);
			response.setNetworkGuid(cheId);
			//If the SKU wall name was provided, automatically put CHE into the PUT mode by computing work for this location and item
			if (skuWallName != null) {
				ComputePutWallInstructionRequest putWallInstructionRequest = new ComputePutWallInstructionRequest(cheId, request.getGtin(), skuWallName);
				ComputePutWallInstructionCommand putWallInstructionCommand = new ComputePutWallInstructionCommand(wsConnection, putWallInstructionRequest, workService);
				return putWallInstructionCommand.exec();
			}
		} else {
			LOGGER.error("Unable to process CHE UUID: {}", cheId);
			response = new InventoryUpdateResponse();
			response.appendStatusMessage("Unable to process CHE UUID");
			response.setStatus(ResponseStatus.Fail);
		}
		return response;
	}	
}