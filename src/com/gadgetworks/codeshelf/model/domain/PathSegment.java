/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: PathSegment.java,v 1.23 2013/03/04 04:47:28 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.domain;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.annotate.JsonAutoDetect.Visibility;
import org.codehaus.jackson.annotate.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.avaje.ebean.annotation.CacheStrategy;
import com.gadgetworks.codeshelf.model.PathDirectionEnum;
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
@Table(name = "PATHSEGMENT", schema = "CODESHELF")
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

	public static final String	DOMAIN_PREFIX	= "SEG";

	private static final Logger	LOGGER			= LoggerFactory.getLogger(PathSegment.class);

	// The owning organization.
	@Column(nullable = false)
	@ManyToOne(optional = false)
	private Path				parent;

	// The path description.
	@Column(nullable = true)
	@ManyToOne(optional = true)
	@Getter
	@Setter
	@JsonProperty
	private LocationABC			associatedLocation;

	@NonNull
	@Getter
	@Setter
	private Integer				segmentOrder;

	@NonNull
	@Getter
	@Setter
	private PathDirectionEnum	directionEnum;

	// The head position.
	@NonNull
	@Getter
	@Setter
	private PositionTypeEnum	headPosTypeEnum;

	@NonNull
	@Getter
	@Setter
	private Double				headPosX;

	@NonNull
	@Getter
	@Setter
	private Double				headPosY;

	// The tail position.
	@NonNull
	@Getter
	@Setter
	private PositionTypeEnum	tailPosTypeEnum;

	@NonNull
	@Getter
	@Setter
	private Double				tailPosX;

	@NonNull
	@Getter
	@Setter
	private Double				tailPosY;

	public PathSegment() {

	}

	public PathSegment(final Path inParentPath,
		final LocationABC<Facility> inLocation,
		final PathDirectionEnum inDirectionEnum,
		final PositionTypeEnum inPosType,
		final Point inHead,
		final Point inTail) {

		associatedLocation = inLocation;
		directionEnum = inDirectionEnum;
		headPosTypeEnum = inHead.getPosTypeEnum();
		headPosX = inHead.getX();
		headPosY = inHead.getY();
		tailPosTypeEnum = inTail.getPosTypeEnum();
		tailPosX = inTail.getX();
		tailPosY = inTail.getY();
	}

	public final ITypedDao<PathSegment> getDao() {
		return DAO;
	}

	public final String getDefaultDomainIdPrefix() {
		return DOMAIN_PREFIX;
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

	public final void setHeadPoint(final Point inPoint) {
		headPosTypeEnum = inPoint.getPosTypeEnum();
		headPosX = inPoint.getX();
		headPosY = inPoint.getY();
	}

	public final void setTailPoint(final Point inPoint) {
		tailPosTypeEnum = inPoint.getPosTypeEnum();
		tailPosX = inPoint.getX();
		tailPosY = inPoint.getY();
	}

	public final Point getHead() {
		return new Point(headPosTypeEnum, headPosX, headPosY, null);
	}

	public final Point getTail() {
		return new Point(tailPosTypeEnum, tailPosX, tailPosY, null);
	}

	public final Double getLength() {
		return Math.sqrt(Math.pow(headPosX - tailPosX, 2) + Math.pow(headPosY - tailPosY, 2));
	}
}
