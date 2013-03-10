/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: WorkInstruction.java,v 1.12 2013/03/10 08:58:43 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.domain;

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
import com.gadgetworks.codeshelf.model.dao.ITypedDao;
import com.gadgetworks.flyweight.command.ColorEnum;
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
 * @author jeffw
 */

@Entity
@Table(name = "WORKINSTRUCTION", schema = "CODESHELF")
@CacheStrategy
@JsonAutoDetect(getterVisibility = Visibility.NONE)
@JsonIgnoreProperties({ "fullDomainId", "parentFullDomainId", "parentPersistentId", "className" })
public class WorkInstruction extends DomainObjectTreeABC<OrderDetail> {

	@Inject
	public static ITypedDao<WorkInstruction>	DAO;

	@Singleton
	public static class WorkInstructionDao extends GenericDaoABC<WorkInstruction> implements ITypedDao<WorkInstruction> {
		public final Class<WorkInstruction> getDaoClass() {
			return WorkInstruction.class;
		}
	}

	private static final Logger			LOGGER	= LoggerFactory.getLogger(WorkInstruction.class);

	// The parent order detail item.
	@Column(nullable = false)
	@ManyToOne(optional = false)
	protected OrderDetail				parent;

	// Type.
	@Column(nullable = false)
	@Enumerated(value = EnumType.STRING)
	@Getter
	@Setter
	@JsonProperty
	protected WorkInstructionTypeEnum	typeEnum;

	// Status.
	@Column(nullable = false)
	@Enumerated(value = EnumType.STRING)
	@Getter
	@Setter
	@JsonProperty
	protected WorkInstructionStatusEnum	statusEnum;

	// The container.
	@Column(nullable = false)
	@Getter
	@Setter
	@JsonProperty
	protected String					containerId;

	// The item.
	@Column(nullable = false)
	@Getter
	@Setter
	@JsonProperty
	protected String					itemId;

	// The pick quantity.
	@Column(nullable = false)
	@Getter
	@Setter
	@JsonProperty
	protected Integer					quantity;

	// From location.
	@Column(nullable = false)
	@Getter
	@Setter
	@JsonProperty
	protected String					locationId;

	// Picker ID.
	@Column(nullable = true)
	@Getter
	@Setter
	@JsonProperty
	protected String					pickerId;

	// Everything below this is transient (not persisted) and only has value when passed around at production time.

	// Aisle controller ID.
	@Column(nullable = true)
	@Getter
	@Setter
	@JsonProperty
	protected String					aisleControllerId;

	// Aisle controller command.
	@Column(nullable = true)
	@Getter
	@Setter
	@JsonProperty
	protected String					aisleControllerCommand;

	// Color used for picking.
	@Column(nullable = true)
	@Enumerated(value = EnumType.STRING)
	@Getter
	@Setter
	@JsonProperty
	protected ColorEnum					colorEnum;

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
}
