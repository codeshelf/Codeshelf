package com.codeshelf.integration;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.device.CheStateEnum;
import com.codeshelf.device.PosControllerInstr;
import com.codeshelf.flyweight.command.NetGuid;
import com.codeshelf.model.DeviceType;
import com.codeshelf.model.dao.PropertyDao;
import com.codeshelf.model.domain.Aisle;
import com.codeshelf.model.domain.Bay;
import com.codeshelf.model.domain.CodeshelfNetwork;
import com.codeshelf.model.domain.DomainObjectProperty;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.LedController;
import com.codeshelf.model.domain.Location;
import com.codeshelf.model.domain.Tier;
import com.codeshelf.service.PropertyService;
import com.codeshelf.sim.worker.PickSimulator;
import com.codeshelf.testframework.ServerTest;

public class CheProcessInfoAndRemoveCommands extends ServerTest{
	private static final Logger	LOGGER		= LoggerFactory.getLogger(CheProcessInfoAndRemoveCommands.class);
	private static final int	WAIT_TIME	= 4000;
	private static final String POSMANID1 = "0x00000001";
	
	private PickSimulator picker;
	private PosManagerSimulator wallController;
	
	@Before
	public void init() throws IOException{
		this.getTenantPersistenceService().beginTransaction();
		
		LOGGER.info("1: Set up facility");
		Facility facility = getFacility();
		//1 Aisle, 1 Bays, 2 Tiers
		String aislesCsvString = "binType,nominalDomainId,lengthCm,slotsInTier,ledCountInTier,tierFloorCm,controllerLED,anchorX,anchorY,orientXorY,depthCm\n" + 
				"Aisle,A1,,,,,tierB1S1Side,2.85,5,X,20\n" + 
				"Bay,B1,100,,,,,,,,\n" + 
				"Tier,T1,100,4,32,0,,,,,\n" +  
				"Tier,T2,100,0,32,0,,,,,";
		importAislesData(facility, aislesCsvString);
		
		String locationsCsvString = "mappedLocationId,locationAlias\n" + 
				"A1.B1,Bay11\n" +
				"A1.B1.T1,Tier111\n" + 
				"A1.B1.T1.S1,Slot1111\n" + 
				"A1.B1.T1.S2,Slot1112\n" + 
				"A1.B1.T1.S3,Slot1113\n" + 
				"A1.B1.T1.S4,Slot1114\n" +
				"A1.B1.T2,Tier112";
		importLocationAliasesData(facility, locationsCsvString);
		
		String inventoryCsvString = "itemId,locationId,description,quantity,uom,inventoryDate,lotId,cmFromLeft,gtin\n" +// 
				"Item1,Slot1111,Item Desc 1,1000,a,12/03/14 12:00,,0,gtin1\n" + //
				"Item2,Slot1113,Item Desc 2,1000,a,12/03/14 12:00,,0,gtin2\n" + //
				"Item3,Slot1114,Item Desc 3,1000,a,12/03/14 12:00,,0,gtin3\n" + //
				"Item4,Slot1114,Item Desc 4,1000,a,12/03/14 12:00,,0,gtin4\n" + //
				"Item5,Tier112,Item Desc 5,1000,a,12/03/14 12:00,,0,gtin5\n" + //
				"Item6,Tier112,Item Desc 6,1000,a,12/03/14 12:00,,25,gtin6\n" + //
				"Item7,Tier112,Item Desc 7,1000,a,12/03/14 12:00,,50,gtin7\n" + //
				"Item8,Tier112,Item Desc 8,1000,a,12/03/14 12:00,,75,gtin8\n";
		importInventoryData(facility, inventoryCsvString);
		
		Aisle a1 = Aisle.staticGetDao().findByDomainId(facility, "A1");
		a1.setUsage(Location.PUTWALL_USAGE);
		Bay b1 = Bay.staticGetDao().findByDomainId(a1, "B1");
		Tier t111 = Tier.staticGetDao().findByDomainId(b1, "T1");
		Tier t112 = Tier.staticGetDao().findByDomainId(b1, "T2");
		t111.setTapeIdUi("0001");
		t112.setTapeIdUi("0002");
		
		CodeshelfNetwork network = facility.getNetworks().get(0);
		LedController controller = network.findOrCreateLedController("PosMan1", new NetGuid(POSMANID1));
		controller.setDeviceType(DeviceType.Poscons);
		b1.setPosconAssignment(controller.getPersistentId().toString(), "1");

		DomainObjectProperty theProperty = PropertyService.getInstance().getProperty(facility, DomainObjectProperty.SCANPICK);
		if (theProperty != null) {
			theProperty.setValue("UPC");
			PropertyDao.getInstance().store(theProperty);
		}
		
		this.getTenantPersistenceService().commitTransaction();
		
		startSiteController();
		picker = createPickSim(cheGuid1);
		wallController = new PosManagerSimulator(this, new NetGuid(POSMANID1));
	}
	
