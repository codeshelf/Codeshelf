/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: OrderDetail.java,v 1.25 2013/09/18 00:40:08 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.domain;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.avaje.ebean.annotation.CacheStrategy;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.gadgetworks.codeshelf.model.OrderStatusEnum;
import com.gadgetworks.codeshelf.model.OrderTypeEnum;
import com.gadgetworks.codeshelf.model.WorkInstructionStatusEnum;
import com.gadgetworks.codeshelf.model.dao.GenericDaoABC;
import com.gadgetworks.codeshelf.model.dao.ISchemaManager;
import com.gadgetworks.codeshelf.model.dao.ITypedDao;
import com.gadgetworks.codeshelf.util.ASCIIAlphanumericComparator;
import com.gadgetworks.codeshelf.util.UomNormalizer;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Singleton;

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
@CacheStrategy(useBeanCache = true)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE)
@ToString(of = { "statusEnum", "quantity", "itemMaster", "uomMaster", "active" }, callSuper = true, doNotUseGetters = true)
public class OrderDetail extends DomainObjectTreeABC<OrderHeader> {

	@Inject
	public static ITypedDao<OrderDetail>	DAO;

	@Singleton
	public static class OrderDetailDao extends GenericDaoABC<OrderDetail> implements ITypedDao<OrderDetail> {
		@Inject
		public OrderDetailDao(final ISchemaManager inSchemaManager) {
			super(inSchemaManager);
		}

		public final Class<OrderDetail> getDaoClass() {
			return OrderDetail.class;
		}
	}

	@SuppressWarnings("unused")
	private static final Logger		LOGGER				= LoggerFactory.getLogger(OrderDetail.class);

	private static final Comparator<String> asciiAlphanumericComparator = new ASCIIAlphanumericComparator();
	
	// The owning order header.
	@Column(nullable = false)
	@ManyToOne(optional = false)
	private OrderHeader				parent;

	// The collective order status.
	@Column(nullable = false)
	@Enumerated(value = EnumType.STRING)
	@Getter
	@Setter
	@JsonProperty
	private OrderStatusEnum			statusEnum;

	// The item master.
	@Column(nullable = false)
	@ManyToOne(optional = false)
	@Getter
	@Setter
	private ItemMaster				itemMaster;

	// The description.
	@Column(nullable = true)
	@Getter
	@Setter
	@JsonProperty
	private String					description;

	// The actual quantity requested.
	@Column(nullable = false)
	@Getter
	@Setter
	@JsonProperty
	private Integer					quantity;

	// The min quantity that we can use.  (Same as quantity in most cases.)
	@Column(nullable = false)
	@Getter
	@Setter
	@JsonProperty
	private Integer					minQuantity;

	// The max quantity that we can use. (Same as quantity in most cases.)
	@Column(nullable = false)
	@Getter
	@Setter
	@JsonProperty
	private Integer					maxQuantity;

	// The UoM.
	@Column(nullable = false)
	@OneToOne(fetch = FetchType.LAZY)
	@Getter
	@Setter
	private UomMaster				uomMaster;

	@Column(nullable = false)
	@Getter
	@Setter
	@JsonProperty
	private Boolean					active;

	@Column(nullable = false)
	@Getter
	@Setter
	@JsonProperty
	private Timestamp				updated;

	// A work area will contain a set of active users (workers).
	@OneToMany(mappedBy = "parent")
	@Getter
	private List<WorkInstruction>	workInstructions	= new ArrayList<WorkInstruction>();

	public OrderDetail() {
	}

	@SuppressWarnings("unchecked")
	public final ITypedDao<OrderDetail> getDao() {
		return DAO;
	}

	public final String getDefaultDomainIdPrefix() {
		return "PS";
	}

	public final String getOrderDetailId() {
		return getDomainId();
	}

	public final void setOrderDetailId(String inOrderDetailId) {
		setDomainId(inOrderDetailId);
	}

	public final OrderHeader getParent() {
		return parent;
	}

	public final void setParent(OrderHeader inParent) {
		parent = inParent;
	}

	public final List<IDomainObject> getChildren() {
		return new ArrayList<IDomainObject>();
	}

	// Even though we don't really use this field, it's tied to an eBean op that keeps the DB in synch.
	public final void addWorkInstruction(WorkInstruction inWorkInstruction) {
		workInstructions.add(inWorkInstruction);
	}

	// Even though we don't really use this field, it's tied to an eBean op that keeps the DB in synch.
	public final void removeWorkInstruction(WorkInstruction inWorkInstruction) {
		workInstructions.remove(inWorkInstruction);
	}

	public final String getParentOrderID() {
		return parent.getDomainId();
	}

	public final String getUomMasterId() {
		return getUomMaster().getDomainId();
	}

	public final String getItemMasterId() {
		return getItemMaster().getDomainId();
	}

	public final String getOrderId() {
		return parent.getOrderId();
	}

	// --------------------------------------------------------------------------
	/**
	 * Meta fields. These appropriate for pick (outbound) order details and/or cross--batch order details
	 * @return
	 */
	public final String getWiLocation() {
		String returnStr = "";
		for (WorkInstruction wi : getWorkInstructions()) {
			if (returnStr.isEmpty())
				if (wi.getStatusEnum() == WorkInstructionStatusEnum.SHORT)
					returnStr = WorkInstructionStatusEnum.SHORT.getName();
				else {
					returnStr = wi.getPickInstruction();					
					if (wi.getStatusEnum() == WorkInstructionStatusEnum.COMPLETE)
						returnStr = returnStr + " (" + WorkInstructionStatusEnum.COMPLETE.getName() + ")";					
				}
			else if (wi.getStatusEnum() != WorkInstructionStatusEnum.SHORT) { // don't pile on extra SHORT if multiple SHORT WIs
				returnStr = returnStr + ", " + wi.getPickInstruction();
			}
		}
		return returnStr;
	}

	public final String getWiChe() {
		String returnStr = "";
		for (WorkInstruction wi : getWorkInstructions()) {
			if (returnStr.isEmpty())
				returnStr = wi.getAssignedCheName();
			else if (wi.getStatusEnum() != WorkInstructionStatusEnum.SHORT) { // don't pile on extra CHE if multiple SHORT WIs
				returnStr = returnStr + ", " + wi.getAssignedCheName();
			}
		}
		return returnStr;
	}
	
	public final String getItemLocations() {
		//If cross batch return empty
		if (getParent().getOrderTypeEnum().equals(OrderTypeEnum.CROSS)) {
			return "";
		}
		else {
			//if work instructions are assigned use the location from that 
			List<String> wiLocationDisplay = getPickableWorkInstructions();
			if (!wiLocationDisplay .isEmpty()) {
				return Joiner.on(",").join(wiLocationDisplay);
			} else {
				List<String> itemLocationIds = new ArrayList<String>();
				List<Item> items = getItemMaster().getItems();
				//filter by uom and join the aliases together
				for (Item item : items) {
					if (UomNormalizer.normalizedEquals(item.getUomMasterId(), this.getUomMasterId())) {
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
		ImmutableSet<WorkInstructionStatusEnum> pickableWiSet = Sets.immutableEnumSet(WorkInstructionStatusEnum.NEW, WorkInstructionStatusEnum.INPROGRESS, WorkInstructionStatusEnum.COMPLETE);
		List<String> pickableWiLocations =  new ArrayList<String>();
		for (WorkInstruction wi : getWorkInstructions()) {
			if (pickableWiSet.contains(wi.getStatusEnum())) {
				pickableWiLocations.add(wi.getPickInstruction());
			}
		}
		return pickableWiLocations;
	}
	
}
