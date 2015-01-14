package com.gadgetworks.codeshelf.edi;


import java.io.IOException;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.gadgetworks.codeshelf.model.domain.Facility;
import com.gadgetworks.codeshelf.model.domain.Item;
import com.gadgetworks.codeshelf.model.domain.ItemMaster;
import com.gadgetworks.codeshelf.model.domain.Location;
import com.gadgetworks.codeshelf.model.domain.Tier;
import com.gadgetworks.codeshelf.model.domain.UomMaster;
import com.gadgetworks.codeshelf.platform.persistence.PersistenceService;
import com.gadgetworks.codeshelf.service.UiUpdateService;
import com.gadgetworks.codeshelf.validation.InputValidationException;

/**
 * @author jeffw
 *
 */
public class InventoryServiceTest extends EdiTestABC {


	private UUID facilityId;
	private UiUpdateService uiUpdate = new UiUpdateService();
	
	@Before
	public void initTest() throws IOException {
		PersistenceService.getInstance().beginTenantTransaction();
		VirtualSlottedFacilityGenerator generator = new VirtualSlottedFacilityGenerator(createAisleFileImporter(), createLocationAliasImporter(), createOrderImporter());
		Facility facility = generator.generateFacilityForVirtualSlotting(testName.getMethodName());
		generator.setupOrders(facility);
		
		this.facilityId=facility.getPersistentId();
		PersistenceService.getInstance().commitTenantTransaction();
	}
	
	/**
	 * Given an each item at a location
	 * When we upsert a another location for the an item with the same itemId and uom
	 * Then the item is "moved" to the new location
	 * @throws IOException 
	 */
	@Test
	public void testExistingItemWithEachIsMoved() throws IOException {
		this.getPersistenceService().beginTenantTransaction();
		Facility facility=Facility.DAO.findByPersistentId(facilityId);

		String testUom = "each";
		testMove(facility,testUom);

		this.getPersistenceService().commitTenantTransaction();
	}

	@Test
	public void testExistingItemWithEACHAliasIsMoved() throws IOException {
		this.getPersistenceService().beginTenantTransaction();
		Facility facility=Facility.DAO.findByPersistentId(facilityId);

		String testUom = "EA";
		testMove(facility,testUom);

		this.getPersistenceService().commitTenantTransaction();
	}
	
	/**
	 * Given a non-each item at a location
	 * When we upsert another location of the item with same itemId and uom
	 * Then a new item is created  
	 */
	@Test
	public void testNonEachItemCreatedIfDifferentLocation() throws IOException {
		this.getPersistenceService().beginTenantTransaction();
		Facility facility=Facility.DAO.findByPersistentId(facilityId);
		
		String testUom = "case";
		String sku = "10706961";
		Integer testCmFromLeft = 10;
		
		Tier tier = (Tier) facility.findSubLocationById("A1.B1.T1");
		ItemMaster itemMaster = facility.getItemMaster(sku);
		String locationAlias = tier.getAliases().get(0).getAlias();


		
		Item createdItem = uiUpdate.upsertItem(facility.getPersistentId().toString(), itemMaster.getItemId(), locationAlias, String.valueOf(testCmFromLeft - 1), "1", testUom, null);
		
		Tier newItemLocation = (Tier) facility.findSubLocationById("A2.B1.T1");
		String newItemLocationAlias = newItemLocation.getAliases().get(0).getAlias();
		Assert.assertNotEquals(locationAlias, newItemLocationAlias);
		Item additionalItem = uiUpdate.upsertItem(facility.getPersistentId().toString(), itemMaster.getItemId(), newItemLocationAlias, String.valueOf(testCmFromLeft), "15", testUom, null);

		Assert.assertNotEquals("Should not be the same item", createdItem.getPersistentId(), additionalItem.getPersistentId());
		Assert.assertEquals(createdItem.getUomMaster(), additionalItem.getUomMaster());
		Assert.assertEquals(createdItem.getItemId(), additionalItem.getItemId());

		this.getPersistenceService().commitTenantTransaction();
	}
	

