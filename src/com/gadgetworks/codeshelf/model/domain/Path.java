/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: Path.java,v 1.9 2012/10/21 02:02:17 jeffw Exp $
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
 * Path
 * 
 * A collection of PathSegments that make up a path that a ContainerHandler can travel for work.
 * 
 * @author jeffw
 */

@Entity
@Table(name = "PATH")
@CacheStrategy
@JsonAutoDetect(getterVisibility = Visibility.NONE)
public class Path extends DomainObjectABC {

	@Inject
	public static ITypedDao<Path>	DAO;

	@Singleton
	public static class PathDao extends GenericDaoABC<Path> implements ITypedDao<Path> {
		public final Class<Path> getDaoClass() {
			return Path.class;
		}
	}

	private static final Log	LOGGER		= LogFactory.getLog(Path.class);

	// The parent facility.
	@Column(nullable = false)
	private Facility			parent;

	// The path description.
	@Column(nullable = false)
	@Getter
	@Setter
	@JsonProperty
	private String				description;

	// For a network this is a list of all of the users that belong in the set.
	@OneToMany(mappedBy = "parent")
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

	public final Facility getParentFacility() {
		return parent;
	}

	public final void setParentFacility(final Facility inFacility) {
		parent = inFacility;
	}

	public final IDomainObject getParent() {
		return parent;
	}

	public final void setParent(IDomainObject inParent) {
		if (inParent instanceof Facility) {
			setParentFacility((Facility) inParent);
		}
	}

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
