package com.gadgetworks.codeshelf.ws.jetty.protocol.response;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

import com.gadgetworks.codeshelf.model.domain.WorkInstruction;
import com.gadgetworks.codeshelf.platform.persistence.PersistenceService;

public class GetOrderDetailWorkResponse extends ResponseABC {
	// Initially a clone of GetWorkResponse, but we expect it will drift.

	@Getter @Setter
	String networkGuid;

	@Getter
	private List<WorkInstruction> workInstructions;
	
	public void setWorkInstructions(List<WorkInstruction> wis) {
		if(wis == null) {
			this.workInstructions = new ArrayList<WorkInstruction>(); 
		} else {
			workInstructions = new ArrayList<WorkInstruction>(wis.size());
			for(WorkInstruction wi : wis) {
				workInstructions.add(PersistenceService.<WorkInstruction>deproxify(wi));
			}
		}
	}
}