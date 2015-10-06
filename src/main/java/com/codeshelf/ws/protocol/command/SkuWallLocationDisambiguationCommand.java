package com.codeshelf.ws.protocol.command;

import java.util.UUID;

import com.codeshelf.model.domain.Che;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.Location;
import com.codeshelf.service.InventoryBehavior;
import com.codeshelf.service.WorkBehavior;
import com.codeshelf.ws.protocol.request.ComputePutWallInstructionRequest;
import com.codeshelf.ws.protocol.request.InventoryUpdateRequest;
import com.codeshelf.ws.protocol.request.SkuWallLocationDisambiguationRequest;
import com.codeshelf.ws.protocol.response.GetPutWallInstructionResponse;
import com.codeshelf.ws.protocol.response.ResponseABC;
import com.codeshelf.ws.protocol.response.ResponseStatus;
import com.codeshelf.ws.server.WebSocketConnection;

public class SkuWallLocationDisambiguationCommand extends CommandABC {
	private SkuWallLocationDisambiguationRequest request;
	private InventoryBehavior inventoryService;
	private WorkBehavior workService;
	
	
	public SkuWallLocationDisambiguationCommand(WebSocketConnection connection, SkuWallLocationDisambiguationRequest request, InventoryBehavior inventoryService, WorkBehavior workService) {
		super(connection);
		this.request = request;
		this.inventoryService = inventoryService;
		this.workService = workService;
	}

	/**
	 * When a user is putting items onto a SKU wall, if an item he needs exists in another wall, but not his, he has 2 options:
	 * 1) Switch CHE to another wall, and place the item there
	 * 2) Place item into the new location on the current wall.
	 * Both these choices are performed by scanning a location and are undistinguisheable from the Site controller.
	 * That's why they both come here.
	 */
	@Override
	public ResponseABC exec() {
		GetPutWallInstructionResponse putWallInstructionResponse = new GetPutWallInstructionResponse();
		String cheId = request.getDeviceId();
		Che che = Che.staticGetDao().findByPersistentId(UUID.fromString(cheId));
		if (che != null) {
			Facility facility = che.getFacility();
			String currentWallStr = request.getSkuWallName();
			String scannedLocationStr = request.getLocation();
			String gtin = request.getGtin();
			//Make sure that the current wall is the actual SKU wall, and not a subloication
			Location currentWall = facility.findSubLocationById(currentWallStr);
			if (currentWall != null){
				currentWall = currentWall.getWall(Location.SKUWALL_USAGE);
			}
			//Make sure that the scanned location exists.
			//If it doesn't, return CHE to the options screen by searching for work on the old location and item
			Location scannedLocation = facility.findSubLocationById(scannedLocationStr);
			if (scannedLocation == null) {
				ComputePutWallInstructionRequest putWallRequest = new ComputePutWallInstructionRequest(cheId, gtin, currentWallStr);
				ComputePutWallInstructionCommand putWallCommand = new ComputePutWallInstructionCommand(wsConnection, putWallRequest, workService);
				return putWallCommand.exec();
			}
			//See if the scanned location is inside the current SKU wall
			if (scannedLocation.hasAncestor(currentWall)){
				//If the scanned location is within the currect wall, place the item there. This will also automatically put CHE into the PUT screen.
				InventoryUpdateRequest inventoryRequest = new InventoryUpdateRequest(cheId, gtin, scannedLocationStr, currentWallStr);
				InventoryUpdateCommand inventoryCommand = new InventoryUpdateCommand(wsConnection, inventoryRequest, inventoryService, workService);
				return inventoryCommand.exec();
			} else {
				//If the scanned location is outside the current wall, compute work on the new location and item
				ComputePutWallInstructionRequest putWallRequest = new ComputePutWallInstructionRequest(cheId, gtin, scannedLocationStr);
				ComputePutWallInstructionCommand putWallCommand = new ComputePutWallInstructionCommand(wsConnection, putWallRequest, workService);
				return putWallCommand.exec();
			}
		}
		putWallInstructionResponse.setStatus(ResponseStatus.Fail);
		putWallInstructionResponse.setStatusMessage("Can't find CHE with id " + cheId);
		return putWallInstructionResponse;
	}

}
