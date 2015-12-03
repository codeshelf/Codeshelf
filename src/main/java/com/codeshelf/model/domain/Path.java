/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: Path.java,v 1.31 2013/09/18 00:40:08 jeffw Exp $
 *******************************************************************************/
package com.codeshelf.model.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.persistence.Cacheable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.MapKey;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import lombok.Getter;
import lombok.Setter;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.model.TravelDirectionEnum;
import com.codeshelf.model.dao.DaoException;
import com.codeshelf.model.dao.GenericDaoABC;
import com.codeshelf.model.dao.ITypedDao;
import com.codeshelf.persistence.TenantPersistenceService;
import com.codeshelf.util.CompareNullChecker;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;

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
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE)
public class Path extends DomainObjectTreeABC<Facility> {

	public static class PathDao extends GenericDaoABC<Path> implements ITypedDao<Path> {
		public final Class<Path> getDaoClass() {
			return Path.class;
		}
	}

	public static final String			DEFAULT_FACILITY_PATH_ID	= "DEFAULT";
	public static final String			DOMAIN_PREFIX				= "P";

	private static final Logger			LOGGER						= LoggerFactory.getLogger(Path.class);

	// The path description.
	@Column(nullable = false)
	@Getter
	@Setter
	@JsonProperty
	private String						description;

	@Column(nullable = false,name="travel_dir")
	@Getter
	@Setter
	@JsonProperty
	private TravelDirectionEnum			travelDir;

	// The work area that goes with this path.
	// It shouldn't be null, but there is no way to create a parent-child relation when neither can be null.
	@OneToOne(mappedBy = "parent",fetch=FetchType.LAZY, orphanRemoval=true)
	@Getter
	private WorkArea					workArea;

	// The computed path length.
	@Setter
	private Double						length;

	// All of the path segments that belong to this path.95
	@OneToMany(mappedBy = "parent", orphanRemoval=true)
	@MapKey(name = "segmentOrder")
	private Map<Integer, PathSegment>	segments					= new HashMap<Integer, PathSegment>();

	public Path() {
		description = "";
		travelDir = TravelDirectionEnum.FORWARD;
	}

	@SuppressWarnings("unchecked")
	public final ITypedDao<Path> getDao() {
		return Path.staticGetDao();
	}

	public static ITypedDao<Path> staticGetDao() {
		return TenantPersistenceService.getInstance().getDao(Path.class);
	}

	public final String getDefaultDomainIdPrefix() {
		return DOMAIN_PREFIX;
	}

	public List<? extends IDomainObject> getChildren() {
		return new ArrayList<PathSegment>(getSegments());
	}

	public void addPathSegment(PathSegment inPathSegment) {
		Path previousPath = inPathSegment.getParent();
		if(previousPath == null) {
			segments.put(inPathSegment.getSegmentOrder(), inPathSegment);
			inPathSegment.setParent(this);
		} else if (previousPath!=this) {
			LOGGER.error("cannot add PathSegment "+inPathSegment.getDomainId()+" to "+this.getDomainId()+" because it has not been removed from "+previousPath.getDomainId(), new Exception());
		}	
	}

	public PathSegment getPathSegment(Integer inOrder) {
		return segments.get(inOrder);
	}

