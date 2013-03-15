/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: Path.java,v 1.20 2013/03/15 14:57:13 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.MapKey;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

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
import com.gadgetworks.codeshelf.model.dao.ITypedDao;
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
@Table(name = "PATH", schema = "CODESHELF")
@CacheStrategy(useBeanCache = true)
@JsonAutoDetect(getterVisibility = Visibility.NONE)
@ToString(doNotUseGetters = true, exclude = { "parent" })
public class Path extends DomainObjectTreeABC<Facility> {

	@Inject
	public static ITypedDao<Path>	DAO;

	@Singleton
	public static class PathDao extends GenericDaoABC<Path> implements ITypedDao<Path> {
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
	 * @param inTravelDirection
	 * @return
	 */
	public final List<PathSegment> getSegments() {
		List<PathSegment> list = new ArrayList<PathSegment>(segments.values());
		Collections.sort(list, new PathSegmentComparator(travelDirEnum));
		return list;
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
	 * Create the path segments for the aisle.
	 * @param inAssociatedAisle
	 * @param inXDimMeters
	 * @param inYDimMeters
	 */
	public final void createPathSegments(final Aisle inAssociatedAisle, final Double inXDimMeters, final Double inYDimMeters) {
		// If there are already path segments then create a connecting path to the new ones.
		Integer segmentOrder = 0;
		PathSegment lastSegment = null;
		if (this.getSegments().size() > 0) {
			lastSegment = this.getPathSegment(this.getSegments().size() - 1);
			if (lastSegment != null) {
				segmentOrder = lastSegment.getSegmentOrder() + 1;
			}
		}

		Point headA = null;
		Point tailA = null;
		Point headB = null;
		Point tailB = null;
		if (inXDimMeters < inYDimMeters) {
			Double xA = inAssociatedAisle.getPosX() - inXDimMeters;
			tailA = new Point(PositionTypeEnum.METERS_FROM_PARENT, xA, inAssociatedAisle.getPosY(), null);
			headA = new Point(PositionTypeEnum.METERS_FROM_PARENT, xA, inAssociatedAisle.getPosY() + inYDimMeters, null);

			Double xB = inAssociatedAisle.getPosX() + inXDimMeters * 2.0;
			headB = new Point(PositionTypeEnum.METERS_FROM_PARENT, xB, inAssociatedAisle.getPosY(), null);
			tailB = new Point(PositionTypeEnum.METERS_FROM_PARENT, xB, inAssociatedAisle.getPosY() + inYDimMeters, null);
		} else {
			Double yA = inAssociatedAisle.getPosY() - inYDimMeters;
			tailA = new Point(PositionTypeEnum.METERS_FROM_PARENT, inAssociatedAisle.getPosX(), yA, null);
			headA = new Point(PositionTypeEnum.METERS_FROM_PARENT, inAssociatedAisle.getPosX() + inXDimMeters, yA, null);

			Double yB = inAssociatedAisle.getPosY() + inYDimMeters * 2.0;
			headB = new Point(PositionTypeEnum.METERS_FROM_PARENT, inAssociatedAisle.getPosX(), yB, null);
			tailB = new Point(PositionTypeEnum.METERS_FROM_PARENT, inAssociatedAisle.getPosX() + inXDimMeters, yB, null);
		}

		String baseSegmentId = inAssociatedAisle.getDomainId() + "." + PathSegment.DOMAIN_PREFIX;
		// Now connect it to the last aisle's path segments.
		if (lastSegment != null) {
			this.createPathSegment(baseSegmentId + "D", this.getParent(), this, segmentOrder++, tailA, lastSegment.getStartPoint());
		}
		// Create the "A" side path.
		PathSegment segmentA = createPathSegment(baseSegmentId + "A", inAssociatedAisle, this, segmentOrder++, headA, tailA);
		// Create a connector path.
		createPathSegment(baseSegmentId + "C", this.getParent(), this, segmentOrder++, tailB, headA);
		// Create the "B" side path.
		createPathSegment(baseSegmentId + "B", inAssociatedAisle, this, segmentOrder++, headB, tailB);

		// Link the "A" path segment as the primary path for the aisle and all of its child locations (recursively).
		inAssociatedAisle.setPathSegment(segmentA);
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
		final LocationABC inAssociatedLocation,
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
		if (inAssociatedLocation != null) {
			result.setAnchorLocation(inAssociatedLocation);
		} else {
			LOGGER.error("No anchor location");
		}
		try {
			PathSegment.DAO.store(result);
		} catch (DaoException e) {
			LOGGER.error("", e);
		}

		inPath.addPathSegment(result);
		result.addLocation(inAssociatedLocation);

		// Force a re-computation of the path distance for this path segment.
		result.computePathDistance();

		return result;
	}

	// --------------------------------------------------------------------------
	/**
	 * Is the passed in location on this path?
	 * @return
	 */
	public final Boolean isLocationOnPath(final LocationABC<?> inLocation) {
		boolean result = false;

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

			if (inWi1.getDistanceAlongPath() == null) {
				LocationABC wiLocation = getParent().getSubLocationById(inWi1.getLocationId());
				inWi1.setDistanceAlongPath(wiLocation.getPathDistance());
			}

			if (inWi2.getDistanceAlongPath() == null) {
				LocationABC wiLocation = getParent().getSubLocationById(inWi2.getLocationId());
				inWi2.setDistanceAlongPath(wiLocation.getPathDistance());
			}

			if (inWi1.getDistanceAlongPath() > inWi2.getDistanceAlongPath()) {
				return -1;
			} else if (inWi1.getDistanceAlongPath() < inWi2.getDistanceAlongPath()) {
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
}
