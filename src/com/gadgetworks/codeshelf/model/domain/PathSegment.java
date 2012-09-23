/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: PathSegment.java,v 1.9 2012/09/23 03:05:42 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.domain;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

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
 * PathSegment
 * 
 * A PathSegment is how a ContainerHandler can move across a location (aisle, etc.) to process the work in a particular order.
 * 
 * @author jeffw
 */

@Entity
@Table(name = "PATHSEGMENT")
@CacheStrategy
public class PathSegment extends DomainObjectABC {

	@Inject
	public static ITypedDao<PathSegment>	DAO;

	@Singleton
	public static class PathSegmentDao extends GenericDaoABC<PathSegment> implements ITypedDao<PathSegment> {
		public final Class<PathSegment> getDaoClass() {
			return PathSegment.class;
		}
	}

	private static final Log	LOGGER	= LogFactory.getLog(PathSegment.class);

	// The owning organization.
	@Column(nullable = false)
	@ManyToOne(optional = false)
	@JsonIgnore
	private Path				parent;

	public PathSegment() {
	}

	@JsonIgnore
	public final ITypedDao<PathSegment> getDao() {
		return DAO;
	}

	public final String getDefaultDomainIdPrefix() {
		return "PS";
	}

	public final Path getParentPath() {
		return parent;
	}

	public final void setParentPath(final Path inPath) {
		parent = inPath;
	}

	public final IDomainObject getParent() {
		return getParentPath();
	}

	public final void setParent(IDomainObject inParent) {
		if (inParent instanceof Path) {
			setParentPath((Path) inParent);
		}
	}

	@JsonIgnore
	public final List<IDomainObject> getChildren() {
		return new ArrayList<IDomainObject>();
	}

	public final String getParentPathID() {
		return getParentPath().getDomainId();
	}

}
