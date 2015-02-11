/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: OrderHeader.java,v 1.28 2013/09/18 00:40:09 jeffw Exp $
 *******************************************************************************/
package com.codeshelf.model.domain;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.persistence.Cacheable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MapKey;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.codeshelf.model.OrderStatusEnum;
import com.codeshelf.model.OrderTypeEnum;
import com.codeshelf.model.PickStrategyEnum;
import com.codeshelf.model.dao.DaoException;
import com.codeshelf.model.dao.GenericDaoABC;
import com.codeshelf.model.dao.ITypedDao;
import com.codeshelf.platform.persistence.TenantPersistenceService;
import com.google.inject.Inject;
import com.google.inject.Singleton;

// --------------------------------------------------------------------------
/**
 * Order
 * 
 * A collection of OrderDetails that make up the work needed in the facility.
 * 
 * @author jeffw
 */

@Entity
@Table(name = "order_header")
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE)
@ToString(of = { "orderType", "status", "orderGroup", "active" }, callSuper = true, doNotUseGetters = true)
public class OrderHeader extends DomainObjectTreeABC<Facility> {

	@Inject
	public static ITypedDao<OrderHeader>	DAO;

	@Singleton
	public static class OrderHeaderDao extends GenericDaoABC<OrderHeader> implements ITypedDao<OrderHeader> {
		@Inject
		public OrderHeaderDao(final TenantPersistenceService tenantPersistenceService) {
			super(tenantPersistenceService);
		}

		public final Class<OrderHeader> getDaoClass() {
			return OrderHeader.class;
		}
	}

	public static String computeCrossOrderId(String inContainerId, Timestamp inTimestamp) {
		return inContainerId + "." + inTimestamp;
	}

	public static OrderHeader createEmptyOrderHeader(Facility inFacility, String inOrderId) {
		OrderHeader header = new OrderHeader();
		header.setDomainId(inOrderId);
		header.setOrderType(OrderTypeEnum.OUTBOUND);
		header.setStatus(OrderStatusEnum.RELEASED);
		header.setPickStrategy(PickStrategyEnum.SERIAL);
		header.setActive(Boolean.TRUE);
		header.setUpdated(new Timestamp(System.currentTimeMillis()));
		inFacility.addOrderHeader(header);
		OrderHeader.DAO.store(header);
		return header;
	}

	private static final Logger			LOGGER			= LoggerFactory.getLogger(OrderHeader.class);

	// The parent facility.
	@ManyToOne(optional = false, fetch=FetchType.LAZY)
	@Getter
	@Setter
	private Facility					parent;

	// The order type.
	@Column(nullable = false,name="order_type")
	@Enumerated(value = EnumType.STRING)
	@Getter
	@Setter
	@JsonProperty
	private OrderTypeEnum				orderType;

	// The collective order status.
	@Column(nullable = false)
	@Enumerated(value = EnumType.STRING)
	@Getter
	@Setter
	@JsonProperty
	private OrderStatusEnum				status;

	// The pick strategy.
	@Column(nullable = false,name="pick_strategy")
	@Enumerated(value = EnumType.STRING)
	@Getter
	@Setter
	@JsonProperty
	private PickStrategyEnum			pickStrategy;

	// The parent order group.
	@ManyToOne(optional = true,fetch=FetchType.LAZY)
	@JoinColumn(name="order_group_persistentid")
	@Getter
	@Setter
	private OrderGroup					orderGroup;

	// The customerID for this order.
	// Lower numbers work first.
	@Column(nullable = true,name="customer_id")
	@Getter
	@Setter
	@JsonProperty
	private String						customerId;

	// Reference to the shipment for this order.
	// Lower numbers work first.
	@Column(nullable = true,name="shipper_id")
	@Getter
	@Setter
	@JsonProperty
	private String						shipperId;

	// The work sequence.
	// This is a sort of the actively working order groups in a facility.
	// Lower numbers work first.
	@Column(nullable = true,name="work_sequence")
	@Getter
	@Setter
	@JsonProperty
	private Integer						workSequence;

	// Order date.
	@Column(nullable = true,name="order_date")
	@Getter
	@Setter
	@JsonProperty
	private Timestamp					orderDate;

	// Due date.
	@Column(nullable = true,name="due_date")
	@Getter
	@Setter
	@JsonProperty
	private Timestamp					dueDate;

	// The container use for this order.
	@OneToOne(optional = true,fetch=FetchType.LAZY)
	@JoinColumn(name="container_use_persistentid")
	@Getter
	@Setter
	private ContainerUse				containerUse;