	@Test
	public void testCheContainers(){
		LOGGER.info("1a: Load orders onto CHE");
		picker.loginAndSetup("Worker1");
		picker.setupContainer("121", "1");
		picker.setupContainer("122", "2");
		picker.setupContainer("123", "3");
		
		LOGGER.info("1b: Verify that poscons are showing order ids. Then, START into SETUP_SUMMARY");
		Assert.assertEquals((byte)21, (byte)picker.getLastSentPositionControllerDisplayValue(1));
		Assert.assertEquals((byte)22, (byte)picker.getLastSentPositionControllerDisplayValue(2));
		Assert.assertEquals((byte)23, (byte)picker.getLastSentPositionControllerDisplayValue(3));
		picker.scanCommand("START");
		picker.waitForCheState(CheStateEnum.SETUP_SUMMARY, WAIT_TIME);
		
		LOGGER.info("2: Scan INFO to return to CONTAINER_SELECT");
		picker.scanCommand("INFO");
		picker.waitForCheState(CheStateEnum.CONTAINER_SELECT, WAIT_TIME);
		Assert.assertEquals((byte)21, (byte)picker.getLastSentPositionControllerDisplayValue(1));
		Assert.assertEquals((byte)22, (byte)picker.getLastSentPositionControllerDisplayValue(2));
		Assert.assertEquals((byte)23, (byte)picker.getLastSentPositionControllerDisplayValue(3));
		
		LOGGER.info("3a: Scan REMOVE, remove container through button press");
		picker.scanCommand("REMOVE");
		picker.waitForCheState(CheStateEnum.REMOVE_CHE_CONTAINER, WAIT_TIME);
		Assert.assertEquals("SELECT POSITION\nTo remove order\n\nCANCEL to exit\n", picker.getLastCheDisplay());
		picker.buttonPress(1, 0);
		picker.waitForCheState(CheStateEnum.CONTAINER_SELECT, WAIT_TIME);
		
		LOGGER.info("3b: Scan REMOVE, remove container through poscon scan");
		picker.scanCommand("REMOVE");
		picker.waitForCheState(CheStateEnum.REMOVE_CHE_CONTAINER, WAIT_TIME);
		Assert.assertEquals("SELECT POSITION\nTo remove order\n\nCANCEL to exit\n", picker.getLastCheDisplay());
		picker.scanPosition("3");
		picker.waitForCheState(CheStateEnum.CONTAINER_SELECT, WAIT_TIME);
		
		LOGGER.info("4: Verify that 2 orders have been removed");
		Assert.assertNull(picker.getLastSentPositionControllerDisplayValue(1));
		Assert.assertEquals((byte)22, (byte)picker.getLastSentPositionControllerDisplayValue(2));
		Assert.assertNull(picker.getLastSentPositionControllerDisplayValue(3));

		LOGGER.info("5a: Go to SETUP_SUMMARY, and, once again, verity that only one order remains");
		picker.scanCommand("START");
		picker.waitForCheState(CheStateEnum.SETUP_SUMMARY, WAIT_TIME);
		LOGGER.info("5b: Assert that poscons 1 and 3 are empty, and 2 shows -- for the remaining, but fake order");
		Assert.assertNull(picker.getLastSentPositionControllerDisplayValue(1));
		Assert.assertNull(picker.getLastSentPositionControllerDisplayValue(3));
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayValue((byte) 2),PosControllerInstr.BITENCODED_SEGMENTS_CODE);
		Assert.assertEquals(picker.getLastSentPositionControllerMinQty((byte) 2), PosControllerInstr.BITENCODED_LED_DASH);
		Assert.assertEquals(picker.getLastSentPositionControllerMaxQty((byte) 2), PosControllerInstr.BITENCODED_LED_DASH);
	}
	
	@Test
	public void testInventory(){
		LOGGER.info("1: Enter INVENTORY.INFO mode");
		picker.loginAndSetup("Worker1");
		picker.scanCommand("INVENTORY");
		picker.waitForCheState(CheStateEnum.SCAN_GTIN, WAIT_TIME);
		picker.scanCommand("INFO");
		picker.waitForCheState(CheStateEnum.INFO_PROMPT, WAIT_TIME);
		Assert.assertEquals("SCAN LOCATION\nTo see contents\n\nCANCEL to exit\n", picker.getLastCheDisplay());
		
		LOGGER.info("2: Attempt to scan a location that is neither Slot nor a Tape scan");
		picker.scanLocation("Bay11");
		picker.waitForCheState(CheStateEnum.INFO_DISPLAY, WAIT_TIME);
		Assert.assertEquals("Bay11 IS BAY\nExpected Slot or Tape\nScan another location\nCANCEL to exit\n", picker.getLastCheDisplay());
		
		LOGGER.info("3: Scan location with no inventoried items");
		picker.scanLocation("Slot1112");
		picker.waitForCheState(CheStateEnum.INFO_DISPLAY, WAIT_TIME);
		Assert.assertEquals("Slot1112\nItem: none\n\nCANCEL to exit\n", picker.getLastCheDisplay());
		
		LOGGER.info("4a: Scan location with an inventoried item");
		picker.scanLocation("Slot1113");
		picker.waitForCheState(CheStateEnum.INFO_DISPLAY, WAIT_TIME);
		Assert.assertEquals("Slot1113\nUPC: gtin2\nItem Desc 2\nCANCEL to exit\n", picker.getLastCheDisplay());
		
		LOGGER.info("4b: Enter the REMOVE screen");
		picker.scanCommand("REMOVE");
		picker.waitForCheState(CheStateEnum.REMOVE_INVENTORY_CONFIRM, WAIT_TIME);
		Assert.assertEquals("SCAN YES OR NO\nYES: remove item\nNO:  cancel\n\n", picker.getLastCheDisplay());
		
		LOGGER.info("4c: Don't remove item, go back to INFO");
		picker.scanCommand("NO");
		picker.waitForCheState(CheStateEnum.INFO_DISPLAY, WAIT_TIME);
		Assert.assertEquals("Slot1113\nUPC: gtin2\nItem Desc 2\nCANCEL to exit\n", picker.getLastCheDisplay());

		LOGGER.info("4d: Enter REMOVE screen again, remove item, verify that it is gone");
		picker.scanCommand("REMOVE");
		picker.waitForCheState(CheStateEnum.REMOVE_INVENTORY_CONFIRM, WAIT_TIME);
		Assert.assertEquals("SCAN YES OR NO\nYES: remove item\nNO:  cancel\n\n", picker.getLastCheDisplay());
		picker.scanCommand("YES");
		picker.waitForCheState(CheStateEnum.INFO_DISPLAY, WAIT_TIME);
		Assert.assertEquals("Slot1113\nItem: none\n\nCANCEL to exit\n", picker.getLastCheDisplay());
		
		LOGGER.info("5a: Scan location with 2 items placed identically");
		picker.scanLocation("Slot1114");
		picker.waitForCheState(CheStateEnum.INFO_DISPLAY, WAIT_TIME);
		LOGGER.info("5b: See what item was picked first by the system (depends on PersistentId)");
		int firstItemId, secondItemId;
		if ("UPC: gtin3".equals(picker.getLastCheDisplayString(2))){
			firstItemId = 3;
			secondItemId = 4;
		} else {
			firstItemId = 4;
			secondItemId = 3;
		}
		Assert.assertEquals("Slot1114\nUPC: gtin"+ firstItemId + "\nItem Desc "+ firstItemId + "\nCANCEL to exit\n", picker.getLastCheDisplay());
		
		LOGGER.info("5c: Remove that item. Verify that the other item is now displayed");
		picker.scanCommand("REMOVE");
		picker.waitForCheState(CheStateEnum.REMOVE_INVENTORY_CONFIRM, WAIT_TIME);
		Assert.assertEquals("SCAN YES OR NO\nYES: remove item\nNO:  cancel\n\n", picker.getLastCheDisplay());
		picker.scanCommand("YES");
		picker.waitForCheState(CheStateEnum.INFO_DISPLAY, WAIT_TIME);
		Assert.assertEquals("Slot1114\nUPC: gtin"+ secondItemId + "\nItem Desc "+ secondItemId + "\nCANCEL to exit\n", picker.getLastCheDisplay());
		
		LOGGER.info("6: Scan tape to a Slot");
		picker.scanSomething("%000000010100");
		picker.waitForCheState(CheStateEnum.INFO_DISPLAY, WAIT_TIME);
		Assert.assertEquals("Slot1111\nUPC: gtin1\nItem Desc 1\nCANCEL to exit\n", picker.getLastCheDisplay());
		
		LOGGER.info("7a: Scan tape to a Tier. Verify that the closest item is displayed");
		picker.scanSomething("%000000020200");
		picker.waitForCheState(CheStateEnum.INFO_DISPLAY, WAIT_TIME);
		Assert.assertEquals("Tier112\nUPC: gtin6\nItem Desc 6\nCANCEL to exit\n", picker.getLastCheDisplay());
		
		LOGGER.info("7b: Remove that item. Verify that the second closes item is displayed");
		picker.scanCommand("REMOVE");
		picker.waitForCheState(CheStateEnum.REMOVE_INVENTORY_CONFIRM, WAIT_TIME);
		Assert.assertEquals("SCAN YES OR NO\nYES: remove item\nNO:  cancel\n\n", picker.getLastCheDisplay());
		picker.scanCommand("YES");
		picker.waitForCheState(CheStateEnum.INFO_DISPLAY, WAIT_TIME);
		Assert.assertEquals("Tier112\nUPC: gtin5\nItem Desc 5\nCANCEL to exit\n", picker.getLastCheDisplay());
		
		LOGGER.info("8: CANCEL back out to CONTAINER_SELECT");
		picker.scanCommand("CANCEL");
		picker.waitForCheState(CheStateEnum.INFO_PROMPT, WAIT_TIME);
		picker.scanCommand("CANCEL");
		picker.waitForCheState(CheStateEnum.SCAN_GTIN, WAIT_TIME);
		picker.scanCommand("CANCEL");
		picker.waitForCheState(CheStateEnum.CONTAINER_SELECT, WAIT_TIME);
	}
	
	@Test
	public void testPutWall() throws IOException{
		LOGGER.info("1: Load orders");
		beginTransaction();
		String orders = "orderId,orderDetailId,itemId,description,quantity,uom,locationId,preAssignedContainerId,workSequence,gtin,destinationid,shipperId,customerId,dueDate\n" + 
				"26768709,26768709111112,3605970829216,,1,each,LocX24,26768709,0,,1,10,19,12/31/14 12:00 PM\n" + 
				"26768711,26768711111113,3605970829131,,2,each,LocX24,26768711,0,,2,11,20,\n" + 
				"26768712,26768712111114,3605970651558,,3,each,LocX25,26768712,0,gtinx1,3,12,21,12/31/14 12:00 PM\n" + 
				"26768712,26768712111115,3605970723293,,4,each,LocX25,26768712,0,,3,13,21,12/31/14 12:01 PM\n" + 
				"26768717,26768717111116,3605970822255,,5,each,LocX25,26768717,0,,5,14,23,\n" + 
				"26768718,26768718111117,887242001718,,6,each,LocX26,26768718,0,,6,15,24,\n" + 
				"26768718,26768718111118,3605970651558,,7,each,LocX26,26768718,0,,7,15,24,\n" + 
				"26768719,26768719111119,887242001718,,8,each,LocX27,26768719,0,,8,17,26,\n" + 
				"26768720,26768720111120,887242001664,,9,each,LocX27,26768720,0,,9,18,27,";
		importOrdersData(getFacility(), orders);
		commitTransaction();
		
		LOGGER.info("2: Put orders into Put Wall. Verify Wall's poscon to show 6 - the total job count");
		picker.login("Worker1");
		picker.scanCommand("ORDER_WALL");
		picker.waitForCheState(CheStateEnum.PUT_WALL_SCAN_ORDER, WAIT_TIME);
		Assert.assertNull(wallController.getLastSentPositionControllerDisplayValue((byte)1));
		loadOrderIntoWall("26768709", "%000000010050");
		loadOrderIntoWall("26768711", "%000000010350");
		loadOrderIntoWall("26768712", "%000000010650");
		loadOrderIntoWall("26768718", "%000000010900");
		picker.scanCommand("CANCEL");
		picker.waitForCheState(CheStateEnum.SETUP_SUMMARY, WAIT_TIME);
		Assert.assertEquals(6, (int)wallController.getLastSentPositionControllerDisplayValue((byte)1));
		
		LOGGER.info("3: Go to PUT_WALL.INFO mode");
		picker.scanCommand("PUT_WALL");
		picker.waitForCheState(CheStateEnum.PUT_WALL_SCAN_WALL, WAIT_TIME);
		picker.scanCommand("INFO");
		picker.waitForCheState(CheStateEnum.INFO_PROMPT, WAIT_TIME);

		LOGGER.info("4: Get tier summary, verify that there are 4 orders and 6 jobs (juast like on poscon)");
		picker.scanLocation("Tier111");
		picker.waitForCheState(CheStateEnum.INFO_DISPLAY, WAIT_TIME);
		Assert.assertEquals("Tier111 has 4 orders\n4 incompl, with 6 jobs\nYES: light completes\nNO: light incompletes\n", picker.getLastCheDisplay());

		LOGGER.info("5: Look up order with 1 job.");
		picker.scanSomething("%000000010350");
		picker.waitForCheState(CheStateEnum.INFO_DISPLAY, WAIT_TIME);
		Assert.assertEquals("Slot1112\nOrder: 26768711\nComplete: 0 jobs\nRemain: 3605970829131\n", picker.getLastCheDisplay());
		
		LOGGER.info("6a: Look up order with 2 jobs.");
		picker.scanSomething("%000000010650");
		picker.waitForCheState(CheStateEnum.INFO_DISPLAY, WAIT_TIME);
		Assert.assertEquals("Slot1113\nOrder: 26768712\nComplete: 0 jobs\nRemain: 2 jobs\n", picker.getLastCheDisplay());
		
		LOGGER.info("6b: Remove that order.");
		picker.scanCommand("REMOVE");
		picker.waitForCheState(CheStateEnum.REMOVE_WALL_ORDERS_CONFIRM, WAIT_TIME);
		Assert.assertEquals("SCAN YES OR NO\nYES: remove order(s)\nNO:  cancel\n\n", picker.getLastCheDisplay());
		picker.scanCommand("YES");
		picker.waitForCheState(CheStateEnum.INFO_DISPLAY, WAIT_TIME);
		Assert.assertEquals("Slot1113\nOrder: none\n\n\n", picker.getLastCheDisplay());
		
		LOGGER.info("7: Look up tier again. Verify that is now has 3 orders and 4 jobs. Same for Wall's poscon.");
		picker.scanLocation("Tier111");
		picker.waitForCheState(CheStateEnum.INFO_DISPLAY, WAIT_TIME);
		Assert.assertEquals("Tier111 has 3 orders\n3 incompl, with 4 jobs\nYES: light completes\nNO: light incompletes\n", picker.getLastCheDisplay());
		Assert.assertEquals(4, (int)wallController.getLastSentPositionControllerDisplayValue((byte)1));
		
		LOGGER.info("8: Remove all orders in the wall.");
		picker.scanCommand("REMOVE");
		picker.waitForCheState(CheStateEnum.REMOVE_WALL_ORDERS_CONFIRM, WAIT_TIME);
		Assert.assertEquals("SCAN YES OR NO\nYES: remove order(s)\nNO:  cancel\n\n", picker.getLastCheDisplay());
		picker.scanCommand("YES");
		picker.waitForCheState(CheStateEnum.INFO_DISPLAY, WAIT_TIME);
		Assert.assertEquals("Tier111\nOrder: none\n\n\n", picker.getLastCheDisplay());
		Assert.assertNull(wallController.getLastSentPositionControllerDisplayValue((byte)1));
		
		LOGGER.info("9: Try to look up a single order, and verify that it is gone as well.");
		picker.scanSomething("%000000010350");
		picker.waitForCheState(CheStateEnum.INFO_DISPLAY, WAIT_TIME);
		Assert.assertEquals("Slot1112\nOrder: none\n\n\n", picker.getLastCheDisplay());

		
	}
	
	private void loadOrderIntoWall(String order, String location) {
		picker.scanSomething(order);
		picker.waitForCheState(CheStateEnum.PUT_WALL_SCAN_LOCATION, WAIT_TIME);
		picker.scanSomething(location);
		picker.waitForCheState(CheStateEnum.PUT_WALL_SCAN_ORDER, WAIT_TIME);
	}
}