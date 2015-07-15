package com.codeshelf.ws.protocol.command;

import java.util.UUID;

import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.service.InventoryService;
import com.codeshelf.service.WorkService;
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
	private InventoryService inventoryService;
	private WorkService workService;
	
	public InventoryUpdateCommand(WebSocketConnection connection, InventoryUpdateRequest request, InventoryService inventoryService, WorkService workService) {
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
			response = inventoryService.moveOrCreateInventory(request.getGtin(), request.getLocation(), cheUUID);
			response.setNetworkGuid(cheId);
			
			String skuWallName = request.getSkuWallName();
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