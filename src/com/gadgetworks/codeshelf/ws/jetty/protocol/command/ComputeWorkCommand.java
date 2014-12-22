package com.gadgetworks.codeshelf.ws.jetty.protocol.command;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gadgetworks.codeshelf.model.WorkInstructionCount;
import com.gadgetworks.codeshelf.model.domain.Che;
import com.gadgetworks.codeshelf.model.domain.Facility;
import com.gadgetworks.codeshelf.model.domain.WorkInstruction;
import com.gadgetworks.codeshelf.ws.jetty.protocol.request.ComputeWorkRequest;
import com.gadgetworks.codeshelf.ws.jetty.protocol.response.ComputeWorkResponse;
import com.gadgetworks.codeshelf.ws.jetty.protocol.response.ResponseABC;
import com.gadgetworks.codeshelf.ws.jetty.protocol.response.ResponseStatus;
import com.gadgetworks.codeshelf.ws.jetty.server.UserSession;

public class ComputeWorkCommand extends CommandABC {
	private static final Logger	LOGGER	= LoggerFactory.getLogger(ComputeWorkCommand.class);

	ComputeWorkRequest request;
	
	public ComputeWorkCommand(UserSession session, ComputeWorkRequest request) {
		super(session);
		this.request = request;
	}

	@Override
	public ResponseABC exec() {
		ComputeWorkResponse response = new ComputeWorkResponse();
		String cheId = request.getCheId();
		Che che = Che.DAO.findByPersistentId(UUID.fromString(cheId));
		if (che != null) {
			String networkGuid =  che.getDeviceNetGuid().getHexStringNoPrefix();
			// Figure out the CHE's work area by its scanned location.
			Facility facility = che.getParent().getParent();
			// Get the work instructions for this CHE at this location for the given containers.
			List<WorkInstruction> workInstructions = facility.computeWorkInstructions(che, request.getContainerIds());

			//Get the counts
			Map<String, WorkInstructionCount> containerToCountMap = computeContainerWorkInstructionCounts(workInstructions,
				request.getContainerIds());
			
			// ~bhe: should we check for null/zero and return a different status?
			response.setContainerToWorkInstructionCountMap(containerToCountMap);
			response.setTotalGoodWorkInstructions(getTotalGoodWorkInstructionsCount(containerToCountMap));
			response.setNetworkGuid(networkGuid);
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
	public static final Map<String, WorkInstructionCount> computeContainerWorkInstructionCounts(List<WorkInstruction> workInstructions,
		List<String> containerIds) {
		Map<String, WorkInstructionCount> containerToWorkInstructCountMap = new HashMap<String, WorkInstructionCount>();
		for (WorkInstruction wi : workInstructions) {
			//Grab count reference
			String containerId = wi.getContainerId();
			WorkInstructionCount count = containerToWorkInstructCountMap.get(containerId);
			if (count == null) {
				count = new WorkInstructionCount();
				containerToWorkInstructCountMap.put(containerId, count);
			}
			if (wi.getStatus() == null) {
				//This should never happen. Log for now. We need to have a count for this because it is work instruction that will
				//be represented in the total count
				LOGGER.error("WorkInstruction status is null or invalid. Wi={}", wi);
				count.incrementInvalidOrUnknownStatusCount();
			} else {
				switch (wi.getStatus()) {
					case COMPLETE:
						count.incrementCompleteCount();
						break;
					case INPROGRESS:
						count.incrementGoodCount();
						break;
					case INVALID:
						count.incrementInvalidOrUnknownStatusCount();
						break;
					case NEW:
						//Ignore Housekeeping
						if (!wi.amIHouseKeepingWi()) {
							count.incrementGoodCount();
						}
						break;
					case REVERT:
						count.incrementInvalidOrUnknownStatusCount();
						break;
					case SHORT:
						count.incrementImmediateShortCount();
						break;
				}
			}
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
