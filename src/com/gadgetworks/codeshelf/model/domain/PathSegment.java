/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: PathSegment.java,v 1.17 2012/11/03 03:24:35 jeffw Exp $
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
	@ManyToOne(optional = false)
	@Getter
	@Setter
	@JsonProperty
	private LocationABC			associatedLocation;

	// The head's X position.
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

	// The tail's Y position.
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

	public PathSegment(final Path inParentPath, final LocationABC<Facility> inLocation, final PositionTypeEnum inPosType, final Point inHead, final Point inTail) {

		associatedLocation = inLocation;
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

}