	public void removePathSegment(Integer inOrder) {
		PathSegment pathSegment = this.getPathSegment(inOrder);
		if(pathSegment != null) {
			pathSegment.setParent(null);
			segments.remove(inOrder);
		} else {
			LOGGER.error("cannot remove PathSegment "+inOrder+" from "+this.getDomainId()+" because it isn't found in children", new Exception());
		}
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
	public SortedSet<PathSegment> getSegments() {
		TreeSet<PathSegment> sorted = new TreeSet<PathSegment>(new PathSegmentComparator(travelDir));
		for(PathSegment segment : segments.values()) {
			sorted.add(segment);
		}
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
	//				Path.staticGetDao().store(this);
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
	public void createDefaultWorkArea() {
		WorkArea tempWorkArea = this.getWorkArea();
		if (tempWorkArea == null) {
			tempWorkArea = new WorkArea();
			tempWorkArea.setDomainId(this.getDomainId());
			tempWorkArea.setDescription("Default work area");
			this.setWorkArea(tempWorkArea);
			try {
				WorkArea.staticGetDao().store(tempWorkArea);
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
	public PathSegment createPathSegment(final Integer inSegmentOrder,
		final Point inHead,
		final Point inTail) {

		/* TODO a zero distance path segment isn't useful and unrealistic
		if (inHead.equals(inTail)) {
			throw new IllegalArgumentException("inHead and inPath points should not be the same");
		}
		*/
		String segmentDomainId = this.getDomainId() + "." + inSegmentOrder;

		// The path segment goes along the longest segment of the aisle.
		PathSegment result = new PathSegment();
		result.setSegmentOrder(inSegmentOrder);
		result.setDomainId(segmentDomainId);
		result.setStartPoint(inHead);
		result.setEndPoint(inTail);
		this.addPathSegment(result);
		try {
			PathSegment.staticGetDao().store(result);
		} catch (DaoException e) {
			LOGGER.error("Failed to store PathSegment", e);
		}
		return result;
	}

	// --------------------------------------------------------------------------
	/**
	 * Is the passed in location on this path?  Important: deleted location is not on path
	 * @return
	 */
	public Boolean isLocationOnPath(final Location inLocation) {
		boolean result = false;

		// There's some weirdness around Ebean CQuery.request.graphContext.beanMap
		// that makes it impossible to search down the graph and then back up for nested classes.
		//		LocationABC parentLocation = (LocationABC) inLocation.getParent();
		//		LocationABC location = parentLocation.getLocation(inLocation.getLocationId());
		if (!inLocation.isActive()) {
			// Note: if we had to report out on otherwise good order locations or items, then we could still do the code below 
			// here and if satisfied log the warning or generate a business event.
			return false;
		}

		Aisle aisle = inLocation.<Aisle> getParentAtLevel(Aisle.class);
		if (aisle != null) {
			PathSegment pathSegment = aisle.getAssociatedPathSegment();
			if (pathSegment != null) {
				result = this.equals(pathSegment.getParent());
			}
		}
		return result;
	}

	/**
	 * Comparator for WI sorting. This is identical to CheDeviceLogic.WiDistanceComparator
	 *
	 */
	private class WiComparable implements Comparator<WorkInstruction> {

		public int compare(WorkInstruction inWi1, WorkInstruction inWi2) {
			int value = CompareNullChecker.compareNulls(inWi1, inWi2);
			if (value != 0)
				return value;

			Double wi1Pos = inWi1.getPosAlongPath();
			Double wi2Pos = inWi2.getPosAlongPath();
			value = CompareNullChecker.compareNulls(wi1Pos, wi2Pos);
			if (value != 0)
				return value;

			return wi1Pos.compareTo(wi2Pos);
		}

	}

	// --------------------------------------------------------------------------
	/**
	 * Sort the WIs based on their distance from the path work origin (based on travel direction).
	 * @param inWiList
	 */
	public void sortWisByDistance(final List<WorkInstruction> inOutWiList) {
		Collections.sort(inOutWiList, new WiComparable());
	}

	/**
	 * @author jeffw
	 *
	 */
	private class LocationsComparable implements Comparator<Location> {

		public int compare(Location inLoc1, Location inLoc2) {

			if ((inLoc1 == null) && (inLoc2 == null)) {
				return 0;
			} else if (inLoc2 == null) {
				return -1;
			} else if (inLoc1 == null) {
				return 1;
			}
			// posAlongPath field is double, which may be null if the value was never set.
			else if (inLoc1.getPosAlongPath() == null || inLoc2.getPosAlongPath() == null) {
				LOGGER.error("posAlongPath value null"); // could output the location name
				return 0; // not sure this makes sense.
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
	public <T extends Location> List<T> getLocationsByClass(final Class<? extends Location> inClassWanted) {

		// First make a list of all the bays on the CHE's path.
		List<T> locations = new ArrayList<T>();

		// Path segments get return in direction order.
		for (PathSegment pathSegment : getSegments()) {
			List<Location> segmentLocations = pathSegment.getLocations();
			for (Location pathLocation : segmentLocations) {
				locations.addAll(pathLocation.<T> getActiveChildrenAtLevel(inClassWanted));
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
	public <T extends Location> List<T> getLocationsByClassAtOrPastLocation(final Location inAtOrPastLocation,
		final Class<? extends Location> inClassWanted) {

		if (inAtOrPastLocation.getPosAlongPath() == null) {
			LOGGER.error("null posAlongPath in getLocationsByClassAtOrPastLocation #1");
			return null; // is this right? or should it return an empty list?
		}

		// First make a list of all the bays on the CHE's path.
		List<T> locations = getLocationsByClass(inClassWanted);

		Iterator<T> iterator = locations.iterator();
		while (iterator.hasNext()) {
			T checkLocation = iterator.next();
			if (checkLocation.getPosAlongPath() == null) {
				LOGGER.error("null posAlongPath in getLocationsByClassAtOrPastLocation #2");
			} else if (checkLocation.getPosAlongPath() < inAtOrPastLocation.getPosAlongPath()) {
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
	public boolean isOrderOnPath(final OrderHeader inOrderHeader) {
		boolean result = false;

		for (OrderLocation outOrderLoc : inOrderHeader.getActiveOrderLocations()) {
			if (isLocationOnPath(outOrderLoc.getLocation())) {
				result = true;
				break;
			}
		}

		return result;
	}

	// For a UI field
	public int getAssociatedLocationCount() {
		int returnInt = 0;
		for (PathSegment segment : this.getSegments()) {
			returnInt += segment.getAssociatedLocationCount();
		}
		return returnInt;
	}

	// --------------------------------------------------------------------------
	/**
	 * Delete the path and its segments. Called from the UI
	 * @return
	 */
	public void deleteThisPath() {

		for (PathSegment segment : this.getSegments()) {
			// make sure segment is not associated to a location			
			for (Location location : segment.getLocations()) {
				if (location.getAssociatedPathSegment().equals(segment)) {
					LOGGER.debug("clearing path segment association");
					location = TenantPersistenceService.<Location>deproxify(location);
					location.setPathSegment(null);
					location.getDao().store(location);
				}
			}
			// delete the segment
			PathSegment.staticGetDao().delete(segment);
		}

		// delete the work area
		WorkArea wa = this.getWorkArea();
		if (wa != null) {
			this.setWorkArea(null);
			Path.staticGetDao().store(this);
			WorkArea.staticGetDao().delete(wa);
		}
		// then delete this path
		Path.staticGetDao().delete(this);
	}

	@Override
	public Facility getFacility() {
		return getParent();
	}

	public void setWorkArea(WorkArea inWorkArea) {
		if(inWorkArea != null) {
			// attach work area
			Path previousPath = inWorkArea.getParent();
			if(previousPath == null) {
				inWorkArea.setParent(this);
				this.workArea = inWorkArea;
			} else if(!previousPath.equals(this)) {
				LOGGER.error("cannot attach WorkArea "+inWorkArea.getDomainId()+" because it is attached to "+previousPath.getDomainId());
			}
		} else if(this.workArea != null) {
			// detach work area (set to null)
			this.workArea.setParent(null);
			this.workArea = null;
		}
	}
	
	public String getPathScript() {
		SortedSet<PathSegment> segments = getSegments();
		StringBuilder script = new StringBuilder();
		for (PathSegment segment : segments) {
			script.append(String.format("- %.2f %.2f %.2f %.2f ", segment.getStartPosX(), segment.getStartPosY(), segment.getEndPosX(), segment.getEndPosY()));
		}
		return script.toString();
	}
}
