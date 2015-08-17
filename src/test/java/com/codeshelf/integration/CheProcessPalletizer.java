package com.codeshelf.integration;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.device.CheStateEnum;
import com.codeshelf.model.domain.Aisle;
import com.codeshelf.model.domain.Bay;
import com.codeshelf.model.domain.Che;
import com.codeshelf.model.domain.CodeshelfNetwork;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.Tier;
import com.codeshelf.model.domain.Che.ProcessMode;
import com.codeshelf.sim.worker.PickSimulator;
import com.codeshelf.testframework.ServerTest;

public class CheProcessPalletizer extends ServerTest{
	private static final Logger	LOGGER		= LoggerFactory.getLogger(CheProcessPalletizer.class);
	private static final int	WAIT_TIME	= 4000;
	
	private PickSimulator picker;
	
	@Before
	public void init() throws IOException{
		this.getTenantPersistenceService().beginTransaction();
		
		LOGGER.info("1: Set up facility");
		Facility facility = getFacility();
		//1 Aisle, 1 Bay, 2 Tiers
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
		
		Aisle a1 = Aisle.staticGetDao().findByDomainId(facility, "A1");
		Bay b1 = Bay.staticGetDao().findByDomainId(a1, "B1");
		Tier t111 = Tier.staticGetDao().findByDomainId(b1, "T1");
		Tier t112 = Tier.staticGetDao().findByDomainId(b1, "T2");
		t111.setTapeIdUi("0001");
		t112.setTapeIdUi("0002");
		
		CodeshelfNetwork network = getNetwork();
		Che che1 = network.getChe(cheId1);
		Assert.assertNotNull(che1);
		Assert.assertEquals(cheGuid1, che1.getDeviceNetGuid()); // just checking since we use cheGuid1 to get the picker.
		che1.setProcessMode(ProcessMode.PALLETIZER);
		Che.staticGetDao().store(che1);

		this.getTenantPersistenceService().commitTransaction();
		
		startSiteController();
		picker = createPickSim(cheGuid1);
		picker.login("Worker1");
		picker.waitForCheState(CheStateEnum.PALLETIZER_SCAN_ITEM, WAIT_TIME);
		Assert.assertEquals("Scan Item\n\n\n\n", picker.getLastCheDisplay());
	}
	
	@Test
	public void testPut(){
		LOGGER.info("1: Open two pallets");
		openNewPallet("10010001", "L%Slot1111", "Slot1111");
		openNewPallet("10020001", "%000000020010", "Tier112");

		LOGGER.info("2: Put item in each pallet");
		picker.scanSomething("10010002");
		picker.waitForCheState(CheStateEnum.PALLETIZER_PUT_ITEM, WAIT_TIME);
		Assert.assertEquals("Slot1111\nItem: 10010002\nStore: 1001\nScan Next Item\n", picker.getLastCheDisplay());

		picker.scanSomething("10020002");
		picker.waitForCheState(CheStateEnum.PALLETIZER_PUT_ITEM, WAIT_TIME);
		Assert.assertEquals("Tier112\nItem: 10020002\nStore: 1002\nScan Next Item\n", picker.getLastCheDisplay());
	}
	
	@Test
	public void testCloseByLicense(){
		LOGGER.info("1: Open a pallet");
		openNewPallet("10010001", "L%Slot1111", "Slot1111");
		
		LOGGER.info("2: Scan REMOVE, and a license coressponding to the pallet");
		picker.scanCommand("REMOVE");
		picker.waitForCheState(CheStateEnum.PALLETIZER_REMOVE, WAIT_TIME);
		Assert.assertEquals("Scan License\nOr Location\nTo Close Pallet\nCANCEL to exit\n", picker.getLastCheDisplay());
		
		picker.scanSomething("10019991");
		picker.waitForCheState(CheStateEnum.PALLETIZER_SCAN_ITEM, WAIT_TIME);
		Assert.assertEquals("Scan Item\n\n\n\n", picker.getLastCheDisplay());
		
		LOGGER.info("3: Try to put another item from that pallet. Verify that the pallet was not found");
		picker.scanSomething("10010003");
		picker.waitForCheState(CheStateEnum.PALLETIZER_NEW_ORDER, WAIT_TIME);
		Assert.assertEquals("Scan New Location\nFor Store 1001\n\nOr Scan Another Item\n", picker.getLastCheDisplay());
	}
	