	@Column(nullable = false)
	@Getter
	@Setter
	@JsonProperty
	private Boolean						active;

	@Column(nullable = false)
	@Getter
	@Setter
	@JsonProperty
	private Timestamp					updated;

	@OneToMany(mappedBy = "parent")
	@MapKey(name = "domainId")
	private Map<String, OrderDetail>	orderDetails	= new HashMap<String, OrderDetail>();

	@OneToMany(mappedBy = "parent")
	@MapKey(name = "domainId")
	private Map<String, OrderLocation>	orderLocations	= new HashMap<String, OrderLocation>();

	public OrderHeader() {
		status = OrderStatusEnum.CREATED;
		pickStrategy = PickStrategyEnum.SERIAL;
	}

	public OrderHeader(String domainId, OrderTypeEnum orderType) {
		super(domainId);
		this.active = true;
		this.orderType = orderType;
		status = OrderStatusEnum.CREATED;
		pickStrategy = PickStrategyEnum.SERIAL;
		updated = new Timestamp(System.currentTimeMillis());
	}
	
	@SuppressWarnings("unchecked")
	public final ITypedDao<OrderHeader> getDao() {
		return DAO;
	}

	public final String getDefaultDomainIdPrefix() {
		return "P";
	}

	public Facility getFacility() {
		return getParent();
	}

	public String getOrderId() {
		return getDomainId();
	}

	public void setOrderId(String inOrderId) {
		setDomainId(inOrderId);
	}

	public List<? extends IDomainObject> getChildren() {
		return getOrderDetails();
	}

	public void addOrderDetail(OrderDetail inOrderDetail) {
		OrderHeader previousOrderHeader = inOrderDetail.getParent();
		if (previousOrderHeader == null) {
			orderDetails.put(inOrderDetail.getDomainId(), inOrderDetail);
			inOrderDetail.setParent(this);
		} else {
			LOGGER.error("cannot add OrderDetail " + inOrderDetail.getDomainId() + " to " + this.getDomainId()
					+ " because it has not been removed from " + previousOrderHeader.getDomainId());
		}
	}

	public OrderDetail getOrderDetail(String inOrderDetailId) {
		return orderDetails.get(inOrderDetailId);
	}

	public void removeOrderDetail(String inOrderDetailId) {
		OrderDetail orderDetail = this.getOrderDetail(inOrderDetailId);
		if (orderDetail != null) {
			orderDetail.setParent(null);
			orderDetails.remove(inOrderDetailId);
		} else {
			LOGGER.error("cannot remove OrderDetail " + inOrderDetailId + " from " + this.getDomainId()
					+ " because it isn't found in children");
		}
	}

	public List<OrderDetail> getOrderDetails() {
		List<OrderDetail> listDetails = new ArrayList<OrderDetail>(this.orderDetails.values());
		return listDetails;
	}

	static boolean containerUseAlreadyConsistentWithHeader(ContainerUse inUse) {
		OrderHeader previousHeader = inUse.getOrderHeader();
		if (previousHeader == null)
			return false;
		ContainerUse previousHeadersUse = previousHeader.getContainerUse();
		if (previousHeadersUse == null)
			return false;
		return previousHeadersUse.equals(inUse);
	}
	
	private final boolean thisHeaderAlreadyConsistentWithUse() {
		ContainerUse previousUse = getContainerUse();
		if (previousUse == null)
			return false;	
		OrderHeader previousUseHeader = previousUse.getOrderHeader();
		if (previousUseHeader == null)
			return false;
		return previousUseHeader.equals(this);
	}

	
	public void addHeadersContainerUse(ContainerUse inContainerUse) {
		if (inContainerUse == null) {
			LOGGER.error("null input to OrderHeader.addHeadersContainerUse");
			return;
		}
		// Intent: set the one-to-one relationship fields unless this orderHeader already has a containerUse
		// However, as the fields in both directions persist, update anyway on inconsistent data. Otherwise an orphan relationship could never be cleaned up 
		if (containerUseAlreadyConsistentWithHeader(inContainerUse)) {
			LOGGER.error("did not add ContainerUse " + inContainerUse.getContainerName() + " to " + this.getDomainId()
				+ " because it already has a consistent relationship with an order header ");
			return;
		}
		if (thisHeaderAlreadyConsistentWithUse()) {
			LOGGER.error("did not add ContainerUse " + inContainerUse.getContainerName() + " to " + this.getDomainId()
				+ " because this OrderHeader already has a consistent relationship with an ContainerUse ");
			return;
		}
		
		// Keep it simple for now. Just do the sets. Don't try to clean up inconsistency if one of the objects has inconsistent one-way relationship.
		setContainerUse(inContainerUse);
		inContainerUse.setOrderHeader(this);
				
	}

