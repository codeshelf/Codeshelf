package com.codeshelf.api.resources;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.api.BaseResponse;
import com.codeshelf.api.BaseResponse.IntervalParam;
import com.codeshelf.api.responses.EventDisplay;
import com.codeshelf.behavior.NotificationBehavior;
import com.codeshelf.behavior.OrderBehavior;
import com.codeshelf.behavior.OrderBehavior.OrderDetailView;
import com.codeshelf.model.OrderStatusEnum;
import com.codeshelf.model.dao.ResultDisplay;
import com.codeshelf.model.domain.Facility;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;

import lombok.Setter;

public class OrdersResource {
	private static final Logger	LOGGER	= LoggerFactory.getLogger(OrdersResource.class);

	private OrderBehavior orderService;

	@Setter
	private Facility facility;

	private NotificationBehavior notificationBehavior;
	
	@Inject 
	public OrdersResource(OrderBehavior orderService, NotificationBehavior notificationBehavior) {
		this.orderService = orderService;
		this.notificationBehavior = notificationBehavior;
	}

	@GET
	@Path("/references")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getOrderReferences(@QueryParam("orderId") String orderIdSubstring, @QueryParam("dueDate") IntervalParam dueDate) {
		List<Object[]> results = this.orderService.findOrderHeaderReferences(facility, orderIdSubstring, dueDate.getValue());
		return BaseResponse.buildResponse(results);
	
	}
	
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response getOrders(@QueryParam("status") String status, @QueryParam("orderId") String orderIdValue, @QueryParam("properties") List<String> propertyNamesList, @QueryParam("limit") Integer limit) {
		String[] propertyNames = propertyNamesList.toArray(new String[]{});

		ResultDisplay<Map<String, Object>> results = new ResultDisplay<Map<String, Object>>(new ArrayList<Map<String, Object>>());
		if (orderIdValue != null) {
			results = this.orderService.findOrderHeadersForOrderId(facility, propertyNames, orderIdValue, limit);
		} else if (status != null) {
	    	results = this.orderService.findOrderHeadersForStatus(facility, propertyNames, new OrderStatusEnum[]{OrderStatusEnum.valueOf(status)});
		} else {
			//TODO dirty implementation to return all
			results = this.orderService.findOrderHeadersForStatus(facility, propertyNames, OrderStatusEnum.values());
		}
		return BaseResponse.buildResponse(results);
	}
	
	@DELETE
	@Produces(MediaType.APPLICATION_JSON)
	public Response deleteOrders() {
		int result = this.orderService.deleteAll(this.facility);
		return BaseResponse.buildResponse(ImmutableMap.<String, Integer>of("count", result));
	}
	
	@GET
	@Path("/{orderId}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getOrder(@PathParam("orderId") String orderDomainId, @QueryParam("properties") List<String> propertyNamesList) {
		String[] propertyNames = propertyNamesList.toArray(new String[]{});
		ResultDisplay<Map<String, Object>> results = this.orderService.findOrderHeadersForOrderId(facility, propertyNames, orderDomainId, null);
		if (results.size() == 1) {
			return BaseResponse.buildResponse(results.getResults().iterator().next());
		} else if (results.size() == 0){
			return BaseResponse.buildResponse(null);
			
		} else {
			LOGGER.error("Found multiple orders for {} in facility {}", orderDomainId, facility); 
			return BaseResponse.buildResponse(null);
		}
	}
	
	
	@GET
	@Path("/{orderId}/details")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getOrders(@PathParam("orderId") String orderDomainId) {
		List<OrderDetailView> results = this.orderService.getOrderDetailsForOrderId(facility, orderDomainId);
		return BaseResponse.buildResponse(results);
	}

	@GET
	@Path("/{orderId}/events")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getOrderEvents(@PathParam("orderId") String orderDomainId) {
		List<EventDisplay> results= this.notificationBehavior.getOrderEventsForOrderId(facility, orderDomainId);
		return BaseResponse.buildResponse(results);
	}

	
}
