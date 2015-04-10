package com.codeshelf.service;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import com.codeshelf.edi.OutboundOrderCsvImporter;
import com.codeshelf.model.OrderStatusEnum;
import com.codeshelf.model.OrderTypeEnum;
import com.codeshelf.model.PickStrategyEnum;
import com.codeshelf.model.domain.Che;
import com.codeshelf.model.domain.Container;
import com.codeshelf.model.domain.ContainerKind;
import com.codeshelf.model.domain.ContainerUse;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.ItemMaster;
import com.codeshelf.model.domain.Location;
import com.codeshelf.model.domain.OrderDetail;
import com.codeshelf.model.domain.OrderHeader;
import com.codeshelf.model.domain.OrderLocation;
import com.codeshelf.model.domain.UomMaster;
import com.google.common.collect.Lists;

public class PutWallOrderGenerator {
	protected static void attemptToGenerateWallOrders(Che che, List<Container> containerList, List<String> inContainerIdList){
		if (containerList == null || inContainerIdList == null || !containerList.isEmpty() || inContainerIdList.isEmpty()) {
			return;
		}
		HashMap<String, List<OrderHeader>> wallOrders = getOrdersInPutWalls(inContainerIdList);
		Iterator<String> wallIds = wallOrders.keySet().iterator();
		while (wallIds.hasNext()) {
			String wallId = wallIds.next();
			List<OrderHeader> ordersInThisWall = wallOrders.get(wallId);
			OrderHeader combinedOrder = generateCombinedOrder(che, wallId, ordersInThisWall);
			containerList.add(combinedOrder.getContainerUse().getParent());
		}
	}
	
	private static OrderHeader generateCombinedOrder(Che che, String wallId, List<OrderHeader> orders){
		String orderId = "SlowPick_" + che.getDomainId() + "_" + wallId;
		Facility facility = che.getFacility();
		Timestamp now = new Timestamp(System.currentTimeMillis());

		OrderHeader combinedOrder = OrderHeader.staticGetDao().findByDomainId(facility, orderId);
		//OrderHeader combinedOrder = facility.getOrderHeader(orderId);
		if (combinedOrder == null) {
			combinedOrder = new OrderHeader();
			combinedOrder.setDomainId(orderId);
			//facility.addOrderHeader(combinedOrder);
		}
		combinedOrder.setParent(facility);
		combinedOrder.setOrderType(OrderTypeEnum.OUTBOUND);
		combinedOrder.setStatus(OrderStatusEnum.RELEASED);
		combinedOrder.setPickStrategy(PickStrategyEnum.SERIAL);
		combinedOrder.setActive(true);
		combinedOrder.setUpdated(new Timestamp(System.currentTimeMillis()));
		List<OrderDetail> details = combinedOrder.getOrderDetails();
		for (OrderDetail detail : details) {
			combinedOrder.removeOrderDetail(detail.getDomainId());
			OrderDetail.staticGetDao().delete(detail);
		}
		OrderHeader.staticGetDao().store(combinedOrder);
		
		for (OrderHeader order : orders) {
			details = order.getOrderDetails();
			for (OrderDetail detail : details){
				addDetailToCombinedOrder(combinedOrder, detail);
			}
		}
		
		ContainerUse containerUse = combinedOrder.getContainerUse();
		if (containerUse == null) {
			containerUse = new ContainerUse();
			containerUse.setDomainId(combinedOrder.getOrderId());
			containerUse.setUsedOn(now);
			containerUse.setUpdated(now);
			containerUse.setActive(true);
			combinedOrder.addHeadersContainerUse(containerUse);
			ContainerKind kind = facility.getContainerKind(ContainerKind.DEFAULT_CONTAINER_KIND);
			Container container = new Container(wallId, kind, true);
			container.setParent(facility);
			container.setUpdated(now);
			//facility.addContainer(container);
			container.addContainerUse(containerUse);
			Container.staticGetDao().store(container);
			ContainerUse.staticGetDao().store(containerUse);
		}
		OrderHeader.staticGetDao().store(combinedOrder);
		return combinedOrder;
	}
	
	private static void addDetailToCombinedOrder(OrderHeader combinedOrder, OrderDetail detail) {
		if (combinedOrder == null || detail == null){return;}
		ItemMaster itemMaster = detail.getItemMaster();
		UomMaster uomMaster = detail.getUomMaster();
		String detailDomainId = OutboundOrderCsvImporter.genItemUomKey(itemMaster, uomMaster);
		OrderDetail combinedDetail = OutboundOrderCsvImporter.findOrder(combinedOrder, detailDomainId, itemMaster, uomMaster);
		if (combinedDetail == null) {
			combinedDetail = new OrderDetail();
			combinedDetail.setOrderDetailId(detailDomainId);
			combinedDetail.setPreferredLocation(detail.getPreferredLocation());
			combinedDetail.setStatus(detail.getStatus());
			combinedDetail.setItemMaster(itemMaster);
			combinedDetail.setUomMaster(uomMaster);
		}
		combinedDetail.setActive(true);
		combinedDetail.setQuantities(add(combinedDetail.getQuantity(), detail.getQuantity()));
		combinedDetail.setMinQuantity(add(combinedDetail.getMinQuantity(), detail.getMinQuantity()));
		combinedDetail.setMaxQuantity(add(combinedDetail.getMaxQuantity(), detail.getMaxQuantity()));
		combinedDetail.setUpdated(new Timestamp(System.currentTimeMillis()));
		combinedOrder.addOrderDetail(combinedDetail);
		OrderDetail.staticGetDao().store(combinedDetail);
	}
	
	private static HashMap<String, List<OrderHeader>> getOrdersInPutWalls(List<String> putWallNames) {
		List<OrderHeader> allOrders = OrderHeader.staticGetDao().getAll();
		HashMap<String, List<OrderHeader>> wallOrders = new HashMap<>();
		for (OrderHeader order : allOrders) {
			List<OrderLocation> orderLocations = order.getOrderLocations();
			if (orderLocations == null || orderLocations.isEmpty()){
				continue;
			}
			Location orderLocation = orderLocations.get(0).getLocation();
			for (String wallName : putWallNames){
				if (doesLocationHaveAncestor(orderLocation, wallName)) {
					List<OrderHeader> ordersInThisWall = wallOrders.get(wallName);
					if (ordersInThisWall == null) {
						ordersInThisWall = Lists.newArrayList();
						wallOrders.put(wallName, ordersInThisWall);
					}
					ordersInThisWall.add(order);
				}
			}
		}
		return wallOrders;
	}
	
	/**
	 * Determine if a given location or any of its ancestors match a provided location id
	 */
	private static boolean doesLocationHaveAncestor(Location location, String ancestorId){
		if (location == null) {return false;}
		Facility facility = location.getFacility();
		Location ancestor = facility.findSubLocationById(ancestorId);
		if (ancestor == null) {return false;}
		return doesLocationHaveAncestorRecursive(location, ancestor);
	}

	private static boolean doesLocationHaveAncestorRecursive(Location location, Location ancestor){
		if (location == null) {return false;}
		if (location == ancestor) {return true;}
		return doesLocationHaveAncestorRecursive(location.getParent(), ancestor);
	}
	
	private static Integer add(Integer a, Integer b) {
		return (a == null?0:a) + (b == null?0:b);
	}
}