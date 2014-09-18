/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2014, Codeshelf, Inc., All rights reserved
 *  $Id: Facility.java,v 1.82 2013/11/05 06:14:55 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model;

/**
 * Primarily a class to return some out parameters.
 * But does the led computation for non-slotted
 * 
 */
public class LedRange {
	public int		mFirstLedToLight;
	public int		mLastLedToLight;
	@SuppressWarnings("unused")
	private int		mLedCountToLight;
	private int		mFirstLocationLed;
	private int		mLastLocationLed;
	private Double	mLocationWidth;
	private Double	mMetersFromAnchor;
	private boolean	mFirstLedNearAnchor;

	public LedRange() {
		mFirstLedToLight = 0;
		mLastLedToLight = 0;
		mLedCountToLight = 4;
		mFirstLocationLed = 0;
		mLastLocationLed = 0;
		mLocationWidth = 0.0;
		mMetersFromAnchor = 0.0;
		mFirstLedNearAnchor = true;
	}

	public void setLedCountToLight(int inLedsToLight) {
		mLedCountToLight = inLedsToLight; // probably will not call this for a while. Just use the default of 4.
	}

	public void computeLedsToLight(int inFirstLocLed,
		int inLastLocLed,
		Double inLocWidth,
		Double inMetersFromAnchor,
		boolean inFirstLedNearAnchor) {
		mFirstLocationLed = inFirstLocLed;
		mLastLocationLed = inLastLocLed;
		mLocationWidth = inLocWidth;
		if (inLocWidth == null)
			mLocationWidth = 0.0;
		mMetersFromAnchor = inMetersFromAnchor;
		if (inMetersFromAnchor == null)
			mMetersFromAnchor = 0.0;
		mFirstLedNearAnchor = inFirstLedNearAnchor;
		_computeLedsToLight();
	}

	public void computeLedsToLightForLocationNoOffset(int inFirstLocLed,
		int inLastLocLed,
		boolean inFirstLedNearAnchor) {
		mFirstLocationLed = inFirstLocLed;
		mLastLocationLed = inLastLocLed;
		mLocationWidth = 0.0;
		mMetersFromAnchor = 0.0;
		mFirstLedNearAnchor = inFirstLedNearAnchor; // Does not really matter. Just centers in the led span
		_computeLedsToLight();
	}
	
	public String getRangeString(){
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


	private void _computeLedsToLight() {
		// Note mLocationWidth = 0 may be commonly used by computeLedsToLightForLocationNoOffset. There is a width, but does not matter to the computation. So passed as 0.
		mFirstLedToLight = 0;
		mLastLedToLight = 0;
		if (mFirstLocationLed <= 0 || mLastLocationLed <= 0) {
			// log?
			return;
		}
		if (mFirstLocationLed > mLastLocationLed) {
			// log?
			return;
		}
		int ledSpan = mLastLocationLed - mFirstLocationLed + 1;
		if (mMetersFromAnchor < 0.0 || mLocationWidth < 0.0) {
			// log?
			return;
		}
		if (mMetersFromAnchor > mLocationWidth) {
			// log?
			return;
		}
		Double fractionOfSpan = 0.0;
		Boolean slottedInventory = false;
		Boolean uninitializedCmFromLeft = false;
		if (mMetersFromAnchor.equals((Double) 0.0) || mLocationWidth.equals((Double) 0.0) || mMetersFromAnchor.equals(mLocationWidth))  {// if uninitialized, just take the middle of the location
			// tricky. cmFromLeft is 0 if uninitialized, but we convert to metersFromAnchor, which may be the full width.
			fractionOfSpan = 0.5;
			uninitializedCmFromLeft = true;
		}
		else
			fractionOfSpan = mMetersFromAnchor/mLocationWidth;
		
		// let's detect slotted inventory
		if (uninitializedCmFromLeft && ledSpan <= 5)
			slottedInventory = true;
		
		// if slotted, we just want to return first and last led for the location
		if (slottedInventory) {
			mFirstLedToLight = mFirstLocationLed;
			mLastLedToLight = mLastLocationLed;
			return;
		}
		
		// non-slotted: do the calculation
		
		if (!mFirstLedNearAnchor)
			fractionOfSpan = 1.0 - fractionOfSpan;
		int centralLed = (int) Math.round((fractionOfSpan) * ledSpan) + mFirstLocationLed - 1;
		//  for four leds, we will go one less, to two more, unless we hit the end of the range.
		//  need to add a computation based on mLedCountToLight someday
		
		int ledsLess = 1;
		int ledsMore = 2;
		mFirstLedToLight = centralLed - ledsLess;
		if (mFirstLedToLight < mFirstLocationLed)
			mFirstLedToLight = mFirstLocationLed;
		mLastLedToLight = centralLed + ledsMore;
		if (mLastLedToLight > mLastLocationLed)
			mLastLedToLight = mLastLocationLed;

	}

}
