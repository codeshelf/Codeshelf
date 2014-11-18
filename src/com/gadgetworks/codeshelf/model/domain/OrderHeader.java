/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: OrderHeader.java,v 1.28 2013/09/18 00:40:09 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.domain;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.ManyToOne;
import javax.persistence.MapKey;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.gadgetworks.codeshelf.model.OrderStatusEnum;
import com.gadgetworks.codeshelf.model.OrderTypeEnum;
import com.gadgetworks.codeshelf.model.PickStrategyEnum;
import com.gadgetworks.codeshelf.model.dao.DaoException;
import com.gadgetworks.codeshelf.model.dao.GenericDaoABC;
import com.gadgetworks.codeshelf.model.dao.ITypedDao;
import com.gadgetworks.codeshelf.platform.persistence.PersistenceService;
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
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE)
@ToString(of = { "orderType", "status", "orderGroup", "active" }, callSuper = true, doNotUseGetters = true)
public class OrderHeader extends DomainObjectTreeABC<Facility> {

	@Inject
	public static ITypedDao<OrderHeader>	DAO;

	@Singleton
	public static class OrderHeaderDao extends GenericDaoABC<OrderHeader> implements ITypedDao<OrderHeader> {
		@Inject
		public OrderHeaderDao(final PersistenceService persistenceService) {
			super(persistenceService);
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
		header.setStatus(OrderStatusEnum.CREATED);
		header.setPickStrategy(PickStrategyEnum.SERIAL);
		header.setActive(Boolean.TRUE);
		header.setUpdated(new Timestamp(System.currentTimeMillis()));
		inFacility.addOrderHeader(header);
		OrderHeader.DAO.store(header);
		return header;
	}

	private static final Logger			LOGGER			= LoggerFactory.getLogger(OrderHeader.class);

	// The parent facility.
	@ManyToOne(optional = false)
	private Facility					parent;

	// The order type.
	@Column(nullable = false)
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
	@Column(nullable = false)
	@Enumerated(value = EnumType.STRING)
	@Getter
	@Setter
	@JsonProperty
	private PickStrategyEnum			pickStrategy;

	// The parent order group.
	@ManyToOne(optional = true)
	@Getter
	@Setter
	private OrderGroup					orderGroup;

	// The customerID for this order.
	// Lower numbers work first.
	@Column(nullable = true)
	@Getter
	@Setter
	@JsonProperty
	private String						customerId;

	// Reference to the shipment for this order.
	// Lower numbers work first.
	@Column(nullable = true)
	@Getter
	@Setter
	@JsonProperty
	private String						shipmentId;

	// The work sequence.
	// This is a sort of the actively working order groups in a facility.
	// Lower numbers work first.
	@Column(nullable = true)
	@Getter
	@Setter
	@JsonProperty
	private Integer						workSequence;

	// Order date.
	@Column(nullable = true)
	@Getter
	@Setter
	@JsonProperty
	private Timestamp					orderDate;

	// Due date.
	@Column(nullable = true)
	@Getter
	@Setter
	@JsonProperty
	private Timestamp					dueDate;

	// The container use for this order.
	@OneToOne(optional = true)
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

	public OrderHeader(Facility facility, String domainId) {
		super(domainId);
		parent = facility;
		parent.addOrderHeader(this);
		status = OrderStatusEnum.CREATED;
		pickStrategy = PickStrategyEnum.SERIAL;
	}

	@SuppressWarnings("unchecked")
	public final ITypedDao<OrderHeader> getDao() {
		return DAO;
	}

	public final String getDefaultDomainIdPrefix() {
		return "P";
	}

	public final Facility getParent() {
		return parent;
	}

	@Override
	public final void setParent(Facility inParent) {
		parent = inParent;
	}

	public final Facility getFacility() {
		return getParent();
	}

	public final String getOrderId() {
		return getDomainId();
	}

	public final void setOrderId(String inOrderId) {
		setDomainId(inOrderId);
	}

	public final List<? extends IDomainObject> getChildren() {
		return getOrderDetails();
	}

