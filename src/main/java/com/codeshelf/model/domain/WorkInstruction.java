/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: WorkInstruction.java,v 1.24 2013/09/18 00:40:09 jeffw Exp $
 *******************************************************************************/
package com.codeshelf.model.domain;

import java.sql.Timestamp;

import javax.persistence.Cacheable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import org.hibernate.Hibernate;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.codeshelf.model.LedRange;
import com.codeshelf.model.TimeFormat;
import com.codeshelf.model.WorkInstructionStatusEnum;
import com.codeshelf.model.WorkInstructionTypeEnum;
import com.codeshelf.model.dao.GenericDaoABC;
import com.codeshelf.model.dao.ITypedDao;
import com.codeshelf.platform.persistence.TenantPersistenceService;
import com.codeshelf.util.StringUIConverter;
import com.google.inject.Inject;
import com.google.inject.Singleton;

// --------------------------------------------------------------------------
/**
 * WorkInstruction
 *
 * A planned or actual request for work.
 *
 * We anticipate that some day this object will split from a pure item->container instruction object
 * to a item/container to container, or container to/from location
 *
 * The references are stored as Strings because we mostly serialize this object (in JSON) to send back-and-forth
 * over the wire to the remote radio/network controllers.
 *
 * NOTE:
 *
 * WorkInstructions also get sent to the site controller (gateway).  The gateway only knows that it needs to distribute and handle
 * work instructions at the site, but the gateway has no business logic at all.  Any changes to those work instructions can only
 * come from the back-end business logic.  We send those changes over the WebSocket to the gateway.  We share this
 * WorkInstruction class between the business logic and remote gateway as a simple way to serial the objects over the WebSocket.
 *
 * @author jeffw
 */

@Entity
@Table(name = "work_instruction")
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE)
@JsonIgnoreProperties({ "fullDomainId", "parentFullDomainId", "parentPersistentId", "className", "container", "itemMaster",
		"location" })
@ToString(of = { "type", "status", "orderDetail", "itemId", "planQuantity", "actualQuantity", "locationId" }, callSuper = true, doNotUseGetters = true)
public class WorkInstruction extends DomainObjectTreeABC<Facility> {

	@Inject
	public static ITypedDao<WorkInstruction>	DAO;

	@Singleton
	public static class WorkInstructionDao extends GenericDaoABC<WorkInstruction> implements ITypedDao<WorkInstruction> {
		@Inject
		public WorkInstructionDao(final TenantPersistenceService tenantPersistenceService) {
			super(tenantPersistenceService);
		}

		public final Class<WorkInstruction> getDaoClass() {
			return WorkInstruction.class;
		}
	}

	private static final Logger			LOGGER				= LoggerFactory.getLogger(WorkInstruction.class);

	// The parent is the facility
	@ManyToOne(optional = false, fetch=FetchType.LAZY)
	@Getter
	@Setter
	private Facility					parent;

	// Type.
	@Column(nullable = false)
	@Enumerated(value = EnumType.STRING)
	@Getter
	@Setter
	@JsonProperty
	private WorkInstructionTypeEnum		type;

	// Status.
	@Column(nullable = false)
	@Enumerated(value = EnumType.STRING)
	@Getter
	@Setter
	@JsonProperty
	private WorkInstructionStatusEnum	status;

	// The container.
	@ManyToOne(optional = true, fetch=FetchType.LAZY)
	@Getter
	private Container					container;

	// Denormalized for serialized WIs at the site controller.
	@Column(nullable = false,name="container_id")
	@Getter
	@JsonProperty
	private String						containerId;

	// The itemMaster.
	@ManyToOne(optional = true, fetch=FetchType.LAZY)
	@JoinColumn(name="item_master_persistentid")
	@Getter
	private ItemMaster					itemMaster;

	// Denormalized for serialized WIs at the site controller.
	@Column(nullable = false,name="item_id")
	@Getter
	@JsonProperty
	private String						itemId;

