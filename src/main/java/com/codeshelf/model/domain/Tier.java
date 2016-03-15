/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: Tier.java,v 1.15 2013/04/11 07:42:45 jeffw Exp $
 *******************************************************************************/
package com.codeshelf.model.domain;

import java.util.ArrayList;
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
import lombok.experimental.Accessors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.model.CodeshelfTape;
import com.codeshelf.model.DeviceType;
import com.codeshelf.model.TierBayComparable;
import com.codeshelf.model.dao.GenericDaoABC;
import com.codeshelf.model.dao.ITypedDao;
import com.codeshelf.persistence.TenantPersistenceService;
import com.codeshelf.util.FormUtility;
import com.codeshelf.validation.ErrorCode;
import com.codeshelf.validation.InputValidationException;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.google.common.base.Strings;

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

	public static final String	THIS_TIER_ONLY		= "";
	public static final String	ALL_TIERS_IN_AISLE	= "aisle";

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

	@Transient
	@Column(nullable = true)
	@Accessors(prefix = "m")
	@Getter
	@Setter
	private short				mTransientLedOffset;

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
		return staticGetDao();
	}

	public static ITypedDao<Tier> staticGetDao() {
		return TenantPersistenceService.getInstance().getDao(Tier.class);
	}

	public final String getDefaultDomainIdPrefix() {
		return "T";
	}

	private TierIds getTierIds() {
		TierIds theTierIds = new TierIds();
		theTierIds.bayName = "";
		theTierIds.aisleName = "";
		theTierIds.tierName = this.getDomainId();
		Bay bayLocation = this.<Bay> getParentAtLevel(Bay.class);
		Aisle aisleLocation = null;

		if (bayLocation != null) {
			theTierIds.bayName = bayLocation.getDomainId();
			aisleLocation = bayLocation.<Aisle> getParentAtLevel(Aisle.class);
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

	public class SlotIDComparator implements Comparator<Slot> {
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

	public String getSlotPosconRange() {

		List<Slot> slotList = this.getActiveChildrenAtLevel(Slot.class);
		int highest = 0;
		int lowest = 0;
		for (Slot slot : slotList) {
			Integer index = slot.getPosconIndex();
			if (index != null) {
				if (index > highest)
					highest = index;
				if (lowest == 0 || index < lowest)
					lowest = index;
			}
		}
		if (highest == 0 || lowest == 0)
			return "";
		else if (highest == lowest)
			return Integer.toString(highest);
		else
			return String.format("%d - %d", lowest, highest);
	}

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

	@Override
	protected void doSetControllerChannel(String inControllerPersistentIDStr, String inChannelStr) {
		super.doSetControllerChannel(inControllerPersistentIDStr, inChannelStr);
		// Following our normal pattern, if we are setting for the tier, let's be sure to clear for the slots. 
		// Coding error from v13 or so set on slots. But we might add other functionality later. We still would want to clear slot controller. DEV-764)

		boolean shouldClearSlotPoscons = false;
		// Rarely needed. If changing from poscons to lights, clear out the old poscon indices.
		LedController ledController = this.getLedController();
		if (ledController != null && ledController.getDeviceType() != DeviceType.Poscons) {
			shouldClearSlotPoscons = true;
		}

		List<Location> slots = this.getActiveChildrenAtLevel(Slot.class);
		for (Location slot : slots) {
			if (shouldClearSlotPoscons) {
				if (slot.getPosconIndex() != null) {
					slot.setPosconIndex(null);
					Slot.staticGetDao().store(slot);
				}
			}
			LedController oldController = slot.getLedController();
			if (oldController != null) {
				oldController.removeLocation(slot);
				slot.clearControllerChannel(); // this does the DAO.store()
			}
		}
	}

	public void setControllerChannel(String inControllerPersistentIDStr, String inChannelStr, String inTiersStr) {
		// this is for callMethod from the UI
		// This, or all of this tier in aisle
		doSetControllerChannel(inControllerPersistentIDStr, inChannelStr);
		boolean allTiers = inTiersStr != null && inTiersStr.equalsIgnoreCase(ALL_TIERS_IN_AISLE);
		// if "aisle", then the rest of tiers at same level
		if (allTiers) {
			// The goal is to get to the aisle, then ask for all tiers. Filter those to the subset with the same domainID (like "T2")
			Bay bayParent = this.<Bay> getParentAtLevel(Bay.class);
			Aisle aisleParent = bayParent.<Aisle> getParentAtLevel(Aisle.class);
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

	public void offSetTierLeds(int offset) throws IllegalArgumentException {
		LedController ledController = this.getLedController();
		if (ledController == null) {
			throw new IllegalArgumentException("Failed to set LED offset on " + this + ": Tier has no LedController.");
		}
		if (ledController.getDeviceType() != DeviceType.Lights) {
			FormUtility.throwUiValidationException("LED Offset", "Failed to set LED offset on " + this + ": LedController "
					+ ledController + " is not of device type Lights.", ErrorCode.FIELD_INVALID);
		}

		LOGGER.info("Offsetting first/last LED by {} for each slot in {}", offset, this);
		List<Slot> slotList = this.getActiveChildrenAtLevel(Slot.class);
		if (slotList.size() == 0) {
			return;
		}
		// Don't really need to sort these as we do something common for each regardless. But follow the pattern.
		Collections.sort(slotList, new SlotIDComparator());

		//Adjust first and last LED of this entire Tier
		short tierNewFirstLed = (short) (getFirstLedNumAlongPath() + offset);
		short tierNewLastLed = (short) (getLastLedNumAlongPath() + offset);
		if (tierNewFirstLed < 0 || tierNewLastLed < 0 || tierNewFirstLed > tierNewLastLed) {
			FormUtility.throwUiValidationException("LED Offset", "Invalid Tier LED interval: [" + tierNewFirstLed + ", "
					+ tierNewLastLed + "].", ErrorCode.FIELD_INVALID);
		}
		setFirstLedNumAlongPath(tierNewFirstLed);
		setLastLedNumAlongPath(tierNewLastLed);

		// adjust first and last for each slot.
		ListIterator<Slot> li = null;
		li = slotList.listIterator();
		while (li.hasNext()) {
			Slot slot = (Slot) li.next();
			short firstLed = (short) (slot.getFirstLedNumAlongPath() + offset);
			short lastLed = (short) (slot.getLastLedNumAlongPath() + offset);
			if (firstLed > lastLed) {
				LOGGER.error("offSetTierLeds. How are first/last inverted? Skipping");
				continue;
			}
			if (firstLed <= 0) {
				LOGGER.warn("offSetTierLeds. User gave bad value leading to value <1. Skipping");
				continue;
			}
			slot.setFirstLedNumAlongPath(firstLed);
			slot.setLastLedNumAlongPath(lastLed);
			Slot.staticGetDao().store(slot);
		}
	}

	public void setPoscons(int startingIndex) {
		setPoscons(startingIndex, false);
	}

	public void setPoscons(int startingIndex, boolean reverseOrder) throws InputValidationException {
		LedController ledController = this.getLedController();
		if (ledController == null) {
			FormUtility.throwUiValidationException("Start Index", "Failed to set poscons on " + this.getBestUsableLocationName()
					+ ": Tier has no direct device controller.", ErrorCode.FIELD_INVALID);
		}
		if (ledController.getDeviceType() != DeviceType.Poscons) {
			String error = "Failed to set poscons on " + this.getBestUsableLocationName() + ": controller " + ledController
					+ " is not of device type Poscon.";
			FormUtility.throwUiValidationException("Start Index", error, ErrorCode.FIELD_INVALID);
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
			// do not set the led controller on the slot itself. Let it inherit up to the tier.
			slot.setPosconIndex(posconIndex);
			Slot.staticGetDao().store(slot);
			posconIndex++;
		}
	}

	public Slot createSlot(String inSlotId, Point inAnchorPoint, Point inPickFaceEndPoint) {
		Slot slot = new Slot();
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

	@Override
	public String getMetersFromLeft() {
		// Tier value same as its parent bay value, at least for now.
		Bay bay = this.getParentAtLevel(Bay.class);
		if (bay == null)
			return "";
		return bay.getMetersFromLeft();
	}

	public static Tier as(Location location) {
		if (location == null) {
			return null;
		}
		if (location.isTier()) {
			return (Tier) location;
		}
		throw new RuntimeException("Location is not a tier: " + location);
	}

	public static void sortByDomainId(List<Tier> tiers) {
		java.util.Collections.sort(tiers, new TierBayComparable());
	}

	/**
	 * Property that uses String type so UX edit works
	 * getTapeIdUi should return the same form as is printed on Codeshelf tape. Base 32 so that a big enough number is represented in
	 * few enough characters for a human to remember and enter.
	 */
	public String getTapeIdUi() {
		Integer value = getTapeId();
		if (value != null && value > 0)
			return CodeshelfTape.intToBase32(value);
		else {
			return "";
		}
	}

	/**
	 * Property that uses String type  so UX edit works
	 * This is primarily for our UX editor. Parameter normally will not have the leading %
	 * Throws InputValidationException if the value does not resolve to a good tape ID. The result of that is
	 * red cell in the UX.
	 * Very important: we allow the user to clear the value by passing in empty string.
	 */
	public void setTapeIdUi(String inTapeGuidString) {
		// Check the clear case. Only clear if changed
		if ("".equals(inTapeGuidString)) {
			Integer value = getTapeId();
			if (value != null && value > 0) {
				LOGGER.info("Clearing tape Id on tier {}.", this.getBestUsableLocationName());
				setTapeId(null);
			}
			return; // either way, return so we o not throw below.
		}

		Integer guidValue = -1; // this matches the error value in CodeshelfTape.extractGuid()
		if (Strings.isNullOrEmpty(inTapeGuidString) || inTapeGuidString.trim().length() == 0) {

		} else {
			try {
				guidValue = CodeshelfTape.extractGuid(inTapeGuidString);
			} catch (NumberFormatException e) {
				throw new InputValidationException(this, "tape Id", inTapeGuidString, ErrorCode.FIELD_WRONG_TYPE);
			}
		}
		if (guidValue > 0) {
			// don't log and update on no change
			Integer oldValue = getTapeId();
			if (!guidValue.equals(oldValue)) {
				LOGGER.info("Setting tape Id on tier {}. Entered value {} converted to database value {}.",
					this.getBestUsableLocationName(),
					inTapeGuidString,
					guidValue);
				setTapeId(guidValue);
			}
		} else
			throw new InputValidationException(this, "tape Id", inTapeGuidString, ErrorCode.FIELD_WRONG_TYPE);
	}

	/**
	 * This is used to force somewhat unusual LED values to the slots and tier.
	 * Must be in a transaction already
	 * inSlotStartingLeds string has the easily parseable form of "2/25/27/42"
	 */
	public void setSlotTierLeds(int inTierStartLed,
		int inLedCountTier,
		boolean inLowerLedNearAnchor,
		int inLedsPerSlot,
		String inSlotStartingLeds) {
		setFirstLedNumAlongPath((short) inTierStartLed);
		int lastTierLed = inTierStartLed + inLedCountTier - 1;
		setLastLedNumAlongPath((short) lastTierLed);
		setLowerLedNearAnchor(inLowerLedNearAnchor);

		Tier.staticGetDao().store(this);
		String[] strArray = inSlotStartingLeds.split("/");
		int slotCountForLeds = strArray.length;
		int[] firstLedArray = new int[slotCountForLeds];
		List<Slot> slots = this.getSlotsInDomainIdOrder();
		if (slots.size() != slotCountForLeds) {
			LOGGER.warn("LEDs format:{} did not match slot count in", this.getFullDomainId());
			return;
		}

		try {
			for (int i = 0; i < strArray.length; i++) {
				firstLedArray[i] = Integer.parseInt(strArray[i]);
			}
		} catch (NumberFormatException e) {
			LOGGER.warn("Bad starting LEDs format:{}. Do as 2/8/14", inSlotStartingLeds);
			return;
		}

		if (inLedsPerSlot <= 0)
			return;
		// We want the slots sorted either S1 to Sn, or Sn to S1
		// Then iterate through the slots But might have to go in reverse

		if (!inLowerLedNearAnchor) {
			Collections.reverse(slots);
		}

		int index = 0;
		for (Location slot : slots) {
			short firstSlotLed = (short) firstLedArray[index];
			short lastSlotLed = (short) (firstSlotLed + inLedsPerSlot - 1);

			slot.setFirstLedNumAlongPath(firstSlotLed);
			slot.setLastLedNumAlongPath(lastSlotLed);
			setLowerLedNearAnchor(inLowerLedNearAnchor);
			Slot.staticGetDao().store(slot);
			index++;
		}
	}

	/**
	 * Convenience function
	 */
	public List<Slot> getSlotsInDomainIdOrder() {
		List<Slot> slotList = new ArrayList<Slot>();
		for (Location slot : getChildren()) {
			slotList.add((Slot) slot);
		}
		Collections.sort(slotList, new SlotIDComparator());
		return slotList;
	}

	private int intFromShort(Short inShort) {
		if (inShort == null)
			return 0;
		else
			return (int) inShort;
	}

	/**
	 * Main purpose is to produce the parameters for a call to setSlotTierLEDs(). See it in the console or log
	 */
	public void logSlotTierLedParameters() {
		int firstLed = intFromShort(getFirstLedNumAlongPath());
		int lastLed = intFromShort(getLastLedNumAlongPath());
		
		int totalLed = 0;
		if (firstLed > 0)
			totalLed = lastLed - firstLed + 1;
		boolean increaseFromAnchor = isLowerLedNearAnchor();
		String increaseStr = "N";
		if (increaseFromAnchor)
			increaseStr = "Y";
		String slotStarts = "";
		int ledsPerSlot = 0;
		List<Slot> slots = getSlotsInDomainIdOrder();
		if (!increaseFromAnchor) {
			Collections.reverse(slots);
		}
		for (Slot slot : slots) {
			slotStarts += "/" + slot.getFirstLedNumAlongPath();
			ledsPerSlot = intFromShort(slot.getLastLedNumAlongPath()) - intFromShort(slot.getFirstLedNumAlongPath()) + 1;
		}
		// strip off the first 
		slotStarts = slotStarts.substring(1);

		LOGGER.info("function,setSlotTierLEDs,{},{},{},{},{},{}",
			this,
			firstLed,
			totalLed,
			increaseStr,
			ledsPerSlot,
			slotStarts);
		/*setSlotTierLEDs(int inTierStartLed,
			int inLedCountTier,
			boolean inLowerLedNearAnchor,
			int inLedsPerSlot,
			String inSlotStartingLeds) */
	}

}
