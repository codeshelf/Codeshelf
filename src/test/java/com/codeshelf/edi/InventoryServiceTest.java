package com.codeshelf.edi;


import java.io.IOException;
import java.util.List;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.codeshelf.model.dao.PropertyDao;
import com.codeshelf.model.domain.DomainObjectProperty;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.Item;
import com.codeshelf.model.domain.ItemMaster;
import com.codeshelf.model.domain.Location;
import com.codeshelf.model.domain.LocationAlias;
import com.codeshelf.model.domain.Tier;
import com.codeshelf.model.domain.UomMaster;
import com.codeshelf.platform.persistence.TenantPersistenceService;
import com.codeshelf.service.UiUpdateService;
import com.codeshelf.validation.InputValidationException;

/**
 * @author jeffw
 *
 */
public class InventoryServiceTest extends EdiTestABC {


	private UUID facilityId;
	private UiUpdateService uiUpdate = new UiUpdateService();
	
	@Before
	public void initTest() throws IOException {
		TenantPersistenceService.getInstance().beginTenantTransaction();
		VirtualSlottedFacilityGenerator generator = new VirtualSlottedFacilityGenerator(getDefaultTenant(),createAisleFileImporter(), createLocationAliasImporter(), createOrderImporter());
		Facility facility = generator.generateFacilityForVirtualSlotting(testName.getMethodName());
		generator.setupOrders(facility);
		
		this.facilityId=facility.getPersistentId();
		TenantPersistenceService.getInstance().commitTenantTransaction();
	}
	
	/**
	 * Given an each item at a location
	 * When we upsert a another location for the an item with the same itemId and uom
	 * Then the item is "moved" to the new location
	 * @throws IOException 
	 */
	@Test
	public void testExistingItemWithEachIsMoved() throws IOException {
		this.getTenantPersistenceService().beginTenantTransaction();
		Facility facility=Facility.DAO.findByPersistentId(facilityId);

		String testUom = "each";
		testMove(facility,testUom);

		this.getTenantPersistenceService().commitTenantTransaction();
	}

	@Test
	public void testExistingItemWithEACHAliasIsMoved() throws IOException {
		this.getTenantPersistenceService().beginTenantTransaction();
		Facility facility=Facility.DAO.findByPersistentId(facilityId);

		String testUom = "EA";
		testMove(facility,testUom);

		this.getTenantPersistenceService().commitTenantTransaction();
	}
	
	@Test
	public void testEachMultiLoc() throws IOException {	
		this.getTenantPersistenceService().beginTenantTransaction();
		Facility facility=Facility.DAO.findByPersistentId(facilityId);

		PropertyDao propDao = PropertyDao.getInstance();
		DomainObjectProperty eachmultProp = propDao.getPropertyWithDefault(facility, DomainObjectProperty.EACHMULT);
		Assert.assertNotNull(eachmultProp);
		boolean eachMult = eachmultProp.getBooleanValue();
		Assert.assertEquals("EACHMULT is supposed to be FALSE by default",false, eachMult);		
		
		String testUom = "EA";
		Tier tier = (Tier) facility.findSubLocationById("A1.B1.T1");
		ItemMaster itemMaster = facility.getItemMaster("10700589");
		Assert.assertNotNull(itemMaster);
		String locationAlias = tier.getAliases().get(0).getAlias();
		Assert.assertNotNull(locationAlias);
		
		Item createdItem = uiUpdate.storeItem(facility.getPersistentId().toString(), itemMaster.getItemId(), locationAlias, "1", "1", testUom, null);

		Tier newItemLocation = (Tier) facility.findSubLocationById("A2.B1.T1");
		String newItemLocationAlias = newItemLocation.getAliases().get(0).getAlias();
		Assert.assertNotEquals(locationAlias, newItemLocationAlias);
		
		Item movedItem = uiUpdate.storeItem(facility.getPersistentId().toString(), itemMaster.getItemId(), newItemLocationAlias, "1", "1", testUom, null);
		Assert.assertEquals("Should have been the same item", createdItem.getPersistentId(), movedItem.getPersistentId());
		Location currentLocation = movedItem.getStoredLocation(); 
		Assert.assertEquals(newItemLocation.getNominalLocationId(), currentLocation.getNominalLocationId());
		this.getTenantPersistenceService().commitTenantTransaction();

		// set eachmult to true and make sure only one item exists
		this.getTenantPersistenceService().beginTenantTransaction();
		eachmultProp = propDao.getPropertyWithDefault(facility, DomainObjectProperty.EACHMULT);
		eachmultProp.setValue(true);
		propDao.store(eachmultProp);
		itemMaster = facility.getItemMaster("10700589");
		List<Item> items = itemMaster.getItemsOfUom(testUom);
		Assert.assertEquals(1,items.size());
		this.getTenantPersistenceService().commitTenantTransaction();	

		// now move the item again and ensure items are different
		this.getTenantPersistenceService().beginTenantTransaction();
		facility = Facility.DAO.reload(facility);

		itemMaster = facility.getItemMaster("10700589");
		Assert.assertNotNull(itemMaster);

		Tier newItemLocation2 = (Tier) facility.findSubLocationById("A1.B1.T1");
		String newItemLocationAlias2 = newItemLocation2.getAliases().get(0).getAlias();
		locationAlias = newItemLocation2.getAliases().get(0).getAlias();
		Assert.assertNotNull(locationAlias);
		
		Item movedItem2 = uiUpdate.storeItem(facility.getPersistentId().toString(), itemMaster.getItemId(), newItemLocationAlias2, "1", "1", testUom, null);
		Assert.assertNotEquals("Should not have been the same item", createdItem.getPersistentId(), movedItem2.getPersistentId());
		Location currentLocation2 = movedItem2.getStoredLocation(); 
		Assert.assertEquals(newItemLocation2.getNominalLocationId(), currentLocation2.getNominalLocationId());
		
		List<Item> items2 = itemMaster.getItemsOfUom(testUom);
		Assert.assertEquals(2,items2.size());
		
		this.getTenantPersistenceService().commitTenantTransaction();	
	}	
	
