package com.codeshelf.ws.protocol.command;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.behavior.WorkBehavior;
import com.codeshelf.model.WorkInstructionCount;
import com.codeshelf.model.WorkInstructionStatusEnum;
import com.codeshelf.model.domain.Che;
import com.codeshelf.model.domain.OrderDetail;
import com.codeshelf.model.domain.WorkInstruction;
import com.codeshelf.model.domain.WorkPackage.WorkList;
import com.codeshelf.ws.protocol.request.ComputeWorkRequest;
import com.codeshelf.ws.protocol.response.ComputeWorkResponse;
import com.codeshelf.ws.protocol.response.ResponseABC;
import com.codeshelf.ws.protocol.response.ResponseStatus;
import com.codeshelf.ws.server.WebSocketConnection;

@RequiresPermissions("wi:get")
public class ComputeWorkCommand extends CommandABC {
	private static final Logger	LOGGER								= LoggerFactory.getLogger(ComputeWorkCommand.class);

	private ComputeWorkRequest	request;

	private WorkBehavior		workService;

	final int					computeDurationLimit				= 5000;
	final int					getWorkInstructionsDurationLimit	= 2000;
	final int					computeCountsDurationLimit			= 1000;

	private void logTimeSpentIfTooLong(long durationSpent, long durationLimit, String description, String cheName, String cheGuid) {
		if (durationSpent > durationLimit) {
			LOGGER.warn("{} took {}ms for {}/{}", description, durationSpent, cheName, cheGuid);
		}
		// Add metrics regardless? Future
		/*
		Timer timer = MetricsService.getInstance().createTimer(MetricsGroup.WSS, "cheWorkFromLocation");
		timer.update(wrapComputeDurationMs, TimeUnit.MILLISECONDS);
		*/

	}

	public ComputeWorkCommand(WebSocketConnection connection, ComputeWorkRequest request, WorkBehavior workService) {
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
			String networkGuid = che.getDeviceNetGuid().getHexStringNoPrefix();
			Map<String, String> positionToContainerMap = request.getPositionToContainerMap();
			//List<String> containerIdList = new ArrayList<String>(positionToContainerMap.values());
			AtomicBoolean pathChanged = new AtomicBoolean(false);
			Boolean reverse = request.getReversePickOrder();

			/*
			// Activate this only for use with CheProcessTestPickPerformance. Make profiling of the guts of server work easier.
			LOGGER.error("doing 9 extra compute work computations. Make sure this does not escape into production.");
			for (int n = 1; n<= 9; n++){
				WorkList allWorkList2 = workService.computeWorkInstructions(che, positionToContainerMap, reverse);
				List<WorkInstruction> instructionsOnPath2 = workService.getWorkInstructions(che,
					request.getLocationId(),
					reverse,
					pathChanged);
				Map<String, WorkInstructionCount> containerToCountMap2 = computeContainerWorkInstructionCounts(allWorkList2,
					instructionsOnPath2);			
			}
			*/

			// Three potentially slow parts. Time and log them
			long timestamp0 = System.currentTimeMillis();

			// Get the work instructions for this CHE at this location for the given containers.
			WorkList allWorkList = workService.computeWorkInstructions(che, positionToContainerMap, reverse);
			long timestamp1 = System.currentTimeMillis();
			logTimeSpentIfTooLong(timestamp1 - timestamp0,
				computeDurationLimit,
				"computeWorkInstructions()",
				che.getDomainId(),
				networkGuid);

			// Get work instructions with housekeeping
			List<WorkInstruction> instructionsOnPath = workService.getWorkInstructions(che,
				request.getLocationId(),
				reverse,
				pathChanged);
			long timestamp2 = System.currentTimeMillis();
			logTimeSpentIfTooLong(timestamp2 - timestamp1,
				getWorkInstructionsDurationLimit,
				"getWorkInstructions()",
				che.getDomainId(),
				networkGuid);

			//Get the counts
			Map<String, WorkInstructionCount> containerToCountMap = computeContainerWorkInstructionCounts(allWorkList,
				instructionsOnPath);
			long timestamp3 = System.currentTimeMillis();
			logTimeSpentIfTooLong(timestamp3 - timestamp2,
				computeCountsDurationLimit,
				"computeCountsDurationLimit()",
				che.getDomainId(),
				networkGuid);

			// create response
			response.setWorkInstructions(instructionsOnPath);
			response.setContainerToWorkInstructionCountMap(containerToCountMap);
			response.setTotalGoodWorkInstructions(getTotalGoodWorkInstructionsCount(containerToCountMap));
			response.setNetworkGuid(networkGuid);
			response.setPathChanged(pathChanged.get());
			response.setStatus(ResponseStatus.Success);
			return response;
		}
		response.setStatusMessage("Can't find CHE with id " + cheId);
		response.setStatus(ResponseStatus.Fail);
		return response;
	}

	/**
	 * Compute work instruction counts by containerId
	 */
	public static final Map<String, WorkInstructionCount> computeContainerWorkInstructionCounts(WorkList allWork,
		List<WorkInstruction> instructionsOnPath) {
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
		for (WorkInstruction wi : allWorkInstructions) {
			WorkInstructionStatusEnum wiStatus = wi.getStatus();
			String containerId = wi.getContainerId();
			// This is sometimes odd. Seeing "None" in a wi for something that may have got a putwall job before.
			// Returning a WorkInstructionCount for "None"  may confuse the site controller. Just filter here.
			if (containerId == null || containerId.isEmpty() || containerId.equals("None")) {
				LOGGER.info("computeContainerWorkInstructionCounts had a wi for 'None' container");
				LOGGER.info("Wi: {}", wi);
				continue; // do not make new WorkInstructionCount for the bad one.
				// Currently getting "None" for completed work instruction. This is not useful. It would become useful to return only if:
				// - Server counts shorts and completes on this path only.
				// - however, two or more separate shorts for the same detail only count as one.
				// - A complete (completing the detail) overrules any short.
				// - And, both the short and completes give use the containerId we need to use. Warning, there are probably other reasons
				// the containerId is not set that way on the completed or short WIs.				
			}

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
							// What happens if server does not increment Complete? Only fails ComputWorkCommandTest. Apparently not needed.
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
							// What happens if server does not increment Short? Only fails ComputWorkCommandTest. Apparently not needed.
							count.incrementImmediateShortCount();
							break;
					}
				}
			} else {
				//This instruction is not on the current Path
				if (wiStatus != WorkInstructionStatusEnum.COMPLETE) {
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
