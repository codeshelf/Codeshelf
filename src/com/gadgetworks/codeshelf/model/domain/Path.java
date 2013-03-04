/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: Path.java,v 1.17 2013/03/04 04:47:27 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.domain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.MapKey;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import lombok.Getter;
import lombok.Setter;

import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.annotate.JsonAutoDetect.Visibility;
import org.codehaus.jackson.annotate.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.avaje.ebean.annotation.CacheStrategy;
import com.gadgetworks.codeshelf.model.dao.DaoException;
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
@Table(name = "PATH", schema = "CODESHELF")
@CacheStrategy
@JsonAutoDetect(getterVisibility = Visibility.NONE)
public class Path extends DomainObjectTreeABC<Facility> {

	@Inject
	public static ITypedDao<Path>	DAO;

	@Singleton
	public static class PathDao extends GenericDaoABC<Path> implements ITypedDao<Path> {
		public final Class<Path> getDaoClass() {
			return Path.class;
		}
	}

	public static final String			DEFAULT_FACILITY_PATH_ID	= "DEFAULT";
	public static final String			DOMAIN_PREFIX				= "P";

	private static final Logger			LOGGER						= LoggerFactory.getLogger(Path.class);

	// The parent facility.
	@Column(nullable = false)
	@ManyToOne(optional = false)
	@Getter
	@Setter
	private Facility					parent;

	// The path description.
	@Column(nullable = false)
	@Getter
	@Setter
	@JsonProperty
	private String						description;

	// The work area that goes with this path.
	// It shouldn't be null, but there is no way to create a parent-child relation when neither can be null.
	@OneToOne(mappedBy = "parent")
	@Getter
	@Setter
	private WorkArea					workArea;

	// All of the path segments that belong to this path.
	@OneToMany(mappedBy = "parent")
	@MapKey(name = "segmentOrder")
	@Getter
	private Map<Integer, PathSegment>	segments					= new HashMap<Integer, PathSegment>();

	public Path() {
		description = "";
	}

	public final ITypedDao<Path> getDao() {
		return DAO;
	}

	public final String getDefaultDomainIdPrefix() {
		return DOMAIN_PREFIX;
	}

	public final List<? extends IDomainObject> getChildren() {
		return new ArrayList<PathSegment>(getSegments().values());
	}

	public final void addPathSegment(PathSegment inPathSegment) {
		segments.put(inPathSegment.getSegmentOrder(), inPathSegment);
	}

	public final PathSegment getPathSegment(Integer inOrder) {
		return segments.get(inOrder);
	}

	public final void removePathSegment(Integer inOrder) {
		segments.remove(inOrder);
	}

	// --------------------------------------------------------------------------
	/**
	 *  Create the default work area for this path.
	 */
	public final void createDefaultWorkArea() {
		WorkArea tempWorkArea = this.getWorkArea();
		if (tempWorkArea == null) {
			tempWorkArea = new WorkArea();
			tempWorkArea.setParent(this);
			tempWorkArea.setDomainId(this.getDomainId());
			tempWorkArea.setDescription("Default work area");
			try {
				WorkArea.DAO.store(tempWorkArea);
			} catch (DaoException e) {
				LOGGER.error("", e);
			}
		}
	}
}
