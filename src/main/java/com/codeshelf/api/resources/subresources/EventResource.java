package com.codeshelf.api.resources.subresources;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.UUID;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import lombok.Setter;

import org.apache.shiro.authz.annotation.RequiresPermissions;

import com.codeshelf.api.BaseResponse;
import com.codeshelf.api.ErrorResponse;
import com.codeshelf.api.responses.EventDisplay;
import com.codeshelf.edi.ICsvOrderImporter;
import com.codeshelf.edi.OutboundOrderCsvBean;
import com.codeshelf.model.OrderTypeEnum;
import com.codeshelf.model.domain.Gtin;
import com.codeshelf.model.domain.ItemMaster;
import com.codeshelf.model.domain.OrderDetail;
import com.codeshelf.model.domain.Resolution;
import com.codeshelf.model.domain.UomMaster;
import com.codeshelf.model.domain.WorkerEvent;
import com.codeshelf.model.domain.WorkerEvent.EventType;
import com.codeshelf.security.CodeshelfSecurityManager;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Provider;

public class EventResource {
	private final Provider<ICsvOrderImporter>			orderImporterProvider;
	
	@Setter
	private WorkerEvent event;

	@Inject
	public EventResource(Provider<ICsvOrderImporter> orderImporterProvider){
		this.orderImporterProvider = orderImporterProvider;
	}
	
	@GET
	@RequiresPermissions("event:view")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getEvent() {
		EventDisplay eventDisplay = EventDisplay.createEventDisplay(event);
		return BaseResponse.buildResponse(eventDisplay);
	}

	@POST
	@Path("resolve")
	@RequiresPermissions("event:edit")
	@Produces(MediaType.APPLICATION_JSON)
	public Response resolveEvent() {
		String resolvedBy = CodeshelfSecurityManager.getCurrentUserContext().getUsername();
		ErrorResponse errors = new ErrorResponse();
		try {
			Resolution resolution = event.getResolution();
			if (resolution == null) {
				resolution = new Resolution();
			}
			resolution.setParent(event.getFacility());
			resolution.setDomainId(event.getDomainId());
			resolution.setResolvedBy(resolvedBy);
			resolution.setTimestamp(new Timestamp(System.currentTimeMillis()));
			event.setResolution(resolution);
			Resolution.staticGetDao().store(resolution);
			WorkerEvent.staticGetDao().store(event);
			
			EventDisplay eventDisplay = EventDisplay.createEventDisplay(event);
			return BaseResponse.buildResponse(eventDisplay);
		} catch (Exception e) {
			return errors.processException(e);
		}
	}

	@POST
	@Path("replenish")
	@RequiresPermissions("event:edit")
	@Produces(MediaType.APPLICATION_JSON)
	public Response createReplenishOrderForEvent() {
		try {
			EventType type = event.getEventType();
			if (type != EventType.SHORT && type != EventType.SHORT_AHEAD && type != EventType.LOW && type != EventType.SUBSTITUTION){
				throw new Exception(type + " event is illegal for replenishing. Call on SHORT, LOW or SUBSTITUTION events");
			}
			UUID orderDetailId = event.getOrderDetailId(); 
			if (orderDetailId != null) {
				OrderDetail detail = OrderDetail.staticGetDao().findByPersistentId(orderDetailId);
				if (detail != null) {
					ItemMaster itemMaster = detail.getItemMaster();
					UomMaster uom = detail.getUomMaster();
					Gtin gtin = itemMaster.getGtinForUom(uom);
					String scannableId = gtin == null ? itemMaster.getDomainId() : gtin.getDomainId();
					String location = event.getLocation();
					if (location == null){
						location = "";
					}
					OutboundOrderCsvBean orderBean = new OutboundOrderCsvBean();
					orderBean.setLineNumber(1);
					orderBean.setOrderId(scannableId);
					orderBean.setItemId(itemMaster.getDomainId());
					orderBean.setQuantity("1");
					orderBean.setUom(uom.getDomainId());
					orderBean.setLocationId(location);
					orderBean.setPreAssignedContainerId(scannableId);
					orderBean.setWorkSequence("0");
					orderBean.setOperationType(OrderTypeEnum.REPLENISH.name());
					ArrayList<OutboundOrderCsvBean> orderBeanList = new ArrayList<>(1);
					orderBeanList.add(orderBean);
					ICsvOrderImporter orderImporter = orderImporterProvider.get();
					orderImporter.importOrdersFromBeanList(orderBeanList, event.getFacility(), new Timestamp(System.currentTimeMillis()));
					
					return BaseResponse.buildResponse(ImmutableMap.of("scannableId", scannableId));
				}
			}
			 ErrorResponse response = new ErrorResponse("Unable to find associated OrderDetail for this event");
			 return response.buildResponse();
		} catch (Exception e) {
			return new ErrorResponse().processException(e);
		}		
	}
}