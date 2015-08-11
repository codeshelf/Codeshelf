package com.codeshelf.service;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.model.OrderStatusEnum;
import com.codeshelf.model.WiFactory;
import com.codeshelf.model.WorkInstructionStatusEnum;
import com.codeshelf.model.WorkInstructionTypeEnum;
import com.codeshelf.model.WiFactory.WiPurpose;
import com.codeshelf.model.domain.Che;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.Item;
import com.codeshelf.model.domain.ItemMaster;
import com.codeshelf.model.domain.Location;
import com.codeshelf.model.domain.OrderDetail;
import com.codeshelf.model.domain.OrderHeader;
import com.codeshelf.model.domain.OrderLocation;
import com.codeshelf.model.domain.UomMaster;
import com.codeshelf.model.domain.WorkInstruction;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.inject.Inject;

import lombok.Getter;
import lombok.Setter;

public class PalletizerService implements IApiService{
	private LightService lightService;
	
	private static final Logger			LOGGER					= LoggerFactory.getLogger(WorkService.class);
	
	@Inject
	public PalletizerService(LightService inLightService) {
		this.lightService = inLightService;
	}

	public PalletizerInfo processPalletizerItemRequest(Che che, String itemId){
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
			} else {
				LOGGER.warn("Palletizer order {} doesn't have a location", storeId);
			}
		}
		info.setOrderFound(true);
		info.setLocation(location.getBestUsableLocationName());
		//Create order detail, or reuse one from the same item in this order
		OrderDetail detail = order.getOrderDetail(itemId);
		ItemMaster itemMaster = null;
		UomMaster uomMaster = null;
		if (detail == null) {
			uomMaster = facility.createUomMaster("EA");
			itemMaster = facility.createItemMaster(itemId, null, uomMaster);
			detail = new OrderDetail(itemId, true);
			detail.setStatus(OrderStatusEnum.RELEASED);
			detail.setUpdated(new Timestamp(System.currentTimeMillis()));
			detail.setQuantities(1);
			detail.setItemMaster(itemMaster);
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
		OrderDetail.staticGetDao().store(detail);
		Item item = itemMaster.findOrCreateItem(location, uomMaster);
		//Create work instruction for the new item
		WorkInstruction wi = WiFactory.createWorkInstruction(WorkInstructionStatusEnum.INPROGRESS,
			WorkInstructionTypeEnum.ACTUAL,
			item,
			che,
			WiPurpose.WiPalletizerPut,
			true,
			new Timestamp(System.currentTimeMillis()));
		info.setWi(wi);
		return info;
	}
	
	public PalletizerInfo processPalletizerNewOrderRequest(Che che, String itemId, String locationStr){
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
			return info;
		}
		String existingStoreId = getPalletizerStoreIdAtLocation(location);
		if (existingStoreId != null){
			LOGGER.error("Palletizer Location {} occupied with {}", location.getBestUsableLocationName(), existingStoreId);
			info.setErrorMessage1("Busy: " + location.getBestUsableLocationName());
			info.setErrorMessage2("Remove " + existingStoreId + " first");
			return info;
		}
		
		OrderHeader order = getActivePalletizerOrder(facility, itemId);
		if (order == null) {
			String orderId = generatePalletizerOrderId(itemId, location.getNominalLocationId());
			LOGGER.info("Creating Order {}", orderId);
			order = OrderHeader.createEmptyOrderHeader(facility, orderId);
			order.addOrderLocation(location);
			OrderHeader.staticGetDao().store(order);
		} else {
			LOGGER.warn("Somewhy processPalletizerNewLocationRequest() was called for an item {} that already has a order", itemId);
		}
		return processPalletizerItemRequest(che, itemId);
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
		Map<String, Object> params = ImmutableMap.<String, Object> of("facilityId", facility.getPersistentId(), "partialDomainId", "P_" + storeId + "%");
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
	
	public String removeOrder(Che che, String license) {
		Facility facility = che.getFacility();
		String storeId = license.length() >= 4 ? license.substring(0, 4) : license;
		OrderHeader order = getActivePalletizerOrder(facility, license);
		if (order == null) {
			LOGGER.warn("Order {} not found for palletizer removal", storeId);
			return storeId + " not found"; 
		}
		order.setActive(false);
		order.setDomainId(license);
		OrderHeader.staticGetDao().store(order);
		
		//Should be just one order location
		List<OrderLocation> orderLocations = order.getActiveOrderLocations();
		List<Location> locations = Lists.newArrayList();
		for (OrderLocation orderLocation : orderLocations) {
			locations.add(orderLocation.getLocation());
		}
		lightService.lightLocationServerCall(locations, che.getColor());
		return null;
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
