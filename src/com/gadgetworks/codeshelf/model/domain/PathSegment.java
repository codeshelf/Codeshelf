/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: PathSegment.java,v 1.27 2013/03/15 14:57:13 jeffw Exp $
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
import lombok.ToString;

import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.annotate.JsonAutoDetect.Visibility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.avaje.ebean.annotation.CacheStrategy;
import com.gadgetworks.codeshelf.model.PositionTypeEnum;
import com.gadgetworks.codeshelf.model.TravelDirectionEnum;
import com.gadgetworks.codeshelf.model.dao.DaoException;
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
@CacheStrategy(useBeanCache = true)
@JsonAutoDetect(getterVisibility = Visibility.NONE)
@ToString(doNotUseGetters = true, exclude = { "parent" })
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

	// The order of this path segment in the path (from the tail/origin).
	@NonNull
	@Getter
	@Setter
	private Integer				segmentOrder;

	// The positioning type.
	@NonNull
	@Getter
	@Setter
	private PositionTypeEnum	posTypeEnum;

	@NonNull
	@Getter
	@Setter
	private Double				startPosX;

	@NonNull
	@Getter
	@Setter
	private Double				startPosY;

	@NonNull
	@Getter
	@Setter
	private Double				endPosX;

	@NonNull
	@Getter
	@Setter
	private Double				endPosY;

	@Setter
	@Getter
	private Double				pathDistance;

	@Column(nullable = false)
	@ManyToOne(optional = false)
	@NonNull
	@Getter
	@Setter
	private LocationABC			anchorLocation;

	@Column(nullable = true)
	@OneToMany(mappedBy = "pathSegment")
	@Getter
	private List<LocationABC>	locations		= new ArrayList<LocationABC>();

	public PathSegment() {

	}

	public PathSegment(final Path inParentPath, final TravelDirectionEnum inTravelDirectionEnum, final PositionTypeEnum inPosType, final Point inBeginPoint, final Point inEndPoint) {

		posTypeEnum = inBeginPoint.getPosTypeEnum();
		startPosX = inBeginPoint.getX();
		startPosY = inBeginPoint.getY();
		endPosX = inEndPoint.getX();
		endPosY = inEndPoint.getY();
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

	public final void addLocation(LocationABC inLocation) {
		locations.add(inLocation);
	}

	public final void removeLocation(LocationABC inLocation) {
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
	}

	public final void setEndPoint(final Point inPoint) {
		posTypeEnum = inPoint.getPosTypeEnum();
		endPosX = inPoint.getX();
		endPosY = inPoint.getY();
	}

	public final Point getStartPoint() {
		return new Point(posTypeEnum, startPosX, startPosY, null);
	}

	public final Point getEndPoint() {
		return new Point(posTypeEnum, endPosX, endPosY, null);
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
			distance += segment.computeLineLength(segment.getStartPoint(), segment.getEndPoint());
		}
		pathDistance = distance;

		try {
			PathSegment.DAO.store(this);
		} catch (DaoException e) {
			LOGGER.error("", e);
		}
	}

	// --------------------------------------------------------------------------
	/**
	 * Compute the length of a line.
	 * @param inPointA
	 * @param inPointB
	 * @return
	 */
	public final Double computeLineLength(final Point inPointA, final Point inPointB) {
		Double result = 0.0;

		return result;
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
