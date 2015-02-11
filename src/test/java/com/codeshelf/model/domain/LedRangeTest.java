/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: FacilityTest.java,v 1.11 2013/11/11 07:46:30 jeffw Exp $
 *******************************************************************************/
package com.codeshelf.model.domain;

import org.junit.Assert;
import org.junit.Test;

import com.codeshelf.model.LedRange;

/**
 * @author ranstrom
 *
 */
public class LedRangeTest {

	@Test
	public final void rangeTest() {

		LedRange ledRange;
		// parameters are the first and last led of the location,
		// then location width, and meters from anchor
		// and lastly whether the first led is on the anchor side or not.

		// 1/5 of 80 is 16, so should light 15 - 18
		ledRange = LedRange.computeLedsToLight(1, 80, true, 2.5, 0.5);
		assertRange(ledRange, 15,18);

		// invert the led direction
		ledRange = LedRange.computeLedsToLight(1, 80, false, 2.5, 0.5);
		assertRange(ledRange, 63, 66);

		// common case of cmFromLeft not being set yet, so value is 0.0
		ledRange = LedRange.computeLedsToLight(1, 80, true, 2.5, 0.0);
		assertRange(ledRange, 39, 42);

		// Same, but the Double is null rather than coming as 0.0
		ledRange = LedRange.computeLedsToLight(1, 80, true, 2.5, null);
		assertRange(ledRange, 39, 42);

		// Edge cases
		// lighting 4, but range is only 3
		ledRange = LedRange.computeLedsToLight(1, 3, true, 2.5, 0.5);
		assertRange(ledRange, 1,3);

		ledRange = LedRange.computeLedsToLight(1, 3, false, 2.5, 0.5);
		assertRange(ledRange, 1,3); // not quite symmetrical. Don't care. Safe.

		// lighting 4, but on low edge. Computed middle is 0, so 0+2 =2 for high number.
		ledRange = LedRange.computeLedsToLight(1, 80, true, 2.5, 0.01);
		assertRange(ledRange, 1,2);

		ledRange = LedRange.computeLedsToLight(1, 80, false, 2.5, 0.01);
		assertRange(ledRange, 79,80);

		// lighting 4, but on high edge
		ledRange = LedRange.computeLedsToLight(1, 80, true, 2.5, 2.49);
		assertRange(ledRange, 79,80);
		ledRange = LedRange.computeLedsToLight(1, 80, false, 2.5, 0.01);
		assertRange(ledRange, 79,80);

		// various out of range. These error cases return zero values. Only some are logged
		ledRange = LedRange.computeLedsToLight(0, 0, true,  2.5, 0.5);
		assertRange(ledRange, 0, 0);

		// this next line will log an error for meters from anchor greater than width.
		ledRange = LedRange.computeLedsToLight(1, 80, false, 2.5, 3.0);
		assertRange(ledRange, 0, 0);

		ledRange = LedRange.computeLedsToLight(-10, 80, false, 2.5, 0.5);
		assertRange(ledRange, 0, 0);

		ledRange = LedRange.computeLedsToLight(80, 1, false, 2.5, 0.5);
		assertRange(ledRange, 0, 0);

		// this next line will log an error for negative meters from anchor.
		ledRange = LedRange.computeLedsToLight(1, 80, false, 2.5, -0.5);
		assertRange(ledRange, 0, 0);

		// Uninitialized tier case. Light the middle 4
		ledRange = LedRange.computeLedsToLight(1, 80, true, 2.5, 0.0);
		assertRange(ledRange, 39,42);

		// Non-slotted case. Light the location values exactly
		ledRange = LedRange.computeLedsToLight(36, 39, true, 2.5, 0.0);
		assertRange(ledRange, 36, 39);

		// Non-slotted case??? but too many leds. Light the middle 4
		ledRange = LedRange.computeLedsToLight(29, 39, true, 2.5, 0.0);
		assertRange(ledRange, 33,36);
	}

	private void assertRange(LedRange inLedRange, int inFirstLedPosition, int inLastLedPosition) {
		Assert.assertEquals(inFirstLedPosition, inLedRange.getFirstLedToLight());
		Assert.assertEquals(inLastLedPosition, inLedRange.getLastLedToLight());

	}


}
