/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: PathSegment.java,v 1.3 2012/07/22 20:14:04 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.domain;

import java.util.ArrayList;
import java.util.List;

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
public class PathSegment extends DomainObjectABC implements IDomainObject {

	@Inject
	public static ITypedDao<PathSegment>	DAO;

	@Singleton
	public static class PathSegmentDao extends GenericDao<PathSegment> implements ITypedDao<PathSegment> {
		public PathSegmentDao() {
			super(PathSegment.class);
		}
	}

	private static final Log	LOGGER	= LogFactory.getLog(PathSegment.class);

	// The owning organization.
	@Column(nullable = false)
	@ManyToOne(optional = false)
	@JsonIgnore
	@Getter
	private Path				parentPath;

	public PathSegment() {
	}

	public final ITypedDao<PathSegment> getDao() {
		return DAO;
	}

	public final String getDefaultDomainIdPrefix() {
		return "PS";
	}

	public final IDomainObject getParent() {
		return getParentPath();
	}

	public final void setParent(IDomainObject inParent) {
		if (inParent instanceof Path) {
			setParentPath((Path) inParent);
		}
	}

	public final void setParentPath(final Path inParentPath) {
		parentPath = inParentPath;
	}

	@JsonIgnore
	public final List<IDomainObject> getChildren() {
		return new ArrayList<IDomainObject>();
	}

	public final String getParentPathID() {
		return getParentPath().getDomainId();
	}

}