	// Description.
	@Column(nullable = true)
	@Getter
	@Setter
	@JsonProperty
	private String						description;

	// The pick instruction (cooked item ID to pick).
	@Column(nullable = false,name="pick_instruction")
	@Getter
	@Setter
	@JsonProperty
	private String						pickInstruction;

	// The nominal planned pick quantity.
	@Column(nullable = false,name="plan_quantity")
	@Getter
	@Setter
	@JsonProperty
	private Integer						planQuantity;

	// The min planned pick quantity.
	@Column(nullable = false,name="plan_min_quantity")
	@Getter
	@Setter
	@JsonProperty
	private Integer						planMinQuantity;

	// The  max planned pick quantity.
	@Column(nullable = false,name="plan_max_quantity")
	@Getter
	@Setter
	@JsonProperty
	private Integer						planMaxQuantity;

	// The pick quantity.
	@Column(nullable = true,name="actual_quantity")
	@Getter
	@Setter
	@JsonProperty
	private Integer						actualQuantity;

	// From location.
	@ManyToOne(optional = false,fetch=FetchType.LAZY)
	@Getter
	private Location					location;

	// Denormalized for serialized WIs at the site controller.
	@Column(nullable = false,name="location_id")
	@Getter
	@Setter
	@JsonProperty
	private String						locationId;

	// Picker ID.
	@Column(nullable = true,name="picker_id")
	@Getter 
	@Setter
	@JsonProperty
	private String						pickerId;

	// Assigned CHE
	@Setter
	@ManyToOne(optional = true,fetch=FetchType.LAZY)
	@JoinColumn(name="assigned_che_persistentid")
	@Getter
	private Che							assignedChe;

	// LED command/processing stream.
	// A formatted stream of LED processing commands that tells the site gateway how to lights LEDs for this WI.
	// See LedStreamProcessor
	@Column(nullable = true,name="led_cmd_stream",columnDefinition="TEXT")
	@Getter
	@Setter
	@JsonProperty
	private String						ledCmdStream;

	// The remote gateway controller will sort and group by this code, and then only send out one group to the radio network at a time.
	@Column(nullable = true,name="group_and_sort_code")
	@Getter
	@Setter
	@JsonProperty
	private String						groupAndSortCode;

	@Getter
	@Setter
	@JsonProperty
	@Column(nullable = true,name="pos_along_path")
	private Double						posAlongPath;

	@Column(nullable = true)
	@Getter
	@Setter
	@JsonProperty
	private Timestamp					created;

	@Column(nullable = true)
	@Getter
	@Setter
	@JsonProperty
	private Timestamp					assigned;

	@Column(nullable = true)
	@Getter
	@Setter
	@JsonProperty
	private Timestamp					started;

	@Column(nullable = true)
	@Getter
	@Setter
	@JsonProperty
	private Timestamp					completed;

	// The parent order detail item.
	@ManyToOne(optional = true,fetch=FetchType.LAZY)
	@JoinColumn(name="order_detail_persistentid")
	@Getter
	@Setter
	private OrderDetail					orderDetail;

	private static final Integer		MAX_WI_DESC_BYTES	= 80;

	public WorkInstruction() {
	}
	
	@SuppressWarnings("unchecked")
	public final ITypedDao<WorkInstruction> getDao() {
		return DAO;
	}

	public final String getDefaultDomainIdPrefix() {
		return "WI";
	}

	// Denormalized for serialized WIs at the site controller.
	public void setContainer(Container inContainer) {
		container = inContainer;
		if (container != null)
			containerId = inContainer.getContainerId();
		else {
			// we might look at a type enumeration to set this more appropriately. See CD_0045 for list of housekeeping and other possibilities with no container.
			containerId = "None";
		}
	}

	//Not for serialization
	@JsonIgnore
	public boolean isHousekeeping() {
		return this.getType().isHousekeeping();
	}

	// Denormalized for serialized WIs at the site controller.
	public void setItemMaster(ItemMaster inItemMaster) {
		itemMaster = inItemMaster;
		if (itemMaster != null)
			itemId = inItemMaster.getItemId();
		else
			itemId = getItemIdIfMasterNull();
	}

