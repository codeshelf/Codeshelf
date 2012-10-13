/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: WorkArea.java,v 1.3 2012/10/13 22:14:24 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.domain;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import lombok.Getter;
import lombok.Setter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.annotate.JsonIgnore;

import com.avaje.ebean.annotation.CacheStrategy;
import com.gadgetworks.codeshelf.model.dao.GenericDaoABC;
import com.gadgetworks.codeshelf.model.dao.ITypedDao;
import com.google.inject.Inject;
import com.google.inject.Singleton;

// --------------------------------------------------------------------------
/**
 * WorkArea
 * 
 * A collection of locations where a worker (user) executes work instructions.
 * 
 * @author jeffw
 */

@Entity
@Table(name = "WORKAREA")
@CacheStrategy
public class WorkArea extends DomainObjectABC {

	@Inject
	public static ITypedDao<WorkArea>	DAO;

	@Singleton
	public static class WorkAreaDao extends GenericDaoABC<WorkArea> implements ITypedDao<WorkArea> {
		public final Class<WorkArea> getDaoClass() {
			return WorkArea.class;
		}
	}

	private static final Log		LOGGER				= LogFactory.getLog(WorkArea.class);

	// The parent facility.
	@Column(nullable = false)
	@ManyToOne(optional = false)
	@JsonIgnore
	private Facility				parent;

	// The work area ID.
	@Getter
	@Setter
	@Column(nullable = false)
	private String					workAreaId;

	// The work description.
	@Getter
	@Setter
	@Column(nullable = false)
	private String					description;

	// A work area is a collection of locations.
	@OneToMany(mappedBy = "parent")
	@JsonIgnore
	@Getter
	private List<LocationABC>		locations			= new ArrayList<LocationABC>();

	// A work area will contain a set of active users (workers).
	@OneToMany(mappedBy = "parent")
	@JsonIgnore
	@Getter
	private List<User>				users				= new ArrayList<User>();

	// A work area will contain a set of active users (workers).
	@OneToMany(mappedBy = "parent")
	@JsonIgnore
	@Getter
	private List<WorkInstruction>	workInstructions	= new ArrayList<WorkInstruction>();

	public WorkArea() {
		workAreaId = "";
	}

	@JsonIgnore
	public final ITypedDao<WorkArea> getDao() {
		return DAO;
	}

	public final String getDefaultDomainIdPrefix() {
		return "P";
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
		return getLocations();
	}

	// Even though we don't really use this field, it's tied to an eBean op that keeps the DB in synch.
	public final void addLocation(LocationABC inLocation) {
		locations.add(inLocation);
	}

	// Even though we don't really use this field, it's tied to an eBean op that keeps the DB in synch.
	public final void removeLocation(LocationABC inLocation) {
		locations.remove(inLocation);
	}

	// Even though we don't really use this field, it's tied to an eBean op that keeps the DB in synch.
	public final void addWorkInstruction(WorkInstruction inWorkInstruction) {
		workInstructions.add(inWorkInstruction);
	}

	// Even though we don't really use this field, it's tied to an eBean op that keeps the DB in synch.
	public final void removeWorkInstruction(WorkInstruction inWorkInstruction) {
		workInstructions.remove(inWorkInstruction);
	}

	// Even though we don't really use this field, it's tied to an eBean op that keeps the DB in synch.
	public final void addUser(User inUser) {
		users.add(inUser);
	}

	// Even though we don't really use this field, it's tied to an eBean op that keeps the DB in synch.
	public final void removeUser(User inUser) {
		users.remove(inUser);
	}
}
