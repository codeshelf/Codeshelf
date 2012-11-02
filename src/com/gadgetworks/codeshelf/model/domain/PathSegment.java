/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: PathSegment.java,v 1.15 2012/11/02 03:00:30 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.domain;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.annotate.JsonAutoDetect.Visibility;
import org.codehaus.jackson.annotate.JsonProperty;

import com.avaje.ebean.annotation.CacheStrategy;
import com.gadgetworks.codeshelf.model.PositionTypeEnum;
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
@JsonAutoDetect(getterVisibility = Visibility.NONE)
public class PathSegment extends DomainObjectTreeABC<Path> {

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
	private Path				parent;

	// The path description.
	@Column(nullable = false)
	@Getter
	@Setter
	@JsonProperty
	private LocationABC			associatedLocation;

	// The head's X position.
	@Embedded
	@NonNull
	@Getter
	@Setter
	@JsonProperty
	@AttributeOverrides({ @AttributeOverride(name = "posTypeEnum", column = @Column(name = "HEAD_POSTYPEENUM")), @AttributeOverride(name = "x", column = @Column(name = "HEAD_X")),
			@AttributeOverride(name = "y", column = @Column(name = "HEAD_Y")) })
	private Point				head;

	// The tail's Y position.
	@Embedded
	@NonNull
	@Getter
	@Setter
	@JsonProperty
	@AttributeOverrides({ @AttributeOverride(name = "posTypeEnum", column = @Column(name = "TAIL_POSTYPEENUM")), @AttributeOverride(name = "x", column = @Column(name = "TAIL_X")),
			@AttributeOverride(name = "y", column = @Column(name = "TAIL_Y")) })
	private Point				tail;

	public PathSegment() {

	}

	public PathSegment(final Path inParentPath, final LocationABC<Facility> inLocation, final PositionTypeEnum inPosType, final Point inHead, final Point inTail) {

		associatedLocation = inLocation;
		head = inHead;
		tail = inTail;
	}

	public final ITypedDao<PathSegment> getDao() {
		return DAO;
	}

	public final String getDefaultDomainIdPrefix() {
		return "PS";
	}

	public final Path getParent() {
		return parent;
	}

	public final void setParent(Path inParent) {
		parent = inParent;
	}

	public final List<IDomainObject> getChildren() {
		return new ArrayList<IDomainObject>();
	}

	public final String getParentPathID() {
		return parent.getDomainId();
	}

}
