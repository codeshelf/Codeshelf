/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: Path.java,v 1.31 2013/09/18 00:40:08 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.MapKey;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import lombok.Getter;
import lombok.Setter;

import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.annotate.JsonAutoDetect.Visibility;
import org.codehaus.jackson.annotate.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.avaje.ebean.annotation.CacheStrategy;
import com.gadgetworks.codeshelf.model.PositionTypeEnum;
import com.gadgetworks.codeshelf.model.TravelDirectionEnum;
import com.gadgetworks.codeshelf.model.dao.DaoException;
import com.gadgetworks.codeshelf.model.dao.GenericDaoABC;
import com.gadgetworks.codeshelf.model.dao.ISchemaManager;
import com.gadgetworks.codeshelf.model.dao.ITypedDao;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Singleton;

// --------------------------------------------------------------------------
/**
 * Path
 *
 * A collection of PathSegments that make up a path that a ContainerHandler can travel for work.
 *
 * @author jeffw
 */

@Entity
@Table(name = "path")
@CacheStrategy(useBeanCache = true)
@JsonAutoDetect(getterVisibility = Visibility.NONE)
//@ToString(doNotUseGetters = true, exclude = { "parent" })
public class Path extends DomainObjectTreeABC<Facility> {

	@Inject
	public static ITypedDao<Path>	DAO;

	@Singleton
	public static class PathDao extends GenericDaoABC<Path> implements ITypedDao<Path> {
		@Inject
		public PathDao(final ISchemaManager inSchemaManager) {
			super(inSchemaManager);
		}

		public final Class<Path> getDaoClass() {
			return Path.class;
		}
	}

	public static final String			DEFAULT_FACILITY_PATH_ID	= "DEFAULT";
	public static final String			DOMAIN_PREFIX				= "P";

	private static final Logger			LOGGER						= LoggerFactory.getLogger(Path.class);

	// The parent facility.
	@Column(nullable = false)
	@ManyToOne(optional = false)
	@Getter
	@Setter
	private Facility					parent;

	// The path description.
	@Column(nullable = false)
	@Getter
	@Setter
	@JsonProperty
	private String						description;

	@Column(nullable = false)
	@Getter
	@Setter
	@JsonProperty
	private TravelDirectionEnum			travelDirEnum;

	// The work area that goes with this path.
	// It shouldn't be null, but there is no way to create a parent-child relation when neither can be null.
	@OneToOne(mappedBy = "parent")
	@Getter
	@Setter
	private WorkArea					workArea;

	// The computed path length.
	@Setter
	private Double						length;

	// All of the path segments that belong to this path.
	@OneToMany(mappedBy = "parent")
	@MapKey(name = "segmentOrder")
	//	@Getter
	private Map<Integer, PathSegment>	segments					= new HashMap<Integer, PathSegment>();

	public Path() {
		description = "";
	}

	public final ITypedDao<Path> getDao() {
		return DAO;
	}

	public final String getDefaultDomainIdPrefix() {
		return DOMAIN_PREFIX;
	}

	public final List<? extends IDomainObject> getChildren() {
		return new ArrayList<PathSegment>(getSegments());
	}

	public final void addPathSegment(PathSegment inPathSegment) {
		segments.put(inPathSegment.getSegmentOrder(), inPathSegment);
	}

	public final PathSegment getPathSegment(Integer inOrder) {
		return segments.get(inOrder);
	}

	public final void removePathSegment(Integer inOrder) {
		segments.remove(inOrder);
	}

	/**
	 * Class to compare path segments by their order.
	 *
	 */
	private class PathSegmentComparator implements Comparator<PathSegment> {

		private TravelDirectionEnum	mTravelDirectionEnum;

		public PathSegmentComparator(final TravelDirectionEnum inTravelDirectionEnum) {
			mTravelDirectionEnum = inTravelDirectionEnum;
		}

