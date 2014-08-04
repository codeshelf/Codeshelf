/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: SubLocationABC.java,v 1.10 2013/04/11 18:11:12 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.domain;

import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;

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
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Entity
@MappedSuperclass
@CacheStrategy(useBeanCache = false)
@JsonAutoDetect(getterVisibility = Visibility.NONE)
//@ToString(doNotUseGetters = true)
public abstract class SubLocationABC<P extends IDomainObject> extends LocationABC<P> implements ISubLocation<P> {

	@Inject
	public static ITypedDao<SubLocationABC>	DAO;

	@Singleton
	public static class SubLocationDao extends GenericDaoABC<SubLocationABC> implements ITypedDao<SubLocationABC> {
		@Inject
		public SubLocationDao(final ISchemaManager inSchemaManager) {
			super(inSchemaManager);
		}

		public final Class<SubLocationABC> getDaoClass() {
			return SubLocationABC.class;
		}
	}

	private static final Logger	LOGGER	= LoggerFactory.getLogger(SubLocationABC.class);

	// The owning location.
	@Column(nullable = true)
	@ManyToOne(optional = true)
	private SubLocationABC		parent;

	@Column(nullable = false)
	@Enumerated(value = EnumType.STRING)
	@Getter
	@Setter
	@JsonProperty
	private PositionTypeEnum	pickFaceEndPosTypeEnum;

	// X pos of pick face end (pick face starts at anchor pos).
	@Column(nullable = false)
	@Getter
	@Setter
	@JsonProperty
	private Double				pickFaceEndPosX;

	// Y pos of pick face end (pick face starts at anchor pos).
	@Column(nullable = false)
	@Getter
	@Setter
	@JsonProperty
	private Double				pickFaceEndPosY;

	// Z pos of pick face end (pick face starts at anchor pos).
	@Column(nullable = false)
	@Getter
	@Setter
	@JsonProperty
	private Double				pickFaceEndPosZ;

