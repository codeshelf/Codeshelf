package com.codeshelf.service;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.device.CheDeviceLogic;
import com.codeshelf.flyweight.command.ColorEnum;
import com.codeshelf.model.CodeshelfTape;
import com.codeshelf.model.CodeshelfTape.TapeLocation;
import com.codeshelf.model.OrderStatusEnum;
import com.codeshelf.model.domain.DomainObjectProperty;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.Item;
import com.codeshelf.model.domain.Location;
import com.codeshelf.model.domain.OrderDetail;
import com.codeshelf.model.domain.OrderHeader;
import com.codeshelf.model.domain.OrderLocation;
import com.codeshelf.ws.protocol.request.InfoRequest;
import com.codeshelf.ws.protocol.request.InfoRequest.InfoRequestType;
import com.google.common.collect.Lists;
import com.google.inject.Inject;

public class InfoService implements IApiService{
	private LightService lightService;
	private WorkService workService;
	@SuppressWarnings("unused")
	private static final Logger	LOGGER			= LoggerFactory.getLogger(InfoService.class);
	
	@Inject
	public InfoService(LightService inLightService, WorkService inWorkService) {
		this.lightService = inLightService;
		this.workService = inWorkService;
	}

	public String[] getInfo(Facility facility, InfoRequest request, ColorEnum color){
		InfoRequestType type = request.getType();
		String info[] = null;
		String location = request.getLocation();
		switch(type){
			case GET_WALL_LOCATION_INFO:
				info = getWallLocationInfo(facility, location, color);
				return info;

			case GET_INVENTORY_INFO:
				info = getInventoryInfo(facility, location, color);
				return info;

			case LIGHT_COMPLETE_ORDERS:
				lightOrdersInWall(facility, location, color, true);
				return null;
				
			case LIGHT_INCOMPLETE_ORDERS:
				lightOrdersInWall(facility, location, color, false);
				return null;
				
			case REMOVE_WALL_ORDERS:
				removeOrdersFromLocation(facility, location);
				info = getWallLocationInfo(facility, location, color);
				return info;
				
			case REMOVE_INVENTORY:
				removeItemFromInventory(facility, location);
				info = getInventoryInfo(facility, location, color);
				return info;
				
			default:
				String unexpectedRequest[] = new String[3];
				unexpectedRequest[0] = "Unexpected Request";
				unexpectedRequest[1] = "Type = " + (type == null ? "null" : type.toString());
				return unexpectedRequest;
		}
	}
	
	private String[] getWallLocationInfo(Facility facility, String locationStr, ColorEnum color){
		String[] info = new String[4];
		Location location = facility.findSubLocationById(locationStr);
		if (location == null) {
			info[0] = "Could not find";
			info[1] = "Location " + locationStr;
			return info;
		}
		lightService.lightLocationServerCall(location, color);
		String locationName = location.getBestUsableLocationName();
		List<OrderHeader> ordersInLocation = getOrdersInLocation(location);
		int numOrders = ordersInLocation.size();
		int incompleteOrders = 0;
		int incompleteDetails = 0;
		OrderDetail singleRemainingDetail = null;
		for (OrderHeader order : ordersInLocation){
			order.reevaluateOrderAndDetails();
			if (order.getStatus() != OrderStatusEnum.COMPLETE) {
				incompleteOrders++;
			}
			for (OrderDetail detail : order.getOrderDetails()) {
				if (detail.getStatus() != OrderStatusEnum.COMPLETE) {
					singleRemainingDetail = detail;
					incompleteDetails++;
				}
			}
		}
		if (numOrders == 0) {
			info[0] = locationName;
			info[1] = "Order: none";
		} else if (numOrders == 1){
			OrderHeader order = ordersInLocation.get(0);
			info[0] = locationName;
			info[1] = "Order: " + order.getDomainId();
			int completeDetails = order.getOrderDetails().size() - incompleteDetails;
			info[2] = "Complete: " + completeDetails + " jobs";
			if (incompleteDetails == 0) {
				//Leave last line blank
			} else if (incompleteDetails == 1) {
				String scanType = PropertyService.getInstance().getPropertyFromConfig(facility, DomainObjectProperty.SCANPICK);
				String gtin = singleRemainingDetail.getGtinId();
				if ("UPC".equalsIgnoreCase(scanType) && !gtin.isEmpty()){
					info[3] = "Remain: " + singleRemainingDetail.getGtinId();
				} else {
					info[3] = "Remain: " + singleRemainingDetail.getItemMasterId();
				}
			} else {
				info[3] = "Remain: " + incompleteDetails + " jobs";
			}
		} else {
			info[0] = locationName + " has " +  numOrders + " orders";
			info[1] = incompleteOrders + " incompl, with " + incompleteDetails + " jobs";
			info[2] = "YES: light completes";
			info[3] = "NO: light incompletes";
		}
		return info;
	}
	
