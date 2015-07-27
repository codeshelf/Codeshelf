package com.codeshelf.api.resources;

import java.util.Collections;
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

import lombok.Setter;

import com.codeshelf.api.BaseResponse;
import com.codeshelf.model.OrderStatusEnum;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.service.OrderService;
import com.codeshelf.service.OrderService.OrderDetailView;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;

public class OrdersResource {

	private OrderService orderService;

	@Setter
	private Facility facility;
	
	@Inject 
	public OrdersResource(OrderService orderService) {
		this.orderService = orderService;
	}
	
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response getOrders(@QueryParam("status") String status, @QueryParam("orderId") String orderIdValue, @QueryParam("properties") List<String> propertyNamesList) {
		String[] propertyNames = propertyNamesList.toArray(new String[]{});

		List<Map<String, Object>> results = Collections.emptyList();
		if (orderIdValue != null) {
			results = this.orderService.findOrderHeadersForOrderId(facility, propertyNames, orderIdValue);
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
	@Path("/{orderId}/details")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getOrders(@PathParam("orderId") String orderDomainId) {
		List<OrderDetailView> results = this.orderService.getOrderDetailsForOrderId(facility, orderDomainId);
		return BaseResponse.buildResponse(results);
	}

}
