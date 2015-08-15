/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2014, Codeshelf, Inc., All rights reserved
 *  $Id: Facility.java,v 1.82 2013/11/05 06:14:55 jeffw Exp $
 *******************************************************************************/
package com.codeshelf.model;

import lombok.ToString;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Primarily a class to return some out parameters.
 * But does the led computation for non-slotted
 * 
 */
@ToString()
public class LedRange {

	private static final Logger		LOGGER	= LoggerFactory.getLogger(LedRange.class);
	private static final LedRange	ZERO	= new LedRange(0, 0);

	private int						mFirstLedToLight;
	private int						mLastLedToLight;

	public LedRange(int inFirstLedToLight, int inLastLedToLight) {
		mFirstLedToLight = inFirstLedToLight;
		mLastLedToLight = inLastLedToLight;
	}

	public static LedRange zero() {
		return ZERO;
	}

	public static LedRange computeLedsToLight(int inFirstLocLed,
		int inLastLocLed,
		boolean inFirstLedNearAnchor,
		Double inLocWidth,
		Double inMetersFromAnchor, boolean inIsSlot) {
		if (inLocWidth == null)
			inLocWidth = 0.0;
		if (inMetersFromAnchor == null)
			inMetersFromAnchor = 0.0;
		return _computeLedsToLight(inFirstLocLed, inLastLocLed, inFirstLedNearAnchor, inLocWidth, inMetersFromAnchor, inIsSlot);
	}

	public static LedRange computeLedsToLightForLocationNoOffset(int inFirstLocLed, int inLastLocLed, boolean inFirstLedNearAnchor, boolean inIsSlot) {
		return _computeLedsToLight(inFirstLocLed, inLastLocLed, inFirstLedNearAnchor, 0.0, 0.0, inIsSlot);
	}

	/**
	 * This restricts, and center, the maxLed value return a different, revised LedRange.
	 * Monstrous kludge for the full pallet case of 32 LEDs for a slot. If so, do not respect the max.
	 * This is Walmart Palletizer case
	 */
	public LedRange capLeds(int maxLeds) {
		int howMany = totalLeds();
		int limit = maxLeds;
		if (howMany > 25 && howMany < 32) {
			limit = howMany;
		} else {
			limit = Math.min(howMany, maxLeds);
		}
		boolean ascending = (mLastLedToLight - mFirstLedToLight) >= 0;
		if (ascending) {
			return new LedRange(mFirstLedToLight, Math.max(0, mFirstLedToLight + limit - 1));
		} else {
			return new LedRange(mFirstLedToLight, Math.max(0, mFirstLedToLight - limit - 1));
		}
	}

	private int totalLeds() {

		return Math.abs(mLastLedToLight - mFirstLedToLight) + 1;
	}

	public String getRangeString() {
		if (mFirstLedToLight == 0)
			return ""; // just blank if there are no values

		String returnStr = "";
		returnStr = ((Integer) (mFirstLedToLight)).toString();
		returnStr += ">";
		returnStr += ((Integer) (mLastLedToLight)).toString();
		return returnStr;
	}

	public short getFirstLedToLight() {
		// This API because LED command groups want shorts
		return (short) mFirstLedToLight;
	}

	public short getLastLedToLight() {
		// This API because LED command groups want shorts
		return (short) mLastLedToLight;
	}

	/**
	 * Inclusive
	 */
	public boolean isWithin(short ledPosition) {
		return (mFirstLedToLight <= ledPosition && ledPosition <= mLastLedToLight);
	}

	private static LedRange _computeLedsToLight(int inFirstLocationLed,
		int inLastLocationLed,
		boolean inFirstLedNearAnchor,
		double inLocationWidth,
		double inMetersFromAnchor, boolean inIsSlot) {
		// Note mLocationWidth = 0 may be commonly used by computeLedsToLightForLocationNoOffset. There is a width, but does not matter to the computation. So passed as 0.
		if (inFirstLocationLed <= 0 || inLastLocationLed <= 0) {
			// log?
			return zero();
		}
		if (inFirstLocationLed > inLastLocationLed) {
			// log?
			return zero();
		}

		if (inMetersFromAnchor < 0.0 || inLocationWidth < 0.0) {
			LOGGER.error("unexpected width or metersFromAnchor in _computeLedsToLight");
			;
			return zero();
		}
		if (inMetersFromAnchor > inLocationWidth) {
			LOGGER.error("metersFromAnchor larger than width in _computeLedsToLight");
			;
			return zero();
		}

		double fractionOfSpan = 0.5;
		boolean uninitializedCmFromLeft = false;
		if (inLocationWidth == 0.0 || inMetersFromAnchor == 0.0 || inMetersFromAnchor == inLocationWidth) {// if uninitialized, just take the middle of the location
			// tricky. cmFromLeft is 0 if uninitialized, but we convert to metersFromAnchor, which may be the full width.
			uninitializedCmFromLeft = true;
		} else {
			fractionOfSpan = inMetersFromAnchor / inLocationWidth;
		}

		// let's detect slotted inventory
		int ledSpan = inLastLocationLed - inFirstLocationLed + 1;

		// if slotted, let a large LED range ride. This is the Walmart Palletizer situation
		if (uninitializedCmFromLeft && (inIsSlot || ledSpan <= 5 ) ) { //slotted
			return new LedRange(inFirstLocationLed, inLastLocationLed);
		} else { // non-slotted: do the calculation
			if (!inFirstLedNearAnchor) {
				fractionOfSpan = 1.0 - fractionOfSpan;
			}

			int centralLed = (int) Math.round((fractionOfSpan) * ledSpan) + inFirstLocationLed - 1;
			//  for four leds, we will go one less, to two more, unless we hit the end of the range.
			//  need to add a computation based on mLedCountToLight someday

			int ledsLess = 1;
			int ledsMore = 2;

			int firstLedToLight = centralLed - ledsLess;
			if (firstLedToLight < inFirstLocationLed) {
				firstLedToLight = inFirstLocationLed;
			}
			int lastLedToLight = centralLed + ledsMore;
			if (lastLedToLight > inLastLocationLed) {
				lastLedToLight = inLastLocationLed;
			}
			return new LedRange(firstLedToLight, lastLedToLight);
		}
	}

}
