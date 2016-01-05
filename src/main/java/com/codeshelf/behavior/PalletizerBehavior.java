package com.codeshelf.behavior;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.model.OrderStatusEnum;
import com.codeshelf.model.WiFactory;
import com.codeshelf.model.WorkInstructionStatusEnum;
import com.codeshelf.model.WorkInstructionTypeEnum;
import com.codeshelf.model.WiFactory.WiPurpose;
import com.codeshelf.model.domain.Che;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.ItemMaster;
import com.codeshelf.model.domain.Location;
import com.codeshelf.model.domain.OrderDetail;
import com.codeshelf.model.domain.OrderHeader;
import com.codeshelf.model.domain.OrderLocation;
import com.codeshelf.model.domain.UomMaster;
import com.codeshelf.model.domain.WorkInstruction;
import com.codeshelf.util.UomNormalizer;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.inject.Inject;

import lombok.Getter;
import lombok.Setter;

public class PalletizerBehavior implements IApiBehavior{
	private LightBehavior lightService;
	private NotificationBehavior notificationBehavior;
	
	private static final Logger			LOGGER					= LoggerFactory.getLogger(WorkBehavior.class);
	
	@Inject
	public PalletizerBehavior(LightBehavior inLightService, NotificationBehavior notificationBehavior) {
		this.lightService = inLightService;
		this.notificationBehavior = notificationBehavior;
	}

	public PalletizerInfo processPalletizerItemRequest(Che che, String itemId, String userId){
		Facility facility = che.getFacility();
		String storeId = generatePalletizerStoreId(itemId);
		PalletizerInfo info = new PalletizerInfo();
		info.setItem(itemId);
		info.setOrderId(storeId);
		Location location = null;
		//Get order for this item
		OrderHeader order = getActivePalletizerOrder(facility, itemId);
		if (order == null){
			info.setOrderFound(false);
			return info;
		} else {
			List<OrderLocation> orderLocations = order.getActiveOrderLocations();
			if (!orderLocations.isEmpty()) {
				location = orderLocations.get(0).getLocation();
				info.setLocation(location.getBestUsableLocationName());
			} else {
				LOGGER.warn("Palletizer order {} doesn't have a location", storeId);
				info.setOrderFound(false);
				return info;
			}
		}
		info.setOrderFound(true);
		//Create order detail, or reuse one from the same item in this order
		OrderDetail detail = order.getOrderDetail(itemId);
		ItemMaster itemMaster = null;
		UomMaster uomMaster = null;
		if (detail == null) {
			uomMaster = facility.getUomMaster(UomNormalizer.EACH);
			if (uomMaster == null) {
				uomMaster = facility.createUomMaster(UomNormalizer.EACH);
			}
			itemMaster = ItemMaster.staticGetDao().findByDomainId(facility, itemId);
			if (itemMaster == null) {
				itemMaster = facility.createItemMaster(itemId, null, uomMaster);
			}
			detail = new OrderDetail(itemId, itemMaster, 1);
			detail.setUomMaster(uomMaster);
			order.addOrderDetail(detail);
			UomMaster.staticGetDao().store(uomMaster);
			ItemMaster.staticGetDao().store(itemMaster);
			OrderHeader.staticGetDao().store(order);
		} else {
			detail.setQuantities(detail.getQuantity() + 1);
			itemMaster = detail.getItemMaster();
			uomMaster = detail.getUomMaster();
		}
		detail.setStatus(OrderStatusEnum.INPROGRESS);
		detail.setUpdated(new Timestamp(System.currentTimeMillis()));
		OrderDetail.staticGetDao().store(detail);
		//Create work instruction for the new order detail
		WorkInstruction wi = WiFactory.createWorkInstruction(WorkInstructionStatusEnum.INPROGRESS,
			WorkInstructionTypeEnum.ACTUAL,
			WiPurpose.WiPurposePalletizerPut,
			detail,
			che,
			new Timestamp(System.currentTimeMillis()),
			null,
			location
			);
		wi.setPickerId(userId);
		info.setWi(wi);
		return info;
	}
	
	public PalletizerInfo processPalletizerNewOrderRequest(Che che, String itemId, String locationStr, String userId){
		Facility facility = che.getFacility();
		String storeId = generatePalletizerStoreId(itemId);
		PalletizerInfo info = new PalletizerInfo();
		info.setItem(itemId);
		info.setOrderId(storeId);
		info.setOrderFound(false);
		Location location = facility.findSubLocationById(locationStr);
		if (location == null) {
			LOGGER.error("Could not find location {}", locationStr);
			info.setErrorMessage1("Not found: " + locationStr);
			info.setErrorMessage2("Scan another location");
			return info;
		}
		String existingStoreId = getPalletizerStoreIdAtLocation(location);
		if (existingStoreId != null){
			LOGGER.error("Palletizer Location {} occupied with {}", location.getBestUsableLocationName(), existingStoreId);
			info.setErrorMessage1("Busy: " + location.getBestUsableLocationName());
			info.setErrorMessage2("Remove " + existingStoreId + " First");
			return info;
		}
		
		OrderHeader order = getActivePalletizerOrder(facility, itemId);
		if (order == null) {
			String orderId = generatePalletizerOrderId(itemId, location.getNominalLocationId());
			LOGGER.info("Creating Order {}", orderId);
			order = OrderHeader.createEmptyOrderHeader(facility, orderId);
			order.addOrderLocation(location);
			OrderHeader.staticGetDao().store(order);
		} else if (order.getActiveOrderLocations().isEmpty()){
			order.addOrderLocation(location);
			OrderHeader.staticGetDao().store(order);
		} else {
			LOGGER.warn("Somewhy processPalletizerNewLocationRequest() was called for an item {} that already has an order with an active location", itemId);
		}
		return processPalletizerItemRequest(che, itemId, userId);
	}
	private String generatePalletizerStoreId(String itemId){
		if (itemId == null) {
			return null;
		}
		return itemId.length() <= 4 ? itemId : itemId.substring(0, 4);
	}
	
