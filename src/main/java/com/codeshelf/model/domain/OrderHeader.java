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
import java.util.Set;
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
import javax.persistence.UniqueConstraint;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.model.OrderStatusEnum;
import com.codeshelf.model.OrderTypeEnum;
import com.codeshelf.model.PickStrategyEnum;
import com.codeshelf.model.WorkInstructionStatusEnum;
import com.codeshelf.model.dao.DaoException;
import com.codeshelf.model.dao.GenericDaoABC;
import com.codeshelf.model.dao.ITypedDao;
import com.codeshelf.persistence.TenantPersistenceService;
import com.codeshelf.util.UomNormalizer;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;

// --------------------------------------------------------------------------
/**
 * Order
 * 
 * A collection of OrderDetails that make up the work needed in the facility.
 * 
 * @author jeffw
 */

@Entity
@Table(name = "order_header", uniqueConstraints = {@UniqueConstraint(columnNames = {"parent_persistentid", "domainid"})})
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE)
@ToString(of = { "orderType", "status", "orderGroup", "active" }, callSuper = true, doNotUseGetters = true)
public class OrderHeader extends DomainObjectTreeABC<Facility> {

	public static class OrderHeaderDao extends GenericDaoABC<OrderHeader> implements ITypedDao<OrderHeader> {
		public final Class<OrderHeader> getDaoClass() {
			return OrderHeader.class;
		}
	}

	public static String computeCrossOrderId(String inContainerId, Timestamp inTimestamp) {
		return inContainerId + "." + inTimestamp;
	}

	public static OrderHeader createEmptyOrderHeader(Facility inFacility, String inOrderId) {
		OrderHeader header = new OrderHeader(inFacility, inOrderId, OrderTypeEnum.OUTBOUND);
		header.setStatus(OrderStatusEnum.RELEASED);
		OrderHeader.staticGetDao().store(header);
		return header;
	}

	/**
	 * Critical. Do not call DAO.delete(order) directly, as it may damage other object relationships.
	 * This uses hibernate magic as much as possible to cascade the delete, but deals with known issues.
	 * This requires that we be in an appropriate transaction already.
	 */
	public static void deleteOrder(OrderHeader order) {
		// The order has a containerUse, that will be deleted. We want to get that delinked from its owning container and optional assignedChe
		ContainerUse use = order.getContainerUse();
		if (use != null) {
			Container cntr = use.getParent();
			Che che = use.getCurrentChe();

			if (cntr != null)
				cntr.removeContainerUse(use);
			if (che != null)
				che.removeContainerUse(use);
		}
		// It seems likely that cascade delete of work instructions would damage the CHE wi list.
		List<WorkInstruction> wis = order.getAllWorkInstructionsForOrder();
		for (WorkInstruction wi : wis) {
			Che che = wi.getAssignedChe();
			if (che != null)
				che.removeWorkInstruction(wi);
		}

		OrderHeader.staticGetDao().delete(order);
	}

	private List<WorkInstruction> getAllWorkInstructionsForOrder() {
		
		ArrayList<WorkInstruction> allWiList = new ArrayList<WorkInstruction>();
		for (OrderDetail detail : this.getOrderDetails()) {
			for (WorkInstruction wi : detail.getWorkInstructions()) {
				allWiList.add(wi);
			}
		}
		
		return allWiList;
	}

	private static final Logger			LOGGER			= LoggerFactory.getLogger(OrderHeader.class);

	// The order type.
	@Column(nullable = false, name = "order_type")
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
	@Column(nullable = false, name = "pick_strategy")
	@Enumerated(value = EnumType.STRING)
	@Getter
	@Setter
	@JsonProperty
	private PickStrategyEnum			pickStrategy;

	// The parent order group.
	@ManyToOne(optional = true, fetch = FetchType.LAZY)
	@JoinColumn(name = "order_group_persistentid")
	@Getter
	@Setter
	private OrderGroup					orderGroup;

	// The customerID for this order.
	// Lower numbers work first.
	@Column(nullable = true, name = "customer_id")
	@Getter
	@Setter
	@JsonProperty
	private String						customerId;

	// Reference to the shipment for this order.
	// Lower numbers work first.
	@Column(nullable = true, name = "shipper_id")
	@Getter
	@Setter
	@JsonProperty
	private String						shipperId;

	// Reference to the desctination address for this order.
	@Column(nullable = true, name = "destination_id")
	@Getter
	@Setter
	@JsonProperty
	private String						destinationId;

	// Order date.
	@Column(nullable = true, name = "order_date")
	@Getter
	@Setter
	@JsonProperty
	private Timestamp					orderDate;

	// Due date.
	@Column(nullable = true, name = "due_date")
	@Getter
	@Setter
	@JsonProperty
	private Timestamp					dueDate;

	// The container use for this order.
	@OneToOne(optional = true, fetch = FetchType.LAZY, orphanRemoval = true)
	@JoinColumn(name = "container_use_persistentid")
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

	@OneToMany(mappedBy = "parent", orphanRemoval = true)
	@MapKey(name = "domainId")
	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	private Map<String, OrderDetail>	orderDetails	= new HashMap<String, OrderDetail>();

