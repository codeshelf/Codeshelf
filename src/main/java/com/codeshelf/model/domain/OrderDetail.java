/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: OrderDetail.java,v 1.25 2013/09/18 00:40:08 jeffw Exp $
 *******************************************************************************/
package com.codeshelf.model.domain;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.persistence.Cacheable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
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

import com.codeshelf.model.OrderStatusEnum;
import com.codeshelf.model.OrderTypeEnum;
import com.codeshelf.model.WorkInstructionStatusEnum;
import com.codeshelf.model.dao.GenericDaoABC;
import com.codeshelf.model.dao.ITypedDao;
import com.codeshelf.platform.persistence.TenantPersistenceService;
import com.codeshelf.service.WorkService;
import com.codeshelf.util.ASCIIAlphanumericComparator;
import com.codeshelf.util.UomNormalizer;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

// --------------------------------------------------------------------------
/**
 * OrderDetail
 *
 * An order detail is a request for items/SKUs from the facility.
 *
 * @author jeffw
 */

@Entity
@Table(name = "order_detail")
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE)
@ToString(of = { "status", "quantity", "itemMaster", "uomMaster", "active" }, callSuper = true, doNotUseGetters = true)
public class OrderDetail extends DomainObjectTreeABC<OrderHeader> {

	public static class OrderDetailDao extends GenericDaoABC<OrderDetail> implements ITypedDao<OrderDetail> {
		public final Class<OrderDetail> getDaoClass() {
			return OrderDetail.class;
		}
	}

	private static final Logger				LOGGER						= LoggerFactory.getLogger(OrderDetail.class);

	private static final Comparator<String>	asciiAlphanumericComparator	= new ASCIIAlphanumericComparator();

	// The owning order header.
	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@Getter
	@Setter
	private OrderHeader						parent;

	// The collective order status.
	@Column(nullable = false)
	@Enumerated(value = EnumType.STRING)
	@Getter
	@Setter
	@JsonProperty
	private OrderStatusEnum					status;

	// The item master.
	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@JoinColumn(name = "item_master_persistentid")
	@Getter
	@Setter
	private ItemMaster						itemMaster;

	// The description.
	@Column(nullable = true)
	@Getter
	@Setter
	@JsonProperty
	private String							description;

	// The actual quantity requested.
	@Column(nullable = false)
	@Getter
	//@Setter use setQuantities() to set all quantities at once
	@JsonProperty
	private Integer							quantity;

	// The min quantity that we can use.  (Same as quantity in most cases.)
	@Column(nullable = false, name = "min_quantity")
	@Getter
	@Setter
	@JsonProperty
	private Integer							minQuantity;

	// The max quantity that we can use. (Same as quantity in most cases.)
	@Column(nullable = false, name = "max_quantity")
	@Getter
	@Setter
	@JsonProperty
	private Integer							maxQuantity;

