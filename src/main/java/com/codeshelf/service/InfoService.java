package com.codeshelf.service;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.flyweight.command.ColorEnum;
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
				removeItemsFromInventory(facility, location);
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
		List<Item> items = location.getStoredItemsInLocationAndChildren();
		info[0] = location.getBestUsableLocationName() + " has " + items.size() + " items";
		int lineCounter = 1;
		StringBuilder lineBuilders[] = {null, new StringBuilder(), new StringBuilder(), new StringBuilder()};
		StringBuilder curLine = lineBuilders[1];
		String scanType = PropertyService.getInstance().getPropertyFromConfig(facility, DomainObjectProperty.SCANPICK);
		boolean showGtin = "UPC".equalsIgnoreCase(scanType);
		boolean ranOutOrSpaceOnChe = false;
		//Fill CHE display with as may inventoried items as will fit there
		for (Item item : items) {
			String itemId = showGtin ? item.getGtinId() : item.getDomainId();
			if (curLine.length() == 0 || curLine.length() + itemId.length() <= 22){
				curLine.append(itemId).append(", ");
			} else if (lineCounter < 3) {
				curLine = lineBuilders[++lineCounter];
			} else {
				ranOutOrSpaceOnChe = true;
				break;
			}
		}
		//If all items fit on the CHE display, remove comma after the last one;
		if (!ranOutOrSpaceOnChe) { 
			int lastLineLen = lineBuilders[lineCounter].length();
			//See if the last filled line has, at least ", "
			if (lastLineLen >= 2) {
				lineBuilders[lineCounter] = new StringBuilder(lineBuilders[lineCounter].substring(0, lastLineLen - 2));
			}
		}
		info[1] = lineBuilders[1].toString();
		info[2] = lineBuilders[2].toString();
		info[3] = lineBuilders[3].toString();
		lightService.lightLocationServerCall(location, color);
		return info;
	}
	
	private void removeItemsFromInventory(Facility facility, String locationStr){
		Location location = facility.findSubLocationById(locationStr);
		if (location == null) {
			return;
		}
		List<Item> items = location.getStoredItemsInLocationAndChildren();
		for (Item item : items) {
			item.getStoredLocation().removeStoredItem(item);
			item.getParent().removeItemFromMaster(item);
			Item.staticGetDao().delete(item);
		}
	}
}