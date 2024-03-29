package com.codeshelf.ws.protocol.response;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;

import com.codeshelf.model.domain.WorkInstruction;
import com.codeshelf.persistence.TenantPersistenceService;

public class GetOrderDetailWorkResponse extends DeviceResponseABC {
	// Initially a clone of GetWorkResponse, but we expect it will drift.

	@Getter
	private List<WorkInstruction> workInstructions;
	
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
