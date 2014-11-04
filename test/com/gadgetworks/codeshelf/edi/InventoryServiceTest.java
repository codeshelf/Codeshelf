package com.gadgetworks.codeshelf.edi;


import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Timestamp;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.gadgetworks.codeshelf.model.domain.Facility;
import com.gadgetworks.codeshelf.model.domain.Item;
import com.gadgetworks.codeshelf.model.domain.ItemMaster;
import com.gadgetworks.codeshelf.model.domain.Tier;
import com.gadgetworks.codeshelf.model.domain.UomMaster;
import com.gadgetworks.codeshelf.validation.InputValidationException;

/**
 * @author jeffw
 *
 */
public class InventoryServiceTest extends EdiTestABC {


	private Facility facility;
	
	@Before
	public void initTest() throws IOException {
		VirtualSlottedFacilityGenerator generator = new VirtualSlottedFacilityGenerator(createAisleFileImporter(), createLocationAliasImporter(), createOrderImporter());
		facility = generator.generateFacilityForVirtualSlotting(testName.getMethodName());
		generator.setupOrders(facility);
	}
	
	/**
	 * Given an each item at a location
	 * When we upsert a another location for the an item with the same itemId and uom
	 * Then the item is "moved" to the new location
	 * @throws IOException 
	 */
	@Test
	public void testExistingItemWithEachIsMoved() throws IOException {
		String testUom = "each";
		testMove(testUom);
	}

	@Test
	public void testExistingItemWithEACHAliasIsMoved() throws IOException {
		String testUom = "EA";
		testMove(testUom);
	}
	
	/**
	 * Given a non-each item at a location
	 * When we upsert another location of the item with same itemId and uom
	 * Then a new item is created  
	 */
	@Test
	public void testNonEachItemCreatedIfDifferentLocation() throws IOException {
		String testUom = "case";
		String sku = "10706961";
		Integer testCmFromLeft = 10;
		
		Tier tier = (Tier) facility.findSubLocationById("A1.B1.T1");
		ItemMaster itemMaster = facility.getItemMaster(sku);
		String locationAlias = tier.getAliases().get(0).getAlias();


		
		Item createdItem = facility.upsertItem(itemMaster.getItemId(), locationAlias, String.valueOf(testCmFromLeft - 1), "1", testUom);
		
		Tier newItemLocation = (Tier) facility.findSubLocationById("A2.B1.T1");
		String newItemLocationAlias = newItemLocation.getAliases().get(0).getAlias();
		Assert.assertNotEquals(locationAlias, newItemLocationAlias);
		Item additionalItem = facility.upsertItem(itemMaster.getItemId(), newItemLocationAlias, String.valueOf(testCmFromLeft), "15", testUom);

		Assert.assertNotEquals("Should not be the same item", createdItem.getPersistentId(), additionalItem.getPersistentId());
		Assert.assertEquals(createdItem.getUomMaster(), additionalItem.getUomMaster());
		Assert.assertEquals(createdItem.getItemId(), additionalItem.getItemId());
	}
	

	/**
	 * Given a non-each item at a location
	 * When we upsert item infor with same itemId, location and uom
	 * Then the existing item is updated  
	 */
	@Test
	public void testNonEachItemIsUpdated() throws IOException {
		String testUom = "case";
		String sku = "10706961";
		Integer testCmFromLeft = 10;
		
		Tier tier = (Tier) facility.findSubLocationById("A1.B1.T1");
		ItemMaster itemMaster = facility.getItemMaster(sku);
		String locationAlias = tier.getAliases().get(0).getAlias();
		
		Item createdItem = facility.upsertItem(itemMaster.getItemId(), locationAlias, String.valueOf(testCmFromLeft - 1), "1", testUom);

		Item updatedItem = facility.upsertItem(itemMaster.getItemId(), locationAlias, String.valueOf(testCmFromLeft), "15", testUom);
		Assert.assertEquals("Should have been the same item", createdItem.getPersistentId(), updatedItem.getPersistentId());
		Assert.assertEquals(testCmFromLeft, updatedItem.getCmFromLeft());
	}
	

	
	private void testMove(String testUomUserInput) throws IOException {
		Tier tier = (Tier) facility.findSubLocationById("A1.B1.T1");
		ItemMaster itemMaster = facility.getItemMaster("10700589");
		String locationAlias = tier.getAliases().get(0).getAlias();
		
		Item createdItem = facility.upsertItem(itemMaster.getItemId(), locationAlias, "1", "1", testUomUserInput);

		Tier newItemLocation = (Tier) facility.findSubLocationById("A2.B1.T1");
		String newItemLocationAlias = newItemLocation.getAliases().get(0).getAlias();
		Assert.assertNotEquals(locationAlias, newItemLocationAlias);
		
		Item movedItem = facility.upsertItem(itemMaster.getItemId(), newItemLocationAlias, "1", "1", testUomUserInput);
		Assert.assertEquals("Should have been the same item", createdItem.getPersistentId(), movedItem.getPersistentId());
		Assert.assertEquals(movedItem.getStoredLocation(), newItemLocation);
	}
	
