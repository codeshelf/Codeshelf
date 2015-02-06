/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2014, Codeshelf, All rights reserved
 *  file IntegrationTest1.java
 *******************************************************************************/
package com.gadgetworks.codeshelf.integration;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Timestamp;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gadgetworks.codeshelf.application.Configuration;
import com.gadgetworks.codeshelf.device.CheStateEnum;
import com.gadgetworks.codeshelf.edi.AislesFileCsvImporter;
import com.gadgetworks.codeshelf.edi.ICsvLocationAliasImporter;
import com.gadgetworks.codeshelf.edi.ICsvOrderImporter;
import com.gadgetworks.codeshelf.model.domain.Aisle;
import com.gadgetworks.codeshelf.model.domain.Che;
import com.gadgetworks.codeshelf.model.domain.Che.ProcessMode;
import com.gadgetworks.codeshelf.model.domain.CodeshelfNetwork;
import com.gadgetworks.codeshelf.model.domain.Facility;
import com.gadgetworks.codeshelf.model.domain.LedController;
import com.gadgetworks.codeshelf.model.domain.Location;
import com.gadgetworks.codeshelf.model.domain.OrderDetail;
import com.gadgetworks.codeshelf.model.domain.OrderHeader;
import com.gadgetworks.codeshelf.model.domain.Organization;
import com.gadgetworks.codeshelf.model.domain.Path;
import com.gadgetworks.codeshelf.model.domain.PathSegment;
import com.gadgetworks.codeshelf.model.domain.WorkInstruction;
import com.gadgetworks.codeshelf.util.ThreadUtils;
import com.gadgetworks.flyweight.command.NetGuid;

/**
 * @author jon ranstrom
 *
 */
public class CheProcessLineScan extends EndToEndIntegrationTest {

	private static final Logger	LOGGER	= LoggerFactory.getLogger(CheProcessLineScan.class);

	static {
		Configuration.loadConfig("test");
	}

	public CheProcessLineScan() {

	}

