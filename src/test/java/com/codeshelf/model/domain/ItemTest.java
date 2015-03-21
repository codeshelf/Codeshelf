package com.codeshelf.model.domain;

import java.util.UUID;

import org.junit.Assert;
import org.junit.Test;

import com.codeshelf.testframework.HibernateTest;
import com.google.common.collect.ImmutableMap;

public class ItemTest extends HibernateTest {

	@Test
	public void testItemWithoutAssignedTier() {
		Item item = new Item();
		String tier = item.getItemTier();
		Assert.assertNotNull(tier);
		Assert.assertEquals("", tier);
	}
	
	@Test
	public void testItemWithLocationNoPath() {
		this.getTenantPersistenceService().beginTransaction(getDefaultTenant());

		Facility facility = createFacility();
		Item item = new Item();
		item.setDomainId("domain");

		ItemMaster itemMaster = new ItemMaster();
		itemMaster.addItemToMaster(item);
		
		item.setStoredLocation(facility);
		String result = item.getPosAlongPathui();
		Assert.assertEquals("0", result);
		
		this.getTenantPersistenceService().commitTransaction(getDefaultTenant());
	}
	
	@Test
	public void testItemWithNullCmFromLeft() {
		this.getTenantPersistenceService().beginTransaction(getDefaultTenant());

		Facility anyLocation = createFacility();
		Item item = new Item();
		item.setDomainId("domain");
		
		ItemMaster itemMaster = new ItemMaster();
		itemMaster.addItemToMaster(item);
		
		item.setStoredLocation(anyLocation);
		item.setCmFromLeftui(" "); //allowed
		Assert.assertNull(item.getPosAlongPath());
		
		this.getTenantPersistenceService().commitTransaction(getDefaultTenant());

	}
	
	@Test
	public void testCriteriaByTier() {
		this.getTenantPersistenceService().beginTransaction(getDefaultTenant());

		Item.staticGetDao().findByFilter(getDefaultTenant(),"itemsByFacilityAndLocation", ImmutableMap.<String, Object>of("facilityId", UUID.randomUUID(), "locationId", UUID.randomUUID()));
		
		this.getTenantPersistenceService().commitTransaction(getDefaultTenant());
		
	}

}