	/**
	 * Given a non-each item at a location
	 * When we upsert item infor with same itemId, location and uom
	 * Then the existing item is updated  
	 */
	@Test
	public void testNonEachItemIsUpdated() throws IOException {
		this.getPersistenceService().beginTenantTransaction();
		Facility facility=Facility.DAO.findByPersistentId(facilityId);

		String testUom = "case";
		String sku = "10706961";
		Integer testCmFromLeft = 10;
		
		Tier tier = (Tier) facility.findSubLocationById("A1.B1.T1");
		ItemMaster itemMaster = facility.getItemMaster(sku);
		String locationAlias = tier.getAliases().get(0).getAlias();
		
		Item createdItem = uiUpdate.upsertItem(facility.getPersistentId().toString(), itemMaster.getItemId(), locationAlias, String.valueOf(testCmFromLeft - 1), "1", testUom, null);

		Item updatedItem = uiUpdate.upsertItem(facility.getPersistentId().toString(), itemMaster.getItemId(), locationAlias, String.valueOf(testCmFromLeft), "15", testUom, null);
		Assert.assertEquals("Should have been the same item", createdItem.getPersistentId(), updatedItem.getPersistentId());
		Assert.assertEquals(testCmFromLeft, updatedItem.getCmFromLeft());

		this.getPersistenceService().commitTenantTransaction();
	}
	

	
	private void testMove(Facility facility,String testUomUserInput) throws IOException {
		Tier tier = (Tier) facility.findSubLocationById("A1.B1.T1");
		ItemMaster itemMaster = facility.getItemMaster("10700589");
		String locationAlias = tier.getAliases().get(0).getAlias();
		
		Item createdItem = uiUpdate.upsertItem(facility.getPersistentId().toString(), itemMaster.getItemId(), locationAlias, "1", "1", testUomUserInput, null);

		Tier newItemLocation = (Tier) facility.findSubLocationById("A2.B1.T1");
		String newItemLocationAlias = newItemLocation.getAliases().get(0).getAlias();
		Assert.assertNotEquals(locationAlias, newItemLocationAlias);
		
		Item movedItem = uiUpdate.upsertItem(facility.getPersistentId().toString(), itemMaster.getItemId(), newItemLocationAlias, "1", "1", testUomUserInput, null);
		Assert.assertEquals("Should have been the same item", createdItem.getPersistentId(), movedItem.getPersistentId());
		Location currentLocation = movedItem.getStoredLocation(); 
		Assert.assertEquals(newItemLocation.getNominalLocationId(), currentLocation.getNominalLocationId());
	}
	
	@SuppressWarnings("unused")
	@Test
	public void testUpsertItemNullLocationAlias() throws IOException {
		this.getPersistenceService().beginTenantTransaction();
		Facility facility=Facility.DAO.findByPersistentId(facilityId);

		Tier tier = (Tier) facility.findSubLocationById("A1.B1.T1");
		UomMaster uomMaster = facility.getUomMaster("each");
		ItemMaster itemMaster = facility.getItemMaster("10700589");
		String locationAlias = tier.getAliases().get(0).getAlias();
		
		try {
			uiUpdate.upsertItem(facility.getPersistentId().toString(), itemMaster.getItemId(), null, "1", "1", uomMaster.getUomMasterId(), null);
			Assert.fail("Should have thrown exception");
		}
		catch (InputValidationException e) {
			Assert.assertTrue(e.hasViolationForProperty("storedLocation"));
		}

		this.getPersistenceService().commitTenantTransaction();
	}

	@SuppressWarnings("unused")
	@Test
	public void testUpsertItemEmptyLocationAlias() throws IOException {
		this.getPersistenceService().beginTenantTransaction();
		Facility facility=Facility.DAO.findByPersistentId(facilityId);

		Tier tier = (Tier) facility.findSubLocationById("A1.B1.T1");
		UomMaster uomMaster = facility.getUomMaster("each");
		ItemMaster itemMaster = facility.getItemMaster("10700589");
		String locationAlias = tier.getAliases().get(0).getAlias();
		try {
			uiUpdate.upsertItem(facility.getPersistentId().toString(), itemMaster.getItemId(), "", "1", "1", uomMaster.getUomMasterId(), null);
			Assert.fail("Should have thrown exception");
		}
		catch (InputValidationException e) {
			Assert.assertTrue(e.hasViolationForProperty("storedLocation"));
		}

		this.getPersistenceService().commitTenantTransaction();
	}

