package com.gadgetworks.codeshelf.service;

import java.util.HashMap;
import java.util.UUID;

import com.gadgetworks.codeshelf.apiresources.BaseResponse;
import com.gadgetworks.codeshelf.model.WorkInstructionStatusEnum;
import com.gadgetworks.codeshelf.model.domain.Che;
import com.gadgetworks.codeshelf.model.domain.OrderGroup;
import com.gadgetworks.codeshelf.model.domain.OrderHeader;
import com.gadgetworks.codeshelf.model.domain.WorkInstruction;

import lombok.Getter;


public class ProductivityCheSummaryList extends BaseResponse{
	
	@Getter
	private HashMap<String, CheSummary> summaries = new HashMap<>();
	
	private class CheSummary{		
		@Getter
		private String groupId, cheId;
		
		@Getter
		private String groupDomainId, cheDomainId;
		
		@Getter
		private short invalid, New, inprogress, Short, complete, revert;
		
		public CheSummary(String groupId, String groupDomainId, String cheId, String cheDomainId) {
			this.groupId = groupId;
			this.cheId = cheId;
			this.groupDomainId = groupDomainId;
			this.cheDomainId = cheDomainId;
		}
	}

	public void processStatus(OrderHeader header, WorkInstruction instruction){
		if (header == null || instruction == null) {return;}
		OrderGroup group = header.getOrderGroup();
		String groupDomainId = group == null? "undefined" : group.getDomainId();
		String groupId = group == null? "undefined" : group.getPersistentId().toString();
		Che che = instruction.getAssignedChe();
		String cheId = che.getPersistentId().toString();
		String key = keyGen(groupId, cheId);
		CheSummary summary = summaries.get(key);
		if (summary == null) {
			summary = new CheSummary(groupId, groupDomainId, cheId, che.getDomainId());
			summaries.put(key, summary);
		}
		WorkInstructionStatusEnum status = instruction.getStatus();
		switch (status) {
			case INVALID:
				summary.invalid++;
				break;
			case NEW:
				summary.New++;
				break;
			case INPROGRESS:
				summary.inprogress++;
				break;
			case SHORT:
				summary.Short++;
				break;
			case COMPLETE:
				summary.complete++;
				break;
			case REVERT:
				summary.revert++;
				break;
		}
	}
	
	private String keyGen(String groupId, String cheId){
		return (groupId == null || cheId == null)? null : groupId + "***" + cheId;
	}
}