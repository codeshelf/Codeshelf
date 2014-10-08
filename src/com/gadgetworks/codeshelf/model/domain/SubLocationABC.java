/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: SubLocationABC.java,v 1.10 2013/04/11 18:11:12 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;

import lombok.Getter;
import lombok.Setter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.avaje.ebean.annotation.CacheStrategy;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.gadgetworks.codeshelf.device.LedCmdGroup;
import com.gadgetworks.codeshelf.device.LedSample;
import com.gadgetworks.codeshelf.model.PositionTypeEnum;
import com.gadgetworks.codeshelf.model.dao.DaoException;
import com.gadgetworks.codeshelf.model.dao.GenericDaoABC;
import com.gadgetworks.codeshelf.model.dao.ISchemaManager;
import com.gadgetworks.codeshelf.model.dao.ITypedDao;
import com.gadgetworks.codeshelf.util.StringUIConverter;
import com.gadgetworks.flyweight.command.ColorEnum;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Entity
@MappedSuperclass
@CacheStrategy(useBeanCache = false)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE)
//@ToString(doNotUseGetters = true)
public abstract class SubLocationABC<P extends IDomainObject & ISubLocation<?>> extends LocationABC<P> implements ISubLocation<P> {

	@SuppressWarnings("rawtypes")
	@Inject
	public static ITypedDao<SubLocationABC>	DAO;

	@SuppressWarnings("rawtypes")
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
	@SuppressWarnings("rawtypes")
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

	public SubLocationABC(P parent, String domainId, final Point inAnchorPoint, final Point inPickFaceEndPoint) {
		super(domainId, inAnchorPoint);
		setParent(parent);
		if (parent != null && parent instanceof SubLocationABC<?>) {
			((SubLocationABC<?>)parent).addLocation(this);
		}
		setPickFaceEndPosTypeEnum(inPickFaceEndPoint.getPosTypeEnum());
		setPickFaceEndPosX(inPickFaceEndPoint.getX());
		setPickFaceEndPosY(inPickFaceEndPoint.getY());
		setPickFaceEndPosZ(inPickFaceEndPoint.getZ());
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.model.domain.SubLocationABC#getParent()
	 */
	@SuppressWarnings("unchecked")
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
		parent = (SubLocationABC<?>) inParent;
	}
	
	public Point getAbsolutePickFaceEndPoint() {
		Point base = getAbsoluteAnchorPoint();
		return base.add(getPickFaceEndPosX(), getPickFaceEndPosY(), getPickFaceEndPosZ());
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
		
		// Complete revision at V4
		Point locationAnchorPoint = getAbsoluteAnchorPoint();
		Point pickFaceEndPoint = getAbsolutePickFaceEndPoint();
		// The location's posAlongPath is the lower of the anchor or pickFaceEnd
		Double locAnchorPathPosition = inPathSegment.computeNormalizedPositionAlongPath(locationAnchorPoint);
		Double pickFaceEndPathPosition = inPathSegment.computeNormalizedPositionAlongPath(pickFaceEndPoint);
		Double newPosition = Math.min(locAnchorPathPosition, pickFaceEndPathPosition);
		Double oldPosition = this.getPosAlongPath();

		// Doing this to avoid the DAO needing to check the change, which also generates a bunch of logging.
		if (!newPosition.equals(oldPosition)) {
			try {
				LOGGER.debug(this.getFullDomainId() + " path pos: " + getPosAlongPath() + " Anchor x: " + locationAnchorPoint.getX()
					+ " y: " + locationAnchorPoint.getY() + " Face x: ");
				setPosAlongPath(newPosition);
				LocationABC.DAO.store(this);
			} catch (DaoException e) {
				LOGGER.error("", e);
			}
		}

		// Also force a recompute for all of the child locations.
		@SuppressWarnings("rawtypes")
		List<ISubLocation> locations = getChildren();
		for (@SuppressWarnings("rawtypes") ISubLocation location : locations) {
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
	
	public Double getLocationWidthMeters() {
		// Seems funny, but it is so. Anchor is relative to parent. PickFaceEnd is relative to anchor.
		// So, the width is just the pickface end value.
		if (isLocationXOriented())
			return this.getPickFaceEndPosX();
		else
			return this.getPickFaceEndPosY();			
	}
	
	public boolean isLocationXOriented() {
		return getPickFaceEndPosY() == 0.0;
	}

	public List<LedCmdGroup> getLedsToCheck(ColorEnum color) {
		ArrayList<LedCmdGroup> ledCmdGroups = new ArrayList<LedCmdGroup>();
		if (this.getEffectiveLedController() != null) {
			LedCmdGroup cmd = new LedCmdGroup(this.getEffectiveLedController().getDeviceGuidStr(), this.getEffectiveLedChannel(), this.getLastLedNumAlongPath(), 
					ImmutableList.of(new LedSample(this.getLastLedNumAlongPath(), color)));
			ledCmdGroups.add(cmd);
			return ledCmdGroups;
		}
		else {
			for (ISubLocation child : getChildren()) {
				ledCmdGroups.addAll(child.getLedsToCheck(color));
			}
			return ledCmdGroups;
		}
	}
	
	// UI fields
	public String getAnchorPosXui() {
		return StringUIConverter.doubleToTwoDecimalsString(getAnchorPosX());
	}
	public String getAnchorPosYui() {
		return StringUIConverter.doubleToTwoDecimalsString(getAnchorPosY());
	}
	public String getAnchorPosZui() {
		return StringUIConverter.doubleToTwoDecimalsString(getAnchorPosZ());
	}
	public String getPickFaceEndPosXui() {
		return StringUIConverter.doubleToTwoDecimalsString(getPickFaceEndPosX());
	}
	public String getPickFaceEndPosYui() {
		return StringUIConverter.doubleToTwoDecimalsString(getPickFaceEndPosY());
	}
	public String getPickFaceEndPosZui() {
		return StringUIConverter.doubleToTwoDecimalsString(getPickFaceEndPosZ());
	}
	public String getPosAlongPathui() {
		return StringUIConverter.doubleToTwoDecimalsString(getPosAlongPath());
	}

}
