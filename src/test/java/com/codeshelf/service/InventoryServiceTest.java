package com.codeshelf.service;

import java.io.StringReader;
import java.sql.Timestamp;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Test;

import com.codeshelf.edi.ICsvInventoryImporter;
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

	@SuppressWarnings("unused")
	private FacilityGenerator facilityGenerator;
	
	UUID facilityForVirtualSlottingId;
	
	@Override
	public void doBefore() {
		super.doBefore();
		this.getTenantPersistenceService().beginTransaction(getDefaultTenant());
		facilityGenerator = new FacilityGenerator(getDefaultTenant());

		VirtualSlottedFacilityGenerator facilityGenerator =
					new VirtualSlottedFacilityGenerator(createAisleFileImporter(),
														createLocationAliasImporter(),
														createOrderImporter());
		
		Facility facilityForVirtualSlotting = facilityGenerator.generateFacilityForVirtualSlotting(testName.getMethodName());
		
		this.facilityForVirtualSlottingId = facilityForVirtualSlotting.getPersistentId();
		
		this.getTenantPersistenceService().commitTransaction(getDefaultTenant());
	}

	@Override
	public boolean ephemeralServicesShouldStartAutomatically() {
		return false; // in this test, we start services manually after defining the work service to start
	}
	
	@Test
	public void testInventoryService() {
		LightService ls = new LightService(this.webSocketManagerService);
		this.inventoryService = new InventoryService(ls);
		this.initializeEphemeralServiceManager();
		
		this.getTenantPersistenceService().beginTransaction(getDefaultTenant());
		Facility facilityForVirtualSlotting = Facility.staticGetDao().findByPersistentId(getDefaultTenant(),facilityForVirtualSlottingId);

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
		inventoryService.moveOrCreateInventory(getDefaultTenant(),"101", "D403", che1.getPersistentId());
		Location locationD401 = facility.findSubLocationById("D403");
		Assert.assertNotNull(locationD401);
		Location locationD502 = facility.findSubLocationById("D502");
		Assert.assertNotNull(locationD502);
	
		this.getTenantPersistenceService().commitTransaction(getDefaultTenant());
		
		this.getTenantPersistenceService().beginTransaction(getDefaultTenant());
		facility  = Facility.staticGetDao().reload(getDefaultTenant(),facility);
		locationD401 = facility.findSubLocationById("D403");
		Item item1121D401 = locationD401.getStoredItemFromMasterIdAndUom("1121", "EA");
		Assert.assertNotNull(item1121D401);
		
		locationD502 = facility.findSubLocationById("D502");
		Item item1120D502 = locationD502.getStoredItemFromMasterIdAndUom("1121", "EA");
		Assert.assertNull(item1120D502);
		this.getTenantPersistenceService().commitTransaction(getDefaultTenant());
	
		this.getTenantPersistenceService().beginTransaction(getDefaultTenant());
		facility = setupInventoryData(facilityForVirtualSlotting, csvString);
		// Try creating a new item
		inventoryService.moveOrCreateInventory(getDefaultTenant(),"201", "D100", che1.getPersistentId());
		
		this.getTenantPersistenceService().commitTransaction(getDefaultTenant());
		this.getTenantPersistenceService().beginTransaction(getDefaultTenant());
		Location locationD100 = facility.findSubLocationById("D100");
		Assert.assertNotNull(locationD100);
		
		Item item201D100 = locationD100.getStoredItemFromMasterIdAndUom("201", "EA");
		Assert.assertNotNull(item201D100);
		
		this.getTenantPersistenceService().commitTransaction(getDefaultTenant());
		
	}
	
	private Facility setupInventoryData(Facility facility, String csvString) {
		Timestamp ediProcessTime = new Timestamp(System.currentTimeMillis());
		ICsvInventoryImporter importer = createInventoryImporter();
		importer.importSlottedInventoryFromCsvStream(new StringReader(csvString), facility, ediProcessTime);
		return facility.getDao().findByPersistentId(getDefaultTenant(),facility.getPersistentId());
	}
}