	private Facility setUpSmallNoSlotFacility() {
		// This returns a facility with aisle A1, with two bays with one tier each. No slots. With a path, associated to the aisle.
		//   With location alias for first baytier only, not second.
		// The organization will get "O-" prepended to the name. Facility F-
		// Caller must use a different organization name each time this is used
		// Valid tier names: A1.B1.T1 = D101, and A1.B2.T1
		// Also, A1.B1 has alias D100
		// Just for variance, bay3 has 4 slots
		// Aisle 2 associated to same path segment. But with aisle controller on the other side
		// Aisle 3 will be on a separate path.
		// All tiers have controllers associated.
		// There are two CHE called CHE1 and CHE2

		/*
		Organization organization = new Organization();
		String oName = "O-" + inOrganizationName;
		organization.setDomainId(oName);
		mOrganizationDao.store(organization);
		*/

		/*
		String fName = "F-" + inOrganizationName;
		organization.createFacility(fName, "TEST", Point.getZeroPoint());
		Facility facility = organization.getFacility(fName);
		*/

		String csvString = "binType,nominalDomainId,lengthCm,slotsInTier,ledCountInTier,tierFloorCm,controllerLED,anchorX,anchorY,orientXorY,depthCm\r\n" //
				+ "Aisle,A1,,,,,tierB1S1Side,12.85,43.45,X,120,Y\r\n" //
				+ "Bay,B1,230,,,,,\r\n" //
				+ "Tier,T1,,0,80,0,,\r\n" //
				+ "Bay,B2,230,,,,,\r\n" //
				+ "Tier,T1,,0,80,80,,\r\n" //
				+ "Bay,B3,230,,,,,\r\n" //
				+ "Tier,T1,,4,80,160,,\r\n" //
				+ "Aisle,A2,,,,,tierNotB1S1Side,12.85,55.45,X,120,Y\r\n" //
				+ "Bay,B1,230,,,,,\r\n" //
				+ "Tier,T1,,0,80,0,,\r\n"//
				+ "Bay,B2,230,,,,,\r\n" //
				+ "Tier,T1,,0,80,80,,\r\n" //
				+ "Bay,B3,230,,,,,\r\n" //
				+ "Tier,T1,,0,80,160,,\r\n" //
				+ "Aisle,A3,,,,,tierNotB1S1Side,12.85,65.45,X,120,Y\r\n" //
				+ "Bay,B1,230,,,,,\r\n" //
				+ "Tier,T1,,0,80,0,,\r\n"//
				+ "Bay,B2,230,,,,,\r\n" //
				+ "Tier,T1,,0,80,80,,\r\n" //
				+ "Bay,B3,230,,,,,\r\n" //
				+ "Tier,T1,,0,80,160,,\r\n"; //

		byte[] csvArray = csvString.getBytes();

		ByteArrayInputStream stream = new ByteArrayInputStream(csvArray);
		InputStreamReader reader = new InputStreamReader(stream);

		Timestamp ediProcessTime = new Timestamp(System.currentTimeMillis());
		AislesFileCsvImporter importer = createAisleFileImporter();
		importer.importAislesFileFromCsvStream(reader, getFacility(), ediProcessTime);

		// Get the aisle
		Aisle aisle1 = Aisle.DAO.findByDomainId(getFacility(), "A1");
		Assert.assertNotNull(aisle1);

		Path aPath = createPathForTest(getFacility());
		PathSegment segment0 = addPathSegmentForTest(aPath, 0, 22.0, 48.45, 12.85, 48.45);

		String persistStr = segment0.getPersistentId().toString();
		aisle1.associatePathSegment(persistStr);

		Aisle aisle2 = Aisle.DAO.findByDomainId(getFacility(), "A2");
		Assert.assertNotNull(aisle2);
		aisle2.associatePathSegment(persistStr);

		Path path2 = createPathForTest(getFacility());
		PathSegment segment02 = addPathSegmentForTest(path2, 0, 22.0, 58.45, 12.85, 58.45);

		Aisle aisle3 = Aisle.DAO.findByDomainId(getFacility(), "A3");
		Assert.assertNotNull(aisle3);
		String persistStr2 = segment02.getPersistentId().toString();
		aisle3.associatePathSegment(persistStr2);

		String csvString2 = "mappedLocationId,locationAlias\r\n" //
				+ "A1.B1, D300\r\n" //
				+ "A1.B2, D400\r\n" //
				+ "A1.B3, D500\r\n" //
				+ "A1.B1.T1, D301\r\n" //
				+ "A1.B2.T1, D302\r\n" //
				+ "A1.B3.T1, D303\r\n" //
				+ "A2.B1.T1, D401\r\n" //
				+ "A2.B2.T1, D402\r\n" //
				+ "A2.B3.T1, D403\r\n"//
				+ "A3.B1.T1, D501\r\n" //
				+ "A3.B2.T1, D502\r\n" //
				+ "A3.B3.T1, D503\r\n";//

		byte[] csvArray2 = csvString2.getBytes();

		ByteArrayInputStream stream2 = new ByteArrayInputStream(csvArray2);
		InputStreamReader reader2 = new InputStreamReader(stream2);

		Timestamp ediProcessTime2 = new Timestamp(System.currentTimeMillis());
		ICsvLocationAliasImporter importer2 = createLocationAliasImporter();
		importer2.importLocationAliasesFromCsvStream(reader2, getFacility(), ediProcessTime2);

		CodeshelfNetwork network = getNetwork();
		Organization organization = getOrganization();
		String organizationId = organization.getDomainId();

		LedController controller1 = network.findOrCreateLedController(organizationId, new NetGuid("0x00000011"));
		LedController controller2 = network.findOrCreateLedController(organizationId, new NetGuid("0x00000012"));
		LedController controller3 = network.findOrCreateLedController(organizationId, new NetGuid("0x00000013"));

		Short channel1 = 1;
		Location tier = getFacility().findSubLocationById("A1.B1.T1");
		controller1.addLocation(tier);
		tier.setLedChannel(channel1);
		tier.getDao().store(tier);
		// Make sure we also got the alias
		String tierName = tier.getPrimaryAliasId();
		if (!tierName.equals("D301"))
			LOGGER.error("D301 vs. A1.B1.T1 alias not set up in setUpSimpleNoSlotFacility");

		tier = getFacility().findSubLocationById("A1.B2.T1");
		controller1.addLocation(tier);
		tier.setLedChannel(channel1);
		tier.getDao().store(tier);
		tier = getFacility().findSubLocationById("A1.B3.T1");
		controller1.addLocation(tier);
		tier.setLedChannel(channel1);
		tier.getDao().store(tier);
		tier = getFacility().findSubLocationById("A2.B1.T1");
		controller2.addLocation(tier);
		tier.setLedChannel(channel1);
		tier.getDao().store(tier);
		tier = getFacility().findSubLocationById("A2.B2.T1");
		controller2.addLocation(tier);
		tier.setLedChannel(channel1);
		tier.getDao().store(tier);
		tier = getFacility().findSubLocationById("A3.B1.T1");
		controller3.addLocation(tier);
		tier.setLedChannel(channel1);
		tier.getDao().store(tier);
		tier = getFacility().findSubLocationById("A3.B2.T1");
		controller3.addLocation(tier);
		tier.setLedChannel(channel1);
		tier.getDao().store(tier);

		return getFacility();
	}

