/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: Path.java,v 1.3 2012/07/22 20:14:04 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.domain;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import lombok.Getter;
import lombok.Setter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.annotate.JsonIgnore;

import com.gadgetworks.codeshelf.model.dao.GenericDao;
import com.gadgetworks.codeshelf.model.dao.ITypedDao;
import com.google.inject.Inject;
import com.google.inject.Singleton;

// --------------------------------------------------------------------------
/**
 * Path
 * 
 * A collection of PathSegments that make up a path that a ContainerHandler can travel for work.
 * 
 * @author jeffw
 */

@Entity
@Table(name = "PATH")
public class Path extends DomainObjectABC {

	@Inject
	public static ITypedDao<Path>	DAO;

	@Singleton
	public static class PathDao extends GenericDao<Path> implements ITypedDao<Path> {
		public PathDao() {
			super(Path.class);
		}
	}

	private static final Log	LOGGER		= LogFactory.getLog(Path.class);

	// The parent facility.
	@Getter
	@Setter
	@Column(nullable = false)
	private Facility			parentFacility;

	// The path description.
	@Getter
	@Setter
	@Column(nullable = false)
	private String				description;

	// For a network this is a list of all of the users that belong in the set.
	@OneToMany(mappedBy = "parentPath")
	@JsonIgnore
	@Getter
	private List<PathSegment>	segments	= new ArrayList<PathSegment>();

	public Path() {
		description = "";
	}

	public final ITypedDao<Path> getDao() {
		return DAO;
	}

	public final String getDefaultDomainIdPrefix() {
		return "P";
	}

	// --------------------------------------------------------------------------
	/**
	 * Someday, organizations may have other organizations.
	 * @return
	 */
	public final IDomainObject getParent() {
		return parentFacility;
	}

	public final void setParent(IDomainObject inParent) {
		if (inParent instanceof Facility) {
			setParentFacility((Facility) inParent);
		}
	}

	@JsonIgnore
	public final List<? extends IDomainObject> getChildren() {
		return getSegments();
	}

	// Even though we don't really use this field, it's tied to an eBean op that keeps the DB in synch.
	public final void addPathSegment(PathSegment inPathSegment) {
		segments.add(inPathSegment);
	}

	// Even though we don't really use this field, it's tied to an eBean op that keeps the DB in synch.
	public final void removePathSegment(PathSegment inPathSegment) {
		segments.remove(inPathSegment);
	}
}