	/**
	 * Given a non-each item at a location
	 * When we upsert another location of the item with same itemId and uom
	 * Then a new item is created  
	 */
	@Test
	public void testNonEachItemCreatedIfDifferentLocation() throws IOException {
		this.getTenantPersistenceService().beginTenantTransaction();
		Facility facility=Facility.DAO.findByPersistentId(facilityId);
		
		String testUom = "case";
		String sku = "10706961";
		Integer testCmFromLeft = 10;
		
		Tier tier = (Tier) facility.findSubLocationById("A1.B1.T1");
		ItemMaster itemMaster = facility.getItemMaster(sku);
		String locationAlias = tier.getAliases().get(0).getAlias();


		
		Item createdItem = uiUpdate.storeItem(facility.getPersistentId().toString(), itemMaster.getItemId(), locationAlias, String.valueOf(testCmFromLeft - 1), "1", testUom, null);
		
		Tier newItemLocation = (Tier) facility.findSubLocationById("A2.B1.T1");
		String newItemLocationAlias = newItemLocation.getAliases().get(0).getAlias();
		Assert.assertNotEquals(locationAlias, newItemLocationAlias);
		Item additionalItem = uiUpdate.storeItem(facility.getPersistentId().toString(), itemMaster.getItemId(), newItemLocationAlias, String.valueOf(testCmFromLeft), "15", testUom, null);

		Assert.assertNotEquals("Should not be the same item", createdItem.getPersistentId(), additionalItem.getPersistentId());
		Assert.assertEquals(createdItem.getUomMaster(), additionalItem.getUomMaster());
		Assert.assertEquals(createdItem.getItemId(), additionalItem.getItemId());

		this.getTenantPersistenceService().commitTenantTransaction();
	}
	

