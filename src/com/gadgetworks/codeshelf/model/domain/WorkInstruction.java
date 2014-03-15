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

import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.annotate.JsonAutoDetect.Visibility;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.avaje.ebean.annotation.CacheStrategy;
import com.gadgetworks.codeshelf.model.WorkInstructionStatusEnum;
import com.gadgetworks.codeshelf.model.WorkInstructionTypeEnum;
import com.gadgetworks.codeshelf.model.dao.GenericDaoABC;
import com.gadgetworks.codeshelf.model.dao.ISchemaManager;
import com.gadgetworks.codeshelf.model.dao.ITypedDao;
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
@JsonAutoDetect(getterVisibility = Visibility.NONE)
@JsonIgnoreProperties({ "fullDomainId", "parentFullDomainId", "parentPersistentId", "className", "container", "itemMaster", "location" })
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

	// The pick quantity.
	@Column(nullable = false)
	@Getter
	@Setter
	@JsonProperty
	private Integer						planQuantity;

	// The pick quantity.
	@Column(nullable = true)
	@Getter
	@Setter
	@JsonProperty
	private Integer						actualQuantity;

	// From location.
	@Column(nullable = false)
	@ManyToOne(optional = false)
	private SubLocationABC<?>			location;

	// Denormalized for serialized WIs at the site controller.
	@Column(nullable = false)
	@Getter
	@JsonProperty
	private String						locationId;

	// Picker ID.
	@Column(nullable = true)
	@Getter
	@Setter
	@JsonProperty
	private String						pickerId;

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
	public final void setLocation(ISubLocation<?> inLocation) {
		location = (SubLocationABC<?>) inLocation;
		locationId = inLocation.getLocationId();
	}
}