		public int compare(PathSegment inSegment1, PathSegment inSegment2) {
			int result = 0;
			if (inSegment1.getSegmentOrder() < inSegment2.getSegmentOrder()) {
				result = -1;
			} else if (inSegment1.getSegmentOrder() > inSegment2.getSegmentOrder()) {
				result = 1;
			}

			if (mTravelDirectionEnum.equals(TravelDirectionEnum.REVERSE)) {
				result *= -1;
			}

			return result;
		}
	};

	// --------------------------------------------------------------------------
	/**
	 * Get the path segments sorted in order of the path's travel direction.
	 * @return
	 */
	public final SortedSet<PathSegment> getSegments() {
		TreeSet<PathSegment> sorted = new TreeSet<PathSegment>(new PathSegmentComparator(travelDirEnum));
		sorted.addAll(segments.values());
		return sorted;
	}

	// --------------------------------------------------------------------------
	/**
	 * @return
	 */
	//	public final Double getLength() {
	//		// If the length is zero then compute it and save it.
	//		if (length == 0.0) {
	//			for (PathSegment pathSegment : segments.values()) {
	//				length += pathSegment.getLength();
	//			}
	//			try {
	//				Path.DAO.store(this);
	//			} catch (DaoException e) {
	//				LOGGER.error("", e);
	//			}
	//		}
	//
	//		return length;
	//	}

	// --------------------------------------------------------------------------
	/**
	 *  Create the default work area for this path.
	 */
	public final void createDefaultWorkArea() {
		WorkArea tempWorkArea = this.getWorkArea();
		if (tempWorkArea == null) {
			tempWorkArea = new WorkArea();
			tempWorkArea.setParent(this);
			tempWorkArea.setDomainId(this.getDomainId());
			tempWorkArea.setDescription("Default work area");
			try {
				WorkArea.DAO.store(tempWorkArea);
			} catch (DaoException e) {
				LOGGER.error("", e);
			}
		}
	}

	// --------------------------------------------------------------------------
	/**
	 * Compute the length of a line.
	 * @param inPointA
	 * @param inPointB
	 * @return
	 */
	public static Double computeLineLength(final Point inPointA, final Point inPointB) {
		return Math.abs(Math.sqrt((inPointB.getX() - inPointA.getX()) * (inPointB.getX() - inPointA.getX())
				+ (inPointB.getY() - inPointA.getY()) * (inPointB.getY() - inPointA.getY())));
	}

