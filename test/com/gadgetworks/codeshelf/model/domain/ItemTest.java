package com.gadgetworks.codeshelf.model.domain;

import org.junit.Assert;
import org.junit.Test;

public class ItemTest {

	@Test
	public void testItemWithoutAssignedTier() {
		Item item = new Item();
		String tier = item.getItemTier();
		Assert.assertNotNull(tier);
		Assert.assertEquals("", tier);
	}

}
