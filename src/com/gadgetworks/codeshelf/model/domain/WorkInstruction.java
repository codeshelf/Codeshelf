/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: WorkInstruction.java,v 1.8 2012/12/25 10:48:13 jeffw Exp $
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.annotate.JsonAutoDetect.Visibility;
import org.codehaus.jackson.annotate.JsonProperty;

import com.avaje.ebean.annotation.CacheStrategy;
import com.gadgetworks.codeshelf.model.WorkInstructionOpEnum;
import com.gadgetworks.codeshelf.model.WorkInstructionPlanEnum;
import com.gadgetworks.codeshelf.model.WorkInstructionStatusEnum;
import com.gadgetworks.codeshelf.model.dao.GenericDaoABC;
import com.gadgetworks.codeshelf.model.dao.ITypedDao;
import com.google.inject.Inject;
import com.google.inject.Singleton;

// --------------------------------------------------------------------------
/**
 * WorkInstruction
 * 
 * A unit of work to move an item to/from a container, or a container to/from a location
 * 
 * @author jeffw
 */

@Entity
@Table(name = "WORKINSTRUCTION", schema = "CODESHELF")
@CacheStrategy
@JsonAutoDetect(getterVisibility = Visibility.NONE)
public abstract class WorkInstruction extends DomainObjectTreeABC<WorkArea> {

	@Inject
	public static ITypedDao<WorkInstruction>	DAO;

	@Singleton
	public static class WorkInstructionDao extends GenericDaoABC<WorkInstruction> implements ITypedDao<WorkInstruction> {
		public final Class<WorkInstruction> getDaoClass() {
			return WorkInstruction.class;
		}
	}

	private static final Log			LOGGER	= LogFactory.getLog(WorkInstruction.class);

	// The parent facility.
	@Column(nullable = false)
	@ManyToOne(optional = false)
	private WorkArea					parent;

	// Operation.
	@Column(nullable = false)
	@Enumerated(value = EnumType.STRING)
	@Getter
	@Setter
	@JsonProperty
	private WorkInstructionOpEnum		opEnum;

	// Kind.
	@Column(nullable = false)
	@Enumerated(value = EnumType.STRING)
	@Getter
	@Setter
	@JsonProperty
	private WorkInstructionPlanEnum		planEnum;

	// Status.
	@Column(nullable = false)
	@Enumerated(value = EnumType.STRING)
	@Getter
	@Setter
	@JsonProperty
	private WorkInstructionStatusEnum	statusEnum;

	// The subject container.
	@Column(nullable = false)
	@Getter
	@Setter
	@JsonProperty
	private Container					subjectContainer;

	// The subject item.
	@Column(nullable = false)
	@Getter
	@Setter
	@JsonProperty
	private Item						subjectItem;

	// fromLoc.
	@Column(nullable = false)
	@Getter
	@Setter
	@JsonProperty
	private LocationABC					fromLocation;

	// toLoc.
	@Column(nullable = false)
	@Getter
	@Setter
	@JsonProperty
	private LocationABC					toLocation;

	// fromContainer.
	@Column(nullable = false)
	@Getter
	@Setter
	@JsonProperty
	private LocationABC					fromContainer;

	// toContainer.
	@Column(nullable = false)
	@Getter
	@Setter
	@JsonProperty
	private LocationABC					toContainer;

	public WorkInstruction() {

	}

	public final ITypedDao<WorkInstruction> getDao() {
		return DAO;
	}

	public final String getDefaultDomainIdPrefix() {
		return "WI";
	}

	public final WorkArea getParent() {
		return parent;
	}

	public final void setParent(WorkArea inParent) {
		parent = inParent;
	}

	public final List<? extends IDomainObject> getChildren() {
		return new ArrayList<IDomainObject>();
	}
}
