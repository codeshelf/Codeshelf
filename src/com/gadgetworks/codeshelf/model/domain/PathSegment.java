/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: PathSegment.java,v 1.33 2013/09/18 00:40:09 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.domain;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Cacheable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.gadgetworks.codeshelf.model.PositionTypeEnum;
import com.gadgetworks.codeshelf.model.dao.GenericDaoABC;
import com.gadgetworks.codeshelf.model.dao.ITypedDao;
import com.gadgetworks.codeshelf.platform.persistence.PersistenceService;
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
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE)
public class PathSegment extends DomainObjectTreeABC<Path> {

	/**
	 * 
	 */
	@SuppressWarnings("unused")
	private static final long		serialVersionUID	= -2776468192822374495L;

	@Inject
	public static PathSegmentDao	DAO;

	@Singleton
	public static class PathSegmentDao extends GenericDaoABC<PathSegment> implements ITypedDao<PathSegment> {
		@Inject
		public PathSegmentDao(final PersistenceService persistenceService) {
			super(persistenceService);
		}

		public final Class<PathSegment> getDaoClass() {
			return PathSegment.class;
		}

	}

	public static final String	DOMAIN_PREFIX	= "SEG";

	private static final Logger	LOGGER			= LoggerFactory.getLogger(PathSegment.class);

	// The owning path.
	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@Getter
	private Path				parent;

	// The order of this path segment in the path (from the tail/origin).
	@NonNull
	@Column(nullable = true, name = "segment_order")
	@Getter
	@Setter
	private Integer				segmentOrder;

	// The positioning type.
	@NonNull
	@Column(nullable = true, name = "pos_type")
	@Getter
	@Setter
	@JsonProperty
	private PositionTypeEnum	posType;

	@NonNull
	@Column(nullable = true, name = "start_pos_x")
	@Getter
	@Setter
	@JsonProperty
	private Double				startPosX;

	@NonNull
	@Column(nullable = true, name = "start_pos_y")
	@Getter
	@Setter
	@JsonProperty
	private Double				startPosY;

	@NonNull
	@Column(nullable = true, name = "start_pos_z")
	@Getter
	@Setter
	@JsonProperty
	private Double				startPosZ;

	@NonNull
	@Column(nullable = true, name = "end_pos_x")
	@Getter
	@Setter
	@JsonProperty
	private Double				endPosX;

	@NonNull
	@Column(nullable = true, name = "end_pos_y")
	@Getter
	@Setter
	@JsonProperty
	private Double				endPosY;

	@NonNull
	@Column(nullable = true, name = "end_pos_z")
	@Getter
	@Setter
	@JsonProperty
	private Double				endPosZ;

	@Column(nullable = true, name = "start_pos_along_path")
	@Setter
	@Getter
	private Double				startPosAlongPath;

	@OneToMany(mappedBy = "pathSegment")
	@Getter
	private List<Location>		locations		= new ArrayList<Location>();

	public PathSegment() {
	}

	@SuppressWarnings("unchecked")
	public final ITypedDao<PathSegment> getDao() {
		return DAO;
	}

	public final String getDefaultDomainIdPrefix() {
		return DOMAIN_PREFIX;
	}

	public void setParent(Path inParent) {
		parent = inParent;
		computePathDistance();
	}

	public Facility getFacility() {
		return getParent().getFacility();
	}

	public String getParentPathID() {
		if (this.parent == null)
			return null;
		return parent.getDomainId();
	}

	public void setStartPoint(final Point inPoint) {
		posType = inPoint.getPosType();
		startPosX = inPoint.getX();
		startPosY = inPoint.getY();
		startPosZ = inPoint.getZ();
	}

	public void setEndPoint(final Point inPoint) {
		posType = inPoint.getPosType();
		endPosX = inPoint.getX();
		endPosY = inPoint.getY();
		endPosZ = inPoint.getZ();
	}

	public Point getStartPoint() {
		return new Point(posType, startPosX, startPosY, startPosZ);
	}

	public Point getEndPoint() {
		return new Point(posType, endPosX, endPosY, endPosZ);
	}

	public Double getLength() {
		return Math.sqrt(Math.pow(startPosX - endPosX, 2) + Math.pow(startPosY - endPosY, 2));
	}

