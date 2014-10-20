/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: Tier.java,v 1.15 2013/04/11 07:42:45 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.domain;

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

import com.avaje.ebean.annotation.CacheStrategy;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.gadgetworks.codeshelf.model.LedChaser;
import com.gadgetworks.codeshelf.model.dao.GenericDaoABC;
import com.gadgetworks.codeshelf.model.dao.ISchemaManager;
import com.gadgetworks.codeshelf.model.dao.ITypedDao;
import com.gadgetworks.flyweight.command.ColorEnum;
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
@CacheStrategy(useBeanCache = false)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE)
public class Tier extends SubLocationABC<Bay> {

	private static final String		THIS_TIER_ONLY		= "";
	private static final String		ALL_TIERS_IN_AISLE	= "aisle";

	@Inject
	public static ITypedDao<Tier>	DAO;

	@Singleton
	public static class TierDao extends GenericDaoABC<Tier> implements ITypedDao<Tier> {
		@Inject
		public TierDao(final ISchemaManager inSchemaManager) {
			super(inSchemaManager);
		}

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

	@SuppressWarnings("unused")
	private static final Logger	LOGGER	= LoggerFactory.getLogger(Tier.class);

	public Tier(Bay bay, String domainId, final Point inAnchorPoint, final Point inPickFaceEndPoint) {
		super(bay, domainId, inAnchorPoint, inPickFaceEndPoint);
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
		Bay bayLocation = this.getParent();
		Aisle aisleLocation = null;

		if (bayLocation != null) {
			theTierIds.bayName = bayLocation.getDomainId();
			aisleLocation = bayLocation.getParent();
		}
		if (aisleLocation != null) {
			theTierIds.aisleName = aisleLocation.getDomainId();
		}

		return theTierIds;
	}

	public final String getBaySortName() {
		// to support list view meta-field baySortName. Note: cannot sort by this string if more than 9 bays or 9 aisles.
		TierIds theTierIds = getTierIds();
		return (theTierIds.aisleName + "-" + theTierIds.bayName + "-" + theTierIds.tierName);
	}

	public final String getTierSortName() {
		// to support list view meta-field tierSortName. Note: cannot sort by this string if more than 9 bays or 9 aisles.
		TierIds theTierIds = getTierIds();
		return (theTierIds.aisleName + "-" + theTierIds.tierName + "-" + theTierIds.bayName);
	}


	public final String getAisleTierBayForComparable() {
		// this is for a sort comparable.
		TierIds theTierIds = getTierIds();
		return (getCompString(theTierIds.aisleName) + "-" + getCompString(theTierIds.tierName) + "-" + getCompString(theTierIds.bayName));
	}

	public final String getAisleBayForComparable() {
		// this is for a sort comparable.
		TierIds theTierIds = getTierIds();
		return (getCompString(theTierIds.aisleName) + "-" + getCompString(theTierIds.bayName));
	}

	public final String getBayName() {
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

	public final String getSlotAliasRange() {
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

		List<Slot> slotList = this.getChildrenAtLevel(Slot.class);
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

	public final void setControllerChannel(String inControllerPersistentIDStr, String inChannelStr, String inTiersStr) {
		// This, or all of this tier in aisle
		doSetControllerChannel(inControllerPersistentIDStr, inChannelStr);
		boolean allTiers = inTiersStr != null && inTiersStr.equalsIgnoreCase(ALL_TIERS_IN_AISLE);
		// if "aisle", then the rest of tiers at same level
		if (allTiers) {
			// The goal is to get to the aisle, then ask for all tiers. Filter those to the subset with the same domainID (like "T2")
			Bay bayParent = this.getParent();
			Aisle aisleParent = bayParent.getParent();
			List<Tier> locationList = aisleParent.getChildrenAtLevel(Tier.class);

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
	
	//--------------------------------------------------------------------------
	/**
	* This will light each inventory item in turn
	*/
	public void chaseInventoryLeds(){
		List<Item> aList = getInventorySortedByPosAlongPath();
		if (aList.size() == 0)
			return;
		Facility facility = this.getParentAtLevel(Facility.class);
		LedChaser aChaser = new LedChaser(facility, ColorEnum.RED);
		for (Item item : aList) {
			aChaser.addChaseForItem(item);
		}
		LOGGER.info("Firing LedChaser for tier " + this.getNominalLocationId());
		aChaser.fireTheChaser(false); // false = not log only
	}

}
