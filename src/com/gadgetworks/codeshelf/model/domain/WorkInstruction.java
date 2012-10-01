/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: WorkInstruction.java,v 1.2 2012/10/01 07:16:28 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.domain;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import lombok.Getter;
import lombok.Setter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.annotate.JsonIgnore;

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
@Table(name = "WORKINSTRUCTION")
@CacheStrategy
public abstract class WorkInstruction extends DomainObjectABC {

	@Inject
	public static WorkInstructionDao	DAO;

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
	@JsonIgnore
	private Facility					parent;

	// Operation.
	@Getter
	@Setter
	@Column(nullable = false)
	private WorkInstructionOpEnum		opEnum;

	// Kind.
	@Getter
	@Setter
	@Column(nullable = false)
	private WorkInstructionPlanEnum		planEnum;

	// Status.
	@Getter
	@Setter
	@Column(nullable = false)
	private WorkInstructionStatusEnum	statusEnum;

	// The subject container.
	@Getter
	@Setter
	@Column(nullable = false)
	private Container					subjectContainer;

	// The subject item.
	@Getter
	@Setter
	@Column(nullable = false)
	private Item						subjectItem;

	// fromLoc.
	@Getter
	@Setter
	@Column(nullable = false)
	private LocationABC					fromLocation;

	// toLoc.
	@Getter
	@Setter
	@Column(nullable = false)
	private LocationABC					toLocation;

	// fromContainer.
	@Getter
	@Setter
	@Column(nullable = false)
	private LocationABC					fromContainer;

	// toContainer.
	@Getter
	@Setter
	@Column(nullable = false)
	private LocationABC					toContainer;

	public WorkInstruction() {

	}

	@JsonIgnore
	public final ITypedDao<WorkInstruction> getDao() {
		return DAO;
	}

	public final String getDefaultDomainIdPrefix() {
		return "WI";
	}

	@JsonIgnore
	public final Facility getParentFacility() {
		return parent;
	}

	public final void setParentFacility(final Facility inFacility) {
		parent = inFacility;
	}

	@JsonIgnore
	public final IDomainObject getParent() {
		return parent;
	}

	public final void setParent(IDomainObject inParent) {
		if (inParent instanceof Facility) {
			setParentFacility((Facility) inParent);
		}
	}

	@JsonIgnore
	public final List<? extends IDomainObject> getChildren() {
		return new ArrayList<IDomainObject>();
	}
}
