/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: PathSegment.java,v 1.4 2012/07/13 21:56:56 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.persist;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import lombok.Getter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.annotate.JsonIgnore;

import com.gadgetworks.codeshelf.model.dao.GenericDao;
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
public class PathSegment extends PersistABC {

	private static final Log	LOGGER	= LogFactory.getLog(PathSegment.class);

	@Singleton
	public static class PathSegmentDao extends GenericDao<PathSegment> implements ITypedDao<PathSegment> {
		public PathSegmentDao() {
			super(PathSegment.class);
		}
	}

	@Inject
	public static ITypedDao<PathSegment> DAO;

	// The owning organization.
	@Column(nullable = false)
	@ManyToOne(optional = false)
	@JsonIgnore
	@Getter
	private Path	parentPath;

	public PathSegment() {
	}

	public final PersistABC getParent() {
		return getParentPath();
	}

	public final void setParent(PersistABC inParent) {
		if (inParent instanceof Path) {
			setParentPath((Path) inParent);
		}
	}

	public final void setParentPath(final Path inParentPath) {
		parentPath = inParentPath;
	}

	public final String getParentPathID() {
		return getParentPath().getDomainId();
	}
}
