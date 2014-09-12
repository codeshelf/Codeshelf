/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: WorkInstruction.java,v 1.24 2013/09/18 00:40:09 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.domain;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

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

import com.avaje.ebean.annotation.CacheStrategy;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.gadgetworks.codeshelf.model.LedRange;
import com.gadgetworks.codeshelf.model.TimeFormat;
import com.gadgetworks.codeshelf.model.WorkInstructionStatusEnum;
import com.gadgetworks.codeshelf.model.WorkInstructionTypeEnum;
import com.gadgetworks.codeshelf.model.dao.DaoException;
import com.gadgetworks.codeshelf.model.dao.GenericDaoABC;
import com.gadgetworks.codeshelf.model.dao.ISchemaManager;
import com.gadgetworks.codeshelf.model.dao.ITypedDao;
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
@CacheStrategy(useBeanCache = true)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE)
@JsonIgnoreProperties({ "fullDomainId", "parentFullDomainId", "parentPersistentId", "className", "container", "itemMaster",
		"location" })
@ToString(of = { "typeEnum", "statusEnum", "itemId", "planQuantity", "actualQuantity", "locationId" }, callSuper = true, doNotUseGetters = true)
public class WorkInstruction extends DomainObjectTreeABC<OrderDetail> {

	@Inject
	public static ITypedDao<WorkInstruction>	DAO;

	@Singleton
	public static class WorkInstructionDao extends GenericDaoABC<WorkInstruction> implements ITypedDao<WorkInstruction> {
		@Inject
		public WorkInstructionDao(final ISchemaManager inSchemaManager) {
			super(inSchemaManager);
		}

		public final Class<WorkInstruction> getDaoClass() {
			return WorkInstruction.class;
		}
		
		
	}

	private static final Logger			LOGGER	= LoggerFactory.getLogger(WorkInstruction.class);

	// The parent order detail item.
	@Column(nullable = false)
	@ManyToOne(optional = false)
	private OrderDetail					parent;

	// Type.
	@Column(nullable = false)
	@Enumerated(value = EnumType.STRING)
	@Getter
	@Setter
	@JsonProperty
	private WorkInstructionTypeEnum		typeEnum;

	// Status.
	@Column(nullable = false)
	@Enumerated(value = EnumType.STRING)
	@Getter
	@Setter
	@JsonProperty
	private WorkInstructionStatusEnum	statusEnum;

	// The container.
	@Column(nullable = false)
	@Getter
	@ManyToOne(optional = false)
	private Container					container;

	// Denormalized for serialized WIs at the site controller.
	@Column(nullable = false)
	@Getter
	@JsonProperty
	private String						containerId;

	// The item id.
	@Column(nullable = false)
	@Getter
	@ManyToOne(optional = false)
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
	@Column(nullable = false)
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
	@Column(nullable = true)
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

   	private static final Integer			MAX_WI_DESC_BYTES	= 80;

   	public WorkInstruction() {

	}

	public final ITypedDao<WorkInstruction> getDao() {
		return DAO;
	}

	public final String getDefaultDomainIdPrefix() {
		return "WI";
	}

	public final OrderDetail getParent() {
		return parent;
	}

	public final void setParent(OrderDetail inParent) {
		parent = inParent;
	}

	public final List<? extends IDomainObject> getChildren() {
		return new ArrayList<IDomainObject>();
	}

	public final ILocation<?> getLocation() {
		return (ILocation<?>) location;
	}

	// Denormalized for serialized WIs at the site controller.
	public final void setContainer(Container inContainer) {
		container = inContainer;
		containerId = inContainer.getContainerId();
	}

	// Denormalized for serialized WIs at the site controller.
	public final void setItemMaster(ItemMaster inItemMaster) {
		itemMaster = inItemMaster;
		itemId = inItemMaster.getItemId();
	}

	// Denormalized for serialized WIs at the site controller.
	public final void setLocation(LocationABC inLocation) {
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
		return parent.getUomMasterId();
	}

	// --------------------------------------------------------------------------
	/**
	 * For a UI meta field
	 * @return
	 */
	public final String getOrderDetailId() {
		OrderDetail detail = this.getParent();
		return detail.getDomainId(); // parent must be there by DB constraint
	}

	// --------------------------------------------------------------------------
	/**
	 * For a UI meta field
	 * @return
	 */
	public final String getOrderId() {
		OrderDetail detail = this.getParent();
		OrderHeader header = detail.getParent();
		return header.getDomainId(); // parents must be there by DB constraint
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

		/* old code
		ILocation<?> theLoc = this.getLocation();
		if (theLoc != null)
			return StringUIConverter.doubleToTwoDecimalsString(theLoc.getPosAlongPath());
		else {
			return "";
		}
		*/
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
		this.setStatusEnum(newStatus);
		this.setTypeEnum(WorkInstructionTypeEnum.ACTUAL);

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
		
		LocationABC theLocation = (LocationABC) theWiLocation;
		if (theLocation.getClass() == Facility.class)
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

	public String getItemMasterId() {
		// Note: there is the denormalized field for itemId/getItemId. For this purpose, I want to show the real SKU only.
		ItemMaster theMaster = this.getItemMaster();
		return theMaster.getItemId();
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
		cookedDesc = cookedDesc.replaceAll("[^a-zA-Z0-9+., -]","");
		return cookedDesc;
	}

	
}
