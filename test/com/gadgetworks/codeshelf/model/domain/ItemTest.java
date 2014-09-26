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
		this.getPersistenceService().beginTenantTransaction();

		Facility facility = createDefaultFacility("ORG-testItemWithLocationNoPath");
		Item item = new Item();
		item.setDomainId("domain");

		ItemMaster itemMaster = new ItemMaster();
		itemMaster.addItem(item);
		
		item.setStoredLocation(facility);
		String result = item.getPosAlongPathui();
		Assert.assertEquals("0", result);
		
		this.getPersistenceService().endTenantTransaction();
	}
	
	@Test
	public void testItemWithNullCmFromLeft() {
		this.getPersistenceService().beginTenantTransaction();

		Facility anyLocation = createDefaultFacility("ORG-testItemWithLocationNoPath");
		Item item = new Item();
		item.setDomainId("domain");
		
		ItemMaster itemMaster = new ItemMaster();
		itemMaster.addItem(item);
		
		item.setStoredLocation(anyLocation);
		item.setItemCmFromLeft(" "); //allowed
		Assert.assertNull(item.getPosAlongPath());
		
		this.getPersistenceService().endTenantTransaction();

	}

}
