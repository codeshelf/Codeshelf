package com.codeshelf.behavior;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;

import org.hibernate.Session;
import org.hibernate.transform.AliasToEntityMapResultTransformer;

import com.codeshelf.model.OrderStatusEnum;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.OrderGroup;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;

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
				case SUBSTITUTION:
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
	
	public ProductivitySummaryList(Facility facility, List<Object[]> picksPerHour, Session session) {
		if (facility == null) {
			return;
		}
		
		@SuppressWarnings("unchecked")
		//group orderdetails by ordergroup and status
		List<Map<String, Object>> results = session
			.createQuery( "select count(od.status) as total, od.status as status, og as orderGroup"
						+ " from OrderDetail as od left join od.parent.orderGroup as og"
						+ " where od.active = true group by og, od.status")
			.setResultTransformer(AliasToEntityMapResultTransformer.INSTANCE)
			.setCacheable(true)
			.list(); 
		
		List<OrderGroup> orderGroups = facility.getOrderGroups();
		//make sure there is a summary for each group
		for (OrderGroup orderGroup : orderGroups) {
			groups.put(toGroupName(orderGroup), new StatusSummary());
		}
		
		for (Map<String, Object> statusRow : results) {
			OrderStatusEnum status = OrderStatusEnum.valueOf(statusRow.get("status").toString()); 
			int total              = Integer.valueOf(statusRow.get("total").toString());
			String groupName       = toGroupName((OrderGroup)statusRow.get("orderGroup"));
			processSummary(groupName, status, total);
		}
		assignPicksPerHour(picksPerHour);
	}
	
	private String toGroupName(OrderGroup orderGroup) {
		if (orderGroup == null) {
			return OrderGroup.UNDEFINED;
		}
		return orderGroup.getDomainId();
	}

	private void processSummary(String groupName, OrderStatusEnum orderStatus, int total){
		StatusSummary statusSummary = groups.get(groupName);
		if (statusSummary == null) {
			statusSummary = new StatusSummary();
			groups.put(groupName, statusSummary);
		}
		statusSummary.add(total, orderStatus);
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