	/**
	 * Given a non-each item at a location
	 * When we upsert item infor with same itemId, location and uom
	 * Then the existing item is updated  
	 */
	@Test
	public void testNonEachItemIsUpdated() throws IOException {
		this.getTenantPersistenceService().beginTenantTransaction();
		Facility facility=Facility.DAO.findByPersistentId(facilityId);

		String testUom = "case";
		String sku = "10706961";
		Integer testCmFromLeft = 10;
		
		Tier tier = (Tier) facility.findSubLocationById("A1.B1.T1");
		ItemMaster itemMaster = facility.getItemMaster(sku);
		String locationAlias = tier.getAliases().get(0).getAlias();
		
		Item createdItem = uiUpdate.storeItem(facility.getPersistentId().toString(), itemMaster.getItemId(), locationAlias, String.valueOf(testCmFromLeft - 1), "1", testUom, null);

		Item updatedItem = uiUpdate.storeItem(facility.getPersistentId().toString(), itemMaster.getItemId(), locationAlias, String.valueOf(testCmFromLeft), "15", testUom, null);
		Assert.assertEquals("Should have been the same item", createdItem.getPersistentId(), updatedItem.getPersistentId());
		Assert.assertEquals(testCmFromLeft, updatedItem.getCmFromLeft());

		this.getTenantPersistenceService().commitTenantTransaction();
	}
	

	
	private void testMove(Facility facility,String testUomUserInput) throws IOException {
		Tier tier = (Tier) facility.findSubLocationById("A1.B1.T1");
		ItemMaster itemMaster = facility.getItemMaster("10700589");
		String locationAlias = tier.getAliases().get(0).getAlias();
		
		Item createdItem = uiUpdate.storeItem(facility.getPersistentId().toString(), itemMaster.getItemId(), locationAlias, "1", "1", testUomUserInput, null);

		Tier newItemLocation = (Tier) facility.findSubLocationById("A2.B1.T1");
		String newItemLocationAlias = newItemLocation.getAliases().get(0).getAlias();
		Assert.assertNotEquals(locationAlias, newItemLocationAlias);
		
		Item movedItem = uiUpdate.storeItem(facility.getPersistentId().toString(), itemMaster.getItemId(), newItemLocationAlias, "1", "1", testUomUserInput, null);
		Assert.assertEquals("Should have been the same item", createdItem.getPersistentId(), movedItem.getPersistentId());
		Location currentLocation = movedItem.getStoredLocation(); 
		Assert.assertEquals(newItemLocation.getNominalLocationId(), currentLocation.getNominalLocationId());
	}
	
	@SuppressWarnings("unused")
	@Test
	public void testUpsertItemNullLocationAlias() throws IOException {
		this.getTenantPersistenceService().beginTenantTransaction();
		Facility facility=Facility.DAO.findByPersistentId(facilityId);

		Tier tier = (Tier) facility.findSubLocationById("A1.B1.T1");
		UomMaster uomMaster = facility.getUomMaster("each");
		ItemMaster itemMaster = facility.getItemMaster("10700589");
		String locationAlias = tier.getAliases().get(0).getAlias();
		
		try {
			uiUpdate.storeItem(facility.getPersistentId().toString(), itemMaster.getItemId(), null, "1", "1", uomMaster.getUomMasterId(), null);
			Assert.fail("Should have thrown exception");
		}
		catch (InputValidationException e) {
			Assert.assertTrue(e.hasViolationForProperty("storedLocation"));
		}

		this.getTenantPersistenceService().commitTenantTransaction();
	}

	@SuppressWarnings("unused")
	@Test
	public void testUpsertItemEmptyLocationAlias() throws IOException {
		this.getTenantPersistenceService().beginTenantTransaction();
		Facility facility=Facility.DAO.findByPersistentId(facilityId);

		Tier tier = (Tier) facility.findSubLocationById("A1.B1.T1");
		UomMaster uomMaster = facility.getUomMaster("each");
		ItemMaster itemMaster = facility.getItemMaster("10700589");
		String locationAlias = tier.getAliases().get(0).getAlias();
		try {
			uiUpdate.storeItem(facility.getPersistentId().toString(), itemMaster.getItemId(), "", "1", "1", uomMaster.getUomMasterId(), null);
			Assert.fail("Should have thrown exception");
		}
		catch (InputValidationException e) {
			Assert.assertTrue(e.hasViolationForProperty("storedLocation"));
		}

		this.getTenantPersistenceService().commitTenantTransaction();
	}

