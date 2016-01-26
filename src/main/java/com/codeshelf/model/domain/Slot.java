/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: Slot.java,v 1.6 2013/04/11 07:42:44 jeffw Exp $
 *******************************************************************************/
package com.codeshelf.model.domain;

import java.util.List;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.model.SlotComparable;
import com.codeshelf.model.dao.GenericDaoABC;
import com.codeshelf.model.dao.ITypedDao;
import com.codeshelf.persistence.TenantPersistenceService;
import com.codeshelf.util.FormUtility;
import com.codeshelf.validation.ErrorCode;
import com.fasterxml.jackson.annotation.JsonAutoDetect;

// --------------------------------------------------------------------------
/**
 * Slot
 * 
 * The object that models a slit within a tier.
 * 
 * @author jeffw
 */

@Entity
@DiscriminatorValue("SLOT")
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE)
public class Slot extends Location {

	public static class SlotDao extends GenericDaoABC<Slot> implements ITypedDao<Slot> {
		public final Class<Slot> getDaoClass() {
			return Slot.class;
		}
	}

	@SuppressWarnings("unused")
	private static final Logger	LOGGER	= LoggerFactory.getLogger(Slot.class);

	public Slot() {
		super();
	}

	@SuppressWarnings("unchecked")
	public final ITypedDao<Slot> getDao() {
		return staticGetDao();
	}

	public static ITypedDao<Slot> staticGetDao() {
		return TenantPersistenceService.getInstance().getDao(Slot.class);
	}

	public final String getDefaultDomainIdPrefix() {
		return "T";
	}
	
	public String getSlotIdForComparable() {
		return getCompString(getDomainId());
	}

	public static void sortByDomainId(List<Slot> slots) {
		java.util.Collections.sort(slots, new SlotComparable());
	}
	
	@Override
	public boolean isSlot() {
		return true;
	}
	
	@Override
	public String getMetersFromLeft() {
		// nearly cloned from Bay.getMetersFromLeft()
		Aisle aisle = this.getParentAtLevel(Aisle.class);
		Tier tier = this.getParentAtLevel(Tier.class);
		if (aisle == null || tier == null || aisle.getPathSegment()== null)
			return "";
		boolean leftB1S1 = aisle.isLeftSideTowardB1S1();
		boolean pathFromAnchor = aisle.isPathIncreasingFromAnchor();
		Double tierValue = tier.getPosAlongPath();
		Double slotValue = this.getPosAlongPath();
		// Hurts to think about it, but it comes down to this simple difference
		if (leftB1S1 != pathFromAnchor) {
			tierValue = tierValue + tier.getLocationWidthMeters();
			slotValue = slotValue + this.getLocationWidthMeters();
		}
		// depending on how the user set up the path relative to aisle, values could be not quite exact.
		Double diff = Math.abs(tierValue - slotValue);
		if (diff < .01)
			return "0";
		return String.format("%.2f", diff);
	}

	
	public static Slot as(Location location) {
		if (location==null) {
			return null;
		}
		if (location.isSlot()) {
	    	return (Slot) location;
	    }
		throw new RuntimeException("Location is not a slot");
	}
	
	public void setSlotLeds(String firstLedStr, String lastLedStr) throws IllegalArgumentException {
		short firstLed = parseShort(firstLedStr);
		short lastLed = parseShort(lastLedStr);
		if (firstLed > lastLed) {
			FormUtility.throwUiValidationException("LED Values", "First led id " + firstLed + " is larger than last id " + lastLed, ErrorCode.FIELD_INVALID);
		}
		Location tier = getParent();
		short tierFirstLed = tier.getFirstLedNumAlongPath();
		short tierLastLed = tier.getLastLedNumAlongPath();
		if (tierFirstLed > firstLed || tierLastLed < lastLed){
			String error = String.format("Provided interval [%d, %d] does not fit within current Tier's bounds [%d, %d]", firstLed, lastLed, tierFirstLed, tierLastLed);
			FormUtility.throwUiValidationException("LED Values", error, ErrorCode.FIELD_INVALID);
		}
		setFirstLedNumAlongPath(firstLed);
		setLastLedNumAlongPath(lastLed);
	}
	
	private short parseShort(String value) throws IllegalArgumentException{
		try {
			return Short.parseShort(value);
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException("Could not parse '" + value + "' as a Short");
		}
	}
}