	@Test
	public void testCloseByLocation(){
		LOGGER.info("1: Open a pallet");
		openNewPallet("10010001", "L%Slot1111", "Slot1111");
		
		LOGGER.info("2: Scan REMOVE, and a location with an existing pallet");
		picker.scanCommand("REMOVE");
		picker.waitForCheState(CheStateEnum.PALLETIZER_REMOVE, WAIT_TIME);
		Assert.assertEquals("Scan License\nOr Location\nTo Close Pallet\nCANCEL to exit\n", picker.getLastCheDisplay());
		
		picker.scanLocation("Slot1111");
		picker.waitForCheState(CheStateEnum.PALLETIZER_SCAN_ITEM, WAIT_TIME);
		Assert.assertEquals("Scan Item\n\n\n\n", picker.getLastCheDisplay());
		
		LOGGER.info("3: Try to put another item from that pallet. Verify that the pallet was not found");
		picker.scanSomething("10010003");
		picker.waitForCheState(CheStateEnum.PALLETIZER_NEW_ORDER, WAIT_TIME);
		Assert.assertEquals("Scan New Location\nFor Store 1001\n\nOr Scan Another Item\n", picker.getLastCheDisplay());
	}
	
	@Test
	public void testCloseNothing(){
		LOGGER.info("1: Try to close a non-open pallet with a license");
		picker.scanCommand("REMOVE");
		picker.waitForCheState(CheStateEnum.PALLETIZER_REMOVE, WAIT_TIME);
		Assert.assertEquals("Scan License\nOr Location\nTo Close Pallet\nCANCEL to exit\n", picker.getLastCheDisplay());
		picker.scanSomething("10010001");
		picker.waitForCheState(CheStateEnum.PALLETIZER_REMOVE, WAIT_TIME);
		Assert.assertEquals("Pallet 1001 Not Found\nCANCEL TO CONTINUE\n\n\n", picker.getLastCheDisplay());
		
		LOGGER.info("2: Try to close a pallet in an empty location");
		picker.scanLocation("Tier111");
		picker.waitForCheState(CheStateEnum.PALLETIZER_REMOVE, WAIT_TIME);
		Assert.assertEquals("No Pallets In Tier111\nCANCEL TO CONTINUE\n\n\n", picker.getLastCheDisplay());
	}
	
	@Test
	public void testNewPalletLocationBusy(){
		LOGGER.info("1: Open a pallet");
		openNewPallet("10010001", "L%Slot1111", "Slot1111");
		
		LOGGER.info("2: Scan item from another pallet");
		picker.scanSomething("10020001");
		picker.waitForCheState(CheStateEnum.PALLETIZER_NEW_ORDER, WAIT_TIME);
		Assert.assertEquals("Scan New Location\nFor Store 1002\n\nOr Scan Another Item\n", picker.getLastCheDisplay());
		
		LOGGER.info("3: Try to put it in the already occupied location");
		picker.scanSomething("%000000010010");
		picker.waitForCheState(CheStateEnum.PALLETIZER_NEW_ORDER, WAIT_TIME);
		Assert.assertEquals("Busy: Slot1111\nRemove 1001 First\n\nCANCEL TO CONTINUE\n", picker.getLastCheDisplay());
	}
	
	@Test
	public void testDamaged(){		
		LOGGER.info("1: Scan item, open pallet");
		openNewPallet("10010001", "L%Slot1111", "Slot1111");
		
		LOGGER.info("2: Scan SHORT, but scan NO at confirmation");
		picker.scanCommand("SHORT");
		picker.waitForCheState(CheStateEnum.PALLETIZER_DAMAGED, WAIT_TIME);
		Assert.assertEquals("CONFIRM DAMAGED\nSCAN YES OR NO\n\n\n", picker.getLastCheDisplay());
		picker.scanCommand("NO");
		picker.waitForCheState(CheStateEnum.PALLETIZER_PUT_ITEM, WAIT_TIME);
		Assert.assertEquals("Slot1111\nItem: 10010001\nStore: 1001\nScan Next Item\n", picker.getLastCheDisplay());
		
		LOGGER.info("3: Scan SHORT, and confrm");
		picker.scanCommand("SHORT");
		picker.waitForCheState(CheStateEnum.PALLETIZER_DAMAGED, WAIT_TIME);
		Assert.assertEquals("CONFIRM DAMAGED\nSCAN YES OR NO\n\n\n", picker.getLastCheDisplay());
		picker.scanCommand("YES");
		picker.waitForCheState(CheStateEnum.PALLETIZER_SCAN_ITEM, WAIT_TIME);
		Assert.assertEquals("Scan Item\n\n\n\n", picker.getLastCheDisplay());
	}
	
	private void openNewPallet(String item, String location, String locationName){
		String store = item.substring(0, 4);
		picker.scanSomething(item);
		picker.waitForCheState(CheStateEnum.PALLETIZER_NEW_ORDER, WAIT_TIME);
		Assert.assertEquals("Scan New Location\nFor Store " + store + "\n\nOr Scan Another Item\n", picker.getLastCheDisplay());
		picker.scanSomething(location);
		picker.waitForCheState(CheStateEnum.PALLETIZER_PUT_ITEM, WAIT_TIME);
		Assert.assertEquals(locationName + "\nItem: " + item + "\nStore: " + store + "\nScan Next Item\n", picker.getLastCheDisplay());
	}
}