	@Test
	public void testUpsertItemUsingAlphaCount() throws IOException {
		this.getTenantPersistenceService().beginTenantTransaction();
		Facility facility=Facility.DAO.findByPersistentId(facilityId);

		Tier tier = (Tier) facility.findSubLocationById("A1.B1.T1");
		Assert.assertNotNull(tier);
		UomMaster uomMaster = facility.getUomMaster("each");
		ItemMaster itemMaster = facility.getItemMaster("10700589");
		List<LocationAlias> aliases = tier.getAliases();
		Assert.assertTrue(aliases.size()>0);
		String locationAlias = tier.getAliases().get(0).getAlias();

		try {
			uiUpdate.storeItem(facility.getPersistentId().toString(), itemMaster.getItemId(), locationAlias, "1", "A", uomMaster.getUomMasterId(), null);
			Assert.fail("Should have thrown exception");
		}
		catch (InputValidationException e) {
			Assert.assertTrue(e.hasViolationForProperty("quantity"));
		}

		this.getTenantPersistenceService().commitTenantTransaction();
	}

	@Test
	public void testUpsertItemUsingNegativeCount() throws IOException {
		this.getTenantPersistenceService().beginTenantTransaction();
		Facility facility=Facility.DAO.findByPersistentId(facilityId);

		Tier tier = (Tier) facility.findSubLocationById("A1.B1.T1");
		UomMaster uomMaster = facility.getUomMaster("each");
		ItemMaster itemMaster = facility.getItemMaster("10700589");
		String locationAlias = tier.getAliases().get(0).getAlias();

		try {
			uiUpdate.storeItem(facility.getPersistentId().toString(), itemMaster.getItemId(), locationAlias, "1", "-1", uomMaster.getUomMasterId(), null);
			Assert.fail("Should have thrown exception");
		}
		catch (InputValidationException e) {
			Assert.assertTrue(e.hasViolationForProperty("quantity"));
		}

		this.getTenantPersistenceService().commitTenantTransaction();
	}

	@SuppressWarnings("unused")
	@Test
	public void testUpsertItemUsingNegativePositionFromLeft() throws IOException {
		this.getTenantPersistenceService().beginTenantTransaction();
		Facility facility=Facility.DAO.findByPersistentId(facilityId);

		Tier tier = (Tier) facility.findSubLocationById("A1.B1.T1");
		UomMaster uomMaster = facility.getUomMaster("each");
		ItemMaster itemMaster = facility.getItemMaster("10700589");
		String locationAlias = tier.getAliases().get(0).getAlias();

		try {
			Item item = uiUpdate.storeItem(facility.getPersistentId().toString(), itemMaster.getItemId(), locationAlias, "-1", "1", uomMaster.getUomMasterId(), null);
			Assert.fail("Should have thrown exception");
		}
		catch (InputValidationException e) {
			Assert.assertTrue(e.hasViolationForProperty("cmFromLeft"));
		}

		this.getTenantPersistenceService().commitTenantTransaction();
	}
	
	@SuppressWarnings("unused")
	@Test
	public void testUpsertItemUsingAlphaPositionFromLeft() throws IOException {
		this.getTenantPersistenceService().beginTenantTransaction();
		Facility facility=Facility.DAO.findByPersistentId(facilityId);

		Tier tier = (Tier) facility.findSubLocationById("A1.B1.T1");
		UomMaster uomMaster = facility.getUomMaster("each");
		ItemMaster itemMaster = facility.getItemMaster("10700589");
		String locationAlias = tier.getAliases().get(0).getAlias();

		try {
			Item item = uiUpdate.storeItem(facility.getPersistentId().toString(), itemMaster.getItemId(), locationAlias, "A", "1", uomMaster.getUomMasterId(), null);
			Assert.fail("Should have thrown exception");
		}
		catch (InputValidationException e) {
			Assert.assertTrue(e.hasViolationForProperty("cmFromLeft"));
		}

		this.getTenantPersistenceService().commitTenantTransaction();
	}
	
	@Test
	public void testUpsertItemUsingEmptyPositionFromLeft() throws IOException {
		this.getTenantPersistenceService().beginTenantTransaction();
		Facility facility=Facility.DAO.findByPersistentId(facilityId);

		Tier tier = (Tier) facility.findSubLocationById("A1.B1.T1");
		UomMaster uomMaster = facility.getUomMaster("each");
		ItemMaster itemMaster = facility.getItemMaster("10700589");
		String locationAlias = tier.getAliases().get(0).getAlias();
		Item item = uiUpdate.storeItem(facility.getPersistentId().toString(), itemMaster.getItemId(), locationAlias, "", "1", uomMaster.getUomMasterId(), null);
		Assert.assertEquals(0, item.getCmFromLeft().intValue());

		this.getTenantPersistenceService().commitTenantTransaction();
	}
	
