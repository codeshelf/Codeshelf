package com.gadgetworks.codeshelf.model.domain;

import org.junit.Assert;
import org.junit.Test;

public class ItemTest extends DomainTestABC {

	@Test
	public void testItemWithoutAssignedTier() {
		Item item = new Item();
		String tier = item.getItemTier();
		Assert.assertNotNull(tier);
		Assert.assertEquals("", tier);
	}
	
	@Test
	public void testItemWithLocationNoPath() {
		Facility facility = createDefaultFacility("ORG-testItemWithLocationNoPath");
		Item item = new Item(new ItemMaster(), "domain");
		item.setStoredLocation(facility);
		String result = item.getPosAlongPathui();
		Assert.assertEquals("0", result);
	}
	
	@Test
	public void testItemWithNullCmFromLeft() {
		Facility anyLocation = createDefaultFacility("ORG-testItemWithLocationNoPath");
		Item item = new Item(new ItemMaster(), "domain");
		item.setStoredLocation(anyLocation);
		item.setItemCmFromLeft(" "); //allowed
		Assert.assertNull(item.getPosAlongPath());
	}

}
