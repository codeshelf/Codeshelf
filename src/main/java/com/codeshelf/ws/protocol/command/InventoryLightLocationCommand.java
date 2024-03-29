package com.codeshelf.ws.protocol.command;

import java.util.UUID;

import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.behavior.InventoryBehavior;
import com.codeshelf.ws.protocol.request.InventoryLightLocationRequest;
import com.codeshelf.ws.protocol.response.InventoryLightLocationResponse;
import com.codeshelf.ws.protocol.response.ResponseABC;
import com.codeshelf.ws.protocol.response.ResponseStatus;
import com.codeshelf.ws.server.WebSocketConnection;

@RequiresPermissions("light:use")
public class InventoryLightLocationCommand extends CommandABC<InventoryLightLocationRequest> {
	@SuppressWarnings("unused")
	private static final Logger	LOGGER	= LoggerFactory.getLogger(ComputeWorkCommand.class);

	private InventoryBehavior inventoryService;
	
	public InventoryLightLocationCommand(WebSocketConnection connection, InventoryLightLocationRequest request, InventoryBehavior inventoryService) {
		super(connection, request);
		this.inventoryService = inventoryService;
	}

	@Override
	public ResponseABC exec() {
		
		InventoryLightLocationResponse response = new InventoryLightLocationResponse();
		
		UUID cheUUID = UUID.fromString(request.getDeviceId());
		if (cheUUID != null ) {
			inventoryService.lightLocationByAliasOrTapeId(request.getLocation(), request.getIsTape(), cheUUID);
			response.setStatus(ResponseStatus.Success);
		} else {
			response.setStatus(ResponseStatus.Fail);
		}	
		return response;
	}
	
}