/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: FacilityTest.java,v 1.11 2013/11/11 07:46:30 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.domain;

import org.junit.Assert;
import org.junit.Test;

import com.gadgetworks.codeshelf.model.LedRange;

/**
 * @author ranstrom
 *
 */
public class LedRangeTest {

	@Test
	public final void rangeTest() {

		LedRange ledRange = new LedRange();
		// parameters are the first and last led of the location,
		// then location width, and meters from anchor
		// and lastly whether the first led is on the anchor side or not.

		// 1/5 of 80 is 16, so should light 15 - 18
		ledRange.computeLedsToLight(1, 80, 2.5, 0.5, true);
		Assert.assertEquals(15, ledRange.mFirstLedToLight);
		Assert.assertEquals(18, ledRange.mLastLedToLight);
		// invert the led direction
		ledRange.computeLedsToLight(1, 80, 2.5, 0.5, false);
		Assert.assertEquals(63, ledRange.mFirstLedToLight);
		Assert.assertEquals(66, ledRange.mLastLedToLight);
		
		// common case of cmFromLeft not being set yet, so value is 0.0
		ledRange.computeLedsToLight(1, 80, 2.5, 0.0, true);
		Assert.assertEquals(39, ledRange.mFirstLedToLight);
		Assert.assertEquals(42, ledRange.mLastLedToLight);

		// Same, but the Double is null rather than coming as 0.0
		ledRange.computeLedsToLight(1, 80, 2.5, null, true);
		Assert.assertEquals(39, ledRange.mFirstLedToLight);
		Assert.assertEquals(42, ledRange.mLastLedToLight);

		// Edge cases
		// lighting 4, but range is only 3
		ledRange.computeLedsToLight(1, 3, 2.5, 0.5, true);
		Assert.assertEquals(1, ledRange.mFirstLedToLight);
		Assert.assertEquals(3, ledRange.mLastLedToLight);
		ledRange.computeLedsToLight(1, 3, 2.5, 0.5, false);
		Assert.assertEquals(1, ledRange.mFirstLedToLight);
		Assert.assertEquals(3, ledRange.mLastLedToLight); // not quite symmetrical. Don't care. Safe.

		// lighting 4, but on low edge. Computed middle is 0, so 0+2 =2 for high number.
		ledRange.computeLedsToLight(1, 80, 2.5, 0.01, true);
		Assert.assertEquals(1, ledRange.mFirstLedToLight);
		Assert.assertEquals(2, ledRange.mLastLedToLight);
		ledRange.computeLedsToLight(1, 80, 2.5, 0.01, false);
		Assert.assertEquals(79, ledRange.mFirstLedToLight);
		Assert.assertEquals(80, ledRange.mLastLedToLight);

		// lighting 4, but on high edge
		ledRange.computeLedsToLight(1, 80, 2.5, 2.49, true);
		Assert.assertEquals(79, ledRange.mFirstLedToLight);
		Assert.assertEquals(80, ledRange.mLastLedToLight);
		ledRange.computeLedsToLight(1, 80, 2.5, 0.01, false);
		Assert.assertEquals(79, ledRange.mFirstLedToLight);
		Assert.assertEquals(80, ledRange.mLastLedToLight);
		
		// various out of range. These error cases return zero values. Only some are logged
		ledRange.computeLedsToLight(0, 0, 2.5, 0.5, true);
		Assert.assertEquals(0, ledRange.mFirstLedToLight);
		Assert.assertEquals(0, ledRange.mLastLedToLight);
		
		// this next line will log an error for meters from anchor greater than width. 
		ledRange.computeLedsToLight(1, 80, 2.5, 3.0, false);
		Assert.assertEquals(0, ledRange.mFirstLedToLight);
		Assert.assertEquals(0, ledRange.mLastLedToLight);

		ledRange.computeLedsToLight(-10, 80, 2.5, 0.5, false);
		Assert.assertEquals(0, ledRange.mFirstLedToLight);
		Assert.assertEquals(0, ledRange.mLastLedToLight);

		ledRange.computeLedsToLight(80, 1, 2.5, 0.5, false);
		Assert.assertEquals(0, ledRange.mFirstLedToLight);
		Assert.assertEquals(0, ledRange.mLastLedToLight);

		// this next line will log an error for negative meters from anchor.
		ledRange.computeLedsToLight(1, 80, 2.5, -0.5, false);
		Assert.assertEquals(0, ledRange.mFirstLedToLight);
		Assert.assertEquals(0, ledRange.mLastLedToLight);

		// Uninitialized tier case. Light the middle 4
		ledRange.computeLedsToLight(1, 80, 2.5, 0.0, true);
		Assert.assertEquals(39, ledRange.mFirstLedToLight);
		Assert.assertEquals(42, ledRange.mLastLedToLight);
		
		// Non-slotted case. Light the location values exactly
		ledRange.computeLedsToLight(36, 39, 2.5, 0.0, true);
		Assert.assertEquals(36, ledRange.mFirstLedToLight);
		Assert.assertEquals(39, ledRange.mLastLedToLight);
		
		// Non-slotted case??? but too many leds. Light the middle 4
		ledRange.computeLedsToLight(29, 39, 2.5, 0.0, true);
		Assert.assertEquals(33, ledRange.mFirstLedToLight);
		Assert.assertEquals(36, ledRange.mLastLedToLight);


	}


}