	public void addLocation(Location inLocation) {
		if (inLocation.isFacility()) {
			LOGGER.error("cannot add Facility in addLocation");
			return;
		}

		PathSegment previousSegment = inLocation.getPathSegment();
		if (previousSegment == null) {
			locations.add(inLocation);
			inLocation.setPathSegment(this);
		}
	}

	public void removeLocation(Location location) {
		if (locations.contains(location)) {
			location.setPathSegment(null);
			locations.remove(location);
		}
	}

	// For a UI field
	public int getAssociatedLocationCount() {
		return getLocations().size();
	}

	// --------------------------------------------------------------------------
	/**
	 * The distance of this path segment from the path origin.
	 * The first time we get this value we have to compute it.
	 * @return
	 */
	public void computePathDistance() {
		Double distance = 0.0;
		Path path = getParent();
		for (PathSegment segment : path.getSegments()) {
			if (segment.equals(this)) {
				break;
			}
			distance += Path.computeLineLength(segment.getStartPoint(), segment.getEndPoint());
		}
		startPosAlongPath = distance;
	}

	// --------------------------------------------------------------------------
	/**
	 * X or Y oriented?
	 * (We are normalizing in the facility coordinate system.)
	 * @return
	 */
	private boolean isPathSegmentXOriented() {
		Point startP = this.getStartPoint();
		Point endP = this.getEndPoint();
		Double deltaX = Math.abs(endP.getX() - startP.getX());
		Double deltaY = Math.abs(endP.getY() - startP.getY());
		return deltaX > deltaY;
	}

	// --------------------------------------------------------------------------
	/**
	 * Helper function for computeNormalizedPositionAlongPath
	 * inNormalizedValue comes from the normal dropped from the point to the path segment.
	 * We need to know if this is between the start and end, or beyond. And if beyond, to adopt a value corresponding to the start or end.
	 * Returns the value to add to the path segment's startPosAlongPath to get the point's value.
	 * @param inStart
	 * @param inEnd
	 * @param inNormalizedValue
	 * @return
	 */
	private Double getValueAlongPathSegment(Double inStart, Double inEnd, Double inNormalizedValue) {
		boolean segmentRunningTowardHigherXorY = inEnd > inStart;
		// are we between the start and end if the path segment. This should be usual case
		// careful: path can run either way, so inEnd could have lower value than inStart
		if ((inStart <= inNormalizedValue && inNormalizedValue <= inEnd)
				|| (inEnd <= inNormalizedValue && inNormalizedValue <= inStart)) {
			// normal case
			Double distanceFromStart = Math.abs(inNormalizedValue - inStart);
			return distanceFromStart;
		}
		// Is our value beyond the end point of the path segment. If so, adopt the end point
		else if ((!segmentRunningTowardHigherXorY && inNormalizedValue < inEnd)
				|| (segmentRunningTowardHigherXorY && inNormalizedValue > inEnd)) {
			return Math.abs(inStart - inEnd); // That is, add the full length of this segment along this coordinate.
		}
		// Our value is beyond  the starting point of the path segment. 
		// Therefore, just taking the starting posAlongPath of the segment. That is, the value to add to that is zero.
		else {
			return 0.0;
		}
	}

	// --------------------------------------------------------------------------
	/**
	 * Compute the distance along the path segment, if you drop a normal from the point to the path segment.
	 * If beyond the end, just take the value of the end of the path segment.
	 * @param inFromPoint
	 * @return
	 */
	public Double computeNormalizedPositionAlongPath(Point inFromPoint) {
		Double distance = getStartPosAlongPath(); // initial value is start of this path segment
		boolean xOrientedSegment = isPathSegmentXOriented();
		Point startP = this.getStartPoint();
		Point endP = this.getEndPoint();
		Double deltaFromStartOfSegment = 0.0;

		// Obviously makes the X or Y assumption here. If we need to handle angled aisles and paths, this algorithm can still work; just some fancy trig.
		if (xOrientedSegment)
			deltaFromStartOfSegment = getValueAlongPathSegment(startP.getX(), endP.getX(), inFromPoint.getX());
		else
			deltaFromStartOfSegment = getValueAlongPathSegment(startP.getY(), endP.getY(), inFromPoint.getY());

		return distance + deltaFromStartOfSegment;
	}

	public static void setDao(PathSegmentDao inPathSegmentDao) {
		PathSegment.DAO = inPathSegmentDao;
	}

}
