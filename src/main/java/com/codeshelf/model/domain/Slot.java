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
			throw new IllegalArgumentException("First led id " + firstLed + " is larger than last id " + lastLed);
		}
		Location tier = getParent();
		short tierFirstLed = tier.getFirstLedNumAlongPath();
		short tierLastLed = tier.getLastLedNumAlongPath();
		if (tierFirstLed > firstLed || tierLastLed < lastLed){
			throw new IllegalArgumentException(String.format("Provided interval %d-%d does not fit within current Tier's bounds %d-%d", firstLed, lastLed, tierFirstLed, tierLastLed));
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
