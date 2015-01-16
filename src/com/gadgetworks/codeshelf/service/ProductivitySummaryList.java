package com.gadgetworks.codeshelf.service;

import java.util.HashMap;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.gadgetworks.codeshelf.model.OrderStatusEnum;
import com.gadgetworks.codeshelf.model.domain.Facility;
import com.gadgetworks.codeshelf.model.domain.OrderDetail;
import com.gadgetworks.codeshelf.model.domain.OrderGroup;
import com.gadgetworks.codeshelf.model.domain.OrderHeader;

@JsonAutoDetect(getterVisibility=Visibility.PUBLIC_ONLY, fieldVisibility=Visibility.NONE)
public class ProductivitySummaryList {
	@Getter
	private HashMap<String, GroupSummary> groups = new HashMap<>();
	
	public class GroupSummary{
		@Getter
		private short invalid, created, released, inprogress, complete, sHort;
		
		@Setter
		@Getter
		private Double picksPerHour;
	}
	
	public ProductivitySummaryList(Facility facility, List<Object[]> picksPerHour) {
		if (facility == null || facility.getOrderHeaders() == null) {return;}
		for (OrderHeader orderHeader : facility.getOrderHeaders()){
			processOrder(orderHeader);
		}
		assignPicksPerHour(picksPerHour);
	}
	
	private void processOrder(OrderHeader orderHeader){
		String groupName = orderHeader.getOrderGroup() == null? OrderGroup.UNDEFINED : orderHeader.getOrderGroup().getDomainId();
		GroupSummary groupSummary = groups.get(groupName);
		if (groupSummary == null) {
			groupSummary = new GroupSummary();
			groups.put(groupName, groupSummary);
		}
		for (OrderDetail orderDetail : orderHeader.getOrderDetails()) {
			OrderStatusEnum status = orderDetail.getStatus();
			switch (status) {
				case INVALID:
					groupSummary.invalid++;
					break;
				case CREATED:
					groupSummary.created++;
					break;
				case RELEASED:
					groupSummary.released++;
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
	
	private void assignPicksPerHour(List<Object[]> picksPerHour) {
		if (picksPerHour == null) {return;}
		GroupSummary group = null;
		String groupName = null;
		Double groupPicks = null;
		for (Object[] picksPerHourOneGroup : picksPerHour) {
			groupName = picksPerHourOneGroup[0].toString(); 
			groupPicks = (Double)picksPerHourOneGroup[1];
			group = groups.get(groupName);
			if (group != null) {
				group.setPicksPerHour(groupPicks);
			}
		}		
	}
}