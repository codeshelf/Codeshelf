package com.codeshelf.service;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.model.WiFactory;
import com.codeshelf.model.WiFactory.WiPurpose;
import com.codeshelf.model.WorkInstructionStatusEnum;
import com.codeshelf.model.WorkInstructionTypeEnum;
import com.codeshelf.model.domain.Che;
import com.codeshelf.model.domain.Container;
import com.codeshelf.model.domain.ContainerKind;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.Item;
import com.codeshelf.model.domain.ItemMaster;
import com.codeshelf.model.domain.Location;
import com.codeshelf.model.domain.OrderDetail;
import com.codeshelf.model.domain.OrderHeader;
import com.codeshelf.model.domain.OrderLocation;
import com.codeshelf.model.domain.Path;
import com.codeshelf.model.domain.UomMaster;
import com.codeshelf.model.domain.WorkInstruction;
import com.google.common.collect.Lists;

public class PutWallOrderGenerator {
	private static final Logger	LOGGER	= LoggerFactory.getLogger(PutWallOrderGenerator.class);

	protected static List<WorkInstruction> attemptToGenerateWallOrders(Che che, Collection<String> inContainerIdList, Timestamp theTime) {
		LOGGER.info("attemptToGenerateWallOrders for che:{}, walls:{}", che.getDomainId(), inContainerIdList);

		List<WorkInstruction> wiList = Lists.newArrayList();
		if (inContainerIdList == null || inContainerIdList.isEmpty()) {
			return wiList;
		}
		//Get all orders in the provided walls 
		HashMap<String, List<OrderHeader>> wallOrders = getOrdersInPutWalls(inContainerIdList);
		Iterator<String> wallIds = wallOrders.keySet().iterator();
		//Iterate over each wall
		while (wallIds.hasNext()) {
			String wallId = wallIds.next();
			List<OrderHeader> ordersInThisWall = wallOrders.get(wallId);
			//Get a list of Work Instructions for the current wall
			List<WorkInstruction> wisInWall = generateWIsForWall(che, wallId, ordersInThisWall, theTime);
			//Add them to the total Work Instruction list 
			wiList.addAll(wisInWall);
		}
		return wiList;
	}

	/**
	 * Generate a Work Instructions list for the current wall
	 */
	private static List<WorkInstruction> generateWIsForWall(Che che, String wallId, List<OrderHeader> orders, Timestamp theTime) {
		LOGGER.info("generateWIsForWall for {}, orders number considered{}", wallId, orders.size());
		
		HashMap<String, WorkInstruction> wiHashThisWallAndRun = new HashMap<String, WorkInstruction>();
		Facility facility = che.getFacility();
		//Retrieve or create a new container for this wall
		Container container = getContainerForWall(facility, wallId, theTime);
		//Iterate over all details within all orders in a current wall
		for (OrderHeader order : orders) {
			List<OrderDetail> details = order.getOrderDetails();
			for (OrderDetail detail : details) {
				ItemMaster itemMaster = detail.getItemMaster();
				String wiKey = genItemUomKey(itemMaster, detail.getUomMaster());
				//Try to retrieve an existing instruction for this item/uom in this wall
				WorkInstruction wi = wiHashThisWallAndRun.get(wiKey);
				if (wi == null) {
					//If no match had been found, create a new Work Instruction at the Detail's location
					Location location = getLocation(detail);
					wi = WiFactory.createWorkInstruction(
						WorkInstructionStatusEnum.NEW, 
						WorkInstructionTypeEnum.PLAN, 
						detail, 
						container, 
						che, 
						location == null ? facility : location, 
						theTime, 
						WiPurpose.WiPurposeOutboundPick,
						false);
					if (wi != null) {
						wiHashThisWallAndRun.put(wiKey, wi);
					}
				} else {
					//If a match had been found, increment its quantity with the current detail
					wi.setPlanQuantity(add(wi.getPlanQuantity(), detail.getQuantity()));
					wi.setPlanMinQuantity(add(wi.getPlanMinQuantity(), detail.getMinQuantity()));
					wi.setPlanMaxQuantity(add(wi.getPlanMaxQuantity(), detail.getMaxQuantity()));
					WorkInstruction.staticGetDao().store(wi);

				}
			}
		}
		List<WorkInstruction> wiList = new ArrayList<>(wiHashThisWallAndRun.values());
		return wiList;
	}
	
