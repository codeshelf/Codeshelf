package com.codeshelf.service;

import java.util.HashMap;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.codeshelf.model.OrderStatusEnum;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.OrderDetail;
import com.codeshelf.model.domain.OrderGroup;
import com.codeshelf.model.domain.OrderHeader;

@JsonAutoDetect(getterVisibility=Visibility.PUBLIC_ONLY, fieldVisibility=Visibility.NONE)
public class ProductivitySummaryList {
	@Getter
	private HashMap<String, StatusSummary> groups = new HashMap<>();
	
	public static class StatusSummary{
		@Getter
		@Setter
		private int invalid, created, released, inprogress, complete;
		
		//Notice that short is a keyword so make strange case and use explicit getter/setter
		private int sHort;
		
		@Setter
		@Getter
		private Double picksPerHour;

		public void add(int subtotal, OrderStatusEnum orderStatus) {
			switch (orderStatus) {
				case INVALID:
					invalid += subtotal;
					break;
				case CREATED:
					created += subtotal;
					break;
				case RELEASED:
					released += subtotal;
					break;
				case INPROGRESS:
					inprogress += subtotal;
					break;
				case COMPLETE:
					complete += subtotal;
					break;
				case SHORT:
					sHort += subtotal;
					break;
			}

		}
		
		public int getShort() {
			return sHort;
		}

		public void setShort(int sHort) {
			this.sHort = sHort;
		}
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
		StatusSummary statusSummary = groups.get(groupName);
		if (statusSummary == null) {
			statusSummary = new StatusSummary();
			groups.put(groupName, statusSummary);
		}
		for (OrderDetail orderDetail : orderHeader.getOrderDetails()) {
			OrderStatusEnum status = orderDetail.getStatus();
			statusSummary.add(1, status);
		}
	}
	
	private void assignPicksPerHour(List<Object[]> picksPerHour) {
		if (picksPerHour == null) {return;}
		StatusSummary statusSummary = null;
		String groupName = null;
		Double groupPicks = null;
		for (Object[] picksPerHourOneGroup : picksPerHour) {
			groupName = picksPerHourOneGroup[0].toString(); 
			groupPicks = (Double)picksPerHourOneGroup[1];
			statusSummary = groups.get(groupName);
			if (statusSummary != null) {
				statusSummary.setPicksPerHour(groupPicks);
			}
		}		
	}
}