	public void removeHeadersContainerUse(ContainerUse inContainerUse) {
		if (inContainerUse == null) {
			LOGGER.error("null input to OrderHeader.removeHeadersContainerUse");
			return;
		}
		// Intent: clear the one-to-one relationship fields, but only if the one being cleared is the expected one.
		ContainerUse previousContainerUse = getContainerUse();
		if (previousContainerUse.equals(inContainerUse)) {
			inContainerUse.setOrderHeader(null);
			setContainerUse(null);
		} else {
			LOGGER.error("cannot remove ContainerUse " + inContainerUse.getDomainId() + " from " + this.getDomainId()
					+ " because it isn't referenced by this OrderHeader");
		}
	}

	public OrderLocation addOrderLocation(Location inLocation) {
		OrderLocation result = createOrderLocation(inLocation);
		addOrderLocation(result);
		OrderLocation.DAO.store(result);
		return result;
	}

	private OrderLocation createOrderLocation(Location inLocation) {
		OrderLocation result = new OrderLocation();
		result.setDomainId(OrderLocation.makeDomainId(this, inLocation));
		result.setLocation(inLocation);
		result.setActive(true);
		result.setUpdated(new Timestamp(System.currentTimeMillis()));
		return result;
	}

	public void addOrderLocation(OrderLocation inOrderLocation) {
		OrderHeader previousOrderHeader = inOrderLocation.getParent();
		if (previousOrderHeader == null || previousOrderHeader == this) {
			orderLocations.put(inOrderLocation.getDomainId(), inOrderLocation);
			inOrderLocation.setParent(this);
		} else {
			LOGGER.error("cannot add OrderLocation" + inOrderLocation.getDomainId() + " to " + this.getDomainId()
					+ " because it has not been removed from " + previousOrderHeader.getDomainId());
		}
	}

	public OrderLocation getOrderLocation(String inOrderLocationId) {
		return orderLocations.get(inOrderLocationId);
	}

	public void removeOrderLocation(OrderLocation inOrderLocation) {
		if (inOrderLocation == null) {
			LOGGER.error("null input to removeOrderLocation");
			return;
		}
		if (orderLocations.containsValue(inOrderLocation)) {
			inOrderLocation.setParent(null);
			orderLocations.remove(inOrderLocation.getDomainId());
		} else {
			LOGGER.error("cannot remove OrderLocation " + inOrderLocation + " from " + this.getDomainId()
					+ " because it isn't found in children");
		}
	}

	private final List<OrderLocation> getOrderHeaderOrderLocations(boolean inIncludeInactiveLocations) {
		//TODO do efficient DB lookup
		List<OrderLocation> newActiveOrderLocations = new ArrayList<OrderLocation>();
		for (OrderLocation orderLocation : getOrderLocations()) {
			if (orderLocation.getActive()) {
				Location loc = orderLocation.getLocation();
				if (inIncludeInactiveLocations || loc.isActive()) // do not need null check due to database constraint
					newActiveOrderLocations.add(orderLocation);
			}
		}
		return newActiveOrderLocations;
	}

	public List<OrderLocation> getActiveOrderLocations() {
		// New from v8. If the location was deleted, do not return with getActiveOrderLocations even if the orderLocation flag is active. (It will archive eventually.)
		return getOrderHeaderOrderLocations(false);
	}

	public List<OrderLocation> getActiveOrderLocationsIncludeInactiveLocations() {
		// new function from v8. Only called by the UI function
		return getOrderHeaderOrderLocations(true);
	}

	public List<OrderLocation> getOrderLocations() {
		return new ArrayList<OrderLocation>(orderLocations.values());
	}

	// Set the status from the websocket by a string.
	public void setStatusStr(final String inStatusString) {
		OrderStatusEnum inStatus = OrderStatusEnum.valueOf(inStatusString);
		if (inStatus != null) {
			status = inStatus;
		}
	}
	
	// --------------------------------------------------------------------------
	/**
	 * Reevaluate the order status based on the status of child order details
	 */
	public void reevaluateStatus() {
		setStatus(OrderStatusEnum.COMPLETE);
		for (OrderDetail detail : getOrderDetails()) {
			if (!detail.getActive()){
				continue;
			}
			if (detail.getStatus().equals(OrderStatusEnum.SHORT)) {
				setStatus(OrderStatusEnum.SHORT);
				break;
			} else if (!detail.getStatus().equals(OrderStatusEnum.COMPLETE)) {
				setStatus(OrderStatusEnum.INPROGRESS);
				break;
			}
		}
		try {
			getDao().store(this);
		} catch (DaoException e) {
			LOGGER.error("Failed to update order status", e);
		}
	}