	@Test
	public void testUpsertItemUsingNullPositionFromLeft() throws IOException {
		this.getTenantPersistenceService().beginTenantTransaction();
		Facility facility=Facility.DAO.findByPersistentId(facilityId);

		Tier tier = (Tier) facility.findSubLocationById("A1.B1.T1");
		UomMaster uomMaster = facility.getUomMaster("each");
		ItemMaster itemMaster = facility.getItemMaster("10700589");
		String locationAlias = tier.getAliases().get(0).getAlias();
		Item item = uiUpdate.storeItem(facility.getPersistentId().toString(), itemMaster.getItemId(), locationAlias, null, "1", uomMaster.getUomMasterId(), null);
		Assert.assertEquals(0, item.getCmFromLeft().intValue());

		this.getTenantPersistenceService().commitTenantTransaction();
	}
	
	@SuppressWarnings("unused")
	@Test
	public void testUpsertItemUsingEmptyUom() throws IOException {
		this.getTenantPersistenceService().beginTenantTransaction();
		Facility facility=Facility.DAO.findByPersistentId(facilityId);

		Tier tier = (Tier) facility.findSubLocationById("A1.B1.T1");
		UomMaster uomMaster = facility.getUomMaster("each");
		ItemMaster itemMaster = facility.getItemMaster("10700589");
		String locationAlias = tier.getAliases().get(0).getAlias();

		try {
			Item item = uiUpdate.storeItem(facility.getPersistentId().toString(), itemMaster.getItemId(), locationAlias, "1", "1", "", null);
			Assert.fail("Should have thrown exception");
		}
		catch (InputValidationException e) {
			Assert.assertTrue(e.toString(), e.hasViolationForProperty("uomMasterId"));
		}

		this.getTenantPersistenceService().commitTenantTransaction();
	}

	@SuppressWarnings("unused")
	@Test
	public void testUpsertItemUsingUomDifferentCase() throws IOException {
		this.getTenantPersistenceService().beginTenantTransaction();
		Facility facility=Facility.DAO.findByPersistentId(facilityId);

		Tier tier = (Tier) facility.findSubLocationById("A1.B1.T1");
		UomMaster uomMaster = facility.getUomMaster("each");
		ItemMaster itemMaster = facility.getItemMaster("10700589");
		Item item = uiUpdate.storeItem(facility.getPersistentId().toString(), itemMaster.getItemId(), tier.getNominalLocationId(), "1", "1", "EACH", null);
		Assert.assertEquals(tier, item.getStoredLocation());

		this.getTenantPersistenceService().commitTenantTransaction();
	}
	
	@Test
	public void testUpsertItemUsingNominalLocationId() throws IOException {
		this.getTenantPersistenceService().beginTenantTransaction();
		Facility facility=Facility.DAO.findByPersistentId(facilityId);

		Tier tier = (Tier) facility.findSubLocationById("A1.B1.T1");
		UomMaster uomMaster = facility.getUomMaster("each");
		ItemMaster itemMaster = facility.getItemMaster("10700589");

		
		Item item = uiUpdate.storeItem(facility.getPersistentId().toString(), itemMaster.getItemId(), tier.getNominalLocationId(), "1", "1", uomMaster.getUomMasterId(), null);
		Assert.assertEquals(tier, item.getStoredLocation());

		this.getTenantPersistenceService().commitTenantTransaction();
	}

	@Test
	public void testUpsertItemUsingLocationAlias() throws IOException {
		this.getTenantPersistenceService().beginTenantTransaction();
		Facility facility=Facility.DAO.findByPersistentId(facilityId);

		Tier tier = (Tier) facility.findSubLocationById("A1.B1.T1");
		UomMaster uomMaster = facility.getUomMaster("each");
		ItemMaster itemMaster = facility.getItemMaster("10700589");

		String locationAlias = tier.getAliases().get(0).getAlias();
		Item item = uiUpdate.storeItem(facility.getPersistentId().toString(), itemMaster.getItemId(), locationAlias, "1", "1", uomMaster.getUomMasterId(), null);
		Assert.assertEquals(tier, item.getStoredLocation());

		this.getTenantPersistenceService().commitTenantTransaction();
	}
	
}