	public final void addOrderDetail(OrderDetail inOrderDetail) {
		OrderHeader previousOrderHeader = inOrderDetail.getParent();
		if (previousOrderHeader == null) {
			orderDetails.put(inOrderDetail.getDomainId(), inOrderDetail);
			inOrderDetail.setParent(this);
		} else {
			LOGGER.error("cannot add OrderDetail " + inOrderDetail.getDomainId() + " to " + this.getDomainId()
					+ " because it has not been removed from " + previousOrderHeader.getDomainId());
		}
	}

	public final OrderDetail getOrderDetail(String inOrderDetailId) {
		return orderDetails.get(inOrderDetailId);
	}

	public final void removeOrderDetail(String inOrderDetailId) {
		OrderDetail orderDetail = this.getOrderDetail(inOrderDetailId);
		if (orderDetail != null) {
			orderDetail.setParent(null);
			orderDetails.remove(inOrderDetailId);
		} else {
			LOGGER.error("cannot remove OrderDetail " + inOrderDetailId + " from " + this.getDomainId()
					+ " because it isn't found in children");
		}
	}

	public final List<OrderDetail> getOrderDetails() {
		return new ArrayList<OrderDetail>(orderDetails.values());
	}

	public final void addHeadersContainerUse(ContainerUse inContainerUse) {
		if (inContainerUse == null) {
			LOGGER.error("null input to OrderHeader.addHeadersContainerUse");
			return;
		}
		// Intent: set the one-to-one relationship fields unless this orderHeader already has a containerUse
		// However, as the fields in both directions persist, update anyway in the error case. Otherwise an orphan relationship could never be cleaned up 
		OrderHeader previousOrderHeader = inContainerUse.getOrderHeader();
		if (previousOrderHeader == null) {
			setContainerUse(inContainerUse);
			inContainerUse.setOrderHeader(this);
			// done
		} else {
			LOGGER.error("problem adding ContainerUse " + inContainerUse.getContainerName() + " to " + this.getDomainId()
					+ " because it has not been removed from " + previousOrderHeader.getDomainId()
					+ ". However, proceeding to make data consistent");
			
			// This is really problematic because the caller has the responsibility to call the two DAOs to store this OrderHeader and the inContainerUse. We must store anything else.
			ContainerUse possibleOrphanCntrUse = previousOrderHeader.getContainerUse();
			if (possibleOrphanCntrUse == null || possibleOrphanCntrUse.equals(inContainerUse))
				return; // no cleanup necessary, but we did not do the updates.
			
			// Or if the possible orphan is self-consistent, don't clean them, but do clean this.
			OrderHeader orphansHeader = possibleOrphanCntrUse.getOrderHeader();
			if (orphansHeader.equals(previousOrderHeader)) {
				inContainerUse.setOrderHeader(null); // Caller will call the DAO.store()
				
			} else {
				//  Finally, the tricky bit. Do what was asked, and clean the others.
				setContainerUse(inContainerUse);
				inContainerUse.setOrderHeader(this);
				
				if (!previousOrderHeader.equals(this)) {
					previousOrderHeader.setContainerUse(null);
					try {
						OrderHeader.DAO.store(previousOrderHeader);
					} catch (DaoException e) {
						LOGGER.error("", e);
					}
				}
				
				// if we did not return already, possibleOrphanCntrUse also needs cleaning
				possibleOrphanCntrUse.setOrderHeader(null);
				try {
					ContainerUse.DAO.store(possibleOrphanCntrUse);
				} catch (DaoException e) {
					LOGGER.error("", e);
				}
			}
		}
	}

