/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: WorkInstruction.java,v 1.24 2013/09/18 00:40:09 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.domain;

import java.sql.Timestamp;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.gadgetworks.codeshelf.model.LedRange;
import com.gadgetworks.codeshelf.model.TimeFormat;
import com.gadgetworks.codeshelf.model.WorkInstructionStatusEnum;
import com.gadgetworks.codeshelf.model.WorkInstructionTypeEnum;
import com.gadgetworks.codeshelf.model.dao.DaoException;
import com.gadgetworks.codeshelf.model.dao.GenericDaoABC;
import com.gadgetworks.codeshelf.model.dao.ITypedDao;
import com.gadgetworks.codeshelf.platform.persistence.PersistenceService;
import com.gadgetworks.codeshelf.util.StringUIConverter;
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
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE)
@JsonIgnoreProperties({ "fullDomainId", "parentFullDomainId", "parentPersistentId", "className", "container", "itemMaster",
		"location" })
@ToString(of = { "type", "status", "itemId", "planQuantity", "actualQuantity", "locationId" }, callSuper = true, doNotUseGetters = true)
public class WorkInstruction extends DomainObjectTreeABC<Facility> {

	@Inject
	public static ITypedDao<WorkInstruction>	DAO;

	@Singleton
	public static class WorkInstructionDao extends GenericDaoABC<WorkInstruction> implements ITypedDao<WorkInstruction> {
		@Inject
		public WorkInstructionDao(final PersistenceService persistenceService) {
			super(persistenceService);
		}

		public final Class<WorkInstruction> getDaoClass() {
			return WorkInstruction.class;
		}
	}

	private static final Logger			LOGGER				= LoggerFactory.getLogger(WorkInstruction.class);

	// The parent is the facility
	@ManyToOne(optional = false)
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

	// The container. Change to nullable v5 upgrade020
	//@Column(nullable = true)
	@Getter
	@ManyToOne(optional = true)
	private Container					container;

	// Denormalized for serialized WIs at the site controller.
	@Column(nullable = false)
	@Getter
	@JsonProperty
	private String						containerId;

	// The itemMaster. Change to nullable v5 upgrade020
	@Getter
	@ManyToOne(optional = true)
	private ItemMaster					itemMaster;

	// Denormalized for serialized WIs at the site controller.
	@Column(nullable = false)
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
	@Column(nullable = false)
	@Getter
	@Setter
	@JsonProperty
	private String						pickInstruction;

	// The nominal planned pick quantity.
	@Column(nullable = false)
	@Getter
	@Setter
	@JsonProperty
	private Integer						planQuantity;

	// The min planned pick quantity.
	@Column(nullable = false)
	@Getter
	@Setter
	@JsonProperty
	private Integer						planMinQuantity;

	// The  max planned pick quantity.
	@Column(nullable = false)
	@Getter
	@Setter
	@JsonProperty
	private Integer						planMaxQuantity;

	// The pick quantity.
	@Column(nullable = true)
	@Getter
	@Setter
	@JsonProperty
	private Integer						actualQuantity;

	// From location.
	@SuppressWarnings("rawtypes")
	//@Column(nullable = false)
	@ManyToOne(optional = false)
	private LocationABC					location;

	// Denormalized for serialized WIs at the site controller.
	@Column(nullable = false)
	@Getter
	@Setter
	@JsonProperty
	private String						locationId;

	// Picker ID.
	@Column(nullable = true)
	@Getter 
	@Setter
	@JsonProperty
	private String						pickerId;

	// Assigned CHE
	//@Column(nullable = true)
	@Getter
	@Setter
	@ManyToOne(optional = true)
	private Che							assignedChe;

	// LED command/processing stream.
	// A formatted stream of LED processing commands that tells the site gateway how to lights LEDs for this WI.
	// See LedStreamProcessor
	@Column(nullable = true)
	@Getter
	@Setter
	@JsonProperty
	private String						ledCmdStream;

	// The remote gateway controller will sort and group by this code, and then only send out one group to the radio network at a time.
	@Column(nullable = true)
	@Getter
	@Setter
	@JsonProperty
	private String						groupAndSortCode;

	@Getter
	@Setter
	@JsonProperty
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
	//@Column(nullable = true)
	@ManyToOne(optional = true)
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

	public final Facility getParent() {
		return parent;
	}

	public final OrderDetail getOrderDetail() {
		// v5 interim
		return orderDetail;
	}

	public final void setParent(Facility inParent) {
		parent = inParent;
	}

	public final void setOrderDetail(OrderDetail inDetail) {
		orderDetail = inDetail;
	}

	public final ILocation<?> getLocation() {
		return (ILocation<?>) location;
	}

