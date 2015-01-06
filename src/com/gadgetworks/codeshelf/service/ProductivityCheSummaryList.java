package com.gadgetworks.codeshelf.service;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.TimeZone;
import java.util.UUID;

import lombok.Getter;

import com.gadgetworks.codeshelf.api.BaseResponse;
import com.gadgetworks.codeshelf.model.WorkInstructionStatusEnum;
import com.gadgetworks.codeshelf.model.domain.Che;
import com.gadgetworks.codeshelf.model.domain.Facility;
import com.gadgetworks.codeshelf.model.domain.OrderDetail;
import com.gadgetworks.codeshelf.model.domain.OrderGroup;
import com.gadgetworks.codeshelf.model.domain.OrderHeader;
import com.gadgetworks.codeshelf.model.domain.WorkInstruction;

public class ProductivityCheSummaryList extends BaseResponse{
	private final static TimeZone TZ = TimeZone.getTimeZone("UTC");
	private final static String TF = "yyyy-MM-dd HH:mm:ss.SSSZ";

	@Getter
	private HashMap<String, List<RunSummary>> runsByGroup = new HashMap<String, List<RunSummary>>();
	
	public void setInstructions(List<WorkInstruction> instructions, UUID facilityId) {
		for (WorkInstruction instruction : instructions) {
			Facility facility = instruction.getFacility(); 
			if (facility == null || !facility.getPersistentId().equals(facilityId)) {
				continue;
			}
			processSingleInstruction(instruction);
		}
		sort();
	}
	
	private void processSingleInstruction(WorkInstruction instruction) {
		OrderDetail detail = instruction.getOrderDetail();
		OrderHeader header = detail.getParent();
		OrderGroup group = header.getOrderGroup();
		String groupId = group == null? "undefined" : group.getPersistentId().toString();
		String groupDomainId = group == null? "undefined" : group.getDomainId();
		Che che = instruction.getAssignedChe();
		String runId = getRunId(instruction);
		
		List<RunSummary> runs = runsByGroup.get(groupDomainId);
		if (runs == null) {
			runs = new ArrayList<>();
			runsByGroup.put(groupDomainId, runs);
		}
		
		RunSummary run = getRun(runs, runId);
		if (run == null) {
			run = new RunSummary(runId, groupId, groupDomainId, che.getPersistentId().toString(), che.getDomainId());
			runs.add(run);
		}
		
		WorkInstructionStatusEnum status = instruction.getStatus();
		switch (status) {
			case INVALID:
				run.invalid++;
				break;
			case NEW:
				run.New++;
				break;
			case INPROGRESS:
				run.inprogress++;
				break;
			case SHORT:
				run.Short++;
				break;
			case COMPLETE:
				run.complete++;
				break;
			case REVERT:
				run.revert++;
				break;
		}
	}
	
	public static RunSummary getRun(List<RunSummary> runs, String runId) {
		for (RunSummary run : runs) {
			if (runId.equals(run.getId())) {return run;}
		}
		return null;
	}
	
	private String getRunId(WorkInstruction instruction) {
		Timestamp time = instruction.getAssigned();
		if(time == null) {
			return "";
		} else {
			SimpleDateFormat df = new SimpleDateFormat(TF);
			df.setTimeZone(TZ);
			return df.format(time);
		}
	}
	
	private void sort(){
		Iterator<List<RunSummary>> runsIter = runsByGroup.values().iterator();
		while (runsIter.hasNext()) {
			Collections.sort(runsIter.next(), Collections.reverseOrder());
		}
	}
	
	public class RunSummary implements Comparable<RunSummary>{
		@Getter
		private String id; //run id
		
		@Getter
		private String groupId, cheId;
		
		@Getter
		private String groupDomainId, cheDomainId;
		
		@Getter
		private short invalid, New, inprogress, Short, complete, revert;
		
		public RunSummary(String id, String groupId, String groupDomainId, String cheId, String cheDomainId) {
			this.id = id;
			this.groupId = groupId;
			this.cheId = cheId;
			this.groupDomainId = groupDomainId;
			this.cheDomainId = cheDomainId;
		}
		
		public int compareTo(RunSummary compareRun) {
			return id.compareTo(compareRun.id);
		}	
	}
}
