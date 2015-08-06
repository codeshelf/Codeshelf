package com.codeshelf.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

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

import lombok.Getter;
import lombok.Setter;

public class InfoService implements IApiService{
	private LightService lightService;
	private WorkService workService;
	//The following 3 variables enable item cycling when scannin the same location
	private String lastScannedInventoryLocation = null;
	private short numRepeatedInventoryScans = 0; 
	private int numItemsInClosestLocation = 0;

	private static final Logger	LOGGER			= LoggerFactory.getLogger(InfoService.class);
	
	@Inject
	public InfoService(LightService inLightService, WorkService inWorkService) {
		this.lightService = inLightService;
		this.workService = inWorkService;
	}

	public InfoPackage getInfo(Facility facility, InfoRequest request, ColorEnum color){
		InfoRequestType type = request.getType();
		String location = request.getLocation();
		InfoPackage info = null;
		switch(type){
			case GET_WALL_LOCATION_INFO:
				info = getWallLocationInfo(facility, location, color);
				break;
				
			case GET_INVENTORY_INFO:
				info = getInventoryInfo(facility, location, color);
				break;

			case LIGHT_COMPLETE_ORDERS:
				lightOrdersInWall(facility, location, color, true);
				break;
				
			case LIGHT_INCOMPLETE_ORDERS:
				lightOrdersInWall(facility, location, color, false);
				break;
				
			case REMOVE_WALL_ORDERS:
				removeOrdersFromLocation(facility, location);
				break;
				
			case REMOVE_INVENTORY:
				removeItemFromInventory(request.getRemoveItemId());
				break;
				
			default:
				info = new InfoPackage();
				info.setDisplayInfoLine(0, "Unexpected Request");
				info.setDisplayInfoLine(1, "Type = " + (type == null ? "null" : type.toString()));
		}
		return info;
	}
	
