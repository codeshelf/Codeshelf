package com.codeshelf.integration;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.device.CheStateEnum;
import com.codeshelf.device.PosControllerInstr;
import com.codeshelf.flyweight.command.NetGuid;
import com.codeshelf.model.DeviceType;
import com.codeshelf.model.domain.Aisle;
import com.codeshelf.model.domain.Bay;
import com.codeshelf.model.domain.CodeshelfNetwork;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.Item;
import com.codeshelf.model.domain.LedController;
import com.codeshelf.model.domain.Location;
import com.codeshelf.model.domain.LocationAlias;
import com.codeshelf.model.domain.Tier;
import com.codeshelf.sim.worker.PickSimulator;
import com.codeshelf.testframework.ServerTest;
import com.codeshelf.util.ThreadUtils;
import com.google.common.collect.ImmutableMap;

public class CheProcessSkuWall extends ServerTest{
	private static final Logger	LOGGER		= LoggerFactory.getLogger(CheProcessSkuWall.class);
	private static final int	WAIT_TIME	= 4000;
	
	private static final String POSMANID1 = "0x00000001";
	
	private void loadInventory(PickSimulator picker, String gtin, String location) {
		picker.scanSomething(gtin);
		picker.waitForCheState(CheStateEnum.SCAN_GTIN, WAIT_TIME);
		Assert.assertEquals(gtin + " will move if", picker.getLastCheDisplayString(2));
		picker.scanSomething(location);
		picker.waitForCheState(CheStateEnum.SCAN_GTIN, WAIT_TIME);
	}
	
	private void verifyLocation(Facility facility, String itemId, String locationAlias) {
		Map<String, Object> filterArgs = ImmutableMap.<String, Object> of("facilityId",facility.getPersistentId(),"sku",itemId);
		List<Item> items = Item.staticGetDao().findByFilter("itemsByFacilityAndSku", filterArgs);
		Assert.assertEquals(1, items.size()); 
		Item item = items.get(0);
		Location location = item.getStoredLocation();
		Assert.assertNotNull(location);
		LocationAlias alias = location.getPrimaryAlias();
		Assert.assertNotNull(alias);
		Assert.assertEquals(locationAlias, alias.getAlias());
	}
	
