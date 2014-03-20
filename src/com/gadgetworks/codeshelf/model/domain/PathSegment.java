/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: PathSegment.java,v 1.33 2013/09/18 00:40:09 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.domain;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.annotate.JsonAutoDetect.Visibility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.avaje.ebean.annotation.CacheStrategy;
import com.gadgetworks.codeshelf.model.PositionTypeEnum;
import com.gadgetworks.codeshelf.model.TravelDirectionEnum;
import com.gadgetworks.codeshelf.model.dao.DaoException;
import com.gadgetworks.codeshelf.model.dao.GenericDaoABC;
import com.gadgetworks.codeshelf.model.dao.ISchemaManager;
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
@Table(name = "path_segment")
@CacheStrategy(useBeanCache = true)
@JsonAutoDetect(getterVisibility = Visibility.NONE)
//@ToString(doNotUseGetters = true, exclude = { "parent" })
public class PathSegment extends DomainObjectTreeABC<Path> {

	@Inject
	public static ITypedDao<PathSegment>	DAO;

	@Singleton
	public static class PathSegmentDao extends GenericDaoABC<PathSegment> implements ITypedDao<PathSegment> {
		@Inject
		public PathSegmentDao(final ISchemaManager inSchemaManager) {
			super(inSchemaManager);
		}

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

	// The order of this path segment in the path (from the tail/origin).
	@NonNull
	@Column(nullable = true)
	@Getter
	@Setter
	private Integer				segmentOrder;

	// The positioning type.
	@NonNull
	@Column(nullable = true)
	@Getter
	@Setter
	private PositionTypeEnum	posTypeEnum;

	@NonNull
	@Column(nullable = true)
	@Getter
	@Setter
	private Double				startPosX;

	@NonNull
	@Column(nullable = true)
	@Getter
	@Setter
	private Double				startPosY;

	@NonNull
	@Column(nullable = true)
	@Getter
	@Setter
	private Double				startPosZ;

	@NonNull
	@Column(nullable = true)
	@Getter
	@Setter
	private Double				endPosX;

	@NonNull
	@Column(nullable = true)
	@Getter
	@Setter
	private Double				endPosY;

	@NonNull
	@Column(nullable = true)
	@Getter
	@Setter
	private Double				endPosZ;

	@Setter
	@Getter
	private Double				startPosAlongPath;

	@Column(nullable = true)
	@OneToMany(mappedBy = "pathSegment")
	@Getter
	private List<LocationABC>	locations		= new ArrayList<LocationABC>();

	public PathSegment() {

	}

	public PathSegment(final Path inParentPath,
		final TravelDirectionEnum inTravelDirectionEnum,
		final Point inBeginPoint,
		final Point inEndPoint) {

		parent = inParentPath;
		posTypeEnum = inBeginPoint.getPosTypeEnum();
		startPosX = inBeginPoint.getX();
		startPosY = inBeginPoint.getY();
		startPosZ = inBeginPoint.getZ();
		endPosX = inEndPoint.getX();
		endPosY = inEndPoint.getY();
		endPosZ = inEndPoint.getZ();
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

	public final void addLocation(ILocation inSubLocation) {
		// Ebean can't deal with interfaces.
		LocationABC subLocation = (LocationABC) inSubLocation;
		locations.add(subLocation);
	}

	public final void removeLocation(ILocation inLocation) {
		locations.remove(inLocation);
	}

	public final List<IDomainObject> getChildren() {
		return new ArrayList<IDomainObject>();
	}

	public final String getParentPathID() {
		return parent.getDomainId();
	}

	public final void setStartPoint(final Point inPoint) {
		posTypeEnum = inPoint.getPosTypeEnum();
		startPosX = inPoint.getX();
		startPosY = inPoint.getY();
		startPosZ = inPoint.getZ();
	}

	public final void setEndPoint(final Point inPoint) {
		posTypeEnum = inPoint.getPosTypeEnum();
		endPosX = inPoint.getX();
		endPosY = inPoint.getY();
		endPosZ = inPoint.getZ();
	}

	public final Point getStartPoint() {
		return new Point(posTypeEnum, startPosX, startPosY, startPosZ);
	}

	public final Point getEndPoint() {
		return new Point(posTypeEnum, endPosX, endPosY, endPosZ);
	}

	public final Double getLength() {
		return Math.sqrt(Math.pow(startPosX - endPosX, 2) + Math.pow(startPosY - endPosY, 2));
	}

	// --------------------------------------------------------------------------
	/**
	 * The distance of this path segment from the path origin.
	 * The first time we get this value we have to compute it.
	 * @return
	 */
	public final void computePathDistance() {
		Double distance = 0.0;
		Path path = getParent();
		for (PathSegment segment : path.getSegments()) {
			if (segment.equals(this)) {
				break;
			}
			distance += Path.computeLineLength(segment.getStartPoint(), segment.getEndPoint());
		}
		startPosAlongPath = distance;

		try {
			PathSegment.DAO.store(this);
		} catch (DaoException e) {
			LOGGER.error("", e);
		}
	}

	// --------------------------------------------------------------------------
	/**
	 * Compute the distance of a point from a line that it's next to.
	 * @param inLinePointA
	 * @param inLinePointB
	 * @param inFromPoint
	 * @return
	 */
	public final Double computeDistanceOfPointFromLine(final Point inLinePointA, final Point inLinePointB, final Point inFromPoint) {
		Double result = 0.0;

		Double k = ((inLinePointB.getY() - inLinePointA.getY()) * (inFromPoint.getX() - inLinePointA.getX()) - (inLinePointB.getX() - inLinePointA.getX())
				* (inFromPoint.getY() - inLinePointA.getY()))
				/ (Math.pow(inLinePointB.getY() - inLinePointA.getY(), 2) + Math.pow(inLinePointB.getX() - inLinePointA.getX(), 2));
		Double x4 = inFromPoint.getX() - k * (inLinePointB.getY() - inLinePointA.getY());
		Double y4 = inFromPoint.getY() + k * (inLinePointB.getX() - inLinePointA.getX());

		result = Math.sqrt(Math.pow(inLinePointA.getX() - x4, 2) + Math.pow(inLinePointA.getY() - y4, 2));

		return result;
	}
}