	private InfoPackage getWallLocationInfo(Facility facility, String locationStr, ColorEnum color){
		InfoPackage infoPackage = new InfoPackage();
		String info[] = new String[4];
		String remove[] = new String[4];
		Location location = facility.findSubLocationById(locationStr);
		if (location == null) {
			info[0] = "Could not find";
			info[1] = "Location " + locationStr;
			infoPackage.setDisplayInfo(info);
			return infoPackage;
		}
		lightOrdersInWall(facility, locationStr, color, null);
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
			info[1] = "Orders: none";
		} else if (numOrders == 1){
			infoPackage.setSomethingToRemove(true);
			OrderHeader order = ordersInLocation.get(0);
			info[0] = locationName;
			info[1] = "Order: " + order.getDomainId();
			int completeDetails = order.getOrderDetails().size() - incompleteDetails;
			info[2] = "Complete: " + completeDetails + (completeDetails == 1 ? " job" : " jobs");
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
			remove[0] = "Remove order " + order.getDomainId();
			remove[1] = "From " + location.getBestUsableLocationName();
			remove[2] = "YES: remove order";
			remove[3] = "CANCEL to exit";
		} else {
			infoPackage.setSomethingToRemove(true);
			info[0] = locationName + " has " +  numOrders + " orders";
			info[1] = incompleteOrders + " incompl, with " + incompleteDetails + " jobs";
			info[2] = "YES: light completes";
			info[3] = "NO: light incompletes";
			remove[0] = "Remove " + numOrders + " orders";;
			remove[1] = "From " + location.getBestUsableLocationName();
			remove[2] = "YES: remove orders";
			remove[3] = "CANCEL to exit";
		}
		infoPackage.setDisplayInfo(info);
		infoPackage.setDisplayRemove(remove);
		return infoPackage;
	}
	
	/**
	 * Lights orders in wall
	 * @complete: null = all orders, true = completed orders, false = incomplete orders
	 */
	private void lightOrdersInWall(Facility facility, String locationStr, ColorEnum color, Boolean complete) {
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
			if (complete == null || orderComplete == complete){
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
	
	private InfoPackage getInventoryInfo(Facility facility, String locationStr, ColorEnum color){
		InfoPackage infoPackage = new InfoPackage(); 
		String[] info = new String[4];
		String[] remove = new String[4];
		Location location = facility.findSubLocationById(locationStr);
		if (location == null) {
			info[0] = "Could not find";
			info[1] = "Location " + locationStr;
			infoPackage.setDisplayInfo(info);
			return infoPackage;
		}
		//If location is not a Slot and not a Tape, stop
		if (!location.isSlot() && !locationStr.startsWith(CheDeviceLogic.TAPE_PREFIX)) {
			String locationType = location.isAisle() ? "AISLE" : location.isBay() ? "BAY" : "TIER";
			info[0] = locationStr + " IS " + locationType;
			info[1] = "Expected Slot or Tape";
			info[2] = "Scan another location";
			info[3] = CheDeviceLogic.CANCEL_TO_EXIT_MSG;
			infoPackage.setDisplayInfo(info);
			return infoPackage;
		}
		Item closestItem = getClosestItem(location, locationStr);
		String scanType = PropertyService.getInstance().getPropertyFromConfig(facility, DomainObjectProperty.SCANPICK);
		String itemId = null;
		boolean showGtin = "UPC".equalsIgnoreCase(scanType);
		info[0] = location.getBestUsableLocationName();
		if (closestItem == null) {
			info[1] = "Item: none";
			lightService.lightLocationServerCall(location, color);
		} else {
			if (numItemsInClosestLocation == 1) {
				info[0] += " (1 item)";
			} else {
				String itemCount = String.format(" (%d of %d items)", (numRepeatedInventoryScans % numItemsInClosestLocation) + 1, numItemsInClosestLocation);
				info[0] += itemCount;
			}
			if (showGtin) {
				itemId = closestItem.getGtinId();
				info[1] = "UPC: " + itemId;
			} else {
				itemId = closestItem.getDomainId();
				info[1] = "SKU: " + itemId;
			}
			infoPackage.setSomethingToRemove(true);
			infoPackage.setRemoveItemId(closestItem.getPersistentId());
			info[2] = closestItem.getItemDescription();
			lightService.lightItem(closestItem, color);
		}
		info[3] = CheDeviceLogic.CANCEL_TO_EXIT_MSG;
		remove[0] = "Remove " + itemId;
		remove[1] = "From " + location.getBestUsableLocationName();
		remove[2] = "YES: remove item";
		remove[3] = "CANCEL to exit";
		infoPackage.setDisplayInfo(info);
		infoPackage.setDisplayRemove(remove);
		return infoPackage;
	}
	
	private void removeItemFromInventory(UUID itemId){
		if (itemId == null) {
			LOGGER.warn("Passed null PersistentId while trying to delete item from inventory");
			return;
		}
		Item item = Item.staticGetDao().findByPersistentId(itemId);
		if (item != null){
			item.getStoredLocation().removeStoredItem(item);
			item.getParent().removeItemFromMaster(item);
			Item.staticGetDao().delete(item);
		}
	}
	
	private Item getClosestItem(Location location, String locationStr) {
		//Count how many times the same location was scanned repeatedly
		if (locationStr.equalsIgnoreCase(lastScannedInventoryLocation)) {
			numRepeatedInventoryScans++;
		} else {
			lastScannedInventoryLocation = locationStr;
			numRepeatedInventoryScans = 0;
		}
		//Get a list of items the same distance away from the scan
		int scanOffsetCm = 0, itemOffsetCm, smallestDistanceCm = 100, distanceCm;
		TapeLocation tapeLocation = CodeshelfTape.findFinestLocationForTape(locationStr);
		scanOffsetCm = tapeLocation.getCmOffset();
		List<Item> items = location.getStoredItemsInLocationAndChildren();
		ArrayList<Item> itemsInTheClosestLocation = Lists.newArrayList();
		for (Item item : items) {
			itemOffsetCm = item.getCmFromLeft();
			distanceCm = Math.abs(itemOffsetCm - scanOffsetCm);
			if (distanceCm > 50) {
				//If the ite	m is further from scan that the above threshold, ignore it
			} else if (distanceCm == smallestDistanceCm) {
				//If the item is at the same distance from scan as the previously closest item, save it too
				itemsInTheClosestLocation.add(item);
			} else if (distanceCm < smallestDistanceCm) {
				//If the item is closer to scan than the previously closest item, save it
				smallestDistanceCm = distanceCm;
				itemsInTheClosestLocation.clear();
				itemsInTheClosestLocation.add(item);
			}
		}
		numItemsInClosestLocation = itemsInTheClosestLocation.size();
		if (itemsInTheClosestLocation.isEmpty()) {
			return null;
		}
		//Sort items by domain id to ensure the same order every time
		Collections.sort(itemsInTheClosestLocation, new Comparator<Item>(){
		     public int compare(Item item1, Item item2){
		    	 return item1.getDomainId().compareTo(item2.getDomainId());
		     }
		});
		//Cycle through the items on repeated scans
		return itemsInTheClosestLocation.get(numRepeatedInventoryScans % itemsInTheClosestLocation.size());
	}
	
	public static class InfoPackage{
		@Getter
		private String[] displayInfo = {"","","",""};
		@Getter
		private String[] displayRemove = {"","","",""};
		@Getter @Setter
		private UUID removeItemId;
		@Getter @Setter
		private boolean somethingToRemove = false;
		
		public String getDisplayInfoLine(int lineNum){
			return displayInfo[lineNum];
		}
		
		public String getDisplayRemoveLine(int lineNum){
			return displayRemove[lineNum];
		}
		
		public void setDisplayInfoLine(int lineNum, String line){
			setDisplayLine(true, lineNum, line);
		}
		
		public void setDisplayRemoveLine(int lineNum, String line){
			setDisplayLine(false, lineNum, line);
		}
		
		public void setDisplayInfo(String displayInfo[]) {
			setDisplayInfoLine(0, displayInfo[0]);
			setDisplayInfoLine(1, displayInfo[1]);
			setDisplayInfoLine(2, displayInfo[2]);
			setDisplayInfoLine(3, displayInfo[3]);
		}
		
		public void setDisplayRemove(String displayRemove[]) {
			setDisplayRemoveLine(0, displayRemove[0]);
			setDisplayRemoveLine(1, displayRemove[1]);
			setDisplayRemoveLine(2, displayRemove[2]);
			setDisplayRemoveLine(3, displayRemove[3]);
		}

		private void setDisplayLine(boolean info, int lineNum, String line) {
			if (lineNum >= 0 && lineNum <= 3) {
				(info ? displayInfo : displayRemove)[lineNum] = line == null ? "" : line;
			}
		}
	}
}