	@Test
	public final void loadInventoryIntoWallAndPutItemsThere() throws IOException{
		this.getTenantPersistenceService().beginTransaction();
		LOGGER.info("1: Set up facility");
		Facility facility = getFacility();
		//1 Aisle, 2 Bays, 1Tier per Bay
		String aislesCsvString = "binType,nominalDomainId,lengthCm,slotsInTier,ledCountInTier,tierFloorCm,controllerLED,anchorX,anchorY,orientXorY,depthCm\n" + 
				"Aisle,A1,,,,,tierB1S1Side,2.85,5,X,20\n" + 
				"Bay,B1,100,,,,,,,,\n" + 
				"Tier,T1,100,4,32,0,,,,,\n" + 
				"Bay,B2,100,,,,,,,,\n" + 
				"Tier,T1,100,0,32,0,,,,,";
		importAislesData(facility, aislesCsvString);
		
		String locationsCsvString = "mappedLocationId,locationAlias\n" + 
				"A1,Aisle1\n" +
				"A1.B1.T1.S1,Slot1111\n" + 
				"A1.B1.T1.S2,Slot1112\n" + 
				"A1.B1.T1.S3,Slot1113\n" + 
				"A1.B1.T1.S4,Slot1114\n";
		importLocationAliasesData(facility, locationsCsvString);
		
		Aisle a1 = Aisle.staticGetDao().findByDomainId(facility, "A1");
		a1.setUsage(Location.SKUWALL_USAGE);
		Bay b1 = Bay.staticGetDao().findByDomainId(a1, "B1");
		Tier t1_1 = Tier.staticGetDao().findByDomainId(b1, "T1");
		Bay b2 = Bay.staticGetDao().findByDomainId(a1, "B2");
		Tier t2_1 = Tier.staticGetDao().findByDomainId(b2, "T1");
		t1_1.setTapeIdUi("0001");
		t2_1.setTapeIdUi("0002");
		
		CodeshelfNetwork network = facility.getNetworks().get(0);
		LedController controller = network.findOrCreateLedController("PosMan1", new NetGuid(POSMANID1));
		controller.setDeviceType(DeviceType.Poscons);
		b1.setPosconAssignment(controller.getPersistentId().toString(), "1");
		b2.setPosconAssignment(controller.getPersistentId().toString(), "2");		
		this.getTenantPersistenceService().commitTransaction();
		
		LOGGER.info("2: Load inventory onto the Sku wall");
		startSiteController();
		PickSimulator picker = createPickSim(cheGuid1);
		PosManagerSimulator posman = new PosManagerSimulator(this, new NetGuid(POSMANID1));
		picker.login("Worker1");
		picker.scanCommand("INVENTORY");
		picker.waitForCheState(CheStateEnum.SCAN_GTIN, WAIT_TIME);
		loadInventory(picker, "Item1", "%000000010050");
		loadInventory(picker, "Item2", "%000000010350");
		loadInventory(picker, "Item3", "%000000010650");
		loadInventory(picker, "Item4", "%000000010950");
		picker.logout();
		ThreadUtils.sleep(600);

		this.getTenantPersistenceService().beginTransaction();
		Facility.staticGetDao().reload(facility);
		verifyLocation(facility, "Item1", "Slot1111");
		verifyLocation(facility, "Item2", "Slot1112");
		verifyLocation(facility, "Item3", "Slot1113");
		verifyLocation(facility, "Item4", "Slot1114");

		this.getTenantPersistenceService().commitTransaction();
		
		LOGGER.info("3: Attempt to put items onto Sku wall");
		
		picker.login("Worker1");
		picker.scanCommand("PUT_WALL");
		picker.waitForCheState(CheStateEnum.PUT_WALL_SCAN_WALL, WAIT_TIME);
		picker.scanLocation("Aisle1");
		picker.waitForCheState(CheStateEnum.PUT_WALL_SCAN_ITEM, WAIT_TIME);
		
		putItemOntoSkuWall(picker, posman, "Item1", "Item1", "Slot1111");
		putItemOntoSkuWall(picker, posman, "Item2", "Item2", "Slot1112");
		putItemOntoSkuWall(picker, posman, "Item3", "Item3", "Slot1113");
		putItemOntoSkuWall(picker, posman, "Item4", "Item4", "Slot1114");
		
		LOGGER.info("3: Attempt to put fake items onto Sku wall");
		picker.scanSomething("Fake item");
		picker.waitForCheState(CheStateEnum.NO_PUT_WORK, WAIT_TIME);
		String expectedDisplay = "NO WORK FOR\nFake item\nIN Aisle1\nSCAN ITEM OR CLEAR\n";
		Assert.assertEquals(expectedDisplay, picker.getLastCheDisplay());
	}
	
	private void putItemOntoSkuWall(PickSimulator picker, PosManagerSimulator posman, String gtin, String expectedItem, String expectedLocation) {
		//Look up item
		picker.scanSomething(gtin);
		picker.waitForCheState(CheStateEnum.DO_PUT, WAIT_TIME);
		//Verify CHE display
		String expectedDisplay = expectedLocation + "\n" + expectedItem + "\nQTY 1\n\n";
		System.out.println(picker.getLastCheDisplay());
		Assert.assertEquals(expectedDisplay, picker.getLastCheDisplay());
		//Verify PosCon display (min = 1, qty = 1, max = 99, blinking, bright)
		posman.waitForControllerDisplayValue((byte)1, (byte)1, WAIT_TIME);
		Assert.assertEquals(1, (int)posman.getLastSentPositionControllerMinQty((byte)1));
		Assert.assertEquals(99, (int)posman.getLastSentPositionControllerMaxQty((byte)1));
		Assert.assertEquals(PosControllerInstr.BLINK_FREQ, posman.getLastSentPositionControllerDisplayFreq((byte)1));
		Assert.assertEquals(PosControllerInstr.BRIGHT_DUTYCYCLE, posman.getLastSentPositionControllerDisplayDutyCycle((byte)1));
		//Press PosCon button, place item into wall
		posman.buttonPress(1, 5);
		picker.waitForCheState(CheStateEnum.PUT_WALL_SCAN_ITEM, WAIT_TIME);
		posman.waitForControllerDisplayValue((byte)1, null, WAIT_TIME);
	}
}