	// Denormalized for serialized WIs at the site controller.
	public final void setContainer(Container inContainer) {
		container = inContainer;
		if (container != null)
			containerId = inContainer.getContainerId();
		else {
			// we might look at a type enumeration to set this more appropriately. See CD_0045 for list of housekeeping and other possibilities with no container.
			containerId = "None";
		}
	}

	public final Boolean amIHouseKeepingWi() {
		// if called the more obvious isHouseKeepingWi, then jackson wants to serialize it.
		WorkInstructionTypeEnum theType = this.getType();
		return (theType == WorkInstructionTypeEnum.HK_REPEATPOS || theType == WorkInstructionTypeEnum.HK_BAYCOMPLETE);
	}

	// Denormalized for serialized WIs at the site controller.
	public final void setItemMaster(ItemMaster inItemMaster) {
		itemMaster = inItemMaster;
		if (itemMaster != null)
			itemId = inItemMaster.getItemId();
		else
			itemId = getItemIdIfMasterNull();
	}

	private final String getItemIdIfMasterNull() {
		// we might look at a type enumeration to set this more appropriately. See CD_0045 for list of housekeeping and other possibilities with no container.
		return "Housekeeping"; // may need to localize on the UI.
	}

	// Denormalized for serialized WIs at the site controller.
	public final void setLocation(ILocation<?> inLocation) {
		location = (SubLocationABC<?>) inLocation;
		// This string is user-readable format set by application logic.
		// locationId = inLocation.getLocationId();
	}

	// --------------------------------------------------------------------------
	/**
	 * Determine if the location contains this work instruction.
	 * @param inCheckLocation
	 * @return
	 */
	public final boolean isContainedByLocation(final ILocation<?> inCheckLocation) {
		boolean result = false;

		if (inCheckLocation == null) {
			result = false;
		} else if (location.equals(inCheckLocation)) {
			// The check location directly is the WI location.
			result = true;
		} else {
			// The check location is parent of the WI location, so it contains it.
			@SuppressWarnings("unchecked")
			ILocation<?> parentLoc = location.getParentAtLevel(inCheckLocation.getClass());
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
	public final String getUomMasterId() {
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
	public final String getOrderDetailId() {
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
	public final String getOrderId() {
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
	public final String getPickInstructionUi() {
		String theStr = getPickInstruction();
		if (theStr != null) {
			ILocation<?> loc = this.getLocation();
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
	public final String getAssignedCheName() {
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
	public final String getWiPosAlongPath() {
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
	public final String getCompleteTimeForUi() {

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
	public final String getAssignTimeForUi() {

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
	public final String getNominalLocationId() {

		ILocation<?> theLoc = this.getLocation();
		if (theLoc != null)
			return theLoc.getNominalLocationId();
		else {
			return "";
		}
	}

	// --------------------------------------------------------------------------
	/**
	 * For a UI simulation
	 * @return
	 */
	public final void fakeCompleteWi(String inCompleteStr) {
		boolean doComplete = inCompleteStr.equalsIgnoreCase("COMPLETE");
		boolean doShort = inCompleteStr.equalsIgnoreCase("SHORT");

		// default to complete values
		Integer actualQuant = this.getPlanQuantity();
		WorkInstructionStatusEnum newStatus = WorkInstructionStatusEnum.COMPLETE;

		if (doComplete) {
		} else if (doShort) {
			actualQuant--;
			newStatus = WorkInstructionStatusEnum.SHORT;
		}
		Timestamp completeTime = new Timestamp(System.currentTimeMillis());
		Timestamp startTime = new Timestamp(System.currentTimeMillis() - (10 * 1000)); // assume 10 seconds earlier

		this.setActualQuantity(actualQuant);
		this.setCompleted(completeTime);
		this.setStarted(startTime);
		this.setStatus(newStatus);
		this.setType(WorkInstructionTypeEnum.ACTUAL);

		try {
			WorkInstruction.DAO.store(this);
		} catch (DaoException e) {
			LOGGER.error("", e);
		}

	}

	// A UI meta-field. Should match what actually lights as it calls the same computation.
	public String getLitLedsForWi() {
		// Questions before we can code this.
		// 1) is this a put work instruction, or a pick? If a put, probably just going to the location, unless it is a replenish.
		// 2) Is there inventory in that location? If a pick, there should be. If a put, there may or may not be.
		// 3) If inventory is there, is there meters from anchor value?

		String returnStr = "";
		ILocation<?> theWiLocation = this.getLocation();
		if (theWiLocation == null)
			return returnStr;

		LocationABC<?> theLocation = (LocationABC<?>) theWiLocation;
		if (theLocation instanceof Facility)
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
		ILocation<?> theWiLocation = this.getLocation();
		if (theWiLocation == null)
			return null; // should not happen

		LocationABC<?> theLocation = (LocationABC<?>) theWiLocation;
		if (theLocation instanceof Facility)
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
