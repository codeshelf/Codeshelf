/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: Tier.java,v 1.15 2013/04/11 07:42:45 jeffw Exp $
 *******************************************************************************/
package com.codeshelf.model.domain;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.ListIterator;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Transient;

import lombok.Getter;
import lombok.Setter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.model.DeviceType;
import com.codeshelf.model.dao.GenericDaoABC;
import com.codeshelf.model.dao.ITypedDao;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.google.inject.Inject;
import com.google.inject.Singleton;

//--------------------------------------------------------------------------
/**
* TierIds
* Just a means to allow tier method to return multi-value
* 
*/
final class TierIds {
	String	aisleName;
	String	bayName;
	String	tierName;
}

//--------------------------------------------------------------------------
/**
* Tier
* 
* The object that models a tier within a bay.
* 
* @author jeffw
*/

@Entity
@DiscriminatorValue("TIER")
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE)
public class Tier extends Location {

	public static final String		THIS_TIER_ONLY		= "";
	public static final String		ALL_TIERS_IN_AISLE	= "aisle";

	@Inject
	public static ITypedDao<Tier>	DAO;

	@Singleton
	public static class TierDao extends GenericDaoABC<Tier> implements ITypedDao<Tier> {
		public final Class<Tier> getDaoClass() {
			return Tier.class;
		}
	}

	// These two transient fields are not in database. Furthermore,
	// two different references to same object from the DAO may disagree on this field.
	@Transient
	@Column(nullable = true)
	@Getter
	@Setter
	private short				mTransientLedsThisTier;

	@Transient
	@Column(nullable = true)
	@Getter
	@Setter
	private boolean				mTransientLedsIncrease;

	private static final Logger	LOGGER	= LoggerFactory.getLogger(Tier.class);
/*
	public Tier(Bay bay, String domainId, final Point inAnchorPoint, final Point inPickFaceEndPoint) {
		super(bay, domainId, inAnchorPoint, inPickFaceEndPoint);
	}
	*/
	public Tier() {
		super();
	}
	
	@SuppressWarnings("unchecked")
	public final ITypedDao<Tier> getDao() {
		return DAO;
	}

	public final String getDefaultDomainIdPrefix() {
		return "T";
	}

	private TierIds getTierIds() {
		TierIds theTierIds = new TierIds();
		theTierIds.bayName = "";
		theTierIds.aisleName = "";
		theTierIds.tierName = this.getDomainId();
		Bay bayLocation = this.<Bay>getParentAtLevel(Bay.class);
		Aisle aisleLocation = null;

		if (bayLocation != null) {
			theTierIds.bayName = bayLocation.getDomainId();
			aisleLocation = bayLocation.<Aisle>getParentAtLevel(Aisle.class);
		}
		if (aisleLocation != null) {
			theTierIds.aisleName = aisleLocation.getDomainId();
		}

		return theTierIds;
	}

	public String getBaySortName() {
		// to support list view meta-field baySortName. Note: cannot sort by this string if more than 9 bays or 9 aisles.
		TierIds theTierIds = getTierIds();
		return (theTierIds.aisleName + "-" + theTierIds.bayName + "-" + theTierIds.tierName);
	}

	public String getTierSortName() {
		// to support list view meta-field tierSortName. Note: cannot sort by this string if more than 9 bays or 9 aisles.
		TierIds theTierIds = getTierIds();
		return (theTierIds.aisleName + "-" + theTierIds.tierName + "-" + theTierIds.bayName);
	}


	public String getAisleTierBayForComparable() {
		// this is for a sort comparable.
		TierIds theTierIds = getTierIds();
		return (getCompString(theTierIds.aisleName) + "-" + getCompString(theTierIds.tierName) + "-" + getCompString(theTierIds.bayName));
	}

	public String getAisleBayForComparable() {
		// this is for a sort comparable.
		TierIds theTierIds = getTierIds();
		return (getCompString(theTierIds.aisleName) + "-" + getCompString(theTierIds.bayName));
	}

	public String getBayName() {
		// this is not for a sort comparable. Used in AislefilesCsvImporter, but could be used for meta field.
		TierIds theTierIds = getTierIds();
		return (theTierIds.aisleName + "-" + theTierIds.bayName);
	}

	private class SlotIDComparator implements Comparator<Slot> {
		// This is clone of SlotNameComparable in AislesFileCsvImporter.  Move to public slot method?
		public int compare(Slot inLoc1, Slot inLoc2) {
			if ((inLoc1 == null) && (inLoc2 == null)) {
				return 0;
			} else if (inLoc2 == null) {
				return -1;
			} else if (inLoc1 == null) {
				return 1;
			} else {
				// We need to sort S1 - S9, S10- S19, etc. Not S1, S10, S11, ... S2
				String slotOneNumerals = inLoc1.getDomainId().substring(1); // Strip off the S
				String slotTwoNumerals = inLoc2.getDomainId().substring(1); // Strip off the S
				Integer slotOneValue = Integer.valueOf(slotOneNumerals);
				Integer slotTwoValue = Integer.valueOf(slotTwoNumerals);
				return slotOneValue.compareTo(slotTwoValue);
			}
		}
	};

