package com.codeshelf.service;

import java.util.UUID;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.edi.OutboundOrderImporterTest;
import com.codeshelf.edi.VirtualSlottedFacilityGenerator;
import com.codeshelf.flyweight.command.ColorEnum;
import com.codeshelf.generators.FacilityGenerator;
import com.codeshelf.model.domain.Che;
import com.codeshelf.model.domain.CodeshelfNetwork;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.Item;
import com.codeshelf.model.domain.Location;
import com.codeshelf.testframework.ServerTest;

public class InventoryServiceTest extends ServerTest {
	private static final Logger	LOGGER	= LoggerFactory.getLogger(OutboundOrderImporterTest.class);

	@SuppressWarnings("unused")
	private FacilityGenerator facilityGenerator;
	
	UUID facilityForVirtualSlottingId;
	
	@Override
	public void doBefore() {
		super.doBefore();
		this.getTenantPersistenceService().beginTransaction();
		facilityGenerator = new FacilityGenerator();

		VirtualSlottedFacilityGenerator facilityGenerator =
					new VirtualSlottedFacilityGenerator(
														createAisleFileImporter(),
														createLocationAliasImporter(),
														createOrderImporter());
		
		Facility facilityForVirtualSlotting = facilityGenerator.generateFacilityForVirtualSlotting(testName.getMethodName());
		
		this.facilityForVirtualSlottingId = facilityForVirtualSlotting.getPersistentId();
		
		this.getTenantPersistenceService().commitTransaction();
	}

	@Override
	public boolean ephemeralServicesShouldStartAutomatically() {
		return false; // in this test, we start services manually after defining the work service to start
	}
	
	@Test
	public void testInventoryService() {
		LightBehavior ls = new LightBehavior();
		this.inventoryService = new InventoryBehavior(ls);
		this.initializeEphemeralServiceManager();
		
		this.getTenantPersistenceService().beginTransaction();
		Facility facilityForVirtualSlotting = Facility.staticGetDao().findByPersistentId(this.facilityForVirtualSlottingId);

		String csvString = "itemId,locationId,description,quantity,uom,inventoryDate,cmFromLeft,gtin\r\n" 	//
				+ "1120,D402,12/16 oz Bowl Lids -PLA Compostable,6,EA,6/25/14 12:00,135,100\r\n" 			//
				+ "1121,D502,12/18 oz Bowl Lids -PLA Compostable,6,EA,6/25/14 12:00,8,101\r\n"				//
				+ "1122,D503,12/20 oz Bowl Lids -PLA Compostable,6,CS,6/25/14 12:00,55,102\r\n";			//
		Facility facility = setupInventoryData(facilityForVirtualSlotting, csvString);
		Assert.assertNotNull(facility);
		
		CodeshelfNetwork network = facility.getNetworks().get(0);
		Che che1 = network.getChe("CHE1");
		Assert.assertNotNull(che1);
		che1.setColor(ColorEnum.GREEN);
		
		// Try moving an existing item
		inventoryService.moveOrCreateInventory("101", "D403", che1.getPersistentId(), null);
		Location locationD401 = facility.findSubLocationById("D403");
		Assert.assertNotNull(locationD401);
		Location locationD502 = facility.findSubLocationById("D502");
		Assert.assertNotNull(locationD502);
	
		this.getTenantPersistenceService().commitTransaction();
		
		this.getTenantPersistenceService().beginTransaction();
		facility  = Facility.staticGetDao().reload(facility);
		locationD401 = facility.findSubLocationById("D403");
		Item item1121D401 = locationD401.getStoredItemFromMasterIdAndUom("1121", "EA");
		Assert.assertNotNull(item1121D401);
		
		locationD502 = facility.findSubLocationById("D502");
		Item item1120D502 = locationD502.getStoredItemFromMasterIdAndUom("1121", "EA");
		Assert.assertNull(item1120D502);
		this.getTenantPersistenceService().commitTransaction();
	
		this.getTenantPersistenceService().beginTransaction();
		facility = setupInventoryData(facilityForVirtualSlotting, csvString);
		// Try creating a new item
		inventoryService.moveOrCreateInventory("201", "D100", che1.getPersistentId(), null);
		
		this.getTenantPersistenceService().commitTransaction();
		this.getTenantPersistenceService().beginTransaction();
		Location locationD100 = facility.findSubLocationById("D100");
		Assert.assertNotNull(locationD100);
		
		Item item201D100 = locationD100.getStoredItemFromMasterIdAndUom("201", "EA");
		Assert.assertNotNull(item201D100);
		
		this.getTenantPersistenceService().commitTransaction();
	}
	