	@SuppressWarnings("unused")
	@Test
	public void testUpsertItemNullLocationAlias() throws IOException {
		Tier tier = (Tier) facility.findSubLocationById("A1.B1.T1");
		UomMaster uomMaster = facility.getUomMaster("each");
		ItemMaster itemMaster = facility.getItemMaster("10700589");
		String locationAlias = tier.getAliases().get(0).getAlias();
		
		try {
			facility.upsertItem(itemMaster.getItemId(), null, "1", "1", uomMaster.getUomMasterId());
			Assert.fail("Should have thrown exception");
		}
		catch (InputValidationException e) {
			Assert.assertTrue(e.hasViolationForProperty("storedLocation"));
		}
	}

	@SuppressWarnings("unused")
	@Test
	public void testUpsertItemEmptyLocationAlias() throws IOException {
		Tier tier = (Tier) facility.findSubLocationById("A1.B1.T1");
		UomMaster uomMaster = facility.getUomMaster("each");
		ItemMaster itemMaster = facility.getItemMaster("10700589");
		String locationAlias = tier.getAliases().get(0).getAlias();
		try {
			facility.upsertItem(itemMaster.getItemId(), "", "1", "1", uomMaster.getUomMasterId());
			Assert.fail("Should have thrown exception");
		}
		catch (InputValidationException e) {
			Assert.assertTrue(e.hasViolationForProperty("storedLocation"));
		}
	}

	@SuppressWarnings("unused")
	@Test
	public void testUpsertItemUsingAlphaCount() throws IOException {
		Tier tier = (Tier) facility.findSubLocationById("A1.B1.T1");
		UomMaster uomMaster = facility.getUomMaster("each");
		ItemMaster itemMaster = facility.getItemMaster("10700589");
		String locationAlias = tier.getAliases().get(0).getAlias();

		try {
			Item item = facility.upsertItem(itemMaster.getItemId(), locationAlias, "1", "A", uomMaster.getUomMasterId());
			Assert.fail("Should have thrown exception");
		}
		catch (InputValidationException e) {
			Assert.assertTrue(e.hasViolationForProperty("quantity"));
		}
	}

	@SuppressWarnings("unused")
	@Test
	public void testUpsertItemUsingNegativeCount() throws IOException {
		Tier tier = (Tier) facility.findSubLocationById("A1.B1.T1");
		UomMaster uomMaster = facility.getUomMaster("each");
		ItemMaster itemMaster = facility.getItemMaster("10700589");
		String locationAlias = tier.getAliases().get(0).getAlias();

		try {
			Item item = facility.upsertItem(itemMaster.getItemId(), locationAlias, "1", "-1", uomMaster.getUomMasterId());
			Assert.fail("Should have thrown exception");
		}
		catch (InputValidationException e) {
			Assert.assertTrue(e.hasViolationForProperty("quantity"));
		}
	}

	@SuppressWarnings("unused")
	@Test
	public void testUpsertItemUsingNegativePositionFromLeft() throws IOException {
		Tier tier = (Tier) facility.findSubLocationById("A1.B1.T1");
		UomMaster uomMaster = facility.getUomMaster("each");
		ItemMaster itemMaster = facility.getItemMaster("10700589");
		String locationAlias = tier.getAliases().get(0).getAlias();

		try {
			Item item = facility.upsertItem(itemMaster.getItemId(), locationAlias, "-1", "1", uomMaster.getUomMasterId());
			Assert.fail("Should have thrown exception");
		}
		catch (InputValidationException e) {
			Assert.assertTrue(e.hasViolationForProperty("cmFromLeft"));
		}
	}
	