	public String getSlotAliasRange() {
		// for a meta field. If none of the slots have aliases yet, then blank
		// if some but not all, then "xxx" (cap X will give a compiler warning)
		// if all, then the first "->" the last.
		// Try to avoid a localization issue here.
		// If there are no slots at all in this tier, then look for the tier alias.
		String resultStr = "";
		boolean foundEmpty = false;
		boolean foundAlias = false;
		String firstSlotName = "";
		String lastSlotName = "";

		List<Slot> slotList = this.getActiveChildrenAtLevel(Slot.class);
		if (slotList.size() == 0) {
			String tierAlias = this.getPrimaryAliasId();
			return tierAlias;
		}
		
		// We definitely have to sort these. Not guaranteed to come in order.
		Collections.sort(slotList, new SlotIDComparator());

		ListIterator<Slot> li = null;
		li = slotList.listIterator();
		while (li.hasNext()) {
			Slot thisSlot = (Slot) li.next();
			String aliasName = thisSlot.getPrimaryAliasId();
			boolean thisHasAlias = !aliasName.isEmpty(); // length = 0. What about white space?
			foundEmpty = foundEmpty || !thisHasAlias;
			foundAlias = foundAlias || thisHasAlias;
			lastSlotName = aliasName;
			if (firstSlotName.isEmpty())
				firstSlotName = aliasName;
		}

		if (!foundAlias)
			resultStr = "";
		else if (foundAlias && foundEmpty) {
			resultStr = "xxx";
		} else {
			resultStr = firstSlotName + " -> " + lastSlotName;
		}

		return resultStr;
	}

	public void setControllerChannel(String inControllerPersistentIDStr, String inChannelStr, String inTiersStr) {
		// this is for callMethod from the UI
		// This, or all of this tier in aisle
		doSetControllerChannel(inControllerPersistentIDStr, inChannelStr);
		boolean allTiers = inTiersStr != null && inTiersStr.equalsIgnoreCase(ALL_TIERS_IN_AISLE);
		// if "aisle", then the rest of tiers at same level
		if (allTiers) {
			// The goal is to get to the aisle, then ask for all tiers. Filter those to the subset with the same domainID (like "T2")
			Bay bayParent = this.<Bay>getParentAtLevel(Bay.class);
			Aisle aisleParent = bayParent.<Aisle>getParentAtLevel(Aisle.class);
			List<Tier> locationList = aisleParent.getActiveChildrenAtLevel(Tier.class);

			String thisDomainId = this.getDomainId();
			UUID thisPersistId = this.getPersistentId();
			ListIterator<Tier> li = null;
			li = locationList.listIterator();
			while (li.hasNext()) {
				Tier iterTier = (Tier) li.next();
				// same domainID?
				if (iterTier.getDomainId().equals(thisDomainId)) {
					if (!iterTier.getPersistentId().equals(thisPersistId)) {
						iterTier.setControllerChannel(inControllerPersistentIDStr, inChannelStr, THIS_TIER_ONLY);
					}
				}

			}
		}
	}

	public void setPoscons(int startingIndex) {
		setPoscons(startingIndex, false);
	}
	
	public void setPoscons(int startingIndex, boolean reverseOrder) {
		LedController ledController = this.getLedController();
		if (ledController==null) {
			LOGGER.warn("Failed to set poscons on "+this+": Tier has no LedController.");
			return;
		}
		if (ledController.getDeviceType()!=DeviceType.Poscons) {
			LOGGER.warn("Failed to set poscons on "+this+": LedController "+ledController+" is not of device type Poscon.");
			return;
		}
		
		List<Slot> slotList = this.getActiveChildrenAtLevel(Slot.class);
		if (slotList.size() == 0) {
			return;
		}
		
		// We definitely have to sort these. Not guaranteed to come in order.
		Collections.sort(slotList, new SlotIDComparator());
		
		// reverse slot list, if required
		if (reverseOrder) {
			Collections.reverse(slotList);
		}
				
		// set position controller index and led controller
		ListIterator<Slot> li = null;
		li = slotList.listIterator();
		int posconIndex = startingIndex;
		while (li.hasNext()) {
			Slot slot = (Slot) li.next();
			slot.setLedController(ledController);
			slot.setPosconIndex(posconIndex);
			Slot.DAO.store(slot);
			posconIndex++;	
		}
	}
	
	public static void setDao(TierDao inTierDao) {
		Tier.DAO = inTierDao;
	}

	public Slot createSlot(String inSlotId, Point inAnchorPoint, Point inPickFaceEndPoint) {
		Slot slot=new Slot();
		slot.setDomainId(inSlotId);
		slot.setAnchorPoint(inAnchorPoint);
		slot.setPickFaceEndPoint(inPickFaceEndPoint);
		
		this.addLocation(slot);
		
		return slot;
	}
	
	@Override
	public boolean isTier() {
		return true;
	}
	
	public static Tier as(Location location) {
		if (location==null) {
			return null;
		}
		if (location.isTier()) {
	    	return (Tier) location;
	    }
		throw new RuntimeException("Location is not a tier: "+location);
	}	
}