	/*String csvString2 = "mappedLocationId,locationAlias\r\n" //
				+ "A1.B1, D100\r\n" //
				+ "A1.B1.T1, D101\r\n" //
				+ "A1.B1.T1.S1, D301\r\n" //
				+ "A1.B1.T1.S2, D302\r\n" //
				+ "A1.B1.T1.S3, D303\r\n" //
				+ "A1.B1.T1.S4, D304\r\n" //
				+ "A2.B1.T1, D402\r\n" //
				+ "A2.B2.T1, D403\r\n"//
				+ "A3.B1.T1, D502\r\n" //
				+ "A3.B2.T1, D503\r\n";//
	 */
	
	@Test
	public void testAssignLocationTapeId(){
		LightBehavior ls = new LightBehavior();
		this.inventoryService = new InventoryBehavior(ls);
		this.initializeEphemeralServiceManager();
		
		this.getTenantPersistenceService().beginTransaction();
		Facility facilityForVirtualSlotting = Facility.staticGetDao().findByPersistentId(this.facilityForVirtualSlottingId);

		String csvString = "itemId,locationId,description,quantity,uom,inventoryDate,cmFromLeft,gtin\r\n" 	//
				+ "1120,D402,12/16 oz Bowl Lids -PLA Compostable,6,EA,6/25/14 12:00,135,100\r\n" 			//
				+ "1121,D502,12/18 oz Bowl Lids -PLA Compostable,6,EA,6/25/14 12:00,8,101\r\n"				//
				+ "1122,D503,12/20 oz Bowl Lids -PLA Compostable,6,CS,6/25/14 12:00,55,102\r\n";			//
		Facility facility = setupInventoryData(facilityForVirtualSlotting, csvString);
		Assert.assertNotNull(facility);
		
		// Assign tapeId to a location
		// tapeId should be set
		LOGGER.info("1a. Assigning tapeId to an existing location");
		inventoryService.setLocationTapeId(facility, 222, "D100");
		Location locationD100 = facility.findSubLocationById("D100");
		Assert.assertNotNull(locationD100);
		Assert.assertEquals(222, (int) locationD100.getTapeId());
		
		// Assign same tapeId to another location
		// tapeId should move
		LOGGER.info("1b. Assigning same tapeId to another existing location");
		inventoryService.setLocationTapeId(facility, 222, "D402");
		Location locationD402 = facility.findSubLocationById("D402");
		Assert.assertNotNull(locationD402);
		Assert.assertNotNull(locationD402.getTapeId());
		Assert.assertEquals(222, (int) locationD402.getTapeId()); 
		Assert.assertNull(locationD100.getTapeId());
		
		// Assign a new tapeId to same location
		// Location should have new tapeId
		LOGGER.info("1c. Assign a new tapeId to previous location");
		inventoryService.setLocationTapeId(facility, 333, "D402");
		Assert.assertNotNull(locationD402.getTapeId());
		Assert.assertEquals(333, (int) locationD402.getTapeId()); 
		
		// Assign a new tapeId to a location that doesn't exsit
		LOGGER.info("1d. Assign tapeId to a location that does not exist");
		inventoryService.setLocationTapeId(facility, 444, "D600");
		
		// Assign the tapeId to a location that does exist
		LOGGER.info("1e. Assign previous tapeId to a location that does exist");
		inventoryService.setLocationTapeId(facility, 444, "D502");
		Location locationD502 = facility.findSubLocationById("D502");
		Assert.assertNotNull(locationD502.getTapeId());
		Assert.assertEquals(444, (int) locationD502.getTapeId()); 
		
		this.getTenantPersistenceService().commitTransaction();
	}
	
	private Facility setupInventoryData(Facility facility, String csvString) {
		importInventoryData(facility, csvString);
		return facility.getDao().findByPersistentId(facility.getPersistentId());
	}
}