	// The UoM.
	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "uom_master_persistentid")
	@Getter
	@Setter
	private UomMaster						uomMaster;

	@Column(nullable = false)
	@Getter
	@Setter
	@JsonProperty
	private Boolean							active;

	@Column(nullable = false)
	@Getter
	@Setter
	@JsonProperty
	private Timestamp						updated;

	// preferred pick location
	@Column(nullable = true, name = "preferred_location")
	@Getter
	@Setter
	@JsonProperty
	private String							preferredLocation;

	@Getter
	@OneToMany(mappedBy = "orderDetail")
	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	private List<WorkInstruction>			workInstructions			= new ArrayList<WorkInstruction>();

	@Column(nullable = true, name = "work_sequence")
	@Getter
	@Setter
	@JsonProperty
	private Integer							workSequence;

	public OrderDetail() {
		this(null, true);
	}

	public OrderDetail(String inDomainId, boolean active) {
		super(inDomainId);
		this.status = OrderStatusEnum.CREATED;
		this.active = active;
	}

	@SuppressWarnings("unchecked")
	public final ITypedDao<OrderDetail> getDao() {
		return staticGetDao();
	}

	public static ITypedDao<OrderDetail> staticGetDao() {
		return TenantPersistenceService.getInstance().getDao(OrderDetail.class);
	}

	public final String getDefaultDomainIdPrefix() {
		return "PS";
	}

	public String getOrderDetailId() {
		return getDomainId();
	}

	public void setOrderDetailId(String inOrderDetailId) {
		setDomainId(inOrderDetailId);
	}

	public Facility getFacility() {
		OrderHeader parent = getParent();
		Facility facility = parent.getFacility();
		return facility;
	}

	public void addWorkInstruction(WorkInstruction inWorkInstruction) {
		OrderDetail previousOrderDetail = inWorkInstruction.getOrderDetail();
		if (previousOrderDetail == null) {
			workInstructions.add(inWorkInstruction);
			inWorkInstruction.setOrderDetail(this);
		} else if (!previousOrderDetail.equals(this)) {
			LOGGER.error("cannot add WorkInstruction " + inWorkInstruction.getDomainId() + " to " + this.getDomainId()
					+ " because it has not been removed from " + previousOrderDetail.getDomainId());
		}
	}

	public void removeWorkInstruction(WorkInstruction inWorkInstruction) {
		if (this.workInstructions.contains(inWorkInstruction)) {
			inWorkInstruction.setParent(null);
			workInstructions.remove(inWorkInstruction);
		} else {
			LOGGER.error("cannot remove WorkInstruction " + inWorkInstruction.getDomainId() + " from " + this.getDomainId()
					+ " because it isn't found in children");
		}
	}

	public String getParentOrderID() {
		return parent.getDomainId();
	}

	public String getUomMasterId() {
		UomMaster uomMaster = getUomMaster();
		if (uomMaster != null) {
			return uomMaster.getDomainId();
		} else {
			LOGGER.error("Unexpected null uomMaster for detail: " + this);
			return "";
		}
	}

	public String getItemMasterId() {
		ItemMaster itemMaster = getItemMaster();
		if (itemMaster != null) {
			return itemMaster.getDomainId();
		} else {
			LOGGER.error("Unexpected null itemMaster for detail: " + this);
			return "";
		}
	}

	public String getOrderId() {
		return parent.getOrderId();
	}
	
	public String getShipperId() {
		return parent.getShipperId();
	}

	/**
	 * Convenience function to set all quantities at once
	 */
	public void setQuantities(Integer quantity) {
		this.quantity = quantity;
		setMinQuantity(quantity);
		setMaxQuantity(quantity);
	}

	// --------------------------------------------------------------------------
	/**
	 * Meta fields. These appropriate for pick (outbound) order details and/or cross--batch order details
	 * @return
	 */
	public String getWiLocation() {
		String returnStr = "";
		for (WorkInstruction wi : getWorkInstructions()) {
			if (returnStr.isEmpty())
				if (wi.getStatus() == WorkInstructionStatusEnum.SHORT)
					returnStr = WorkInstructionStatusEnum.SHORT.getName();
				else {
					returnStr = wi.getPickInstruction();
					if (wi.getStatus() == WorkInstructionStatusEnum.COMPLETE)
						returnStr = returnStr + " (" + WorkInstructionStatusEnum.COMPLETE.getName() + ")";
				}
			else if (wi.getStatus() != WorkInstructionStatusEnum.SHORT) { // don't pile on extra SHORT if multiple SHORT WIs
				returnStr = returnStr + ", " + wi.getPickInstruction();
			}
		}
		return returnStr;
	}

	public String getWiChe() {
		String returnStr = "";
		for (WorkInstruction wi : getWorkInstructions()) {
			if (returnStr.isEmpty())
				returnStr = wi.getAssignedCheName();
			else if (wi.getStatus() != WorkInstructionStatusEnum.SHORT) { // don't pile on extra CHE if multiple SHORT WIs
				returnStr = returnStr + ", " + wi.getAssignedCheName();
			}
		}
		return returnStr;
	}

	public String getItemLocations() {
		//If cross batch return empty
		if (getParent().getOrderType().equals(OrderTypeEnum.CROSS)) {
			return "";
		} else {
			//if work instructions are assigned use the location from that
			List<String> wiLocationDisplay = getPickableWorkInstructions();
			if (!wiLocationDisplay.isEmpty()) {
				return Joiner.on(",").join(wiLocationDisplay);
			} else {
				List<String> itemLocationIds = new ArrayList<String>();
				List<Item> items = getItemMaster().getItems();
				//filter by uom and join the aliases together
				for (Item item : items) {
					String itemUom = item.getUomMasterId();
					String thisUom = this.getUomMasterId();
					if (UomNormalizer.normalizedEquals(itemUom, thisUom)) {
						String itemLocationId = item.getStoredLocation().getPrimaryAliasId();
						itemLocationIds.add(itemLocationId);
					}
				}
				Collections.sort(itemLocationIds, asciiAlphanumericComparator);
				return Joiner.on(",").join(itemLocationIds);
			}
		}
	}

	private List<String> getPickableWorkInstructions() {
		ImmutableSet<WorkInstructionStatusEnum> pickableWiSet = Sets.immutableEnumSet(WorkInstructionStatusEnum.NEW,
			WorkInstructionStatusEnum.INPROGRESS,
			WorkInstructionStatusEnum.COMPLETE);
		List<String> pickableWiLocations = new ArrayList<String>();
		for (WorkInstruction wi : getWorkInstructions()) {
			if (pickableWiSet.contains(wi.getStatus())) {
				pickableWiLocations.add(wi.getPickInstruction());
			}
		}
		return pickableWiLocations;
	}

	// --------------------------------------------------------------------------
	/**
	 * Convenience method
	 */
	public OrderTypeEnum getParentOrderType() {
		OrderHeader myParent = this.getParent(); // Guaranteed to have parent by database constraint.
		return myParent.getOrderType();
	}

	// --------------------------------------------------------------------------
	/**
	 * Convenience method, but very tricky.  May return null. This assumes maximum one cross detail per outbound detail. Seems true for now.
	 * The other direction needs to return a list.
	 *
	 */
	public OrderDetail outboundDetailToMatchingCrossDetail() {
		if (getParentOrderType() != OrderTypeEnum.OUTBOUND) {
			LOGGER.error("incorrect use of getCorrespondingCrossDetail");
			return null;
		}
		// see Facility.toAllMatchingOutboundOrderDetails
		OrderGroup theGroup = getParent().getOrderGroup();
		if (theGroup == null)
			return null;
		// then it might be cross batch. If no group, definitely not.
		// Look for a cross batch order that has the same order group.
		List<OrderHeader> theGroupHeaders = theGroup.getOrderHeaders();
		for (OrderHeader outOrder : theGroupHeaders) {
			boolean match = true;
			match &= outOrder.getOrderType().equals(OrderTypeEnum.CROSS);
			match &= outOrder.getActive();
			if (match) {
				for (OrderDetail crossOrderDetail : outOrder.getOrderDetails()) {
					if (crossOrderDetail.getActive()) {
						boolean matchDetail = true;
						matchDetail &= crossOrderDetail.getItemMaster().equals(this.getItemMaster());
						matchDetail &= UomNormalizer.normalizedEquals(crossOrderDetail.getUomMasterId(), this.getUomMasterId());
						if (matchDetail) {
							return crossOrderDetail;
						}
					}
				}
			}
		}
		return null;
	}

	// --------------------------------------------------------------------------
	/**
	 * Currently only called for outbound order detail. Only outbound details produce work instructions currently, even though some are part of crossbatch case.
	 */
	public boolean willProduceWi(WorkService workService) {
		return workService.willOrderDetailGetWi(this);
	}

	// --------------------------------------------------------------------------
	/**
	 * If the order header is crossbatch, leave blank. If outbound, then Y or -. Other types not implemented. Return ??
	 * Advanced: If already completed work instruction: C. If short and not complete yet: s
	 */
	public String getWillProduceWiUi(WorkService workService) {
		OrderTypeEnum myParentType = getParentOrderType();
		if (myParentType == OrderTypeEnum.CROSS)
			return "";
		else if (myParentType != OrderTypeEnum.OUTBOUND)
			return "??";

		// do I have work instructions yet? For the moment, if any complete, return C.
		List<WorkInstruction> wiList = this.getWorkInstructions();
		boolean foundShort = false;
		if (wiList.size() > 0) {
			for (WorkInstruction wi : wiList) {
				if (wi.getStatus() == WorkInstructionStatusEnum.COMPLETE)
					return "C";
				if (wi.getStatus() == WorkInstructionStatusEnum.SHORT)
					foundShort = true;
			}
		}
		if (willProduceWi(workService))
			return "Y";
		else if (foundShort)
			return "-, short";
		else
			return "-"; // Make it more distinguishable from "Y".
	}

	// --------------------------------------------------------------------------
	/**
	 * For the UI. preferred location may be null, or no longer valid.
	 */
	public String getPreferredLocationUi() {
		String internalString = getPreferredLocation();
		if (internalString == null)
			return "";
		// TODO  check that alias exists and location is not inactive. If so, show as inactive location.
		return internalString;
	}

	public String getGroupUi() {
		OrderGroup theGroup = parent.getOrderGroup();
		if (theGroup == null)
			return "";
		return theGroup.getDomainId();
	}

	public Location getPreferredLocObject() {
		Facility facility = getParent().getFacility();
		String preferredLocationString = getPreferredLocation();
		if (preferredLocationString == null || preferredLocationString.isEmpty()) {
			return null;
		}
		Location foundLocation = facility.findLocationById(preferredLocationString);
		return foundLocation;
	}

	/**
	 * Revaluate the status
	 * @return if it was changed
	 */
	public boolean reevaluateStatus() {
		OrderStatusEnum priorStatus = getStatus();
		if(getWorkInstructions().isEmpty()) {
			if (priorStatus == OrderStatusEnum.INPROGRESS) {
				setStatus(OrderStatusEnum.RELEASED);
				return true;
			}
			return false;
		}
		
		int qtyPicked = 0;
		boolean anyToPick = false;
		for (WorkInstruction sumWi : getWorkInstructions()) {
			WorkInstructionStatusEnum status = sumWi.getStatus();
			if (status.equals(WorkInstructionStatusEnum.COMPLETE)
				|| status.equals(WorkInstructionStatusEnum.SHORT)) {
				
				qtyPicked += sumWi.getActualQuantity();
			} else if (status.equals(WorkInstructionStatusEnum.INPROGRESS)
					  || status.equals(WorkInstructionStatusEnum.NEW)) {
			
				anyToPick |= true;
			}
		}
		if (qtyPicked >= getMinQuantity()) {
			setStatus(OrderStatusEnum.COMPLETE);
		} else if (anyToPick){
			setStatus(OrderStatusEnum.INPROGRESS);
		} else {
			setStatus(OrderStatusEnum.SHORT);
		}
		if (!priorStatus.equals(getStatus())) {
			LOGGER.info("Changed status of order detai, was: " + priorStatus + ", now: " + this);
			this.getDao().store(this);
			return true;
		} else {
			return false;
		}
	}
	
	public boolean isPreferredDetail(){
		return getPreferredLocation() != null && getWorkSequence() != null;
	}
	
	public String getGtinId(){
		ItemMaster im = getItemMaster();
		UomMaster um = getUomMaster();
		
		if (im != null && um != null) {
			return im.getGtinForUom(um).getDomainId();
		} else {
			return "";
		}
	}
}
