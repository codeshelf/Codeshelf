/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: WorkArea.java,v 1.6 2012/10/31 16:55:08 jeffw Exp $
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
import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.annotate.JsonAutoDetect.Visibility;
import org.codehaus.jackson.annotate.JsonProperty;

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
@JsonAutoDetect(getterVisibility = Visibility.NONE)
public class WorkArea extends DomainObjectTreeABC<Facility> {

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
	private Facility				parent;

	// The work area ID.
	@Column(nullable = false)
	@Getter
	@Setter
	@JsonProperty
	private String					workAreaId;

	// The work description.
	@Column(nullable = false)
	@Getter
	@Setter
	@JsonProperty
	private String					description;

	// A work area is a collection of locations.
	@OneToMany(mappedBy = "parent")
	@Getter
	private List<SubLocationABC>		locations			= new ArrayList<SubLocationABC>();

	// A work area will contain a set of active users (workers).
	@OneToMany(mappedBy = "parent")
	@Getter
	private List<User>				users				= new ArrayList<User>();

	// A work area will contain a set of active users (workers).
	@OneToMany(mappedBy = "parent")
	@Getter
	private List<WorkInstruction>	workInstructions	= new ArrayList<WorkInstruction>();

	public WorkArea() {
		workAreaId = "";
	}

	public final ITypedDao<WorkArea> getDao() {
		return DAO;
	}

	public final String getDefaultDomainIdPrefix() {
		return "P";
	}

	public final Facility getParent() {
		return parent;
	}

	public final void setParent(Facility inParent) {
		parent = inParent;
	}

	public final List<? extends IDomainObject> getChildren() {
		return getLocations();
	}

	// Even though we don't really use this field, it's tied to an eBean op that keeps the DB in synch.
	public final void addLocation(SubLocationABC inLocation) {
		locations.add(inLocation);
	}

	// Even though we don't really use this field, it's tied to an eBean op that keeps the DB in synch.
	public final void removeLocation(SubLocationABC inLocation) {
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
