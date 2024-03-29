package com.codeshelf.behavior;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import lombok.Getter;

import com.codeshelf.model.WiSetSummary;
import com.codeshelf.model.domain.Che;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.OrderDetail;
import com.codeshelf.model.domain.OrderGroup;
import com.codeshelf.model.domain.OrderHeader;
import com.codeshelf.model.domain.WorkInstruction;

public class ProductivityCheSummaryList {

	@Getter
	private HashMap<String, List<WiSetSummary>>	runsByGroup	= new HashMap<String, List<WiSetSummary>>();
	
	public ProductivityCheSummaryList(UUID facilityId, List<WorkInstruction> instructions) {
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
		if (instruction.isHousekeeping()) {
			return; 
		}

		OrderDetail detail = instruction.getOrderDetail();
		if (detail == null) {
			return;
		}
		OrderHeader header = detail.getParent();
		OrderGroup group = header.getOrderGroup();
		String groupDomainId = group == null ? OrderGroup.UNDEFINED : group.getDomainId();
		Che che = instruction.getAssignedChe();

		List<WiSetSummary> runs = runsByGroup.get(groupDomainId);
		if (runs == null) {
			runs = new ArrayList<>();
			runsByGroup.put(groupDomainId, runs);
		}

		WiSetSummary run = getRun(runs, instruction.getAssigned());
		if (run == null) {
			run = new WiSetSummary(instruction.getAssigned(), che.getPersistentId().toString(), che.getDomainId());
			runs.add(run);
		}

		run.incrementStatus(instruction.getStatus());
	}

	public static WiSetSummary getRun(List<WiSetSummary> runs, Timestamp runAssignedTime) {
		for (WiSetSummary run : runs) {
			if (runAssignedTime.equals(run.getAssignedTime())) {
				return run;
			}
		}
		return null;
	}

	private void sort() {
		Iterator<List<WiSetSummary>> runsIter = runsByGroup.values().iterator();
		while (runsIter.hasNext()) {
			Collections.sort(runsIter.next(), Collections.reverseOrder());
		}
	}
}
