package com.gadgetworks.codeshelf.model.domain;

import java.util.HashMap;

import lombok.Getter;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.gadgetworks.codeshelf.apiresources.BaseResponse;
import com.gadgetworks.codeshelf.model.OrderStatusEnum;
import com.gadgetworks.codeshelf.model.dao.ITypedDao;

@JsonAutoDetect(getterVisibility=Visibility.PUBLIC_ONLY, fieldVisibility=Visibility.NONE)
public class ProductivitySummary extends BaseResponse{
	@Getter
	private HashMap<String, GroupSummary> groups = new HashMap<>();
	
	private class GroupSummary{
		@Getter
		private short invalid, created, release, inprogress, complete, sHort;
	}
	
	public ProductivitySummary(Facility facility) {
		if (facility == null || facility.getOrderHeaders() == null) {return;}
		for (OrderHeader orderHeader : facility.getOrderHeaders()){
			processOrder(orderHeader);
		}
	}
	
	private void processOrder(OrderHeader orderHeader){
		String groupName = orderHeader.getOrderGroup() == null? "undefined" : orderHeader.getOrderGroup().getDomainId();
		GroupSummary groupSummary = groups.get(groupName);
		if (groupSummary == null) {
			groupSummary = new GroupSummary();
			groups.put(groupName, groupSummary);
		}
		OrderStatusEnum status = orderHeader.getStatus();
		switch (status) {
			case INVALID:
				groupSummary.invalid++;
				break;
			case CREATED:
				groupSummary.created++;
				break;
			case RELEASE:
				groupSummary.release++;
				break;
			case INPROGRESS:
				groupSummary.inprogress++;
				break;
			case COMPLETE:
				groupSummary.complete++;
				break;
			case SHORT:
				groupSummary.sHort++;
				break;
		}
	}
}