	// --------------------------------------------------------------------------
	/**
	 * Return the order group persistent ID as a property.
	 * @return
	 */
	public UUID getOrderGroupPersistentId() {
		UUID result = null;
		if (orderGroup != null) {
			result = orderGroup.getPersistentId();
		}
		return result;
	}

	// --------------------------------------------------------------------------
	/**
	 * @return
	 */
	public String getContainerId() {
		String result = "";

		if (containerUse != null) {
			result = containerUse.getParent().getContainerId();
		}

		return result;
	}

	// --------------------------------------------------------------------------
	/**
	 * @return
	 */
	public String getReadableDueDate() {
		if (getDueDate() != null) {
			return new java.text.SimpleDateFormat("ddMMMyy HH:mm").format(getDueDate()).toUpperCase();
		} else {
			return "";
		}
	}

	// --------------------------------------------------------------------------
	/**
	 * @return
	 */
	public String getReadableOrderDate() {
		if (getOrderDate() != null) {
			return new java.text.SimpleDateFormat("ddMMMyy HH:mm").format(getOrderDate()).toUpperCase();
		} else {
			return "";
		}
	}

	// --------------------------------------------------------------------------
	/**
	 * Order Head does not have a description field. This is for UI meta field that simply shows "Order Header" to help orient the user in the orders view.
	 * @return
	 */
	public String getDescription() {
		return "--- Order Header ---";
		// localization issue
	}

	// --------------------------------------------------------------------------
	/**
	 * Order Head does not have a description field. This is for UI meta field that simply shows "Order Header" to help orient the user in the orders view.
	 * @return
	 */
	public Integer getActiveDetailCount() {
		Integer result = 0;
		for (OrderDetail orderDetail : getOrderDetails()) {
			if (orderDetail.getActive()) {
				result++;
			}
		}
		return result;
	}

	// --------------------------------------------------------------------------
	/**
	 * Return the first order location we find along the path (in path working order).
	 * This is used in Crossbatch order processing. This may return null, especially if 
	 * Locations for some orders were deleted as getActiveOrderLocations() excludes those.
	 * @param inPath
	 * @return
	 */
	public OrderLocation getFirstOrderLocationOnPath(final Path inPath) {
		OrderLocation result = null;
		for (OrderLocation orderLoc : getActiveOrderLocations()) {
			Location location = orderLoc.getLocation();
			if (location==null) {
				continue;
			}
			PathSegment segment = location.getAssociatedPathSegment(); 
			if (segment==null) {
				continue;
			}
			Path path = segment.getParent();
			if (path==null) {
				continue;
			}
			if (path.equals(inPath)) {
				if ((result == null) || (orderLoc.getLocation().getPosAlongPath() < result.getLocation().getPosAlongPath())) {
					result = orderLoc;
				}
			}
		}
		return result;
	}

	// --------------------------------------------------------------------------
	/**
	 * Return the alias name of the first order location found. If we should later know the first or only path, could use getFirstOrderLocationOnPath
	 * Jeff wants delimitted list if multiple locations.
	 * Jeff really wants that list in the order the user thinks about, which he says may not be the path order.
	 * Note: this is called ONLY by the UI.  We want to include locations with deleted locations for UI purpose.
	 */
	public String getOrderLocationAliasIds() {
		String result = "";

		List<OrderLocation> oLocations = getActiveOrderLocationsIncludeInactiveLocations();
		int numLocations = oLocations.size();
		if (numLocations == 0)
			return result;

		OrderLocation firstLocation = oLocations.get(0);
		if (firstLocation != null) {
			Location theLoc = firstLocation.getLocation();
			if (theLoc != null)
				result = theLoc.getPrimaryAliasId();
		}
		if (numLocations > 1) {
			// add delimiter and next one on
			for (int n = 1; n < numLocations; n++) {
				OrderLocation nextLocation = oLocations.get(n);
				if (nextLocation != null) {
					Location theLoc = nextLocation.getLocation();
					if (theLoc != null)
						result = result + ";" + theLoc.getPrimaryAliasId();
				}
			}
		}

		return result;
	}

	public String getGroupUi() {
		OrderGroup theGroup = getOrderGroup();
		if (theGroup == null)
			return "";
		return theGroup.getDomainId();
	}

	public static void setDao(OrderHeaderDao inOrderHeaderDao) {
		OrderHeader.DAO = inOrderHeaderDao;
	}

}