	private void lightOrdersInWall(Facility facility, String locationStr, ColorEnum color, boolean complete) {
		Location location = facility.findSubLocationById(locationStr);
		if (location == null) {
			return;
		}
		List<OrderLocation> orderLocations = getOrderLocationsInLocation(location);
		List<Location> locationsToLight = Lists.newArrayList();
		OrderHeader order = null;
		for (OrderLocation orderLocation : orderLocations){
			order = orderLocation.getParent();
			order.reevaluateOrderAndDetails();
			boolean orderComplete = order.getStatus() == OrderStatusEnum.COMPLETE;
			if (orderComplete == complete){
				locationsToLight.add(orderLocation.getLocation());
			}
		}
		lightService.lightLocationServerCall(locationsToLight, color);
	}
	
	private void removeOrdersFromLocation(Facility facility, String locationStr) {
		Location location = facility.findSubLocationById(locationStr);
		if (location == null) {
			return;
		}
		List<OrderLocation> orderLocations = getOrderLocationsInLocation(location);
		for (OrderLocation orderLocation : orderLocations) {
			orderLocation.getParent().removeOrderLocation(orderLocation);
			OrderLocation.staticGetDao().delete(orderLocation);
		}
		workService.reinitPutWallFeedback(facility);
	}
	
	private List<OrderHeader> getOrdersInLocation(Location location){
		List<OrderLocation> orderLocations = getOrderLocationsInLocation(location);
		List<OrderHeader> orderHeaders = Lists.newArrayList();
		OrderHeader orderHeader = null;
		for (OrderLocation orderLocation : orderLocations){
			orderHeader = orderLocation.getParent();
			if (orderHeader.getActive()){
				orderHeaders.add(orderHeader);
			}
		}
		return orderHeaders;
	}
	
	private List<OrderLocation> getOrderLocationsInLocation(Location location) {
		List<Location> sublocations = location.getAllDescendants();
		List<Criterion> filterParams = new ArrayList<Criterion>();
		filterParams.add(Restrictions.in("location", sublocations));
		filterParams.add(Restrictions.eq("active", true));
		List<OrderLocation> orderLocations = OrderLocation.staticGetDao().findByFilter(filterParams);
		return orderLocations;
	}
	
	private String[] getInventoryInfo(Facility facility, String locationStr, ColorEnum color){
		String[] info = new String[4];
		Location location = facility.findSubLocationById(locationStr);
		if (location == null) {
			info[0] = "Could not find";
			info[1] = "Location " + locationStr;
			return info;
		}
		//If location is not a Slot and not a Tape, stop
		if (!location.isSlot() && !locationStr.startsWith(CheDeviceLogic.TAPE_PREFIX)) {
			String locationType = location.isAisle() ? "Aisle" : location.isBay() ? "Bay" : "Tier";
			info[0] = locationStr + " is " + locationType;
			info[1] = "Expected Slot or Tape";
			return info;
		}
		Item closestItem = getClosestItem(location, locationStr);
		String scanType = PropertyService.getInstance().getPropertyFromConfig(facility, DomainObjectProperty.SCANPICK);
		boolean showGtin = "UPC".equalsIgnoreCase(scanType);
		info[0] = location.getBestUsableLocationName();
		if (closestItem == null) {
			info[1] = "Item: none";
			lightService.lightLocationServerCall(location, color);
		} else if (showGtin) {
			info[1] = "UPC: " + closestItem.getGtinId();
			lightService.lightItem(closestItem, color);
		} else {
			info[1] = "SKU: " + closestItem.getDomainId();
			lightService.lightItem(closestItem, color);
		}
		return info;
	}
	
	private void removeItemFromInventory(Facility facility, String locationStr){
		Location location = facility.findSubLocationById(locationStr);
		if (location == null) {
			return;
		}
		if (!location.isSlot() && !locationStr.startsWith(CheDeviceLogic.TAPE_PREFIX)) {
			return;
		}
		Item item = getClosestItem(location, locationStr);
		if (item != null){
			item.getStoredLocation().removeStoredItem(item);
			item.getParent().removeItemFromMaster(item);
			Item.staticGetDao().delete(item);
		}
	}
	
	private Item getClosestItem(Location location, String locationStr) {
		Item closestItem = null;
		int scanOffset = 0, itemOffset, smallestDistance = 1000000, distance;
		TapeLocation tapeLocation = CodeshelfTape.findFinestLocationForTape(locationStr);
		scanOffset = tapeLocation.getCmOffset();
		List<Item> items = location.getStoredItemsInLocationAndChildren();
		for (Item item : items) {
			itemOffset = item.getCmFromLeft();
			distance = Math.abs(itemOffset - scanOffset);
			if (distance < smallestDistance) {
				smallestDistance = distance;
				closestItem = item;
			}
		}
		return closestItem;
	}
}