	private final String getItemIdIfMasterNull() {
		// we might look at a type enumeration to set this more appropriately. See CD_0045 for list of housekeeping and other possibilities with no container.
		if (this.isHousekeeping())
			return "Housekeeping"; // may need to localize on the UI.
		else
			return "";
	}

	// Denormalized for serialized WIs at the site controller.
	public void setLocation(Location inLocation) {
		location = inLocation;
		// This string is user-readable format set by application logic.
		// locationId = inLocation.getLocationId();
	}

	// --------------------------------------------------------------------------
	/**
	 * Determine if the location contains this work instruction.
	 * @param inCheckLocation
	 * @return
	 */
	public boolean isContainedByLocation(final Location inCheckLocation) {
		boolean result = false;

		if (inCheckLocation == null) {
			result = false;
		} else if (location.equals(inCheckLocation)) {
			// The check location directly is the WI location.
			result = true;
		} else {
			// The check location is parent of the WI location, so it contains it.
			@SuppressWarnings("unchecked")
			Location parentLoc = location.getParentAtLevel(Hibernate.getClass(inCheckLocation));
			if ((parentLoc != null) && (parentLoc.equals(inCheckLocation))) {
				result = true;
			}
		}

		return result;
	}

	// --------------------------------------------------------------------------
	/**
	 * For a UI meta field
	 * @return
	 */
	public String getUomMasterId() {
		OrderDetail detail = getOrderDetail();
		if (detail != null)
			return detail.getUomMasterId();
		else
			return "";
	}

	// --------------------------------------------------------------------------
	/**
	 * For a UI meta field
	 * @return
	 */
	public String getOrderDetailId() {
		OrderDetail detail = this.getOrderDetail();
		if (detail == null)
			return "";
		else
			return detail.getDomainId(); // detail may be null from v5
	}

	// --------------------------------------------------------------------------
	/**
	 * For a UI meta field
	 * @return
	 */
	public String getOrderId() {
		OrderDetail detail = this.getOrderDetail();
		if (detail == null)
			return "";
		else {
			OrderHeader header = detail.getParent();
			return header.getDomainId(); // parent must be there by DB constraint
		}
	}

	// --------------------------------------------------------------------------
	/**
	 * For a UI meta field
	 * @return
	 */
	public String getPickInstructionUi() {
		String theStr = getPickInstruction();
		if (theStr != null) {
			Location loc = this.getLocation();
			if (loc != null && !loc.isActive())
				return "<" + theStr + ">";
			else
				return theStr;
		}
		return "";
	}

	// --------------------------------------------------------------------------
	/**
	 * For a UI meta field
	 * @return
	 */
	public String getAssignedCheName() {
		Che theChe = getAssignedChe();
		if (theChe != null)
			return theChe.getDomainId();
		else {
			return "";
		}
	}

	// --------------------------------------------------------------------------
	/**
	 * For a UI meta field
	 * @return
	 */
	public String getWiPosAlongPath() {
		// This needs work for Accu non-slotted inventory.
		//
		Double wiPosValue = getPosAlongPath();
		if (wiPosValue == null || wiPosValue == 0.0)
			return ""; // 0.0 happens for short plans to facility
		else
			return StringUIConverter.doubleToTwoDecimalsString(wiPosValue);
	}

	// --------------------------------------------------------------------------
	/**
	 * For a UI meta field
	 * @return
	 */
	public String getCompleteTimeForUi() {

		Timestamp completeTime = this.getCompleted();
		if (completeTime == null)
			return "";
		else {
			return TimeFormat.getUITime(completeTime);
		}
	}

	// --------------------------------------------------------------------------
	/**
	 * For a UI meta field
	 * @return
	 */
	public String getAssignTimeForUi() {

		Timestamp assignTime = this.getAssigned();
		if (assignTime == null)
			return "";
		else {
			return TimeFormat.getUITime(assignTime);
		}
	}