	@OneToMany(mappedBy = "parent", orphanRemoval = true)
	@MapKey(name = "domainId")
	private Map<String, OrderLocation>	orderLocations	= new HashMap<String, OrderLocation>();

	public OrderHeader() {
		status = OrderStatusEnum.CREATED;
		pickStrategy = PickStrategyEnum.SERIAL;
	}

	//special case taking the parent usually you do a parent.addXYZ();
	public OrderHeader(Facility parent, String domainId, OrderTypeEnum orderType) {
		super(domainId);
		this.setParent(parent);
		this.active = true;
		this.orderType = orderType;
		status = OrderStatusEnum.CREATED;
		pickStrategy = PickStrategyEnum.SERIAL;
		dueDate = new Timestamp(DateTime.now().withHourOfDay(23).withMinuteOfHour(59).withSecondOfMinute(59).getMillis());
		updated = new Timestamp(System.currentTimeMillis());
	}

	@SuppressWarnings("unchecked")
	public final ITypedDao<OrderHeader> getDao() {
		return staticGetDao();
	}

	public static ITypedDao<OrderHeader> staticGetDao() {
		return TenantPersistenceService.getInstance().getDao(OrderHeader.class);
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

	public void removeOrderDetail(OrderDetail orderDetail) {
		if (orderDetail != null) {
			orderDetail.setParent(null);
			orderDetails.remove(orderDetail.getDomainId());
		}
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
		OrderLocation.staticGetDao().store(result);
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
		return OrderLocation.staticGetDao().findByDomainId(this, inOrderLocationId);
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
		int total = 0, complete = 0, substitution = 0, released = 0, created = 0, shorts = 0;
		for (OrderDetail detail : getOrderDetails()) {
			if (!detail.getActive()) {
				continue;
			}
			total++;
			OrderStatusEnum detailStatus = detail.getStatus();
			switch (detailStatus) {
				case COMPLETE:
					complete++;
					break;
				case SUBSTITUTION:
					substitution++;
					break;
				case RELEASED:
					released++;
					break;
				case CREATED:
					created++;
					break;
				case SHORT:
					shorts++;
					break;
				default:
			}
		}
		if (released > 0) {
			setStatus(OrderStatusEnum.RELEASED);
		} else if (created == total){
			setStatus(OrderStatusEnum.CREATED);
		} else if (shorts > 0) {
			setStatus(OrderStatusEnum.SHORT);
		} else if (complete == total){
			setStatus(OrderStatusEnum.COMPLETE);
		} else if (complete + substitution == total) {
			setStatus(OrderStatusEnum.SUBSTITUTION);
		} else {
			setStatus(OrderStatusEnum.INPROGRESS);
		}
		try {
			getDao().store(this);
		} catch (DaoException e) {
			LOGGER.error("Failed to update order status", e);
		}
	}

	public void reevaluateOrderAndDetails() {
		for (OrderDetail detail : getOrderDetails()) {
			detail.reevaluateStatus();
		}
		reevaluateStatus();
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
	@JsonProperty
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

	public String getPivotDetailCount() {
		Integer result = 0;
		for (OrderDetail orderDetail : getOrderDetails()) {
			if (orderDetail.getActive()) {
				result++;
			}
		}

		if (result > 2) {
			return ">2";
		} else {
			return String.valueOf(result);
		}
	}

	public String getPivotRemainingDetailCount() {
		Integer result = 0;
		for (OrderDetail orderDetail : getOrderDetails()) {
			if (orderDetail.getActive() && (!orderDetail.getStatus().equals(OrderStatusEnum.COMPLETE))) {
				result++;
			}
		}

		if (result > 2) {
			return ">2";
		} else {
			return String.valueOf(result);
		}
	}

	public Integer getCaseQuantity() {
		return getQuantitiesByUOM().get(UomNormalizer.CASE);
	}

	public Integer getEachQuantity() {
		return getQuantitiesByUOM().get(UomNormalizer.EACH);

	}

	public Integer getOtherQuantity() {
		Set<String> keys = getQuantitiesByUOM().keySet();
		keys.remove(UomNormalizer.CASE);
		keys.remove(UomNormalizer.EACH);
		int total = 0;
		for (String other : keys) {
			total += getQuantitiesByUOM().get(other);
		}
		return total;
	}

	public Map<String, Integer> getQuantitiesByUOM() {
		Map<String, Integer> quantities = new HashMap<>();
		for (OrderDetail orderDetail : getOrderDetails()) {
			if (orderDetail.getActive()) {
				String normalizedUom = UomNormalizer.normalizeString(orderDetail.getUomMasterId());
				Integer quantity = orderDetail.getQuantity();
				Integer lastQuantity = quantities.get(normalizedUom);
				Integer newQuantity = 0;
				if (lastQuantity == null) {
					newQuantity = quantity;
				} else {
					newQuantity = lastQuantity + quantity;
				}
				quantities.put(normalizedUom, newQuantity);
			}
		}
		return quantities;
	}

	// --------------------------------------------------------------------------
	/**
	 * Return the first order location we find along the path (in path working order).
	 * This is used in Crossbatch order processing. This may return null, especially if 
	 * Locations for some orders were deleted as getActiveOrderLocations() excludes those.
	 * @param inPath
	 * @return
	 * A new case from DEV-713 put wall. We do not care about the path, and usually have only one order location.
	 * So, pass in null for path to get the one and only.
	 */
	public OrderLocation getFirstOrderLocationOnPath(final Path inPath) {
		OrderLocation result = null;
		List<OrderLocation> olList = getActiveOrderLocations();

		if (inPath == null) {
			int olCount = olList.size();
			if (olCount == 1) {
				result = olList.get(0);
			} else {
				LOGGER.error("looks like misunderstanding of getFirstOrderLocationOnPath(null)");
			}
		} else {
			for (OrderLocation orderLoc : olList) {
				Location location = orderLoc.getLocation();
				if (location == null) {
					continue;
				}
				PathSegment segment = location.getAssociatedPathSegment();
				if (segment == null) {
					continue;
				}
				Path path = segment.getParent();
				if (path == null) {
					continue;
				}
				if (path.equals(inPath)) {
					if ((result == null) || (orderLoc.getLocation().getPosAlongPath() < result.getLocation().getPosAlongPath())) {
						result = orderLoc;
					}
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

	/**
	 * For put wall case, assume only one order location.
	 * Ideally, this will show both the wall name, and the specific wall slot.
	 */
	public String getWallUi() {
		List<OrderLocation> orderLocations = this.getActiveOrderLocations();
		int count = orderLocations.size();
		if (count == 0)
			return "";
		Location loc = orderLocations.get(0).getLocation();
		// should never be null, but safe...
		if (loc != null) {
			if (loc.isImmediateWallLocation())
				return loc.getWallUi(); // could indicate something if more than one, but that would be an odd case. ORDER_WALL deletes old and makes a new one.
			else {
				return loc.getWallUi() + " - " + loc.getBestUsableLocationName();
			}
		}
		return "";
	}

	public String getGroupUi() {
		OrderGroup theGroup = getOrderGroup();
		if (theGroup == null)
			return "";
		return theGroup.getDomainId();
	}

	/**
	 * Archives orderHeaders based on inProcessTime.
	 * 
	 * Note: This will archive all orderHeaders that have no orderDetails.
	 * 
	 * @param inProcessTime	Time to compare orderHeaders against.
	 * @return Returns number of orderHeaders archived
	 */
	public static int archiveOrderHeaders(Timestamp inProcessTime) {
		Session session = TenantPersistenceService.getInstance().getSession();

		String queryString = "UPDATE OrderHeader oh set oh.active = false WHERE oh.active = true AND oh NOT IN"
				+ "(SELECT od.parent.persistentId FROM OrderDetail od WHERE od.active = true GROUP BY od.parent.persistentId HAVING count(od.active) > 0)";

		Query q = session.createQuery(queryString);
		int numArchived = 0;
		numArchived = q.executeUpdate();
		LOGGER.info("Archived: {} OrderHeaders ", numArchived);
		return numArchived;
	}

	/**
	 * This method deleted the order and all dependencies from the DB.
	 * It is to be used for testing of Automated Pick Scripts, so that the same orders can be picked repeatedly
	 */
	public void delete() {
		// LOGGER.info("Deleting order {}", this); // too much! for orders purge or script runner purge
		// OrderHeader.staticGetDao().delete(this); dangerous. Damages Container as ContainerUse is deleted
		OrderHeader.deleteOrder(this); // safe
	}

	/**
	 * Is this order fully complete as far as this cart is concerned. Called just after a wi complete, so somewhat answers the 
	 * question of whether the last wi resulted in the order going OC. But choosing to do this server side rather than message
	 * from site controller
	 */
	public boolean didOrderCompleteOnCart(Che wiChe) {
		List<WorkInstruction> wiList = getWorkInstructionsThisOrderForCart(wiChe);
		for (WorkInstruction wi : wiList) {
			WorkInstructionStatusEnum wiStatus = wi.getStatus();
			if (wiStatus == WorkInstructionStatusEnum.INPROGRESS || wiStatus == WorkInstructionStatusEnum.NEW) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Server-side does not have a strong concept of on this cart now.
	 * This returns a list of work instruction that are on or were ever done by this cart. Therefore, leftover
	 * uncompleted WI from previous cart run will be in this list and may make didOrderCompleteOnCart return the wrong answer.
	 * Probably fix here, restricting this list to wis on this cart now, if we can know it.
	 */
	private List<WorkInstruction> getWorkInstructionsThisOrderForCart(Che wiChe) {
		// What is best, query, or java relations? Or this might be bad for performance, in which case site controller message might be better.
		List<WorkInstruction> wisThisChe = new ArrayList<WorkInstruction>();
		if (wiChe != null) {
			List<OrderDetail> details = this.getOrderDetails();
			for (OrderDetail detail : details) {
				List<WorkInstruction> wis = detail.getWorkInstructions();
				for (WorkInstruction wi : wis) {
					if (wiChe.equals(wi.getAssignedChe()))
						wisThisChe.add(wi);
				}
			}
		}
		return wisThisChe;
	}
}