	private void setUpLineScanOrders(Facility inFacility) throws IOException {
		// Outbound order. No group. Using 5 digit order number and .N detail ID. No preassigned container number.
		// Using preferredLocation. No inventory.
		// Locations D301-303, 401-403, 501-503 are modeled. 600s and 700s are not.
		// Order 12345 has 2 modeled locations and one not.
		// Order 11111 has 5 unmodeled locations.

		String csvString2 = "orderGroupId,shipmentId,customerId,orderId,orderDetailId,itemId,description,quantity,uom, locationId"
				+ "\r\n,USF314,COSTCO,12345,12345.1,1123,12/16 oz Bowl Lids -PLA Compostable,1,each, D301"
				+ "\r\n,USF314,COSTCO,12345,12345.2,1493,PARK RANGER Doll,1,each, D302"
				+ "\r\n,USF314,COSTCO,12345,12345.3,1522,Butterfly Yoyo,1,each, D601"
				+ "\r\n,USF314,COSTCO,11111,11111.1,1122,8 oz Bowl Lids -PLA Compostable,1,each, D401"
				+ "\r\n,USF314,COSTCO,11111,11111.2,1522,Butterfly Yoyo,1,each, D601"
				+ "\r\n,USF314,COSTCO,11111,11111.3,1523,SJJ BPP,1,each, D602"
				+ "\r\n,USF314,COSTCO,11111,11111.4,1124,8 oz Bowls -PLA Compostable,1,each, D603"
				+ "\r\n,USF314,COSTCO,11111,11111.5,1555,paper towel,2,each, D604";

		byte[] csvArray2 = csvString2.getBytes();

		ByteArrayInputStream stream2 = new ByteArrayInputStream(csvArray2);
		InputStreamReader reader2 = new InputStreamReader(stream2);

		Timestamp ediProcessTime2 = new Timestamp(System.currentTimeMillis());
		ICsvOrderImporter importer2 = createOrderImporter();
		importer2.importOrdersFromCsvStream(reader2, inFacility, ediProcessTime2);

	}

	/**
	 * Wait until a recent CHE update went through the updateNetwork mechanism, replacing the device logic for the che
	 * May want to promote this.
	*/
	private PickSimulator waitAndGetPickerForProcessType(EndToEndIntegrationTest test, NetGuid cheGuid, String inProcessType) {
		// took over 250 ms on JR's fast macbook pro. Hence the initial wait, then checking more frequently in the loop
		ThreadUtils.sleep(250);
		long start = System.currentTimeMillis();
		final long maxTimeToWaitMillis = 5000;
		String existingType = "";
		int count = 0;
		while (System.currentTimeMillis() - start < maxTimeToWaitMillis) {
			count++;
			PickSimulator picker = new PickSimulator(test, cheGuid);
			existingType = picker.getProcessType();
			if (existingType.equals(inProcessType)) {
				LOGGER.info(count + " pickers made in waitAndGetPickerForProcessType before getting it right");
				return picker;
			}
			ThreadUtils.sleep(100); // retry every 100ms
		}
		Assert.fail("Process type " + inProcessType + " not encountered in " + maxTimeToWaitMillis + "ms. Process type is "
				+ existingType);
		return null;
	}