	// --------------------------------------------------------------------------
	/**
	 * For a UI meta field
	 * @return
	 */
	public String getNominalLocationId() {

		Location theLoc = this.getLocation();
		if (theLoc != null)
			return theLoc.getNominalLocationId();
		else {
			return "";
		}
	}

	// A UI meta-field. Should match what actually lights as it calls the same computation.
	public String getLitLedsForWi() {
		// Questions before we can code this.
		// 1) is this a put work instruction, or a pick? If a put, probably just going to the location, unless it is a replenish.
		// 2) Is there inventory in that location? If a pick, there should be. If a put, there may or may not be.
		// 3) If inventory is there, is there meters from anchor value?

		String returnStr = "";
		Location theWLocationABC = this.getLocation();
		if (theWLocationABC == null)
			return returnStr;

		Location theLocation = theWLocationABC;
		if (theLocation.isFacility())
			return returnStr;

		Item wiItem = theLocation.getStoredItemFromMasterIdAndUom(getItemId(), getUomMasterId());

		// If there is the right item at the location the WI is going to, then use it.
		if (wiItem != null) {
			LedRange theRange = wiItem.getFirstLastLedsForItem();
			returnStr = theRange.getRangeString();
		} else {
			LedRange theRange = theLocation.getFirstLastLedsForLocation();
			returnStr = theRange.getRangeString();
		}
		return returnStr;
	}

	// convenience method. This may return null! Normally returns something only for pick wi
	public Item getWiItem() {
		Location theWLocationABC = this.getLocation();
		if (theWLocationABC == null)
			return null; // should not happen

		Location theLocation = theWLocationABC;
		if (theLocation.isFacility())
			return null;

		return theLocation.getStoredItemFromMasterIdAndUom(getItemId(), getUomMasterId());
	}

	public String getItemMasterId() {
		// Note: there is the denormalized field for itemId/getItemId. For this purpose, I want to show the real SKU only.
		// the itemId may set to the itemId, rather than the master domainID.
		ItemMaster theMaster = this.getItemMaster();
		if (theMaster != null)
			return theMaster.getItemId();
		else
			return getItemIdIfMasterNull();
	}
	
	public String getStatusString() {
		// getStatus works fine, except when the status is null. As in logging WI on the site controller side. Just don't throw if null.
		WorkInstructionStatusEnum theStatus = this.getStatus();
		if (theStatus != null)
			return theStatus.getName();
		else
			return "";
	}
	
	// --------------------------------------------------------------------------
	/**
	 * A simple wrapper that does not interfere with Json serialization
	 */
	public void doSetPickInstruction(String inPickInstruction){
		this.setPickInstruction(inPickInstruction);
		if (inPickInstruction == null || inPickInstruction.isEmpty()) {
			LOGGER.error("probable bug by caller of doSetPickInstruction");
		}
	}


	// --------------------------------------------------------------------------
	/**
	 * The description comes in from customer orders. It could have quite a mess in it. Ok for most of our application,
	 * But not ok to ship over to our radio controller elements.
	 * @param inDescription
	 */
	public static final String cookDescription(final String inDescription) {
		String cookedDesc = inDescription.substring(0, Math.min(MAX_WI_DESC_BYTES, inDescription.length()));
		// This was the original remove ASCII line
		// cookedDesc = cookedDesc.replaceAll("[^\\p{ASCII}]", "");

		// This might over do it. Letters or numbers or white space ok. Nothing else. +, - % $ etc. stripped
		// cookedDesc = cookedDesc.replaceAll("[^\\p{L}\\p{Z}]","");

		// Or this
		cookedDesc = cookedDesc.replaceAll("[^a-zA-Z0-9+., -]", "");
		return cookedDesc;
	}

	public static void setDao(WorkInstructionDao inWorkInstructionDao) {
		WorkInstruction.DAO = inWorkInstructionDao;
	}

	@Override
	public Facility getFacility() {
		return getParent();
	}
}