	public SubLocationABC(final Point inAnchorPoint, final Point inPickFaceEndPoint) {
		super(inAnchorPoint);
		setPickFaceEndPosTypeEnum(inPickFaceEndPoint.getPosTypeEnum());
		setPickFaceEndPosX(inPickFaceEndPoint.getX());
		setPickFaceEndPosY(inPickFaceEndPoint.getY());
		setPickFaceEndPosZ(inPickFaceEndPoint.getZ());
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.model.domain.SubLocationABC#getParent()
	 */
	@Override
	public final P getParent() {
		// There's some weirdness with Ebean and navigating a recursive hierarchy. (You can't go down and then back up to a different class.)
		// This fixes that problem, but it's not pretty.
		//		return (P) parent;
		if (parent == null) {
			return (P) parent;
		} else {
			return (P) DAO.findByPersistentId(parent.getClass(), parent.getPersistentId());
		}
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.model.domain.SubLocationABC#setParent(P)
	 */
	@Override
	public final void setParent(P inParent) {
		parent = (SubLocationABC) inParent;
	}

	// --------------------------------------------------------------------------
	/**
	 * @return
	 */
	public Point getPickFaceEndPoint() {
		return new Point(pickFaceEndPosTypeEnum, pickFaceEndPosX, pickFaceEndPosY, pickFaceEndPosZ);
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inPickFaceEndPoint
	 */
	public void setPickFaceEndPoint(final Point inPickFaceEndPoint) {
		pickFaceEndPosTypeEnum = inPickFaceEndPoint.getPosTypeEnum();
		pickFaceEndPosX = inPickFaceEndPoint.getX();
		pickFaceEndPosY = inPickFaceEndPoint.getY();
		pickFaceEndPosZ = inPickFaceEndPoint.getZ();
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.model.domain.LocationABC#computePosAlongPath(com.gadgetworks.codeshelf.model.domain.PathSegment)
	 */
	public final void computePosAlongPath(final PathSegment inPathSegment) {
		if (inPathSegment == null) {
			LOGGER.error("null pathSegment in computePosAlongPath");
			return;
		}
		
		// A fundamental question is whether the bays and slots in an aisle increase or decrease as you move along the path in the forward direction.
		// I think this question does not need to be answered here. Hence the if (false). CD_0036 explains the new aisle file field that lets the aisle children
		// set anchor and pickEndPos correctly based on which way the pick face is relative to the aisle anchor. With that, just use the path direction.
		Aisle theAisle = this.getParentAtLevel(Aisle.class);
		Boolean forwardIncrease = true;
		if (false && theAisle != null)
			forwardIncrease = theAisle.doesAisleIncreaseForwardAlongPath(); // Inefficient to calculate each time. Should cache it somehow.
		
		// For the logic below, we want the forward increasing logic if path direction is forward and forwardIncrease, or if both not.
		Boolean wantForwardCalculation = forwardIncrease == inPathSegment.getParent().getTravelDirEnum().equals(TravelDirectionEnum.FORWARD);

		Point locationAnchorPoint = getAbsoluteAnchorPoint();
		Point pickFaceEndPoint = parent.getAbsoluteAnchorPoint(); // JR this looks wrong. Should want this.getAbsolutePickEndPoint();
		pickFaceEndPoint.translateX(getPickFaceEndPosX());
		pickFaceEndPoint.translateY(getPickFaceEndPosY());
		pickFaceEndPoint.translateZ(getPickFaceEndPosZ());

		Double locAnchorPathPosition = inPathSegment.getStartPosAlongPath()
				+ inPathSegment.computeDistanceOfPointFromLine(inPathSegment.getStartPoint(),
					inPathSegment.getEndPoint(),
					locationAnchorPoint);

		Double pickFacePathPosition = inPathSegment.getStartPosAlongPath()
				+ inPathSegment.computeDistanceOfPointFromLine(inPathSegment.getStartPoint(),
					inPathSegment.getEndPoint(),
					pickFaceEndPoint);

		// if (inPathSegment.getParent().getTravelDirEnum().equals(TravelDirectionEnum.FORWARD)) {
		if (wantForwardCalculation) {
			// In the forward direction take the "lowest" path pos value.
			Double position = Math.min(locAnchorPathPosition, pickFacePathPosition);
			// It can't be "lower" than its parent.
			if ((parent.getPosAlongPath() == null) || (position >= parent.getPosAlongPath())) {
				setPosAlongPath(position);
			} else {
				// I believe if we hit this, we found a model or algorithm error
				Double parentPos = parent.getPosAlongPath(); // just so able to debug and know if we are setting to null
				setPosAlongPath(parentPos);
			}
		} else {
			// In the reverse direction take the "highest" path pos value.
			Double position = Math.max(locAnchorPathPosition, pickFacePathPosition);
			// I t can't be "higher" than its parent.
			if ((parent.getPosAlongPath() == null) || (position <= parent.getPosAlongPath())) {
				setPosAlongPath(position);
			} else {
				// I believe if we hit this, we found a model or algorithm error
				Double parentPos = parent.getPosAlongPath(); // just so able to debug and know if we are setting to null
				setPosAlongPath(parentPos);
			}
		}
		

		LOGGER.debug(this.getFullDomainId() + " path pos: " + getPosAlongPath() + " Anchor x: " + locationAnchorPoint.getX()
				+ " y: " + locationAnchorPoint.getY() + " Face x: ");

		try {
			LocationABC.DAO.store(this);
		} catch (DaoException e) {
			LOGGER.error("", e);
		}

		// Also force a recompute for all of the child locations.
		for (ILocation<P> location : getChildren()) {
			location.computePosAlongPath(inPathSegment);
		}
	}
	
	protected final void doSetControllerChannel(String inControllerPersistentIDStr, String inChannelStr) {
		// this is for callMethod from the UI
		// We are setting the controller and channel for the tier. Depending on the inTierStr parameter, may set also for
		// on all other same tier in the aisle, or perhaps other patterns.
		
		// Initially, log
		LOGGER.debug("On " + this + ", set LED controller to " + inControllerPersistentIDStr);
		
		// Get the LedController
		UUID persistentId = UUID.fromString(inControllerPersistentIDStr);
		LedController theLedController = LedController.DAO.findByPersistentId(persistentId);
		
		// set this tier's
		if (theLedController != null) {
			// Get the channel
			Short theChannel;
			try { theChannel = Short.valueOf(inChannelStr); }
			catch (NumberFormatException e) { 
				theChannel = 0; // not recognizable as a number
			}
			if (theChannel < 0) {
				theChannel = 0; // means don't change if there is a channel. Or set to 1 if there isn't.
			}

			// set the controller. And set the channel
			this.setLedController(theLedController);
			if (theChannel != null && theChannel > 0) {
				this.setLedChannel(theChannel);
			}
			else {
				// if channel passed is 0 or null Short, make sure tier has a ledChannel. Set to 1 if there is not yet a channel.
				Short thisLedChannel = this.getLedChannel();
				if (thisLedChannel == null || thisLedChannel <= 0)
					this.setLedChannel((short) 1);
			}
			
			this.getDao().store(this);		
		}
		else {
			throw new DaoException("Unable to set controller, controller " + inControllerPersistentIDStr + " not found");
		}
	}
	
	// converts A3 into 003.  Could put the A back on.  Could be a static, but called this way conveniently from tier and from bay
	public String getCompString(String inString) {
		String s = inString.substring(1); // Strip off the A, B, T, or S
		// we will pad with leading spaces to 3
		int padLength = 3;
		int needed = padLength - s.length();
		if (needed <= 0) {
			return s;
		}
		char[] padding = new char[needed];
		java.util.Arrays.fill(padding, '0');
		StringBuffer sb = new StringBuffer(padLength);
		sb.append(padding);
		sb.append(s);
		String ss = sb.toString();
		return ss;
	}


}
