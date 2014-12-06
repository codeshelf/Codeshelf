package com.gadgetworks.codeshelf.model.domain;

import java.util.UUID;

import org.junit.Assert;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;

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
		itemMaster.addItemToMaster(item);
		
		item.setStoredLocation(facility);
		String result = item.getPosAlongPathui();
		Assert.assertEquals("0", result);
		
		this.getPersistenceService().commitTenantTransaction();
	}
	
	@Test
	public void testItemWithNullCmFromLeft() {
		this.getPersistenceService().beginTenantTransaction();

		Facility anyLocation = createDefaultFacility("ORG-testItemWithLocationNoPath");
		Item item = new Item();
		item.setDomainId("domain");
		
		ItemMaster itemMaster = new ItemMaster();
		itemMaster.addItemToMaster(item);
		
		item.setStoredLocation(anyLocation);
		item.setItemCmFromLeft(" "); //allowed
		Assert.assertNull(item.getPosAlongPath());
		
		this.getPersistenceService().commitTenantTransaction();

	}
	
	@Test
	public void testCriteriaByTier() {
		this.getPersistenceService().beginTenantTransaction();

		Item.DAO.findByFilterAndClass("itemsByFacilityAndLocation", ImmutableMap.<String, Object>of("facilityId", UUID.randomUUID(), "locationId", UUID.randomUUID()), Item.class);
		
		this.getPersistenceService().commitTenantTransaction();
		
	}

}
