package com.codeshelf.integration;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.behavior.PropertyBehavior;
import com.codeshelf.device.CheStateEnum;
import com.codeshelf.model.FacilityPropertyType;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.WorkInstruction;
import com.codeshelf.sim.worker.PickSimulator;
import com.codeshelf.testframework.ServerTest;

public class CheProcessPickSubstitution extends ServerTest{
	private static final Logger	LOGGER	= LoggerFactory.getLogger(CheProcessPickSubstitution.class);

	private PickSimulator		picker;
	
	@Before
	public void init() throws IOException{
		beginTransaction();
		PropertyBehavior.setProperty(getFacility(), FacilityPropertyType.WORKSEQR, "WorkSequence");
		PropertyBehavior.setProperty(getFacility(), FacilityPropertyType.SCANPICK, "SKU");
		PropertyBehavior.turnOffHK(getFacility());
		commitTransaction();
		
		beginTransaction();
		String csvOrders = "orderId,orderDetailId,itemId,description,quantity,uom,locationId,preAssignedContainerId,workSequence,substituteAllowed\n" + 
				"1111,1,ItemS1,ItemS1 Description,3,each,LocX24,1111,1,true\n" + 
				"1111,2,ItemS2,ItemS2 Description,4,each,LocX25,1111,2,\n" + 
				"1111,3,ItemS3,ItemS3 Description,5,each,LocX26,1111,3,TRUE";
		
		importOrdersData(getFacility(), csvOrders);
		commitTransaction();
		
		startSiteController();
		picker = createPickSim(cheGuid1);
	}
	
	@Test
	public void testSubstitutionFlag() throws IOException {
		LOGGER.info("1: Setup order on CHE and start pick");
		picker.loginAndSetup("Worker1");
		picker.setupContainer("1111", "1");
		picker.scanCommand("START");
		picker.waitForCheState(CheStateEnum.SETUP_SUMMARY, 4000);
		picker.scanCommand("START");
		picker.waitForCheState(CheStateEnum.SCAN_SOMETHING, 4000);
		Assert.assertEquals("LocX24\nItemS1\nQTY 3\nSCAN SKU NEEDED\n", picker.getLastCheDisplay());
		
		LOGGER.info("2: Assert that first item has substituteAllowed = true");
		WorkInstruction wi = picker.getActivePick();
		Assert.assertTrue(wi.getSubstituteAllowed());
		
		LOGGER.info("3: Pick first item and advance to the second one");
		picker.scanSomething("ItemS1");
		picker.waitForCheState(CheStateEnum.DO_PICK, 4000);
		picker.pickItemAuto();
		picker.waitForCheState(CheStateEnum.SCAN_SOMETHING, 4000);
		Assert.assertEquals("LocX25\nItemS2\nQTY 4\nSCAN SKU NEEDED\n", picker.getLastCheDisplay());
		
		LOGGER.info("4: Assert that second item has substituteAllowed = false");
		wi = picker.getActivePick();
		Assert.assertFalse(wi.getSubstituteAllowed());
	}
}