	public final void removeHeadersContainerUse(ContainerUse inContainerUse) {
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

	public final OrderLocation addOrderLocation(ILocation<?> inLocation) {
		OrderLocation result = createOrderLocation(inLocation);
		addOrderLocation(result);
		OrderLocation.DAO.store(result);
		return result;
	}

	private OrderLocation createOrderLocation(ILocation<?> inLocation) {
		OrderLocation result = new OrderLocation();
		result.setDomainId(OrderLocation.makeDomainId(this, inLocation));
		result.setLocation(inLocation);
		result.setActive(true);
		result.setUpdated(new Timestamp(System.currentTimeMillis()));
		return result;
	}

	public final void addOrderLocation(OrderLocation inOrderLocation) {
		OrderHeader previousOrderHeader = inOrderLocation.getParent();
		if (previousOrderHeader == null || previousOrderHeader == this) {
			orderLocations.put(inOrderLocation.getDomainId(), inOrderLocation);
			inOrderLocation.setParent(this);
		} else {
			LOGGER.error("cannot add OrderLocation" + inOrderLocation.getDomainId() + " to " + this.getDomainId()
					+ " because it has not been removed from " + previousOrderHeader.getDomainId());
		}
	}

	public final OrderLocation getOrderLocation(String inOrderLocationId) {
		return orderLocations.get(inOrderLocationId);
	}

	public final void removeOrderLocation(String inOrderLocationId) {
		OrderLocation orderLocation = this.getOrderLocation(inOrderLocationId);
		if (orderLocation != null) {
			orderLocation.setParent(null);
			orderLocations.remove(inOrderLocationId);
		} else {
			LOGGER.error("cannot remove OrderLocation " + inOrderLocationId + " from " + this.getDomainId()
					+ " because it isn't found in children");
		}
	}

	private final List<OrderLocation> getOrderHeaderOrderLocations(boolean inIncludeInactiveLocations) {
		//TODO do efficient DB lookup
		List<OrderLocation> newActiveOrderLocations = new ArrayList<OrderLocation>();
		for (OrderLocation orderLocation : getOrderLocations()) {
			if (orderLocation.getActive()) {
				ISubLocation<?> loc = orderLocation.getLocation();
				if (inIncludeInactiveLocations || loc.isActive()) // do not need null check due to database constraint
					newActiveOrderLocations.add(orderLocation);
			}
		}
		return newActiveOrderLocations;
	}

	public final List<OrderLocation> getActiveOrderLocations() {
		// New from v8. If the location was deleted, do not return with getActiveOrderLocations even if the orderLocation flag is active. (It will archive eventually.)
		return getOrderHeaderOrderLocations(false);
	}

	public final List<OrderLocation> getActiveOrderLocationsIncludeInactiveLocations() {
		// new function from v8. Only called by the UI function
		return getOrderHeaderOrderLocations(true);
	}

	public final List<OrderLocation> getOrderLocations() {
		return new ArrayList<OrderLocation>(orderLocations.values());
	}

	// Set the status from the websocket by a string.
	public final void setStatusStr(final String inStatusString) {
		OrderStatusEnum inStatus = OrderStatusEnum.valueOf(inStatusString);
		if (inStatus != null) {
			status = inStatus;
		}
	}

	// --------------------------------------------------------------------------
	/**
	 * Return the order group persistent ID as a property.
	 * @return
	 */
	public final UUID getOrderGroupPersistentId() {
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
	public final String getContainerId() {
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
	public final String getReadableDueDate() {
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
	public final String getReadableOrderDate() {
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
	public final String getDescription() {
		return "--- Order Header ---";
		// localization issue
	}

	// --------------------------------------------------------------------------
	/**
	 * Order Head does not have a description field. This is for UI meta field that simply shows "Order Header" to help orient the user in the orders view.
	 * @return
	 */
	public final Integer getActiveDetailCount() {
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
	public final OrderLocation getFirstOrderLocationOnPath(final Path inPath) {
		OrderLocation result = null;

		for (OrderLocation orderLoc : getActiveOrderLocations()) {
			if (orderLoc.getLocation().getAssociatedPathSegment().getParent().equals(inPath)) {
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
	public final String getOrderLocationAliasIds() {
		String result = "";

		List<OrderLocation> oLocations = getActiveOrderLocationsIncludeInactiveLocations();
		int numLocations = oLocations.size();
		if (numLocations == 0)
			return result;

		OrderLocation firstLocation = oLocations.get(0);
		if (firstLocation != null) {
			ISubLocation<?> theLoc = firstLocation.getLocation();
			if (theLoc != null)
				result = ((LocationABC<?>) theLoc).getPrimaryAliasId();
		}
		if (numLocations > 1) {
			// add delimmiter and next one on
			for (int n = 1; n < numLocations; n++) {
				OrderLocation nextLocation = oLocations.get(n);
				if (nextLocation != null) {
					ISubLocation<?> theLoc = nextLocation.getLocation();
					if (theLoc != null)
						result = result + ";" + ((LocationABC<?>) theLoc).getPrimaryAliasId();
				}
			}
		}

		return result;
	}

	public static void setDao(OrderHeaderDao inOrderHeaderDao) {
		OrderHeader.DAO = inOrderHeaderDao;
	}

}
