package com.gadgetworks.codeshelf.service;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.TimeZone;
import java.util.UUID;

import lombok.Getter;

import com.gadgetworks.codeshelf.api.BaseResponse;
import com.gadgetworks.codeshelf.model.WorkInstructionStatusEnum;
import com.gadgetworks.codeshelf.model.domain.Che;
import com.gadgetworks.codeshelf.model.domain.OrderGroup;
import com.gadgetworks.codeshelf.model.domain.OrderHeader;
import com.gadgetworks.codeshelf.model.domain.WorkInstruction;


public class ProductivityCheSummaryList extends BaseResponse{
	public final static TimeZone timeZone = TimeZone.getTimeZone("UTC");
	public final static String timeFormat = "yyyy-MM-dd HH:mm:ss.SSSZ";
	
	//Groups->Ches->Runs
	@Getter
	private HashMap<String, HashMap<UUID, HashMap<String, RunSummary>>> groups = new HashMap<>();
	
	public class RunSummary{
		@Getter
		private String groupId, cheId;
		
		@Getter
		private String groupDomainId, cheDomainId;
		
		@Getter
		private short invalid, New, inprogress, Short, complete, revert;
		
		public RunSummary(String groupId, String groupDomainId, String cheId, String cheDomainId) {
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
		UUID cheId = che.getPersistentId();
		
		//Get all che's for this group 
		HashMap<UUID, HashMap<String, RunSummary>> ches = groups.get(groupId);
		if (ches == null){
			ches = new HashMap<>();
			groups.put(groupId, ches);
		}
		
		//Get all runs for this che
		HashMap<String, RunSummary> cheRuns = ches.get(cheId);
		if (cheRuns == null){
			cheRuns = new HashMap<>();
			ches.put(cheId, cheRuns);
		}
		
		//Get the correct run
		Timestamp time = instruction.getAssigned();

		String timeStr;
		if(time == null) {
			timeStr = "";
		} else {
			SimpleDateFormat df = new SimpleDateFormat(ProductivityCheSummaryList.timeFormat);
			df.setTimeZone(ProductivityCheSummaryList.timeZone);
			timeStr = df.format(time);
		}
		
		RunSummary summary = cheRuns.get(timeStr);
		if (summary == null) {
			summary = new RunSummary(groupId, groupDomainId, cheId.toString(), che.getDomainId());
			cheRuns.put(timeStr, summary);
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
}