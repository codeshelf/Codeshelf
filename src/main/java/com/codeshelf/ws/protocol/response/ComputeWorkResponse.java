package com.codeshelf.ws.protocol.response;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;

import com.codeshelf.model.WorkInstructionCount;
import com.codeshelf.model.domain.WorkInstruction;
import com.codeshelf.persistence.TenantPersistenceService;
import com.codeshelf.ws.protocol.request.ComputeWorkRequest.ComputeWorkPurpose;

public class ComputeWorkResponse extends DeviceResponseABC {
	@Getter @Setter
	private ComputeWorkPurpose purpose = ComputeWorkPurpose.COMPUTING_WORK;
	
	@Getter @Setter
	Integer	totalGoodWorkInstructions = null;

	@Getter
	@Setter
	Map<String, WorkInstructionCount>	containerToWorkInstructionCountMap	= new HashMap<String, WorkInstructionCount>();
	
	@Getter
	private List<WorkInstruction> workInstructions;
	
	@Getter @Setter
	private Boolean pathChanged = false;

	public void addWorkInstructionCount(String containerId, WorkInstructionCount workInstructionCount) {
		containerToWorkInstructionCountMap.put(containerId, workInstructionCount);
	}

	public void setWorkInstructions(List<WorkInstruction> wis) {
		if(wis == null) {
			this.workInstructions = new ArrayList<WorkInstruction>(); 
		} else {
			workInstructions = new ArrayList<WorkInstruction>(wis.size());
			for(WorkInstruction wi : wis) {
				workInstructions.add(TenantPersistenceService.<WorkInstruction>deproxify(wi));
			}
		}
	}
}