	private String generatePalletizerOrderId(String itemId, String location){
		String storeId = generatePalletizerStoreId(itemId);
		String orderId = "P_" + storeId + "-" + location + "-" + System.currentTimeMillis();
		return orderId;
	}
	
	private OrderHeader getActivePalletizerOrder(Facility facility, String itemId) {
		String storeId = generatePalletizerStoreId(itemId);
		Map<String, Object> params = ImmutableMap.<String, Object> of(
			"facilityId", facility.getPersistentId(), 
			"partialDomainId", "P_" + storeId + "%",
			"notStatus", OrderStatusEnum.COMPLETE);
		List<OrderHeader> orders = OrderHeader.staticGetDao().findByFilter("orderHeadersByFacilityAndPartialDomainId", params);
		return orders.isEmpty() ? null : orders.get(0);
	}
	
	private String getPalletizerStoreIdAtLocation(Location location) {
		if (location == null) {
			return null;
		}
		List<OrderLocation> existingOrdersLocations = OrderLocation.findOrderLocationsAtLocation(location, location.getFacility(), true);
		if (existingOrdersLocations.isEmpty()) {
			return null;
		}
		OrderHeader order = existingOrdersLocations.get(0).getParent();
		String orderId = order.getDomainId();
		if (!orderId.startsWith("P_")){
			return null;
		}
		return orderId.length() >= 6 ? orderId.substring(2, 6) : orderId.substring(2);
	}
	
	public String removeOrder(Che che, String prefix, String scan) {
		if (prefix == null || prefix.isEmpty()) {
			return removeOrderByLicense(che, scan);
		} else if ("%".equalsIgnoreCase(prefix) || "L%".equalsIgnoreCase(prefix)) {
			return removeOrderByLocation(che, prefix, scan); 
		} else {
			return "Bad scan " + prefix + scan;
		}
	} 
	
	private String removeOrderByLocation(Che che, String prefix, String scan) {
		String locationStr = "%".equals(prefix) ? prefix + scan : scan;
		Facility facility = che.getFacility();
		Location location = facility.findSubLocationById(locationStr);
		if (location == null) {
			return locationStr + " not found";
		}
		List<OrderLocation> orderLocations = OrderLocation.findOrderLocationsAtLocationAndChildren(location, facility, true);
		if (orderLocations == null || orderLocations.isEmpty()) {
			return "No Pallets In " + locationStr;
		}
		deactivateAndIlluminateOrders(che, orderLocations, null);
		return null;
	}
	
	private String removeOrderByLicense(Che che, String license){
		Facility facility = che.getFacility();
		String storeId = license.length() >= 4 ? license.substring(0, 4) : license;
		OrderHeader order = getActivePalletizerOrder(facility, license);
		if (order == null) {
			LOGGER.warn("Order {} not found for palletizer removal", storeId);
			return "Pallet " + storeId + " Not Found"; 
		}
		order.setDomainId(license + "_" + System.currentTimeMillis());
		
		//Should be just one order location
		List<OrderLocation> orderLocations = order.getActiveOrderLocations();
		deactivateAndIlluminateOrders(che, orderLocations, license);
		return null;		
	}
	
	private void deactivateAndIlluminateOrders(Che che, List<OrderLocation> orderLocations, String license) {
		List<Location> locations = Lists.newArrayList();
		for (OrderLocation orderLocation : orderLocations) {
			locations.add(orderLocation.getLocation());
			OrderHeader order = orderLocation.getParent();
			order.setStatus(OrderStatusEnum.COMPLETE);
			List<OrderDetail> details = order.getOrderDetails();
			for (OrderDetail detail : details) {
				detail.setStatus(OrderStatusEnum.COMPLETE);
				OrderDetail.staticGetDao().store(detail);
				for (WorkInstruction wi : detail.getWorkInstructions()){
					completeWi(wi.getPersistentId(), false);
					if (license != null) {
						wi.setContainerId(license);
					}
					WorkInstruction.staticGetDao().store(wi);
				}
			}
			OrderHeader.staticGetDao().store(order);
		}
		lightService.lightLocationServerCall(locations, che.getColor());
	}
	
	public void completeWi(UUID wiId, Boolean shorted) {
		WorkInstruction wi = WorkInstruction.staticGetDao().findByPersistentId(wiId);
		if (wi == null) {
			LOGGER.error("Palletizer Complete Wi Error: Did not find Wi " + wiId);
			return;
		}
		Timestamp now = new Timestamp(System.currentTimeMillis());
		wi.setActualQuantity(Boolean.TRUE.equals(shorted) ? 0 : 1);
		wi.setCompleted(now);
		wi.setStatus(WorkInstructionStatusEnum.COMPLETE);
		OrderDetail detail = wi.getOrderDetail();
		detail.setUpdated(now);
		detail.setStatus(OrderStatusEnum.COMPLETE);
		WorkInstruction.staticGetDao().store(wi);
		OrderDetail.staticGetDao().store(detail);
		
		if (!wi.isHousekeeping()) {
			notificationBehavior.saveFinishedWI(wi);
		}

	}
	
	public static class PalletizerInfo {
		@Getter 
		@Setter
		private boolean orderFound = false;
		
		@Getter 
		@Setter
		private String orderId;
		
		@Getter 
		@Setter
		private String item;
		
		@Getter 
		@Setter
		private String location;
		
		@Getter 
		@Setter
		private WorkInstruction wi;
		
		@Getter 
		@Setter
		private String errorMessage1, errorMessage2;
	}
}
