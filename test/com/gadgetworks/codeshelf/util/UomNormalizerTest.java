package com.gadgetworks.codeshelf.util;

import org.junit.Assert;
import org.junit.Test;

public class UomNormalizerTest {

	@Test
	public void testNormalizeString() {
		// If these change, then existing data in databases is in trouble. Complicated upgrade actions needed.
		Assert.assertEquals("EA", UomNormalizer.EACH);
		Assert.assertEquals("CS", UomNormalizer.CASE);
		
		// case equivalents
		Assert.assertEquals(UomNormalizer.CASE, UomNormalizer.normalizeString("Case"));
		Assert.assertEquals(UomNormalizer.CASE, UomNormalizer.normalizeString("case"));
		Assert.assertEquals(UomNormalizer.CASE, UomNormalizer.normalizeString("cs"));
		Assert.assertEquals(UomNormalizer.CASE, UomNormalizer.normalizeString("CASE"));

		// each equivalents
		Assert.assertEquals(UomNormalizer.EACH, UomNormalizer.normalizeString("Each"));
		Assert.assertEquals(UomNormalizer.EACH, UomNormalizer.normalizeString("EACH"));
		Assert.assertEquals(UomNormalizer.EACH, UomNormalizer.normalizeString("ea"));
		Assert.assertEquals(UomNormalizer.EACH, UomNormalizer.normalizeString("Ea"));
		Assert.assertEquals(UomNormalizer.EACH, UomNormalizer.normalizeString("pick"));
		Assert.assertEquals(UomNormalizer.EACH, UomNormalizer.normalizeString("pk"));
		Assert.assertEquals(UomNormalizer.EACH, UomNormalizer.normalizeString("PK")); // For Accu-Logistics and their Amazon orders

		// General capitalization behavior
		Assert.assertEquals("BAG", UomNormalizer.normalizeString("bag"));
		Assert.assertEquals("BAG", UomNormalizer.normalizeString("Bag"));
		
		// Show some things that do not normalize
		Assert.assertNotEquals("BAG", UomNormalizer.normalizeString("bg"));   // bag is not a standard unit
		Assert.assertNotEquals("BAG", UomNormalizer.normalizeString("bag ")); // does not do trailing blanks (ok to add that someday if needed)
		Assert.assertNotEquals("BAG", UomNormalizer.normalizeString(" bag")); // nor leading

	}
}
