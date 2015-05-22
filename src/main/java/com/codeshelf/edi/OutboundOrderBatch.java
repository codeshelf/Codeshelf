package com.codeshelf.edi;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import lombok.Getter;
import lombok.Setter;

public class OutboundOrderBatch {

	@Getter @Setter
	List<OutboundOrderCsvBean> lines = new LinkedList<OutboundOrderCsvBean>();
	
	@Getter
	int batchId;

	@Getter
	HashSet<String> orderIds = new HashSet<String>();

	@Getter
	HashSet<String> containerIds = new HashSet<String>();

	@Getter
	Set<String> itemIds = new HashSet<String>();

	@Getter
	Set<String> gtinIds = new HashSet<String>();

	@Getter
	Set<String> orderGroupIds = new HashSet<String>();
	
	@Getter @Setter
	int processingAttempts = 0;

	public OutboundOrderBatch(int batchId) {
		this.batchId = batchId;
	}

	public void add(OutboundOrderCsvBean orderBean) {
		this.lines.add(orderBean);
		this.orderIds.add(orderBean.getOrderId());
		this.containerIds.add(orderBean.getPreAssignedContainerId());
		this.itemIds.add(orderBean.getItemId());
		this.gtinIds.add(orderBean.getGtin());
		String orderGroupId = orderBean.getOrderGroupId();
		if (orderGroupId!=null) {
			orderGroupIds.add(orderGroupId);
		}		
	}

	public void add(OutboundOrderBatch batch) {
		for (OutboundOrderCsvBean line : batch.getLines()) {
			this.add(line);
		}
	}

	public int size() {
		return this.lines.size();
	}
	
	@Override
	public String toString() {
		return "OutboundOrderBatch #"+this.batchId+"("+this.lines.size()+")";
	}
}