	@SuppressWarnings("unused")
	@Test
	public void testUpsertItemUsingAlphaCount() throws IOException {
		this.getPersistenceService().beginTenantTransaction();
		Facility facility=Facility.DAO.findByPersistentId(facilityId);

		Tier tier = (Tier) facility.findSubLocationById("A1.B1.T1");
		UomMaster uomMaster = facility.getUomMaster("each");
		ItemMaster itemMaster = facility.getItemMaster("10700589");
		String locationAlias = tier.getAliases().get(0).getAlias();

		try {
			uiUpdate.upsertItem(facility.getPersistentId().toString(), itemMaster.getItemId(), locationAlias, "1", "A", uomMaster.getUomMasterId(), null);
			Assert.fail("Should have thrown exception");
		}
		catch (InputValidationException e) {
			Assert.assertTrue(e.hasViolationForProperty("quantity"));
		}

		this.getPersistenceService().commitTenantTransaction();
	}

	@SuppressWarnings("unused")
	@Test
	public void testUpsertItemUsingNegativeCount() throws IOException {
		this.getPersistenceService().beginTenantTransaction();
		Facility facility=Facility.DAO.findByPersistentId(facilityId);

		Tier tier = (Tier) facility.findSubLocationById("A1.B1.T1");
		UomMaster uomMaster = facility.getUomMaster("each");
		ItemMaster itemMaster = facility.getItemMaster("10700589");
		String locationAlias = tier.getAliases().get(0).getAlias();

		try {
			uiUpdate.upsertItem(facility.getPersistentId().toString(), itemMaster.getItemId(), locationAlias, "1", "-1", uomMaster.getUomMasterId(), null);
			Assert.fail("Should have thrown exception");
		}
		catch (InputValidationException e) {
			Assert.assertTrue(e.hasViolationForProperty("quantity"));
		}

		this.getPersistenceService().commitTenantTransaction();
	}

	@SuppressWarnings("unused")
	@Test
	public void testUpsertItemUsingNegativePositionFromLeft() throws IOException {
		this.getPersistenceService().beginTenantTransaction();
		Facility facility=Facility.DAO.findByPersistentId(facilityId);

		Tier tier = (Tier) facility.findSubLocationById("A1.B1.T1");
		UomMaster uomMaster = facility.getUomMaster("each");
		ItemMaster itemMaster = facility.getItemMaster("10700589");
		String locationAlias = tier.getAliases().get(0).getAlias();

		try {
			Item item = uiUpdate.upsertItem(facility.getPersistentId().toString(), itemMaster.getItemId(), locationAlias, "-1", "1", uomMaster.getUomMasterId(), null);
			Assert.fail("Should have thrown exception");
		}
		catch (InputValidationException e) {
			Assert.assertTrue(e.hasViolationForProperty("cmFromLeft"));
		}

		this.getPersistenceService().commitTenantTransaction();
	}
	
	@SuppressWarnings("unused")
	@Test
	public void testUpsertItemUsingAlphaPositionFromLeft() throws IOException {
		this.getPersistenceService().beginTenantTransaction();
		Facility facility=Facility.DAO.findByPersistentId(facilityId);

		Tier tier = (Tier) facility.findSubLocationById("A1.B1.T1");
		UomMaster uomMaster = facility.getUomMaster("each");
		ItemMaster itemMaster = facility.getItemMaster("10700589");
		String locationAlias = tier.getAliases().get(0).getAlias();

		try {
			Item item = uiUpdate.upsertItem(facility.getPersistentId().toString(), itemMaster.getItemId(), locationAlias, "A", "1", uomMaster.getUomMasterId(), null);
			Assert.fail("Should have thrown exception");
		}
		catch (InputValidationException e) {
			Assert.assertTrue(e.hasViolationForProperty("cmFromLeft"));
		}

		this.getPersistenceService().commitTenantTransaction();
	}
	
	@Test
	public void testUpsertItemUsingEmptyPositionFromLeft() throws IOException {
		this.getPersistenceService().beginTenantTransaction();
		Facility facility=Facility.DAO.findByPersistentId(facilityId);

		Tier tier = (Tier) facility.findSubLocationById("A1.B1.T1");
		UomMaster uomMaster = facility.getUomMaster("each");
		ItemMaster itemMaster = facility.getItemMaster("10700589");
		String locationAlias = tier.getAliases().get(0).getAlias();
		Item item = uiUpdate.upsertItem(facility.getPersistentId().toString(), itemMaster.getItemId(), locationAlias, "", "1", uomMaster.getUomMasterId(), null);
		Assert.assertEquals(0, item.getCmFromLeft().intValue());

		this.getPersistenceService().commitTenantTransaction();
	}
	
