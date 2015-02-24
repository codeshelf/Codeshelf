/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2014, Codeshelf, All rights reserved
 *  file IntegrationTest1.java
 *******************************************************************************/
package com.codeshelf.integration;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.sql.Timestamp;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.device.CheStateEnum;
import com.codeshelf.device.CsDeviceManager;
import com.codeshelf.edi.AislesFileCsvImporter;
import com.codeshelf.edi.ICsvLocationAliasImporter;
import com.codeshelf.edi.ICsvOrderImporter;
import com.codeshelf.flyweight.command.NetGuid;
import com.codeshelf.model.dao.PropertyDao;
import com.codeshelf.model.domain.Aisle;
import com.codeshelf.model.domain.Che;
import com.codeshelf.model.domain.Che.ProcessMode;
import com.codeshelf.model.domain.CodeshelfNetwork;
import com.codeshelf.model.domain.DomainObjectProperty;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.LedController;
import com.codeshelf.model.domain.Location;
import com.codeshelf.model.domain.OrderDetail;
import com.codeshelf.model.domain.OrderHeader;
import com.codeshelf.model.domain.Path;
import com.codeshelf.model.domain.PathSegment;
import com.codeshelf.model.domain.WorkInstruction;
import com.codeshelf.service.PropertyService;
import com.codeshelf.util.ThreadUtils;

/**
 * @author jon ranstrom
 *
 */
public class CheProcessLineScan extends EndToEndIntegrationTest {

	private static final Logger	LOGGER	= LoggerFactory.getLogger(CheProcessLineScan.class);

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

		String csvAisles = "binType,nominalDomainId,lengthCm,slotsInTier,ledCountInTier,tierFloorCm,controllerLED,anchorX,anchorY,orientXorY,depthCm\r\n" //
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

		Timestamp ediProcessTime = new Timestamp(System.currentTimeMillis());
		AislesFileCsvImporter importer = createAisleFileImporter();
		importer.importAislesFileFromCsvStream(new StringReader(csvAisles), getFacility(), ediProcessTime);

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

		String csvAliases = "mappedLocationId,locationAlias\r\n" //
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

		Timestamp ediProcessTime2 = new Timestamp(System.currentTimeMillis());
		ICsvLocationAliasImporter locationAliasImporter = createLocationAliasImporter();
		locationAliasImporter.importLocationAliasesFromCsvStream(new StringReader(csvAliases), getFacility(), ediProcessTime2);

		CodeshelfNetwork network = getNetwork();

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

	private void setUpLineScanOrdersNoCntr(Facility inFacility) throws IOException {
		// Outbound order. No group. Using 5 digit order number and .N detail ID. No preassigned container number.
		// Using preferredLocation. No inventory.
		// Locations D301-303, 401-403, 501-503 are modeled. 600s and 700s are not.
		// Order 12345 has 2 modeled locations and one not.
		// Order 11111 has 4 unmodeled locations and one modeled.

		String csvOrders = "orderGroupId,shipmentId,customerId,orderId,orderDetailId,itemId,description,quantity,uom, locationId"
				+ "\r\n,USF314,COSTCO,12345,12345.1,1123,12/16 oz Bowl Lids -PLA Compostable,1,each, D301"
				+ "\r\n,USF314,COSTCO,12345,12345.2,1493,PARK RANGER Doll,1,each, D302"
				+ "\r\n,USF314,COSTCO,12345,12345.3,1522,Butterfly Yoyo,3,each, D601"
				+ "\r\n,USF314,COSTCO,11111,11111.1,1122,8 oz Bowl Lids -PLA Compostable,2,each, D401"
				+ "\r\n,USF314,COSTCO,11111,11111.2,1522,Butterfly Yoyo,1,each, D601"
				+ "\r\n,USF314,COSTCO,11111,11111.3,1523,SJJ BPP,1,each, D602"
				+ "\r\n,USF314,COSTCO,11111,11111.4,1124,8 oz Bowls -PLA Compostable,1,each, D603"
				+ "\r\n,USF314,COSTCO,11111,11111.5,1555,paper towel,2,each, D604";

		Timestamp ediProcessTime = new Timestamp(System.currentTimeMillis());
		ICsvOrderImporter orderImporter = createOrderImporter();
		orderImporter.importOrdersFromCsvStream(new StringReader(csvOrders), inFacility, ediProcessTime);
	}
	