	public final void createPathSegments(List<Point> points) {
		Preconditions.checkArgument(points.size() >= 2, "the segments are made of two or more points");
		// Create the path segment.
		Point previousPoint = null;
		int segmentOrder = 0;
		for (Point point : points) {
			if (previousPoint != null) {
				String baseSegmentId = this.getParent().getDomainId() + "." + PathSegment.DOMAIN_PREFIX;
				this.createPathSegment(baseSegmentId + "." + segmentOrder, this.getParent(), this, segmentOrder, previousPoint, point);
				segmentOrder++;
			}
			previousPoint = point;
		}


	}
	// --------------------------------------------------------------------------
	/**
	 * Create the path segments for the aisle.
	 * @param inAssociatedAisle
	 * @param inXDimMeters
	 * @param inYDimMeters
	 */
	public final void createPathSegments(final Aisle inAssociatedAisle,
		final Double inXDimMeters,
		final Double inYDimMeters,
		final boolean inOpensLowSide) {

		Point endA = null;
		Point startA = null;
		if (inXDimMeters < inYDimMeters) {
			Double xA = inAssociatedAisle.getAnchorPoint().getX() + 2 * inXDimMeters;
			if (inOpensLowSide) {
				xA = inAssociatedAisle.getAnchorPoint().getX() - inXDimMeters;
			}
			startA = new Point(PositionTypeEnum.METERS_FROM_PARENT, xA, inAssociatedAisle.getAnchorPoint().getY(), 0.0);
			endA = new Point(PositionTypeEnum.METERS_FROM_PARENT,
				xA,
				inAssociatedAisle.getAnchorPoint().getY() + inYDimMeters,
				0.0);
		} else {
			Double yA = inAssociatedAisle.getAnchorPoint().getY() + 2 * inYDimMeters;
			if (inOpensLowSide) {
				yA = inAssociatedAisle.getAnchorPoint().getY() - inYDimMeters;
			}
			startA = new Point(PositionTypeEnum.METERS_FROM_PARENT, inAssociatedAisle.getAnchorPoint().getX(), yA, 0.0);
			endA = new Point(PositionTypeEnum.METERS_FROM_PARENT,
				inAssociatedAisle.getAnchorPoint().getX() + inXDimMeters,
				yA,
				0.0);
		}

		// If there are already path segments then create a connecting path to the new ones.
		Integer segmentOrder = 0;
		PathSegment lastSegment = getSegments().last();
		if (lastSegment != null) {
			segmentOrder = lastSegment.getSegmentOrder() + 1;
		}
		String baseSegmentId = inAssociatedAisle.getDomainId() + "." + PathSegment.DOMAIN_PREFIX;
		// Now connect it to the last aisle's path segments.
		if (lastSegment != null) {
			// Figure out which point is closer to the end of the last path.
			if (Path.computeLineLength(startA, lastSegment.getEndPoint()) > Path.computeLineLength(endA, lastSegment.getEndPoint())) {
				Point temp = startA;
				startA = endA;
				endA = temp;
			}
			this.createPathSegment(baseSegmentId + "D", this.getParent(), this, segmentOrder++, lastSegment.getEndPoint(), startA);
		}
		// Create the path segment.
		PathSegment segmentA = createPathSegment(baseSegmentId + "A", inAssociatedAisle, this, segmentOrder++, startA, endA);

		// Link the path segment as the primary path for the aisle and all of its child locations (recursively).
		inAssociatedAisle.setPathSegment(segmentA);
		try {
			LocationABC.DAO.store(inAssociatedAisle);
		} catch (DaoException e) {
			LOGGER.error("", e);
		}
	}

	// --------------------------------------------------------------------------
	/**
	 * Create a path segment for the aisle.
	 * PathSegments can associate with any location, but we limit it to aisles for now.
	 * @param inSegmentId
	 * @param inAssociatedLocation
	 * @param inPath
	 * @param inSegmentOrder
	 * @param inHead
	 * @param inTail
	 */
	public final PathSegment createPathSegment(final String inSegmentId,
		final ILocation inAssociatedLocation,
		final Path inPath,
		final Integer inSegmentOrder,
		final Point inHead,
		final Point inTail) {

		PathSegment result = null;

		// The path segment goes along the longest segment of the aisle.
		result = new PathSegment();
		result.setParent(inPath);
		result.setSegmentOrder(inSegmentOrder);
		result.setDomainId(inSegmentId);
		result.setStartPoint(inHead);
		result.setEndPoint(inTail);
		result.addLocation(inAssociatedLocation);
		try {
			PathSegment.DAO.store(result);
		} catch (DaoException e) {
			LOGGER.error("", e);
		}

		inPath.addPathSegment(result);

		// TODO: REMOVE THIS AS SOON AS WE HAVE THE NEW PATH CREATION TOOL FROM JR AND PAUL.
		inAssociatedLocation.setPathSegment(result);
		try {
			LocationABC.DAO.store((LocationABC) inAssociatedLocation);
		} catch (DaoException e) {
			LOGGER.error("", e);
		}

		// Force a re-computation of the path distance for this path segment.
		result.computePathDistance();

		return result;
	}

	// --------------------------------------------------------------------------
	/**
	 * Is the passed in location on this path?
	 * @return
	 */
	public final Boolean isLocationOnPath(final ILocation<?> inLocation) {
		boolean result = false;

		// There's some weirdness around Ebean CQuery.request.graphContext.beanMap
		// that makes it impossible to search down the graph and then back up for nested classes.
		//		ISubLocation<?> parentLocation = (ISubLocation<?>) inLocation.getParent();
		//		ISubLocation<?> location = parentLocation.getLocation(inLocation.getLocationId());

		Aisle aisle = inLocation.<Aisle> getParentAtLevel(Aisle.class);
		if (aisle != null) {
			PathSegment pathSegment = aisle.getPathSegment();
			if (pathSegment != null) {
				result = this.equals(pathSegment.getParent());
			}
		}
		return result;
	}