	@Test
	public void testUpsertItemUsingNullPositionFromLeft() throws IOException {
		this.getPersistenceService().beginTenantTransaction();
		Facility facility=Facility.DAO.findByPersistentId(facilityId);

		Tier tier = (Tier) facility.findSubLocationById("A1.B1.T1");
		UomMaster uomMaster = facility.getUomMaster("each");
		ItemMaster itemMaster = facility.getItemMaster("10700589");
		String locationAlias = tier.getAliases().get(0).getAlias();
		Item item = uiUpdate.upsertItem(facility.getPersistentId().toString(), itemMaster.getItemId(), locationAlias, null, "1", uomMaster.getUomMasterId(), null);
		Assert.assertEquals(0, item.getCmFromLeft().intValue());

		this.getPersistenceService().commitTenantTransaction();
	}
	
	@SuppressWarnings("unused")
	@Test
	public void testUpsertItemUsingEmptyUom() throws IOException {
		this.getPersistenceService().beginTenantTransaction();
		Facility facility=Facility.DAO.findByPersistentId(facilityId);

		Tier tier = (Tier) facility.findSubLocationById("A1.B1.T1");
		UomMaster uomMaster = facility.getUomMaster("each");
		ItemMaster itemMaster = facility.getItemMaster("10700589");
		String locationAlias = tier.getAliases().get(0).getAlias();

		try {
			Item item = uiUpdate.upsertItem(facility.getPersistentId().toString(), itemMaster.getItemId(), locationAlias, "1", "1", "", null);
			Assert.fail("Should have thrown exception");
		}
		catch (InputValidationException e) {
			Assert.assertTrue(e.toString(), e.hasViolationForProperty("uomMasterId"));
		}

		this.getPersistenceService().commitTenantTransaction();
	}

	@SuppressWarnings("unused")
	@Test
	public void testUpsertItemUsingUomDifferentCase() throws IOException {
		this.getPersistenceService().beginTenantTransaction();
		Facility facility=Facility.DAO.findByPersistentId(facilityId);

		Tier tier = (Tier) facility.findSubLocationById("A1.B1.T1");
		UomMaster uomMaster = facility.getUomMaster("each");
		ItemMaster itemMaster = facility.getItemMaster("10700589");
		Item item = uiUpdate.upsertItem(facility.getPersistentId().toString(), itemMaster.getItemId(), tier.getNominalLocationId(), "1", "1", "EACH", null);
		Assert.assertEquals(tier, item.getStoredLocation());

		this.getPersistenceService().commitTenantTransaction();
	}
	
	@Test
	public void testUpsertItemUsingNominalLocationId() throws IOException {
		this.getPersistenceService().beginTenantTransaction();
		Facility facility=Facility.DAO.findByPersistentId(facilityId);

		Tier tier = (Tier) facility.findSubLocationById("A1.B1.T1");
		UomMaster uomMaster = facility.getUomMaster("each");
		ItemMaster itemMaster = facility.getItemMaster("10700589");

		
		Item item = uiUpdate.upsertItem(facility.getPersistentId().toString(), itemMaster.getItemId(), tier.getNominalLocationId(), "1", "1", uomMaster.getUomMasterId(), null);
		Assert.assertEquals(tier, item.getStoredLocation());

		this.getPersistenceService().commitTenantTransaction();
	}

	@Test
	public void testUpsertItemUsingLocationAlias() throws IOException {
		this.getPersistenceService().beginTenantTransaction();
		Facility facility=Facility.DAO.findByPersistentId(facilityId);

		Tier tier = (Tier) facility.findSubLocationById("A1.B1.T1");
		UomMaster uomMaster = facility.getUomMaster("each");
		ItemMaster itemMaster = facility.getItemMaster("10700589");

		String locationAlias = tier.getAliases().get(0).getAlias();
		Item item = uiUpdate.upsertItem(facility.getPersistentId().toString(), itemMaster.getItemId(), locationAlias, "1", "1", uomMaster.getUomMasterId(), null);
		Assert.assertEquals(tier, item.getStoredLocation());

		this.getPersistenceService().commitTenantTransaction();
	}
	
}