	private void setUpLineScanOrdersWithCntr(Facility inFacility) throws IOException {
		// Exactly the same as above, but with preAssignedContainerId set equal to the orderId

		String csvOrders = "orderGroupId,shipmentId,customerId,orderId,preAssignedContainerId,orderDetailId,itemId,description,quantity,uom, locationId"
				+ "\r\n,USF314,COSTCO,12345,12345,12345.1,1123,12/16 oz Bowl Lids -PLA Compostable,1,each, D301"
				+ "\r\n,USF314,COSTCO,12345,12345,12345.2,1493,PARK RANGER Doll,1,each, D302"
				+ "\r\n,USF314,COSTCO,12345,12345,12345.3,1522,Butterfly Yoyo,3,each, D601"
				+ "\r\n,USF314,COSTCO,11111,11111,11111.1,1122,8 oz Bowl Lids -PLA Compostable,2,each, D401"
				+ "\r\n,USF314,COSTCO,11111,11111,11111.2,1522,Butterfly Yoyo,1,each, D601"
				+ "\r\n,USF314,COSTCO,11111,11111,11111.3,1523,SJJ BPP,1,each, D602"
				+ "\r\n,USF314,COSTCO,11111,11111,11111.4,1124,8 oz Bowls -PLA Compostable,1,each, D603"
				+ "\r\n,USF314,COSTCO,11111,11111,11111.5,1555,paper towel,2,each, D604";

		Timestamp ediProcessTime = new Timestamp(System.currentTimeMillis());
		ICsvOrderImporter orderImporter = createOrderImporter();
		orderImporter.importOrdersFromCsvStream(new StringReader(csvOrders), inFacility, ediProcessTime);
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
		Assert.fail(String.format("Process type %s not encounter in %dms after %d checks. Process type is %s", inProcessType, maxTimeToWaitMillis, count, existingType));
		return null;
	}