	/**
	 * Basic login, logout, log in again, scan a valid order detail ID, see the job. Complete the job. Same scan again does not give you the job again.
	 * LOCAPICK is false, so no inventory created at the location.
	 * Scanning detail 11111.1 with locationId D401, which is modeled.
	 */
	@Test
	public final void testLineScanLogin() throws IOException {

		this.getPersistenceService().beginTenantTransaction();
		Facility facility = setUpSmallNoSlotFacility();
		setUpLineScanOrders(facility);
		this.getPersistenceService().commitTenantTransaction();

		this.getPersistenceService().beginTenantTransaction();
		facility = Facility.DAO.reload(facility);
		Assert.assertNotNull(facility);

		// Prove that our orders file is working
		OrderHeader order1 = facility.getOrderHeader("11111");
		Assert.assertNotNull(order1);
		OrderDetail detail1_1 = order1.getOrderDetail("11111.1");
		Assert.assertNotNull(detail1_1);
		String loc1_1 = detail1_1.getPreferredLocationUi();
		Assert.assertEquals("D401", loc1_1);

		// we need to set che1 to be in line scan mode
		CodeshelfNetwork network = getNetwork();
		Che che1 = network.getChe("CHE-E2E-1");
		Assert.assertNotNull(che1);
		Assert.assertEquals(cheGuid1, che1.getDeviceNetGuid()); // just checking since we use cheGuid1 to get the picker.
		che1.setProcessMode(ProcessMode.LINE_SCAN);
		Che.DAO.store(che1);

		this.getPersistenceService().commitTenantTransaction();

		// Need to give time for the the CHE update to process through the site controller before settling on our picker.
		PickSimulator picker = waitAndGetPickerForProcessType(this, cheGuid1, "CHE_LINESCAN");

		LOGGER.info(picker.getPickerTypeAndState("0:"));

		Assert.assertEquals(CheStateEnum.IDLE, picker.currentCheState());

		// login goes to ready state. (Says to scan a line).
		picker.loginAndCheckState("Picker #1", CheStateEnum.READY);
		LOGGER.info(picker.getPickerTypeAndState("1:"));

		// logout back to idle state.
		picker.logout();
		picker.waitForCheState(CheStateEnum.IDLE, 2000);
		LOGGER.info(picker.getPickerTypeAndState("2:"));

		// login again
		picker.loginAndCheckState("Picker #1", CheStateEnum.READY);
		LOGGER.info(picker.getPickerTypeAndState("3:")); //picker.simulateCommitByChangingTransaction(this.persistenceService);

		// scan an order detail id results in sending to server, but transitioning to a computing state to wait for work instruction from server.
		LOGGER.info(picker.getPickerTypeAndState("4:"));
		picker.scanOrderDetailId("11111.1"); // does not add "%"	

		// GET_WORK happened immediately. DO_PICK happens when the command response comes back
		picker.waitForCheState(CheStateEnum.DO_PICK, 5000);
		String firstLine = picker.getLastCheDisplayString();
		LOGGER.info(picker.getPickerTypeAndState("5:"));

		// Should be showing the job now. 
		Assert.assertEquals("D401", firstLine);
		
		// Complete this job. For line scan, the poscon index is always 1.
		WorkInstruction wi = picker.getActivePick();
		int quant = wi.getPlanQuantity();
		picker.pick(1, quant);
		picker.waitForCheState(CheStateEnum.READY, 2000);

		// Try to get the same job again.
		picker.scanOrderDetailId("11111.1"); // does not add "%"	
		// GET_WORK happened immediately. Instead of DO_PICK, back to READY if there was no job.
		picker.waitForCheState(CheStateEnum.READY, 5000);
		// second line would say "Already completed", but we do not have a means to check htis.

		// logout back to idle state.
		picker.logout();
		picker.waitForCheState(CheStateEnum.IDLE, 2000);
	}

	/**
	 * Login, scan a valid order detail ID, see the job. Then assorted re-scans and clears.
	 * LOCAPICK is false, so no attempt at inventory creation.
	 * Scanning detail 12345.3 with locationId D601, which is not modeled.
	 * Other scans of non-existent 44444.1, and of 11111.1 with modeled location D401
	 */
	@Test
	public final void testLineScanProcessExceptions() throws IOException {

		this.getPersistenceService().beginTenantTransaction();
		Facility facility = setUpSmallNoSlotFacility();
		setUpLineScanOrders(facility);
		this.getPersistenceService().commitTenantTransaction();

		this.getPersistenceService().beginTenantTransaction();
		facility = Facility.DAO.reload(facility);
		Assert.assertNotNull(facility);

		// Prove that our orders file is working
		OrderHeader order5 = facility.getOrderHeader("12345");
		Assert.assertNotNull(order5);
		OrderDetail detail5_3 = order5.getOrderDetail("12345.3");
		Assert.assertNotNull(detail5_3);
		String loc5_3 = detail5_3.getPreferredLocationUi();
		Assert.assertEquals("D601", loc5_3);

		// we need to set che1 to be in line scan mode
		CodeshelfNetwork network = getNetwork();
		Che che1 = network.getChe("CHE-E2E-1");
		Assert.assertNotNull(che1);
		Assert.assertEquals(cheGuid1, che1.getDeviceNetGuid()); // just checking since we use cheGuid1 to get the picker.
		che1.setProcessMode(ProcessMode.LINE_SCAN);
		Che.DAO.store(che1);

		this.getPersistenceService().commitTenantTransaction();

		// Need to give time for the the CHE update to process through the site controller before settling on our picker.
		PickSimulator picker = waitAndGetPickerForProcessType(this, cheGuid1, "CHE_LINESCAN");
		Assert.assertEquals(CheStateEnum.IDLE, picker.currentCheState());

		LOGGER.info("1: login, should go to READY state");
		picker.loginAndCheckState("Picker #1", CheStateEnum.READY);

		LOGGER.info("2: scan order, should go to DO_PICK state");
		picker.scanOrderDetailId("12345.3"); // does not add "%"	

		// GET_WORK happened immediately. DO_PICK happens when the command response comes back
		picker.waitForCheState(CheStateEnum.DO_PICK, 5000);
		LOGGER.info("2b: List the work instructions as the site controller sees them");
		List<WorkInstruction> theWiList = picker.getActivePickList();
		logWiList(theWiList);

		String firstLine = picker.getLastCheDisplayString();
		// Should be showing the job now. 
		Assert.assertEquals("D601", firstLine);

		// logout back to idle state.
		picker.logout();
		picker.waitForCheState(CheStateEnum.IDLE, 2000);
	}

}
