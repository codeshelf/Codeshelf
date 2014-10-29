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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.gadgetworks.codeshelf.model.TravelDirectionEnum;
import com.gadgetworks.codeshelf.model.dao.DaoException;
import com.gadgetworks.codeshelf.model.dao.GenericDaoABC;
import com.gadgetworks.codeshelf.model.dao.ITypedDao;
import com.gadgetworks.codeshelf.platform.persistence.PersistenceService;
import com.gadgetworks.codeshelf.util.CompareNullChecker;
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
//@CacheStrategy(useBeanCache = true)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE)
//@ToString(doNotUseGetters = true, exclude = { "parent" })
public class Path extends DomainObjectTreeABC<Facility> {

	@Inject
	public static ITypedDao<Path>	DAO;

	@Singleton
	public static class PathDao extends GenericDaoABC<Path> implements ITypedDao<Path> {
		@Inject
		public PathDao(final PersistenceService persistenceService) {
			super(persistenceService);
		}

		public final Class<Path> getDaoClass() {
			return Path.class;
		}
	}

	public static final String			DEFAULT_FACILITY_PATH_ID	= "DEFAULT";
	public static final String			DOMAIN_PREFIX				= "P";

	private static final Logger			LOGGER						= LoggerFactory.getLogger(Path.class);

	// The parent facility.
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
	private WorkArea					workArea;

	// The computed path length.
	@Setter
	private Double						length;

	// All of the path segments that belong to this path.
	@OneToMany(mappedBy = "parent")
	@MapKey(name = "segmentOrder")
	//	@Getter
	private Map<Integer, PathSegment>	segments					= new HashMap<Integer, PathSegment>();

	// private Map<Integer, PathSegment>	segments					= null;
/*
	public static final Path create(Facility parent, String inDomainId) {
		Path path = new Path(parent, inDomainId, "A Facility Path");
		DAO.store(path);
		return path;
	}
	*/
	public Path() {
		description = "";
		travelDirEnum = TravelDirectionEnum.FORWARD;
	}

	/*
	public Path(Facility facility, String inDomainId, String inDescription) {
		super(inDomainId);
		parent = facility;
		description = inDescription;
		travelDirEnum = TravelDirectionEnum.FORWARD;
	}
*/
	@SuppressWarnings("unchecked")
	public final ITypedDao<Path> getDao() {
		return Path.DAO;
	}

	public final String getDefaultDomainIdPrefix() {
		return DOMAIN_PREFIX;
	}

	public final List<? extends IDomainObject> getChildren() {
		return new ArrayList<PathSegment>(getSegments());
	}

	public final void addPathSegment(PathSegment inPathSegment) {
		Path previousPath = inPathSegment.getParent();
		if(previousPath == null) {
			segments.put(inPathSegment.getSegmentOrder(), inPathSegment);
			inPathSegment.setParent(this);
		} else if (previousPath!=this) {
			LOGGER.error("cannot add PathSegment "+inPathSegment.getDomainId()+" to "+this.getDomainId()+" because it has not been removed from "+previousPath.getDomainId());
		}	
	}

	public final PathSegment getPathSegment(Integer inOrder) {
		return segments.get(inOrder);
	}

	public final void removePathSegment(Integer inOrder) {
		PathSegment pathSegment = this.getPathSegment(inOrder);
		if(pathSegment != null) {
			pathSegment.setParent(null);
			segments.remove(inOrder);
		} else {
			LOGGER.error("cannot remove PathSegment "+inOrder+" from "+this.getDomainId()+" because it isn't found in children");
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
			tempWorkArea.setDomainId(this.getDomainId());
			tempWorkArea.setDescription("Default work area");
			this.setWorkArea(tempWorkArea);
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
		final Integer inSegmentOrder,
		final Point inHead,
		final Point inTail) {

		/* TODO a zero distance path segment isn't useful and unrealistic
		if (inHead.equals(inTail)) {
			throw new IllegalArgumentException("inHead and inPath points should not be the same");
		}
		*/

		// The path segment goes along the longest segment of the aisle.
		PathSegment result = new PathSegment();
		result.setSegmentOrder(inSegmentOrder);
		result.setDomainId(inSegmentId);
		result.setStartPoint(inHead);
		result.setEndPoint(inTail);
		this.addPathSegment(result);
		try {
			PathSegment.DAO.store(result);
		} catch (DaoException e) {
			LOGGER.error("Failed to store PathSegment", e);
		}


		// Force a re-computation of the path distance for this path segment.
		result.computePathDistance();

		return result;
	}

	// --------------------------------------------------------------------------
	/**
	 * Is the passed in location on this path?  Important: deleted location is not on path
	 * @return
	 */
	public final Boolean isLocationOnPath(final ILocation<?> inLocation) {
		boolean result = false;

		// There's some weirdness around Ebean CQuery.request.graphContext.beanMap
		// that makes it impossible to search down the graph and then back up for nested classes.
		//		ISubLocation<?> parentLocation = (ISubLocation<?>) inLocation.getParent();
		//		ISubLocation<?> location = parentLocation.getLocation(inLocation.getLocationId());
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
	public final void sortWisByDistance(final List<WorkInstruction> inOutWiList) {
		Collections.sort(inOutWiList, new WiComparable());
	}

	/**
	 * @author jeffw
	 *
	 */
	@SuppressWarnings("rawtypes")
	private class LocationsComparable implements Comparator<ILocation> {

		public int compare(ILocation inLoc1, ILocation inLoc2) {

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
	public final <T extends ISubLocation<?>> List<T> getLocationsByClass(final Class<? extends ISubLocation<?>> inClassWanted) {

		// First make a list of all the bays on the CHE's path.
		List<T> locations = new ArrayList<T>();

		// Path segments get return in direction order.
		for (PathSegment pathSegment : getSegments()) {
			for (ILocation<?> pathLocation : pathSegment.getLocations()) {
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
	public final <T extends ISubLocation<?>> List<T> getLocationsByClassAtOrPastLocation(final ILocation<?> inAtOrPastLocation,
		final Class<? extends ISubLocation<?>> inClassWanted) {

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
	public final boolean isOrderOnPath(final OrderHeader inOrderHeader) {
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
	public final int getAssociatedLocationCount() {
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
	public final void deleteThisPath() {

		for (PathSegment segment : this.getSegments()) {

			// make sure segment is not associated to a location			
			for (ILocation<?> location : segment.getLocations()) {
				if (location.getAssociatedPathSegment().equals(segment)) {
					LOGGER.info("clearing path segment association");
					location.setPathSegment(null);
					// which DAO?
					location.getDao().store(location);
				}
			}
			// delete the segment
			PathSegment.DAO.delete(segment);
		}

		// delete the work area
		WorkArea wa = this.getWorkArea();
		if (wa != null) {
			this.setWorkArea(null);
			Path.DAO.store(this);
			WorkArea.DAO.delete(wa);

			/*			
			wa.setParent(null);
			WorkArea.DAO.store(wa);
			*/
			// WorkArea has lists of locations, users and active ches also.
			// Jeff said users and ches will come later, but for now, they do not point at the wa. He said location list can be removed.

		}
		// then delete this path
		Path.DAO.delete(this);
	}

	public static void setDao(PathDao inPathDao) {
		Path.DAO = inPathDao;
	}

	@Override
	public final Facility getFacility() {
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
}