	/**
	 * Basic login, logout, log in again, scan a valid order detail ID, see the job. Complete the job. Same scan again does not give you the job again.
	 * LOCAPICK is false, so no inventory created at the location.
	 * Scanning detail 11111.1 with locationId D401, which is modeled.
	 */
	@Test
	public final void testLineScanLogin() throws IOException {

		this.getTenantPersistenceService().beginTransaction();
		Facility facility = setUpSmallNoSlotFacility();
		setUpLineScanOrdersNoCntr(facility);
		this.getTenantPersistenceService().commitTransaction();

		this.getTenantPersistenceService().beginTransaction();
		facility = Facility.DAO.reload(facility);
		Assert.assertNotNull(facility);

		// Prove that our orders file is working. D401 is a modeled location (alias for a Tier)
		OrderHeader order1 = facility.getOrderHeader("11111");
		Assert.assertNotNull(order1);
		OrderDetail detail1_1 = order1.getOrderDetail("11111.1");
		Assert.assertNotNull(detail1_1);
		String loc1_1 = detail1_1.getPreferredLocationUi();
		Assert.assertEquals("D401", loc1_1);

		// we need to set che1 to be in line scan mode
		CodeshelfNetwork network = getNetwork();
		Che che1 = network.getChe("CHE1");
		Assert.assertNotNull(che1);
		Assert.assertEquals(cheGuid1, che1.getDeviceNetGuid()); // just checking since we use cheGuid1 to get the picker.
		che1.setProcessMode(ProcessMode.LINE_SCAN);
		Che.DAO.store(che1);

		this.getTenantPersistenceService().commitTransaction();

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
	 * Basic login, logout, log in again, scan a valid order detail ID, see the job. Complete the job. Same scan again does not give you the job again.
	 * LOCAPICK is false, so no inventory created at the location.
	 * Scanning detail 11111.1 with locationId D401, which is modeled.
	 */
	
	@Test
	public final void testSiteParamConfig() throws IOException {

		this.getTenantPersistenceService().beginTransaction();
		Facility facility = setUpSmallNoSlotFacility();
		setUpLineScanOrdersNoCntr(facility);
		this.getTenantPersistenceService().commitTransaction();

		this.getTenantPersistenceService().beginTransaction();
		facility = Facility.DAO.reload(facility);
		Assert.assertNotNull(facility);
		this.getTenantPersistenceService().commitTransaction();
		
		this.getTenantPersistenceService().beginTransaction();
		CsDeviceManager manager = this.getDeviceManager();
		Assert.assertNotNull(manager);
		
		// Test autoShort
		manager.setAutoShortValue(true);
		Assert.assertTrue(manager.getAutoShortValue());
		
		manager.setAutoShortValue(false);
		Assert.assertFalse(manager.getAutoShortValue());
		
		// Test containterType
		manager.setContainerTypeValue("Both");
		Assert.assertEquals("Both", manager.getContainerTypeValue());
		
		manager.setContainerTypeValue("SKU");
		Assert.assertEquals("SKU", manager.getContainerTypeValue());
		
		manager.setContainerTypeValue("Description");
		Assert.assertEquals("Description", manager.getContainerTypeValue());
		
		// Test scanType
		manager.setScanTypeValue("SKU");
		Assert.assertEquals("SKU", manager.getScanTypeValue());
		
		manager.setScanTypeValue("UPC");
		Assert.assertEquals("UPC", manager.getScanTypeValue());
		
		manager.setScanTypeValue("Disabled");
		Assert.assertEquals("Disabled", manager.getScanTypeValue());
		
		this.getTenantPersistenceService().commitTransaction();
		
	}
	
	/**
	 * Login, scan a valid order detail ID, see the job. Then assorted re-scans and clears.
	 * LOCAPICK is false, so no attempt at inventory creation.
	 * Scanning detail 12345.3 with locationId D601, which is not modeled.
	 * Other scans of non-existent 44444.1, and of 11111.1 with modeled location D401
	 */
	@Test
	public final void testLineScanProcessExceptions() throws IOException {

		this.getTenantPersistenceService().beginTransaction();
		Facility facility = setUpSmallNoSlotFacility();
		setUpLineScanOrdersNoCntr(facility);
		this.getTenantPersistenceService().commitTransaction();

		this.getTenantPersistenceService().beginTransaction();
		facility = Facility.DAO.reload(facility);
		Assert.assertNotNull(facility);

		// Prove that our orders file is working. D601 is an unmodeled location
		OrderHeader order5 = facility.getOrderHeader("12345");
		Assert.assertNotNull(order5);
		OrderDetail detail5_3 = order5.getOrderDetail("12345.3");
		Assert.assertNotNull(detail5_3);
		String loc5_3 = detail5_3.getPreferredLocationUi();
		Assert.assertEquals("D601", loc5_3);

		// we need to set che1 to be in line scan mode
		CodeshelfNetwork network = getNetwork();
		Che che1 = network.getChe("CHE1");
		Assert.assertNotNull(che1);
		Assert.assertEquals(cheGuid1, che1.getDeviceNetGuid()); // just checking since we use cheGuid1 to get the picker.
		che1.setProcessMode(ProcessMode.LINE_SCAN);
		Che.DAO.store(che1);

		this.getTenantPersistenceService().commitTransaction();

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

		LOGGER.info("3: scan clear, which should bring us directly to ready state");
		picker.scanCommand("CLEAR");
		picker.waitForCheState(CheStateEnum.READY, 2000);

		LOGGER.info("4: scan non-existent order detail. Goes through GET_WORK, then back to READY state");
		picker.scanOrderDetailId("44444.1");
		picker.waitForCheState(CheStateEnum.READY, 4000);

		LOGGER.info("5: scan good detail.");
		picker.scanOrderDetailId("12345.3");
		picker.waitForCheState(CheStateEnum.DO_PICK, 4000);

		LOGGER.info("6: while on this job scan another good detail. Screen will ask for yes or no.");
		picker.scanOrderDetailId("11111.1");
		picker.waitForCheState(CheStateEnum.ABANDON_CHECK, 2000);

		LOGGER.info("7b: scan NO, so we should be on the 12345.3 job");
		picker.scanCommand("NO");
		picker.waitForCheState(CheStateEnum.DO_PICK, 2000);
		Assert.assertEquals("D601", picker.getLastCheDisplayString());

		LOGGER.info("7: repeat: scan another good detail. Screen will ask for yes or no.");
		picker.scanOrderDetailId("11111.1");
		picker.waitForCheState(CheStateEnum.ABANDON_CHECK, 2000);

		LOGGER.info("7b: however,scan clear from the yes/no screen; back to ready state");
		picker.scanCommand("CLEAR");
		picker.waitForCheState(CheStateEnum.READY, 2000);

		LOGGER.info("8: scan good detail.");
		picker.scanOrderDetailId("12345.3");
		picker.waitForCheState(CheStateEnum.DO_PICK, 4000);

		LOGGER.info("8b: while on this job scan another good detail. Screen will ask for yes or no.");
		picker.scanOrderDetailId("11111.1");
		picker.waitForCheState(CheStateEnum.ABANDON_CHECK, 2000);

		LOGGER.info("8c: scan YES, so we should be on the 11111.1 job");
		picker.scanCommand("YES");
		picker.waitForCheState(CheStateEnum.DO_PICK, 4000);
		Assert.assertEquals("D401", picker.getLastCheDisplayString());

		// make sure we can logout from ABANDON_CHECK and DO_PICK		
		LOGGER.info("9: logout works from DO_PICK");
		picker.logout();
		picker.waitForCheState(CheStateEnum.IDLE, 2000);

		LOGGER.info("10a: login");
		picker.loginAndCheckState("Picker #1", CheStateEnum.READY);
		LOGGER.info("10b: scan good detail.");
		picker.scanOrderDetailId("12345.3");
		picker.waitForCheState(CheStateEnum.DO_PICK, 4000);
		LOGGER.info("10c: while on this job scan another good detail. Screen will ask for yes or no.");
		picker.scanOrderDetailId("11111.1");
		picker.waitForCheState(CheStateEnum.ABANDON_CHECK, 2000);
		LOGGER.info("10d: logout works from ABANDON_CHECK");
		picker.logout();
		picker.waitForCheState(CheStateEnum.IDLE, 2000);

	}

	/**
	 * Login, scan a valid order detail ID, see the job.  Short it. Then assorted re-scans, clears, as well as successful short.
	 * LOCAPICK is false, so no attempt at inventory creation.
	 * Scanning detail 12345.3 with locationId D601, which is not modeled.
	 * Other scans of non-existent 44444.1, and of 11111.1 with modeled location D401
	 */
	@Test
	public final void testLineScanShorts() throws IOException {

		this.getTenantPersistenceService().beginTransaction();
		Facility facility = setUpSmallNoSlotFacility();
		setUpLineScanOrdersNoCntr(facility);
		this.getTenantPersistenceService().commitTransaction();

		this.getTenantPersistenceService().beginTransaction();
		facility = Facility.DAO.reload(facility);
		Assert.assertNotNull(facility);

		// we need to set che1 to be in line scan mode
		CodeshelfNetwork network = getNetwork();
		Che che1 = network.getChe("CHE1");
		Assert.assertNotNull(che1);
		Assert.assertEquals(cheGuid1, che1.getDeviceNetGuid()); // just checking since we use cheGuid1 to get the picker.
		che1.setProcessMode(ProcessMode.LINE_SCAN);
		Che.DAO.store(che1);

		this.getTenantPersistenceService().commitTransaction();

		// Need to give time for the the CHE update to process through the site controller before settling on our picker.
		PickSimulator picker = waitAndGetPickerForProcessType(this, cheGuid1, "CHE_LINESCAN");
		Assert.assertEquals(CheStateEnum.IDLE, picker.currentCheState());

		LOGGER.info("1a: login, should go to READY state");
		picker.loginAndCheckState("Picker #1", CheStateEnum.READY);

		LOGGER.info("1b: scan order, should go to DO_PICK state");
		picker.scanOrderDetailId("12345.3"); // does not add "%"	
		// GET_WORK happened immediately. DO_PICK happens when the command response comes back
		picker.waitForCheState(CheStateEnum.DO_PICK, 5000);
		Assert.assertEquals("D601", picker.getLastCheDisplayString());
		WorkInstruction wi = picker.getActivePick();
		int quant = wi.getPlanQuantity();
		Assert.assertEquals(3, quant); // This order detail has quantity 3

		LOGGER.info("2a: scan SHORT");
		picker.scanCommand("SHORT");
		picker.waitForCheState(CheStateEnum.SHORT_PICK, 2000);

		LOGGER.info("2b: check that the count should be 2");
		wi = picker.getActivePick();
		quant = wi.getPlanQuantity();
		Assert.assertEquals(3, quant); // not affected by being in SHORT_PICK state

		LOGGER.info("2c: submit only 2");
		picker.pick(1, 2);
		picker.waitForCheState(CheStateEnum.SHORT_PICK_CONFIRM, 2000);

		LOGGER.info("2c: scan YES, to complete the short");
		picker.scanCommand("YES");
		picker.waitForCheState(CheStateEnum.READY, 2000);

		LOGGER.info("3a: scan same order again");
		picker.scanOrderDetailId("12345.3"); 	
		picker.waitForCheState(CheStateEnum.DO_PICK, 5000);
		Assert.assertEquals("D601", picker.getLastCheDisplayString());

		LOGGER.info("3b: check that the count should be 1");
		wi = picker.getActivePick();
		quant = wi.getPlanQuantity();
		Assert.assertEquals(1, quant);
		
		LOGGER.info("3c: scan SHORT");
		picker.scanCommand("SHORT");
		picker.waitForCheState(CheStateEnum.SHORT_PICK, 2000);
		
		LOGGER.info("3d: submit 0");
		picker.pick(1, 0);
		picker.waitForCheState(CheStateEnum.SHORT_PICK_CONFIRM, 2000);

		LOGGER.info("3e: scan NO, so we should be back on job");
		picker.scanCommand("NO");
		picker.waitForCheState(CheStateEnum.DO_PICK, 2000);
		Assert.assertEquals("D601", picker.getLastCheDisplayString());

		LOGGER.info("4: CLEAR from SHORT_PICK state");
		picker.scanCommand("SHORT");
		picker.waitForCheState(CheStateEnum.SHORT_PICK, 2000);
		picker.scanCommand("CLEAR");
		picker.waitForCheState(CheStateEnum.READY, 2000);

		LOGGER.info("5: CLEAR from SHORT_PICK_CONFIRM state");
		picker.scanOrderDetailId("12345.3"); 	
		picker.waitForCheState(CheStateEnum.DO_PICK, 5000);
		picker.scanCommand("SHORT");
		picker.waitForCheState(CheStateEnum.SHORT_PICK, 2000);
		picker.pick(1, 0);
		picker.waitForCheState(CheStateEnum.SHORT_PICK_CONFIRM, 2000);
		picker.scanCommand("CLEAR");
		picker.waitForCheState(CheStateEnum.READY, 2000);

		LOGGER.info("6: Scan other order line from SHORT_PICK state");
		picker.scanOrderDetailId("12345.3"); 	
		picker.waitForCheState(CheStateEnum.DO_PICK, 5000);
		picker.scanCommand("SHORT");
		picker.waitForCheState(CheStateEnum.SHORT_PICK, 2000);
		picker.scanOrderDetailId("11111.1");
		picker.waitForCheState(CheStateEnum.ABANDON_CHECK, 2000);
		
		LOGGER.info("6b: scan NO, so we should be on the 12345.3 job");
		picker.scanCommand("NO");
		picker.waitForCheState(CheStateEnum.DO_PICK, 2000);
		Assert.assertEquals("D601", picker.getLastCheDisplayString());
		
		LOGGER.info("6c: Repeat, but scan YES, so go to the 11111.1 job");
		picker.scanCommand("SHORT");
		picker.waitForCheState(CheStateEnum.SHORT_PICK, 2000);
		picker.scanOrderDetailId("11111.1");
		picker.waitForCheState(CheStateEnum.ABANDON_CHECK, 2000);
		picker.scanCommand("YES");
		picker.waitForCheState(CheStateEnum.DO_PICK, 2000);
		Assert.assertEquals("D401", picker.getLastCheDisplayString());

		LOGGER.info("7: Scan other order line from SHORT_PICK_CONFIRM state");
		picker.scanCommand("SHORT");
		picker.waitForCheState(CheStateEnum.SHORT_PICK, 2000);
		picker.pick(1, 0);
		picker.waitForCheState(CheStateEnum.SHORT_PICK_CONFIRM, 2000);
		picker.scanOrderDetailId("12345.3"); 	
		picker.waitForCheState(CheStateEnum.ABANDON_CHECK, 2000);
		
		LOGGER.info("7b: scan NO, so we should still be on the 11111.1 job");
		picker.scanCommand("NO");
		picker.waitForCheState(CheStateEnum.DO_PICK, 2000);
		Assert.assertEquals("D401", picker.getLastCheDisplayString());

		LOGGER.info("7c: Repeat, but scan YES, so go to the 12345.3 job");
		picker.scanCommand("SHORT");
		picker.waitForCheState(CheStateEnum.SHORT_PICK, 2000);
		picker.pick(1, 0);
		picker.waitForCheState(CheStateEnum.SHORT_PICK_CONFIRM, 2000);
		picker.scanOrderDetailId("12345.3");
		picker.waitForCheState(CheStateEnum.ABANDON_CHECK, 2000);
		picker.scanCommand("YES");
		picker.waitForCheState(CheStateEnum.DO_PICK, 2000);
		Assert.assertEquals("D601", picker.getLastCheDisplayString());
	
		picker.logout();
		picker.waitForCheState(CheStateEnum.IDLE, 2000);
	}
	
	/**
	 * Using the same data as linescan tests, try to setup up normal cart run
	 * LOCAPICK is false, so no inventory created at the location.
	 */
	@Test
	public final void testSetupOrderUnmodeledLocation() throws IOException {

		this.getTenantPersistenceService().beginTransaction();
		Facility facility = setUpSmallNoSlotFacility();
		setUpLineScanOrdersNoCntr(facility);
		this.getTenantPersistenceService().commitTransaction();

		this.getTenantPersistenceService().beginTransaction();
		facility = Facility.DAO.reload(facility);
		Assert.assertNotNull(facility);

		this.getTenantPersistenceService().commitTransaction();

		PickSimulator picker = waitAndGetPickerForProcessType(this, cheGuid1, "CHE_SETUPORDERS");

		Assert.assertEquals(CheStateEnum.IDLE, picker.currentCheState());
		picker.loginAndCheckState("Picker #1", CheStateEnum.CONTAINER_SELECT);

		LOGGER.info("1a: setup two orders on the cart. Several of the details have unmodelled preferred locations");
		// The setup is all the same, so just blow through it.
		picker.setupContainer("12345", "1"); 
		picker.waitForCheState(CheStateEnum.CONTAINER_SELECT, 1000);
		picker.setupContainer("11111", "2"); 
		picker.waitForCheState(CheStateEnum.CONTAINER_SELECT, 1000);
		
		LOGGER.info("1b: START. NO work, because the file did not have containerID set for the order");
		picker.scanCommand("START");
		picker.waitForCheState(CheStateEnum.NO_WORK, 4000);

		// logout back to idle state.
		picker.logout();
		picker.waitForCheState(CheStateEnum.IDLE, 2000);

		LOGGER.info("2a: Import the orders file again, but with containerId");
		this.getTenantPersistenceService().beginTransaction();
		facility = Facility.DAO.reload(facility);
		Assert.assertNotNull(facility);
		setUpLineScanOrdersWithCntr(facility);
		this.getTenantPersistenceService().commitTransaction();
	
		picker.loginAndCheckState("Picker #1", CheStateEnum.CONTAINER_SELECT);
		
		LOGGER.info("2b: setup two orders on the cart. Several of the details have unmodelled preferred locations");
		picker.setupContainer("12345", "1"); 
		picker.waitForCheState(CheStateEnum.CONTAINER_SELECT, 1000);
		picker.setupContainer("11111", "2"); 
		picker.waitForCheState(CheStateEnum.CONTAINER_SELECT, 1000);

		LOGGER.info("2c: START. Still no work, because no inventory. (and LOCAPICK was off, so not made).");
		// This is important. We could in principle make these work instructions as a special case of location-based pick. 
		// No inventory, but the order detail preferred location is resolvable, so we could do it
		picker.scanCommand("START");
		picker.waitForCheState(CheStateEnum.NO_WORK, 4000);
	
		// logout back to idle state.
		picker.logout();
		picker.waitForCheState(CheStateEnum.IDLE, 2000);
		
		LOGGER.info("3a: Set LOCAPICK, then import the orders file again, with containerId");
		this.getTenantPersistenceService().beginTransaction();
		facility = Facility.DAO.reload(facility);
		Assert.assertNotNull(facility);
		DomainObjectProperty theProperty = PropertyService.getPropertyObject(facility, DomainObjectProperty.LOCAPICK);
		if (theProperty != null) {
			theProperty.setValue(true);
			PropertyDao.getInstance().store(theProperty);
		}
		setUpLineScanOrdersWithCntr(facility);
		mPropertyService.turnOffHK(facility);
		this.getTenantPersistenceService().commitTransaction();
	
		picker.loginAndCheckState("Picker #1", CheStateEnum.CONTAINER_SELECT);

		LOGGER.info("3b: setup two orders on the cart. Several of the details have unmodelled preferred locations");
		picker.setupContainer("12345", "1"); 
		picker.waitForCheState(CheStateEnum.CONTAINER_SELECT, 1000);
		picker.setupContainer("11111", "2"); 
		picker.waitForCheState(CheStateEnum.CONTAINER_SELECT, 1000);
		
		LOGGER.info("3c: START. Now we get some work. 3 jobs, since only 3 details had modeled locatoins");
		picker.scanCommand("START");
		picker.waitForCheState(CheStateEnum.LOCATION_SELECT_REVIEW, 4000);
		
		LOGGER.info("3d: scan a valid location. Log out the work instructions that we got.");
		picker.scanLocation("D303");
		picker.waitForCheState(CheStateEnum.DO_PICK, 4000);
		
		this.getTenantPersistenceService().beginTransaction();
		List<WorkInstruction> serverWiList = picker.getServerVersionAllPicksList();
		Assert.assertEquals(3, serverWiList.size());
		logWiList(serverWiList);
		this.getTenantPersistenceService().commitTransaction();


		// logout back to idle state.
		picker.logout();
		picker.waitForCheState(CheStateEnum.IDLE, 2000);

	}
	
	/**
	 * Login, scan a valid order detail ID, see the job.
	 * LOCAPICK is false, so no attempt at inventory creation.
	 * SCANPICK is set to SKU.
	 */
	@Test
	public final void testLineScanPick() throws IOException {

		this.getTenantPersistenceService().beginTransaction();
		Facility facility = setUpSmallNoSlotFacility();
		setUpLineScanOrdersNoCntr(facility);
		this.getTenantPersistenceService().commitTransaction();

		this.getTenantPersistenceService().beginTransaction();
		facility = Facility.DAO.reload(facility);
		Assert.assertNotNull(facility);
		
		// we need to set che1 to be in line scan mode
		CodeshelfNetwork network = getNetwork();
		Che che1 = network.getChe("CHE1");
		Assert.assertNotNull(che1);
		Assert.assertEquals(cheGuid1, che1.getDeviceNetGuid()); // just checking since we use cheGuid1 to get the picker.
		che1.setProcessMode(ProcessMode.LINE_SCAN);
		Che.DAO.store(che1);
		
		DomainObjectProperty scanPickProperty = PropertyService.getPropertyObject(facility, DomainObjectProperty.SCANPICK);
		if (scanPickProperty != null) {
			scanPickProperty.setValue("SKU");
			PropertyDao.getInstance().store(scanPickProperty);
		}
		
		this.getTenantPersistenceService().commitTransaction();
		
		// Need to give time for the the CHE update to process through the site controller before settling on our picker.
		PickSimulator picker = waitAndGetPickerForProcessType(this, cheGuid1, "CHE_LINESCAN");
		Assert.assertEquals(CheStateEnum.IDLE, picker.currentCheState());
		
		CsDeviceManager manager = this.getDeviceManager();
		Assert.assertNotNull(manager);
		
		String scanPickValue = manager.getScanTypeValue();
		LOGGER.info("Default SCANPICK value for test is " + scanPickValue);
		Assert.assertNotEquals("SKU", manager.getScanTypeValue());
		// We would rather have the device manager know from the SCANPICK parameter update, but that does not happen yet in the integration test.
		 // kludgy! Somewhat simulates restarting site controller
		manager.setScanTypeValue("SKU");
		Assert.assertEquals("SKU", manager.getScanTypeValue());
		picker.forceDeviceToMatchManagerConfiguration();

		LOGGER.info("1a: login, should go to READY state");
		picker.loginAndCheckState("Picker #1", CheStateEnum.READY);

		LOGGER.info("1b: scan order, should go to SCAN_SOMETHING state");
		picker.scanOrderDetailId("12345.3"); // does not add "%"
		picker.waitForCheState(CheStateEnum.SCAN_SOMETHING, 5000);
		
		LOGGER.info("1c: although the poscon shows the count, prove that the button does nothing");
		WorkInstruction wi = picker.getActivePick();
		int button = picker.buttonFor(wi);
		int quant = wi.getPlanQuantity();
		picker.pick(button, quant);
		picker.waitForCheState(CheStateEnum.SCAN_SOMETHING, 4000);
		WorkInstruction wi2 = picker.nextActiveWi();
		Assert.assertEquals(wi, wi2);
		
		LOGGER.info("1d: scan the SKU. This data has 1522");
		picker.scanSomething("1522");
		picker.waitForCheState(CheStateEnum.DO_PICK, 4000);
		
		LOGGER.info("1e: now the button press works");
		wi = picker.nextActiveWi();
		button = picker.buttonFor(wi);
		quant = wi.getPlanQuantity();
		picker.pick(button, quant);
		picker.waitForCheState(CheStateEnum.READY, 4000);
		
		// logout back to idle state.
		picker.logout();
		picker.waitForCheState(CheStateEnum.IDLE, 2000);
		
		LOGGER.info("2: test shorting before scanning upc");
		LOGGER.info("2a: login, should go to READY state");
		picker.loginAndCheckState("Picker #1", CheStateEnum.READY);
		
		LOGGER.info("2b: scan order, should go to SCAN_SOMETHING state");
		picker.scanOrderDetailId("12345.1"); // does not add "%"
		picker.waitForCheState(CheStateEnum.SCAN_SOMETHING, 5000);
		
		LOGGER.info("2c: scan short command");
		picker.scanCommand("SHORT");
		picker.waitForCheState(CheStateEnum.SCAN_SOMETHING_SHORT, 4000);
		
		LOGGER.info("2d: scan yes and go back to ready state");
		picker.scanCommand("YES");
		picker.waitForCheState(CheStateEnum.READY, 2000);

		LOGGER.info("2e: scan order, should go to SCAN_SOMETHING state");
		picker.scanOrderDetailId("12345.1"); // does not add "%"
		picker.waitForCheState(CheStateEnum.SCAN_SOMETHING, 5000);
		
		LOGGER.info("2f: make sure need quant is still the same");
		wi = picker.getActivePick();
		quant = wi.getPlanQuantity();
		Assert.assertEquals(1,quant);
		
		LOGGER.info("2g: scan SKU, should go to DO_PICK state");
		picker.scanSomething("1123");
		picker.waitForCheState(CheStateEnum.DO_PICK, 4000);
		
		LOGGER.info("2h: press button to confirm pickup");
		wi = picker.nextActiveWi();
		button = picker.buttonFor(wi);
		quant = wi.getPlanQuantity();
		picker.pick(button, quant);
		picker.waitForCheState(CheStateEnum.READY, 4000);
		
		// logout back to idle state.
		picker.logout();
		picker.waitForCheState(CheStateEnum.IDLE, 2000);
		
		LOGGER.info("3: test shorting after scanning upc");
		LOGGER.info("3a: login, should go to READY state");
		picker.loginAndCheckState("Picker #1", CheStateEnum.READY);
		
		LOGGER.info("3b: scan order, should go to SCAN_SOMETHING state");
		picker.scanOrderDetailId("11111.5"); // does not add "%"
		picker.waitForCheState(CheStateEnum.SCAN_SOMETHING, 5000);
		
		LOGGER.info("3c: make sure need quant is correct");
		wi = picker.getActivePick();
		quant = wi.getPlanQuantity();
		Assert.assertEquals(2,quant);
		
		LOGGER.info("3d: scan SKU, should go to DO_PICK state");
		picker.scanSomething("1555");
		picker.waitForCheState(CheStateEnum.DO_PICK, 4000);
		
		LOGGER.info("3e: scan SHORT");
		picker.scanCommand("SHORT");
		picker.waitForCheState(CheStateEnum.SHORT_PICK, 2000);
		
		LOGGER.info("3f: submit 0");
		picker.pick(1, 1);
		picker.waitForCheState(CheStateEnum.SHORT_PICK_CONFIRM, 2000);

		LOGGER.info("3g: scan YES, so we should be back on job");
		picker.scanCommand("YES");
		picker.waitForCheState(CheStateEnum.READY, 2000);

		LOGGER.info("3h: scan order, should go to SCAN_SOMETHING state");
		picker.scanOrderDetailId("11111.5"); // does not add "%"
		picker.waitForCheState(CheStateEnum.SCAN_SOMETHING, 5000);
		
		LOGGER.info("3i: make sure need quant is correct");
		wi = picker.getActivePick();
		quant = wi.getPlanQuantity();
		Assert.assertEquals(1,quant);
		
		LOGGER.info("3j: scan SKU, should go to DO_PICK state");
		picker.scanSomething("1555");
		picker.waitForCheState(CheStateEnum.DO_PICK, 4000);
		
		LOGGER.info("2h: press button to confirm pickup");
		wi = picker.nextActiveWi();
		button = picker.buttonFor(wi);
		quant = wi.getPlanQuantity();
		picker.pick(button, quant);
		picker.waitForCheState(CheStateEnum.READY, 4000);
		
	}
	
	/**
	 * Login, scan a valid order detail ID, see the job.
	 * LOCAPICK is false, so no attempt at inventory creation.
	 * SCANPICK is set to SKU.
	 * Do some bad scans to make sure we handle those correctly
	 */
	@Test
	public final void testLineScanPickBadScans() throws IOException {

		this.getTenantPersistenceService().beginTransaction();
		Facility facility = setUpSmallNoSlotFacility();
		setUpLineScanOrdersNoCntr(facility);
		this.getTenantPersistenceService().commitTransaction();

		this.getTenantPersistenceService().beginTransaction();
		facility = Facility.DAO.reload(facility);
		Assert.assertNotNull(facility);
		
		// we need to set che1 to be in line scan mode
		CodeshelfNetwork network = getNetwork();
		Che che1 = network.getChe("CHE1");
		Assert.assertNotNull(che1);
		Assert.assertEquals(cheGuid1, che1.getDeviceNetGuid()); // just checking since we use cheGuid1 to get the picker.
		che1.setProcessMode(ProcessMode.LINE_SCAN);
		Che.DAO.store(che1);
		
		DomainObjectProperty scanPickProperty = PropertyService.getPropertyObject(facility, DomainObjectProperty.SCANPICK);
		if (scanPickProperty != null) {
			scanPickProperty.setValue("SKU");
			PropertyDao.getInstance().store(scanPickProperty);
		}
		
		this.getTenantPersistenceService().commitTransaction();
		
		// Need to give time for the the CHE update to process through the site controller before settling on our picker.
		PickSimulator picker = waitAndGetPickerForProcessType(this, cheGuid1, "CHE_LINESCAN");
		Assert.assertEquals(CheStateEnum.IDLE, picker.currentCheState());
		
		CsDeviceManager manager = this.getDeviceManager();
		Assert.assertNotNull(manager);
		
		String scanPickValue = manager.getScanTypeValue();
		LOGGER.info("Default SCANPICK value for test is " + scanPickValue);
		Assert.assertNotEquals("SKU", manager.getScanTypeValue());
		// We would rather have the device manager know from the SCANPICK parameter update, but that does not happen yet in the integration test.
		 // kludgy! Somewhat simulates restarting site controller
		manager.setScanTypeValue("SKU");
		Assert.assertEquals("SKU", manager.getScanTypeValue());
		picker.forceDeviceToMatchManagerConfiguration();

		LOGGER.info("1a: login, should go to READY state");
		picker.loginAndCheckState("Picker #1", CheStateEnum.READY);

		LOGGER.info("1b: scan order, should go to SCAN_SOMETHING state");
		picker.scanOrderDetailId("12345.3"); // does not add "%"
		picker.waitForCheState(CheStateEnum.SCAN_SOMETHING, 4000);
		
		LOGGER.info("1c: although the poscon shows the count, prove that the button does nothing");
		WorkInstruction wi = picker.getActivePick();
		int button = picker.buttonFor(wi);
		int quant = wi.getPlanQuantity();
		picker.pick(button, quant);
		picker.waitForCheState(CheStateEnum.SCAN_SOMETHING, 4000);
		WorkInstruction wi2 = picker.nextActiveWi();
		Assert.assertEquals(wi, wi2);
		
		LOGGER.info("1d: scan the wrong SKU. This data has 1522");
		picker.scanSomething("8888");
		picker.waitForCheState(CheStateEnum.SCAN_SOMETHING, 4000);
		
		LOGGER.info("1e: see if we can logout from SCAN_SOMETHING state");
		picker.logout();
		picker.waitForCheState(CheStateEnum.IDLE, 2000);
		
		LOGGER.info("2a: login, should go to READY state");
		picker.loginAndCheckState("Picker #1", CheStateEnum.READY);

		LOGGER.info("2b: scan order, should go to SCAN_SOMETHING state");
		picker.scanOrderDetailId("12345.3"); // does not add "%"
		picker.waitForCheState(CheStateEnum.SCAN_SOMETHING, 5000);
		
		LOGGER.info("2c: scan SKU. This data has 1522");
		picker.scanSomething("1522");
		picker.waitForCheState(CheStateEnum.DO_PICK, 2000);
		
		LOGGER.info("2d: see that normal short process works from this point");
		picker.scanCommand("SHORT");
		picker.waitForCheState(CheStateEnum.SHORT_PICK, 5000);
		picker.pick(button, 0);
		picker.waitForCheState(CheStateEnum.SHORT_PICK_CONFIRM, 5000);
		picker.scanCommand("NO");
		picker.waitForCheState(CheStateEnum.DO_PICK, 5000);
		
		picker.logout();
		picker.waitForCheState(CheStateEnum.IDLE, 2000);
		
		LOGGER.info("3a: login, should go to READY state");
		picker.loginAndCheckState("Picker #1", CheStateEnum.READY);

		LOGGER.info("3b: scan order, should go to SCAN_SOMETHING state");
		picker.scanOrderDetailId("12345.3"); // does not add "%"
		picker.waitForCheState(CheStateEnum.SCAN_SOMETHING, 5000);
		
		LOGGER.info("3c: No product present. Worker's only choice is to scan short");
		picker.scanCommand("SHORT");
		picker.waitForCheState(CheStateEnum.SCAN_SOMETHING_SHORT, 4000); // like SHORT_PICK_CONFIRM

		LOGGER.info("3d: Scan NO on the confirm message");
		picker.scanCommand("NO");
		picker.waitForCheState(CheStateEnum.SCAN_SOMETHING, 4000);
		
		LOGGER.info("3e: Worker decides to complete the short.");
		picker.scanCommand("SHORT");
		picker.waitForCheState(CheStateEnum.SCAN_SOMETHING_SHORT, 4000); // like SHORT_PICK_CONFIRM
		
		LOGGER.info("3f: Scan YES on the confirm message");
		picker.scanCommand("YES");
		picker.waitForCheState(CheStateEnum.READY, 4000);
		picker.logout();
		picker.waitForCheState(CheStateEnum.IDLE, 2000);
	}

}
