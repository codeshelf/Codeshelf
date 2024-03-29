/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: Aisle.java,v 1.26 2013/09/18 00:40:08 jeffw Exp $
 *******************************************************************************/
package com.codeshelf.model.domain;

import java.util.List;
import java.util.UUID;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.model.dao.DaoException;
import com.codeshelf.model.dao.GenericDaoABC;
import com.codeshelf.model.dao.ITypedDao;
import com.codeshelf.persistence.TenantPersistenceService;
import com.fasterxml.jackson.annotation.JsonAutoDetect;

// --------------------------------------------------------------------------
/**
 * Aisle
 * 
 * Aisle is a facility-level location that holds a collection of bays.
 * 
 * @author jeffw
 */

@Entity
@DiscriminatorValue("AISLE")
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE)
public class Aisle extends Location {

	public static class AisleDao extends GenericDaoABC<Aisle> implements ITypedDao<Aisle> {
		public final Class<Aisle> getDaoClass() {
			return Aisle.class;
		}
	}

	private static final Logger	LOGGER	= LoggerFactory.getLogger(Aisle.class);

	public Aisle() {
		super();
	}

	@Override
	@SuppressWarnings("unchecked")
	public final ITypedDao<Aisle> getDao() {
		return staticGetDao();
	}

	public final static ITypedDao<Aisle> staticGetDao() {
		return TenantPersistenceService.getInstance().getDao(Aisle.class);
	}

	public final String getDefaultDomainIdPrefix() {
		return "A";
	}

	/**
	 * this is for callMethod from the UI.  The main complexity is that this aisle may have been set to different path segment earlier. If so, that should be removed first.
	 */
	public void associatePathSegment(String inPathSegPersistentID) {
		/* In all cases, the result should be aisle has its pathseg field set, the pathseg should have this aisle in its list.  AND this aisle should not be in other path seg lists.
		 * 1a) Simple. No path association yet. Normal add. 
		 * 1b) Simple error. No path association yet. Throws. Covered in test updateNonexistantPathSegment()
		 * 2) Repeat association. Aisle already associated to this path segment. Nothing happens.
		 * 3a) Aisle associated to different segment. Remove. Then add.
		 * 3b) Aisle associated to different segment, but this add does not resolve. No actions aside from Throw. Covered in test associatePathSegment()
		 */
		UUID persistentId = UUID.fromString(inPathSegPersistentID);
		PathSegment newPathSegment = PathSegment.staticGetDao().findByPersistentId(persistentId);
		if (newPathSegment == null) {
			throw new DaoException("Could not associate path segment, segment not found: " + inPathSegPersistentID);
		}

		associatePathSegment(newPathSegment);
	}

	public void associatePathSegment(PathSegment inPathSegment) {

		if (inPathSegment == null) {
			throw new DaoException("null call to associatePathSegment");
		}

		PathSegment oldPathSegment = this.getAssociatedPathSegment();
		// don't add a second time if the UI chose to do what is already there.
		if (oldPathSegment != null && oldPathSegment.equals(inPathSegment))
			return;

		if (oldPathSegment != null)
			oldPathSegment.removeLocation(this);
		// normally we would call this.getDao().store(this); after remove. But store will be called soon.

		// Just an extra check on locations Array maintenance. Barely worth it.
		int initialLocationCount = inPathSegment.getLocations().size();
		inPathSegment.addLocation(this);
		this.computePosAlongPath(inPathSegment);
		this.getDao().store(this);

		int afterLocationCount = inPathSegment.getLocations().size();
		if (initialLocationCount == afterLocationCount)
			LOGGER.error("associatePathSegment did not correctly update locations array");
	}

	/**
	 * this is for callMethod from the UI.  Huge side effect, new from v8. If we are setting for the aisle, then clear out any tier sets done earlier.
	 * This fixes the possible perceived bug of set tier controller. Oops, this is zigzag aisle. Set aisle controller correctly. Some tiers never set.
	 * This makes the getEffectiveXXX() calls work, searching upward until they find the aisle value
	 */
	public void setControllerChannel(String inControllerPersistentIDStr, String inChannelStr) {
		doSetControllerChannel(inControllerPersistentIDStr, inChannelStr);

		List<Tier> aList = getActiveChildrenAtLevel(Tier.class);
		for (Tier aTier : aList) {
			aTier.clearControllerChannel();
		}
	}

	// As you face the pick face, is the left toward the anchor point? If so, any cm offset adds to an anchor point. 
	// If not, any cm offset subtracts from a pick face end	
	public Boolean isLeftSideTowardB1S1() {
		// JR in progress for DEV-310
		// The answer depends on the aisle's relationship to its pathSegment
		Boolean returnValue = true;

		PathSegment mySegment = getPathSegment();
		if (mySegment != null) {
			/*
			 * 
			 * Legend: = aisle, - path, * B1S1, L left side of pick face
			 * 
			 * X ORIENTED:
			 * Y coordinate: 
			 * 0  --------------------------L		If aisle is South of path, left side of pick face is away from B1S1
			 * 1 *===========================
			 * 2  L--------------------------		If aisle is North of path, left side of pick face is toward B1S1
			 * 
			 * 
			 * Y ORIENTED:
			 * X coordinate:
			 * 0 1 2
			 *   *
			 * L = |
			 * | = |
			 * | = |
			 * | = |
			 * | = |
			 * | = |
			 * | = |
			 * | = L
			 * 
			 * If aisle is West of path, left side of pick face is away B1S1
			 * If aisle is East of path, left side of pick face is toward B1S1
			 * 
			 */
			Boolean xOriented = this.isLocationXOriented();
			if (xOriented) {
				Double aisleY = this.getAnchorPosY();
				Double pathY = mySegment.getStartPosY(); // assume start and end Y are roughly the same.
				boolean aisleNorthOfPath = aisleY < pathY;
				returnValue = aisleNorthOfPath;
			} else {
				Double aisleX = this.getAnchorPosX();
				Double pathX = mySegment.getStartPosX(); // assume start and end X are roughly the same.
				boolean aisleEastOfPath = aisleX > pathX;
				returnValue = aisleEastOfPath;
			}
		}
		return returnValue;
	}

	public Boolean associatedPathSegmentIncreasesFromAnchor() {
		PathSegment mySegment = getPathSegment();
		Boolean pathSegIncreaseFromAisleAnchor = true;
		if (mySegment != null) {
			Boolean xOriented = this.getPickFaceEndPosY() == 0.0;
			if (xOriented) {
				pathSegIncreaseFromAisleAnchor = mySegment.getEndPosX() > mySegment.getStartPosX();
			} else {
				pathSegIncreaseFromAisleAnchor = mySegment.getEndPosY() > mySegment.getStartPosY();
			}
		}
		return pathSegIncreaseFromAisleAnchor;
	}

	public Bay createBay(String inBayId, Point inAnchorPoint, Point inPickFaceEndPoint) {
		Bay bay = new Bay();
		bay.setDomainId(inBayId);
		bay.setAnchorPoint(inAnchorPoint);
		bay.setPickFaceEndPoint(inPickFaceEndPoint);

		this.addLocation(bay);

		return bay;
	}
		
	@Override
	public boolean isAisle() {
		return true;
	}
	
	public static Aisle as(Location location) {
		if (location==null) {
			return null;
		}
		if (location.isAisle()) {
	    	return (Aisle) location;
	    }
		throw new RuntimeException("Location is not an aisle");
	}
}
