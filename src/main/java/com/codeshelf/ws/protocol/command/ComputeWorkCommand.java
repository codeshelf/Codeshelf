package com.codeshelf.ws.protocol.command;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.omg.CORBA.BooleanHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.model.WorkInstructionCount;
import com.codeshelf.model.WorkInstructionStatusEnum;
import com.codeshelf.model.domain.Che;
import com.codeshelf.model.domain.OrderDetail;
import com.codeshelf.model.domain.WorkInstruction;
import com.codeshelf.model.domain.WorkPackage.WorkList;
import com.codeshelf.service.WorkService;
import com.codeshelf.ws.protocol.request.ComputeWorkRequest;
import com.codeshelf.ws.protocol.response.ComputeWorkResponse;
import com.codeshelf.ws.protocol.response.ResponseABC;
import com.codeshelf.ws.protocol.response.ResponseStatus;
import com.codeshelf.ws.server.WebSocketConnection;

@RequiresPermissions("wi:get")
public class ComputeWorkCommand extends CommandABC {
	private static final Logger	LOGGER	= LoggerFactory.getLogger(ComputeWorkCommand.class);

	private ComputeWorkRequest request;

	private WorkService	workService;
	
	public ComputeWorkCommand(WebSocketConnection connection, ComputeWorkRequest request, WorkService workService) {
		super(connection);
		this.request = request;
		this.workService = workService;
	}

	@Override
	public ResponseABC exec() {
		ComputeWorkResponse response = new ComputeWorkResponse();
		response.setPurpose(request.getPurpose());
		String cheId = request.getDeviceId();
		Che che = Che.staticGetDao().findByPersistentId(UUID.fromString(cheId));
		if (che != null) {
			String networkGuid =  che.getDeviceNetGuid().getHexStringNoPrefix();
			Map<String, String> positionToContainerMap = request.getPositionToContainerMap();
			List<String> containerIdList = new ArrayList<String>(positionToContainerMap.values());
			BooleanHolder pathChanged = new BooleanHolder(false);
			
			// Get the work instructions for this CHE at this location for the given containers.
			WorkList allWorkList = workService.computeWorkInstructions(che, containerIdList, request.getReversePickOrder());
						
			// Get work instructions with housekeeping
			List<WorkInstruction> instructionsOnPath = workService.getWorkInstructions(che, request.getLocationId(), request.getReversePickOrder(), pathChanged);
			
			//Get the counts
			Map<String, WorkInstructionCount> containerToCountMap = computeContainerWorkInstructionCounts(allWorkList, instructionsOnPath);
			
			// ~bhe: should we check for null/zero and return a different status?
			response.setWorkInstructions(instructionsOnPath);
			response.setContainerToWorkInstructionCountMap(containerToCountMap);
			response.setTotalGoodWorkInstructions(getTotalGoodWorkInstructionsCount(containerToCountMap));
			response.setNetworkGuid(networkGuid);
			response.setPathChanged(pathChanged.value);
			response.setStatus(ResponseStatus.Success);
			return response;
		}
		response.setStatusMessage("Can't find CHE with id "+cheId);
		response.setStatus(ResponseStatus.Fail);
		return response;
	}
	
	/**
	 * Compute work instruction counts by containerId
	 */
	public static final Map<String, WorkInstructionCount> computeContainerWorkInstructionCounts(WorkList allWork, List<WorkInstruction> instructionsOnPath) {
		Map<String, WorkInstructionCount> containerToWorkInstructCountMap = new HashMap<String, WorkInstructionCount>();
		WorkInstructionCount count = null;
		List<OrderDetail> allWorkUnpickableDetails = allWork.getDetails();
		
		//Create a searcheable set on UUIDs for instructions on a current Path
		HashSet<String> instructionsOnPathSet = new HashSet<String>();
		for (WorkInstruction wi : instructionsOnPath) {
			instructionsOnPathSet.add(wi.getPersistentId().toString());
		}
		
		//Iterate through all instructions.
		//  If instruction is on a current Path, analyze it as active
		//  If not - add it to auto-shorted items
		List<WorkInstruction> allWorkInstructions = allWork.getInstructions();
		for (WorkInstruction wi : allWorkInstructions){
			WorkInstructionStatusEnum wiStatus = wi.getStatus();
			String containerId = wi.getContainerId();
			count = containerToWorkInstructCountMap.get(containerId);
			if (count == null) {
				count = new WorkInstructionCount();
				containerToWorkInstructCountMap.put(containerId, count);
			}
			
			if (instructionsOnPathSet.contains(wi.getPersistentId().toString())) {
				//This instruction is on the current Path
				if (wiStatus == null) {
					//This should never happen. Log for now. We need to have a count for this because it is work instruction that will
					//be represented in the total count
					LOGGER.error("WorkInstruction status is null or invalid. Wi={}", wi);
					count.incrementInvalidOrUnknownStatusCount();
				} else {
					switch (wiStatus) {
						case COMPLETE:
							count.incrementCompleteCount();
							break;
						case NEW:
						case INPROGRESS:
							//Ignore Housekeeping
							if (!wi.isHousekeeping()) {
								count.incrementGoodCount();
							}
							break;
						case INVALID:
						case REVERT:
							count.incrementInvalidOrUnknownStatusCount();
							break;
						case SHORT:
							count.incrementImmediateShortCount();
							break;
					}
				}
			} else {
				//This instruction is not on the current Path
				if (wiStatus != WorkInstructionStatusEnum.COMPLETE){
					count.incrementUncompletedInstructionsOnOtherPaths();
				}
			}
		}
		
		//Process all auto-shorted items
		for (OrderDetail detail : allWorkUnpickableDetails) {
			String containerId = detail.getParent().getContainerId();
			count = containerToWorkInstructCountMap.get(containerId);
			if (count == null) {
				count = new WorkInstructionCount();
				containerToWorkInstructCountMap.put(containerId, count);
			}
			containerToWorkInstructCountMap.get(containerId).incrementDetailsNoWiMade();
		}
		return containerToWorkInstructCountMap;
	}

	/**
	 * Adds up the good work instructions in the map and returns the total
	 */
	public static final int getTotalGoodWorkInstructionsCount(Map<String, WorkInstructionCount> containerToWorkInstructionMap) {
		int total = 0;
		for (WorkInstructionCount count : containerToWorkInstructionMap.values()) {
			total += count.getGoodCount();
		}
		return total;
	}

}