	@SuppressWarnings("unused")
	@Test
	public void testUpsertItemUsingAlphaPositionFromLeft() throws IOException {
		Tier tier = (Tier) facility.findSubLocationById("A1.B1.T1");
		UomMaster uomMaster = facility.getUomMaster("each");
		ItemMaster itemMaster = facility.getItemMaster("10700589");
		String locationAlias = tier.getAliases().get(0).getAlias();

		try {
			Item item = facility.upsertItem(itemMaster.getItemId(), locationAlias, "A", "1", uomMaster.getUomMasterId());
			Assert.fail("Should have thrown exception");
		}
		catch (InputValidationException e) {
			Assert.assertTrue(e.hasViolationForProperty("positionFromLeft"));
		}
	}
	
	@Test
	public void testUpsertItemUsingEmptyPositionFromLeft() throws IOException {
		Tier tier = (Tier) facility.findSubLocationById("A1.B1.T1");
		UomMaster uomMaster = facility.getUomMaster("each");
		ItemMaster itemMaster = facility.getItemMaster("10700589");
		String locationAlias = tier.getAliases().get(0).getAlias();
		Item item = facility.upsertItem(itemMaster.getItemId(), locationAlias, "", "1", uomMaster.getUomMasterId());
		Assert.assertEquals(0, item.getCmFromLeft().intValue());
	}
	
	@Test
	public void testUpsertItemUsingNullPositionFromLeft() throws IOException {
		Tier tier = (Tier) facility.findSubLocationById("A1.B1.T1");
		UomMaster uomMaster = facility.getUomMaster("each");
		ItemMaster itemMaster = facility.getItemMaster("10700589");
		String locationAlias = tier.getAliases().get(0).getAlias();
		Item item = facility.upsertItem(itemMaster.getItemId(), locationAlias, null, "1", uomMaster.getUomMasterId());
		Assert.assertEquals(0, item.getCmFromLeft().intValue());
	}
	
	@SuppressWarnings("unused")
	@Test
	public void testUpsertItemUsingEmptyUom() throws IOException {
		Tier tier = (Tier) facility.findSubLocationById("A1.B1.T1");
		UomMaster uomMaster = facility.getUomMaster("each");
		ItemMaster itemMaster = facility.getItemMaster("10700589");
		String locationAlias = tier.getAliases().get(0).getAlias();

		try {
			Item item = facility.upsertItem(itemMaster.getItemId(), locationAlias, "1", "1", "");
			Assert.fail("Should have thrown exception");
		}
		catch (InputValidationException e) {
			Assert.assertTrue(e.toString(), e.hasViolationForProperty("uomMasterId"));
		}
	}

	@SuppressWarnings("unused")
	@Test
	public void testUpsertItemUsingUomDifferentCase() throws IOException {
		Tier tier = (Tier) facility.findSubLocationById("A1.B1.T1");
		UomMaster uomMaster = facility.getUomMaster("each");
		ItemMaster itemMaster = facility.getItemMaster("10700589");
		Item item = facility.upsertItem(itemMaster.getItemId(), tier.getNominalLocationId(), "1", "1", "EACH");
		Assert.assertEquals(tier, item.getStoredLocation());
	}
	
	@Test
	public void testUpsertItemUsingNominalLocationId() throws IOException {
		Tier tier = (Tier) facility.findSubLocationById("A1.B1.T1");
		UomMaster uomMaster = facility.getUomMaster("each");
		ItemMaster itemMaster = facility.getItemMaster("10700589");

		
		Item item = facility.upsertItem(itemMaster.getItemId(), tier.getNominalLocationId(), "1", "1", uomMaster.getUomMasterId());
		Assert.assertEquals(tier, item.getStoredLocation());
	}

	@Test
	public void testUpsertItemUsingLocationAlias() throws IOException {
		Tier tier = (Tier) facility.findSubLocationById("A1.B1.T1");
		UomMaster uomMaster = facility.getUomMaster("each");
		ItemMaster itemMaster = facility.getItemMaster("10700589");

		String locationAlias = tier.getAliases().get(0).getAlias();
		Item item = facility.upsertItem(itemMaster.getItemId(), locationAlias, "1", "1", uomMaster.getUomMasterId());
		Assert.assertEquals(tier, item.getStoredLocation());
	}
	
}