	/**
	 * Comparator for WI sorting.
	 *
	 */
	private class WiComparable implements Comparator<WorkInstruction> {

		public int compare(WorkInstruction inWi1, WorkInstruction inWi2) {

			if ((inWi1 == null) && (inWi2 == null)) {
				return 0;
			} else if (inWi2 == null) {
				return -1;
			} else if (inWi1 == null) {
				return 1;
			} else if (inWi1.getPosAlongPath() < inWi2.getPosAlongPath()) {
				return -1;
			} else if (inWi1.getPosAlongPath() > inWi2.getPosAlongPath()) {
				return 1;
			} else {
				return 0;
			}
		}
	}

	// --------------------------------------------------------------------------
	/**
	 * Sort the WIs based on their distance from the path work origin (based on travel direction).
	 * @param inWiList
	 */
	public final void sortWisByDistance(final List<WorkInstruction> inOutWiList) {
		Collections.sort(inOutWiList, new WiComparable());
	}

	/**
	 * @author jeffw
	 *
	 */
	private class LocationsComparable implements Comparator<ILocation> {

		public int compare(ILocation inLoc1, ILocation inLoc2) {

			if ((inLoc1 == null) && (inLoc2 == null)) {
				return 0;
			} else if (inLoc2 == null) {
				return -1;
			} else if (inLoc1 == null) {
				return 1;
			} else if (inLoc1.getPosAlongPath() < inLoc2.getPosAlongPath()) {
				return -1;
			} else if (inLoc1.getPosAlongPath() > inLoc2.getPosAlongPath()) {
				return 1;
			} else {
				return 0;
			}
		}
	}

	// --------------------------------------------------------------------------
	/**
	 * Get all locations on this path of the type requested.
	 * @param inCrosswallWiList
	 * @param inCheLocation
	 * @param inPath
	 * @return
	 */
	public final <T extends ISubLocation> List<T> getLocationsByClass(final Class<? extends ISubLocation> inClassWanted) {

		// First make a list of all the bays on the CHE's path.
		List<T> locations = new ArrayList<T>();

		// Path segments get return in direction order.
		for (PathSegment pathSegment : getSegments()) {
			for (ILocation<?> pathLocation : pathSegment.getLocations()) {
				locations.addAll(pathLocation.<T> getChildrenAtLevel(inClassWanted));
			}
		}

		// Now sort them by path working distance.
		Collections.sort(locations, new LocationsComparable());

		return locations;
	}

	// --------------------------------------------------------------------------
	/**
	 * Get all locations on this path of the requested that are at (or beyond) the specified location.
	 * @param inAtOrPastLocation
	 * @param inClassWanted
	 * @return
	 */
	public final <T extends ISubLocation<?>> List<T> getLocationsByClassAtOrPastLocation(final ILocation<?> inAtOrPastLocation,
		final Class<? extends ISubLocation<?>> inClassWanted) {

		// First make a list of all the bays on the CHE's path.
		List<T> locations = getLocationsByClass(inClassWanted);

		Iterator<T> iterator = locations.iterator();
		while (iterator.hasNext()) {
			T checkLocation = iterator.next();
			if (checkLocation.getPosAlongPath() < inAtOrPastLocation.getPosAlongPath()) {
				iterator.remove();
			}
		}
		return locations;
	}

	// --------------------------------------------------------------------------
	/**
	 * Check if the order header is on this path (at least in one OrderLocation).
	 * @param inOrderDetail
	 * @return
	 */
	public final boolean isOrderOnPath(final OrderHeader inOrderHeader) {
		boolean result = false;

		for (OrderLocation outOrderLoc : inOrderHeader.getOrderLocations()) {
			if ((isLocationOnPath(outOrderLoc.getLocation()) && (outOrderLoc.getActive()))) {
				result = true;
				break;
			}
		}

		return result;
	}
}
