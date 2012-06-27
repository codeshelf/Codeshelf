/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: PathSegment.java,v 1.1 2012/06/27 05:07:51 jeffw Exp $
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
public class PathSegment {

	private static final Log	LOGGER	= LogFactory.getLog(PathSegment.class);

	// The owning organization.
	@Column(nullable = false)
	@ManyToOne(optional = false)
	@JsonIgnore
	@Getter
	private Path				parentPath;

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