	/**
	 * Retrieve or generate a Container to be used for a single Wall in this Slow Pick run
	 */
	private static Container getContainerForWall(Facility facility, String wallId, Timestamp theTime){
		//Container domainId names should be unique. However, if, for some reason there are multiple Conainers with the same domainId,
		//the following conde ensures that the first one gets reused rather than creating a third one
		//(calling findByDomainId() returns null when there are multiple matches)
		Container container = null;
		ContainerKind kind = facility.getContainerKind(ContainerKind.DEFAULT_CONTAINER_KIND);
		List<Criterion> filterParams = new ArrayList<Criterion>();
		filterParams.add(Restrictions.eq("parent", facility));
		filterParams.add(Restrictions.eq("domainId", wallId));
		List<Container> containers = Container.staticGetDao().findByFilter(filterParams);
		if (containers != null && !containers.isEmpty()){
			container = containers.get(0);
			container.setKind(kind);
			container.setActive(true);
		} else {
			container = new Container(wallId, kind, true);
			container.setParent(facility);			
		}
		container.setUpdated(theTime);
		Container.staticGetDao().store(container);

		LOGGER.info("getContainerForWall for {} found {} potential match", wallId, containers.size());
		
		return container;
	}
	
	/**
	 * Determine Detail's location.
	 */
	private static Location getLocation(OrderDetail detail) {
		Location location = detail.getPreferredLocObject();
		if (location != null) {
			return location;
		}
		List<Path> paths = detail.getFacility().getPaths();
		ItemMaster itemMaster = detail.getItemMaster();
		String uomStr = detail.getUomMasterId();
		for (Path path : paths) {
			Item item = itemMaster.getFirstActiveItemMatchingUomOnPath(path, uomStr);
			if (item != null) {
				return item.getStoredLocation();
			}
		}
		return null;
	}

	/**
	 * Returns a map of provided Put Walls to orders located in those walls
	 */
	private static HashMap<String, List<OrderHeader>> getOrdersInPutWalls(Collection<String> putWallNames) {
		LOGGER.info("getOrdersInPutWalls called for {}", putWallNames);

		List<OrderHeader> allOrders = OrderHeader.staticGetDao().getAll();
		HashMap<String, List<OrderHeader>> wallOrders = new HashMap<>();
		for (OrderHeader order : allOrders) {
			List<OrderLocation> orderLocations = order.getOrderLocations();
			if (!order.getActive() || orderLocations == null || orderLocations.isEmpty()) {
				continue;
			}
			Location orderLocation = orderLocations.get(0).getLocation();
			for (String wallName : putWallNames) {
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
		LOGGER.info("getOrdersInPutWalls return {} orders", wallOrders.size());

		return wallOrders;
	}

	/**
	 * Determine if a given location or any of its ancestors match a provided location id
	 */
	private static boolean doesLocationHaveAncestor(Location location, String ancestorId) {
		if (location == null) {
			return false;
		}
		Facility facility = location.getFacility();
		Location ancestor = facility.findSubLocationById(ancestorId);
		if (ancestor == null) {
			return false;
		}
		return doesLocationHaveAncestorRecursive(location, ancestor);
	}

	private static boolean doesLocationHaveAncestorRecursive(Location location, Location ancestor) {
		if (location == null) {
			return false;
		}
		if (location == ancestor) {
			return true;
		}
		return doesLocationHaveAncestorRecursive(location.getParent(), ancestor);
	}

	private static Integer add(Integer a, Integer b) {
		return (a == null ? 0 : a) + (b == null ? 0 : b);
	}
	
	private static String genItemUomKey(ItemMaster item, UomMaster uom) {
		if (item == null) {return null;}
		return item.getDomainId() + ((uom==null || uom.getDomainId().isEmpty())?"":"-"+uom.getDomainId());
	}
}