/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: Bay.java,v 1.21 2013/04/11 18:11:12 jeffw Exp $
 *******************************************************************************/
package com.codeshelf.model.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.model.BayComparable;
import com.codeshelf.model.DeviceType;
import com.codeshelf.model.dao.GenericDaoABC;
import com.codeshelf.model.dao.ITypedDao;
import com.codeshelf.persistence.TenantPersistenceService;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.google.common.collect.Ordering;

// --------------------------------------------------------------------------
/**
 * Bay
 * 
 * The object that models a storage bay in an aisle (pallet bay, etc.)
 * 
 * @author jeffw
 */

@Entity
@DiscriminatorValue("BAY")
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE)
public class Bay extends Location {

	public static class BayDao extends GenericDaoABC<Bay> implements ITypedDao<Bay> {
		public final Class<Bay> getDaoClass() {
			return Bay.class;
		}
	}

	private static final Logger			LOGGER				= LoggerFactory.getLogger(Bay.class);

	private static Comparator<Location>	topDownTierOrder	= new TopDownTierOrder();

	public Bay() {
		super();
	}

	@SuppressWarnings("unchecked")
	public final ITypedDao<Bay> getDao() {
		return staticGetDao();
	}

	public static ITypedDao<Bay> staticGetDao() {
		return TenantPersistenceService.getInstance().getDao(Bay.class);
	}

	public final String getDefaultDomainIdPrefix() {
		return "B";
	}

	public String getBaySortName() {
		// to support list view meta-field tierSortName
		String bayName = this.getDomainId();
		String aisleName = "";
		Aisle aisleLocation = this.<Aisle> getParentAtLevel(Aisle.class);

		if (aisleLocation != null) {
			aisleName = aisleLocation.getDomainId();
		}
		return (aisleName + "-" + bayName);
	}

	public String getBayIdForComparable() {
		return getCompString(getDomainId());
	}

	@Override
	public List<Location> getSubLocationsInWorkingOrder() {
		List<Location> copy = new ArrayList<Location>(getActiveChildren());
		Collections.sort(copy, topDownTierOrder);
		List<Location> result = new ArrayList<Location>();
		for (Location childLocation : copy) {
			// add sublocation
			result.add(childLocation);
			// and its sublocations recursively
			result.addAll(childLocation.getSubLocationsInWorkingOrder());
		}
		return result;

	}

	private static final class TopDownTierOrder implements Comparator<Location> {
		final Ordering<Double>	doubleOrdering	= Ordering.<Double> natural().reverse().nullsLast();

		@Override
		public int compare(Location o1, Location o2) {
			Double o1Z = o1.getAbsoluteAnchorPoint().getZ();
			Double o2Z = o2.getAbsoluteAnchorPoint().getZ();
			int result = doubleOrdering.compare(o1Z, o2Z);
			return result;
		}

	}

	public Tier createTier(String inTierId, Point inAnchorPoint, Point inPickFaceEndPoint) {
		Tier tier = new Tier();
		tier.setDomainId(inTierId);
		tier.setAnchorPoint(inAnchorPoint);
		tier.setPickFaceEndPoint(inPickFaceEndPoint);

		this.addLocation(tier);

		return tier;
	}

	@Override
	public boolean isBay() {
		return true;
	}

	public static Bay as(Location location) {
		if (location == null) {
			return null;
		}
		if (location.isBay()) {
			return (Bay) location;
		}
		throw new RuntimeException("Location is not a bay: " + location);
	}

	public static void sortByDomainId(List<Bay> bays) {
		java.util.Collections.sort(bays, new BayComparable());
	}

	/**
	 * Called by UX
	 */
	public void setPosconAssignment(String inControllerPersistentIDStr, String inIndexStr) {
		doSetControllerChannel(inControllerPersistentIDStr, "1");
		Integer posconIndex = 0;
		try {
			posconIndex = Integer.parseInt(inIndexStr);
		} catch (NumberFormatException e) {

		}
		if (posconIndex < 1 || posconIndex > 255) {
			LOGGER.warn("Bad poscon index value: {} in setPosconAssignment", posconIndex);
			return;
		}

		LedController ledController = this.getLedController();
		if (ledController == null) {
			LOGGER.warn("Failed to set poscons on " + this + ": Bay has controller set.");
			return;
		}
		if (ledController.getDeviceType() != DeviceType.Poscons) {
			LOGGER.warn("Failed to set poscons on " + this + ": LedController " + ledController + " is not of device type Poscon.");
			return;
		}

		LOGGER.info("Set bay {} poscon assignment to {} index:{}", this.getBestUsableLocationName(), ledController.getDeviceGuidStrNoPrefix(), posconIndex);
		this.setPosconIndex(posconIndex);
		Bay.staticGetDao().store(this);
		
		// Big side effect to make mistake correction easier. If setting the poscon index for a bay, let's clear out any set for
		// the bay's tiers and slots. Don't clear the tier and slot controllers though, as that will then be set to LED controller probably.
		
		List<Location> tiers = this.getActiveChildren();
		for (Location tier :tiers) {
			// shall we assume bay child, if any, must be tier?
			if (tier.getPosconIndex() != null){
				tier.setPosconIndex(null);
				Tier.staticGetDao().store(tier);				
			}
			List<Location> slots = tier.getActiveChildren();
			for (Location slot :slots) {
				// shall we assume bay child, if any, must be tier?
				if (slot.getPosconIndex() != null){
					slot.setPosconIndex(null);
					Location.staticGetLocationDao().store(slot);
				}
			}	
		}
	}
	
	/**
	 * Called by UX
	 */
	public void clearPosconAssignment() {
		this.setPosconIndex(null);
		clearControllerChannel(); // does the store
	}

}
