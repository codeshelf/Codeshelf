/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2014, Codeshelf, All rights reserved
 *  file IntegrationTest1.java
 *******************************************************************************/
package com.codeshelf.integration;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.device.CheStateEnum;
import com.codeshelf.edi.EdiExportService;
import com.codeshelf.edi.IEdiExportGateway;
import com.codeshelf.edi.IFacilityEdiExporter;
import com.codeshelf.edi.SftpConfiguration;
import com.codeshelf.edi.WorkInstructionCsvBean;
import com.codeshelf.model.WorkInstructionSequencerType;
import com.codeshelf.model.dao.PropertyDao;
import com.codeshelf.model.domain.DomainObjectProperty;
import com.codeshelf.model.domain.ExportMessage;
import com.codeshelf.model.domain.ExportMessage.ExportMessageType;
import com.codeshelf.model.domain.ExtensionPoint;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.OrderHeader;
import com.codeshelf.model.domain.SftpGateway;
import com.codeshelf.model.domain.SftpWiGateway;
import com.codeshelf.model.domain.WorkInstruction;
import com.codeshelf.service.ExtensionPointType;
import com.codeshelf.service.PropertyService;
import com.codeshelf.sim.worker.PickSimulator;
import com.codeshelf.testframework.ServerTest;
import com.codeshelf.util.ThreadUtils;

/**
 * @author jon ranstrom
 *
 */
public class CheProcessTestPickOutboundEdi extends ServerTest {

	private static final Logger	LOGGER	= LoggerFactory.getLogger(CheProcessTestPickOutboundEdi.class);

	public CheProcessTestPickOutboundEdi() {

	}

	/**
	 * This does just enough to trivially call work service apis to add order to cart, remove order, complete one work instruction but not complete
	 * Complete second, completing the order, and removing a partially complete order. As well as an extra trip through setup summary 
	 * to make sure we are not sending repeating messages for what was done before.
	 * 
	 * This is rather weak. Look in the console, and see these lines during cart setup
	 * 2015-09-15T13:10:53,480 [INFO ] Order: 44444 added onto cart:CHE1 [] [] [] [5000] (com.codeshelf.service.WorkService)
	 * 2015-09-15T13:10:53,490 [INFO ] Order: 22222 added onto cart:CHE1 [] [] [] [5000] (com.codeshelf.service.WorkService)
	 * 2015-09-15T13:10:53,499 [INFO ] Order: 11111 added onto cart:CHE1 [] [] [] [5000] (com.codeshelf.service.WorkService)
	 * These show the workService API was called, but in the end, there is no accumulating EDI service, so nothing is sent.
	 *
	 */
	@Test
	public void testEDIAccumulatorCalls() throws IOException {
		Facility facility = setUpSimpleNoSlotFacility();
		beginTransaction();
		facility.reload();
		propertyService.turnOffHK(facility);
		commitTransaction();

		beginTransaction();
		facility.reload();
		// We are going to put everything in A1 and A2 since they are on the same path.
		//Item 5 is out of stock and item 6 is case only.
		String csvInventory = "itemId,locationId,description,quantity,uom,inventoryDate,cmFromLeft\r\n" //
				+ "1,D301,Test Item 1,6,EA,6/25/14 12:00,135\r\n" //
				+ "2,D302,Test Item 2,6,EA,6/25/14 12:00,8\r\n" //
				+ "3,D303,Test Item 3,6,EA,6/25/14 12:00,55\r\n" //
				+ "4,D401,Test Item 4,6,EA,6/25/14 12:00,66\r\n";
		importInventoryData(facility, csvInventory);
		commitTransaction();

		beginTransaction();
		facility.reload();

		String csvOrders = "orderGroupId,shipmentId,customerId,preAssignedContainerId,orderId,itemId,description,quantity,uom,orderDate,dueDate,workSequence"
				+ "\r\n1,USF314,COSTCO,11111,11111,1,Test Item 1,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,11111,11111,1,Test Item 2,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,22222,22222,2,Test Item 2,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,22222,22222,2,Test Item 2,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,44444,44444,5,Test Item 5,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,55555,55555,2,Test Item 2,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0";
		importOrdersData(facility, csvOrders);
		commitTransaction();

		this.startSiteController();
		// Start setting up cart etc
		PickSimulator picker = createPickSim(cheGuid1);
		picker.loginAndSetup("Picker #1");

		LOGGER.info("1a: Set up order 11111 at position 2");
		picker.setupOrderIdAsContainer("11111", "2");
		picker.waitForCheState(CheStateEnum.CONTAINER_SELECT, 3000);

		LOGGER.info("1b: Revise to order 11111 at position 1");
		picker.setupOrderIdAsContainer("11111", "1");
		picker.waitForCheState(CheStateEnum.CONTAINER_SELECT, 3000);

		LOGGER.info("1c: Set up order 22222 at position 2");
		picker.setupOrderIdAsContainer("22222", "2");
		picker.waitForCheState(CheStateEnum.CONTAINER_SELECT, 3000);

		LOGGER.info("1c: Set up order 44444 at position 3");
		picker.setupOrderIdAsContainer("44444", "3");
		picker.waitForCheState(CheStateEnum.CONTAINER_SELECT, 3000);

		LOGGER.info("2a: Start, getting the first pick");
		picker.scanCommand("START");
		picker.waitForCheState(CheStateEnum.SETUP_SUMMARY, 3000);
		picker.scanCommand("START");
		picker.waitForCheState(CheStateEnum.DO_PICK, 3000);

		LOGGER.info("2b: START to return to summary screen ");
		picker.scanCommand("START");
		picker.waitForCheState(CheStateEnum.SETUP_SUMMARY, 3000);

		LOGGER.info("3a: Remove order 44444");
		picker.scanCommand("INFO");
		picker.waitForCheState(CheStateEnum.CONTAINER_SELECT, 3000);
		picker.logCheDisplay();
		Assert.assertTrue(picker.getLastSentPositionControllerDisplayValue((byte) 3) == Byte.valueOf("44"));

		LOGGER.info("3a1: Can you just press the button? No");
		picker.pick(3, 44);
		picker.waitForCheState(CheStateEnum.CONTAINER_SELECT, 3000);
		picker.logCheDisplay();

		LOGGER.info("3a2: Can you scan the button position? No.");
		picker.scanPosition("3");
		picker.waitForCheState(CheStateEnum.CONTAINER_SELECTION_INVALID, 3000);
		picker.logCheDisplay();

		picker.scanCommand("CANCEL");
		picker.waitForCheState(CheStateEnum.CONTAINER_SELECT, 3000);

		LOGGER.info("3a3: Can you scan the orderID at this point? Not for info. Only to try to move it.");
		picker.scanSomething("44444");
		picker.waitForCheState(CheStateEnum.CONTAINER_POSITION, 3000);
		picker.logCheDisplay();

		LOGGER.info("3a3: cancel should work here, but doesn't. Still on CONTAINER_POSITION");
		picker.scanCommand("CANCEL");
		picker.waitForCheState(CheStateEnum.CONTAINER_POSITION, 3000);

		LOGGER.info("3a4: clumsy way to get back");
		picker.scanCommand("START");
		picker.waitForCheState(CheStateEnum.CONTAINER_POSITION_INVALID, 3000);

		picker.scanCommand("CANCEL");
		picker.waitForCheState(CheStateEnum.CONTAINER_SELECT, 3000);

		picker.scanCommand("START");
		picker.waitForCheState(CheStateEnum.SETUP_SUMMARY, 3000);

		LOGGER.info("3b1: remove the order by scanning the order after info");
		picker.scanCommand("INFO");
		picker.waitForCheState(CheStateEnum.CONTAINER_SELECT, 3000);
		picker.logCheDisplay();

		LOGGER.info("3b2: Can you scan the orderID at this point? Not for info. Only to try to move it.");
		picker.scanSomething("44444");
		picker.waitForCheState(CheStateEnum.CONTAINER_POSITION, 3000);
		picker.logCheDisplay();

		LOGGER.info("3b: then go to first pick again. Make sure we do not send twice");
		// picker.scanCommand("START");
		//picker.waitForCheState(CheStateEnum.DO_PICK, 3000);

	}

	/**
	 * Requires to be in a transaction already
	 */
	private void addPfswebExtensions(Facility inFacility) {

		String onCartScript = "def OrderOnCartContent(bean) { def returnStr = " //
				+ "'0073' +'^'" //
				+ "+ 'ORDERSTATUS'.padRight(20) +'^'" //
				+ "+ '0'.padLeft(10,'0') +'^'" //
				+ "+ bean.orderId.padRight(20) +'^'" //
				+ "+ bean.cheId.padRight(7) +'^'" //
				+ "+ bean.customerId.padRight(2) +'^'" //
				+ "+ 'OPEN'.padRight(15);" //
				+ " return returnStr;}";

		String headerScript = "def WorkInstructionExportCreateHeader(bean) { def returnStr = " //
				+ "'0073' +'^'" //
				+ "+ 'ORDERSTATUS'.padRight(20) +'^'" //
				+ "+ '0'.padLeft(10,'0') +'^'" //
				+ "+ bean.orderId.padRight(20) +'^'" //
				+ "+ 'CHE'.padRight(20) +'^'" //
				+ "+ bean.cheId.padRight(20) +'^'" //
				+ "+ 'CLOSED'.padRight(15);" //
				+ " return returnStr;}";

		String trailerScript = "def WorkInstructionExportCreateTrailer(bean) { def returnStr = " //
				+ "'0057' +'^'" //
				+ "+ 'ENDORDER'.padRight(20) +'^'" //
				+ "+ '0'.padLeft(10,'0') +'^'" //
				+ "+ bean.orderId.padRight(20);" //
				+ " return returnStr;}";

		// This matches the short specification. Customer sent a longer specification with timestamps and user names.
		String contentScript = "def WorkInstructionExportContent(bean) { def returnStr = " //
				+ "'0090' +'^'" //
				+ "+ 'PICKMISSIONSTATUS'.padRight(20) +'^'" //
				+ "+ '0'.padLeft(10,'0') +'^'" //
				+ "+ bean.orderId.padRight(20) +'^'" //
				+ "+ bean.locationId.padRight(20) +'^'" //
				+ "+ bean.planQuantity.padLeft(15,'0') +'^'" //
				+ "+ bean.actualQuantity.padLeft(15,'0') +'^'" //
				+ "+ bean.itemId.padRight(25);" //
				+ " return returnStr;}";

		// For PFSWeb (and Dematic carts), the OrderOnCart is approximately the same as the work instruction header, 
		// but this will not be universally true
		ExtensionPoint onCartExt = new ExtensionPoint(inFacility, ExtensionPointType.OrderOnCartContent);
		onCartExt.setScript(onCartScript);
		onCartExt.setActive(true);
		ExtensionPoint.staticGetDao().store(onCartExt);

		ExtensionPoint headerExt = new ExtensionPoint(inFacility, ExtensionPointType.WorkInstructionExportCreateHeader);
		headerExt.setScript(headerScript);
		headerExt.setActive(true);
		ExtensionPoint.staticGetDao().store(headerExt);

		ExtensionPoint trailerExt = new ExtensionPoint(inFacility, ExtensionPointType.WorkInstructionExportCreateTrailer);
		trailerExt.setScript(trailerScript);
		trailerExt.setActive(true);
		ExtensionPoint.staticGetDao().store(trailerExt);

		ExtensionPoint contentExt = new ExtensionPoint(inFacility, ExtensionPointType.WorkInstructionExportContent);
		contentExt.setScript(contentScript);
		contentExt.setActive(true);
		ExtensionPoint.staticGetDao().store(contentExt);

	}

	/**
	 * The intent is to do enough to cause the export beans to populate.
	 * DEV-1127 from PFSWeb go live had location-based pick not populating the from location for the pick correctly.
	 */
	@Ignore
	@Test
	public void testEDIAccumulatorExportBean() throws Exception {

		LOGGER.info("1: Set up facility. Add the export extensions");
		// somewhat cloned from FacilityAccumulatingExportTest
		Facility facility = setUpSimpleNoSlotFacility();

		beginTransaction();
		facility = facility.reload();
		propertyService.turnOffHK(facility);
		DomainObjectProperty theProperty = PropertyService.getInstance().getProperty(facility, DomainObjectProperty.WORKSEQR);
		if (theProperty != null) {
			theProperty.setValue("WorkSequence");
			PropertyDao.getInstance().store(theProperty);
		}

		addPfswebExtensions(facility);

		commitTransaction();

		LOGGER.info("2: Load orders. No inventory, so uses locationA, etc. as the location-based pick");
		beginTransaction();
		facility = facility.reload();

		String csvOrders = "preAssignedContainerId,orderId,itemId,description,quantity,uom,locationId,workSequence"
				+ "\r\n11111,11111,1,Test Item 1,1,each,locationA,1" //
				+ "\r\n22222,22222,2,Test Item 2,1,each,locationB,20" //
				+ "\r\n22222,22222,3,Test Item 3,1,each,locationC,30" //				
				+ "\r\n44444,44444,5,Test Item 5,1,each,locationD,502" //
				+ "\r\n44444,44444,6,Test Item 6,1,each,locationE,501" //
				+ "\r\n44444,44444,7,Test Item 7,1,each,locationF,500" //
				+ "\r\n55555,55555,2,Test Item 2,1,each,locationA,20";
		importOrdersData(facility, csvOrders);
		commitTransaction();

		beginTransaction();
		facility = facility.reload();
		SftpConfiguration config = setupSftpOutConfiguration();
		SftpWiGateway sftpWIs = configureSftpService(facility, config, SftpWiGateway.class);
		sftpWIs.setActive(true);
		Assert.assertTrue(sftpWIs.isLinked());
		commitTransaction();

		this.startSiteController();
		// Start setting up cart etc
		PickSimulator picker = createPickSim(cheGuid1);
		picker.loginAndSetup("Picker #1");

		LOGGER.info("3a: Set up order 11111 at position 1");
		picker.setupOrderIdAsContainer("11111", "1");

		LOGGER.info("3b: Set up order 22222 at position 2");
		picker.setupOrderIdAsContainer("22222", "2");

		LOGGER.info("3c: Set up order 44444 at position 3");
		picker.setupOrderIdAsContainer("44444", "3");

		LOGGER.info("4: Start, getting the first pick");
		picker.scanCommand("START");
		picker.waitForCheState(CheStateEnum.SETUP_SUMMARY, 3000);
		LOGGER.info(picker.getLastCheDisplay());
		picker.scanCommand("START");
		picker.waitForCheState(CheStateEnum.DO_PICK, 3000);
		// Look in console for line like this. No easy way to get it for unit test
		// 0073^ORDERSTATUS         ^0000000000^44444               ^CHE1   ^  ^OPEN            

		List<WorkInstruction> wiList = picker.getAllPicksList();
		Assert.assertEquals(6, wiList.size());

		picker.pickItemAuto();// This should complete order 11111, yielding the message from work service to the edi to send
		// See line like this in the console
		// 0090^PICKMISSIONSTATUS   ^0000000000^11111               ^locationA           ^000000000000001^000000000000001^1                        

		// Just wasting some time to allow the EDI to process through
		picker.logout();
		picker.loginAndSetup("Picker #1");
		picker.logout();
		picker.loginAndSetup("Picker #1");
		picker.logout();

		beginTransaction();
		waitForExporterThreadToEmpty(facility);

		LOGGER.info("5: Verify sent messages");
		List<ExportMessage> messages = ExportMessage.staticGetDao().getAll();
		Assert.assertEquals(4, messages.size());
		for (ExportMessage message : messages) {
			String orderId = message.getOrderId();
			String expectedContents = null;
			if ("11111".equals(orderId) && message.getType() == ExportMessageType.ORDER_ON_CART_FINISHED) {
				expectedContents = "0073^ORDERSTATUS         ^0000000000^11111               ^CHE                 ^CHE1                ^CLOSED         \r\n"
						+ "0090^PICKMISSIONSTATUS   ^0000000000^11111               ^locationA           ^000000000000001^000000000000001^1                        \r\n"
						+ "0057^ENDORDER            ^0000000000^11111";
			} else if ("11111".equals(orderId)) {
				expectedContents = "0073^ORDERSTATUS         ^0000000000^11111               ^CHE1   ^  ^OPEN";
			} else if ("22222".equals(orderId)) {
				expectedContents = "0073^ORDERSTATUS         ^0000000000^22222               ^CHE1   ^  ^OPEN";
			} else if ("44444".equals(orderId)) {
				expectedContents = "0073^ORDERSTATUS         ^0000000000^44444               ^CHE1   ^  ^OPEN";
			} else {
				Assert.fail("Unexpected message: order = " + orderId + ", contents = " + message.getContents());
			}
			Assert.assertEquals(expectedContents, message.getContents().trim());
		}
		commitTransaction();

		LOGGER.info("6: Investigate whether deleting the work intruction also sends out pickMissionStatus");
		beginTransaction();
		facility = facility.reload();
		OrderHeader oh = OrderHeader.staticGetDao().findByDomainId(facility, "11111"); // this one completed
		oh.delete(); // This will cascade to delete the completed work instructions
		OrderHeader oh2 = OrderHeader.staticGetDao().findByDomainId(facility, "22222"); // this one not completed
		oh2.delete(); // This will cascade to delete the uncompleted work instructions
		waitForExporterThreadToEmpty(facility);
		commitTransaction();
		
		LOGGER.info("7: Go through START which will not bring the deleted 22222 back. Then complete the 3 44444 jobs.");
		picker.loginAndSetup("Picker #1");
		LOGGER.info("3c: Set up order 44444 at position 3");
		picker.setupOrderIdAsContainer("44444", "3");
		picker.scanCommand("START");
		picker.waitForCheState(CheStateEnum.SETUP_SUMMARY, 3000);
		picker.scanCommand("START");
		picker.waitForCheState(CheStateEnum.DO_PICK, 3000);
		picker.pickItemAuto();
		picker.waitForCheState(CheStateEnum.DO_PICK, 3000);
		picker.pickItemAuto();
		picker.waitForCheState(CheStateEnum.DO_PICK, 3000);
		picker.pickItemAuto();
		picker.waitForCheState(CheStateEnum.SETUP_SUMMARY, 3000);
		picker.logout();
		// Above is just to see 3 jobs in 1 order to force the beans to sort
		
		// Just waste some time
		picker.loginAndSetup("Picker #1");
		picker.logout();
		picker.loginAndSetup("Picker #1");
		picker.logout();
	}

	/**
	 * This is to replicate the situation seen at PFSWeb
	 */
	@Test
	@Ignore
	//Temporarily ignored to see if it is affecting test runs on TC
	public void testChangeCartMidOrder() throws Exception {

		// Let's see what is taking time. 12 seconds for this test.
		long t0 = System.currentTimeMillis();

		LOGGER.info("1: Set up facility. Add the export extensions");
		// somewhat cloned from FacilityAccumulatingExportTest
		Facility facility = setUpSimpleNoSlotFacility();

		beginTransaction();
		facility = facility.reload();
		propertyService.turnOffHK(facility);
		DomainObjectProperty theProperty = PropertyService.getInstance().getProperty(facility, DomainObjectProperty.WORKSEQR);
		if (theProperty != null) {
			theProperty.setValue("WorkSequence");
			PropertyDao.getInstance().store(theProperty);
		}

		long t1 = System.currentTimeMillis();
		if (t1 - t0 > 1000)
			LOGGER.info("___t1 is {}ms", t1 - t0);

		addPfswebExtensions(facility);
		commitTransaction();

		long t2 = System.currentTimeMillis();
		if (t2 - t1 > 1000)
			LOGGER.info("___t2 is {}ms", t2 - t1);

		LOGGER.info("2: Load orders. No inventory, so uses locationA, etc. as the location-based pick");
		beginTransaction();
		facility = facility.reload();

		String csvOrders = "preAssignedContainerId,orderId,itemId,description,quantity,uom,locationId,workSequence"
				+ "\r\n11111,11111,1,Test Item 1,1,each,locationA,1" //
				+ "\r\n11111,11111,2,Test Item 2,1,each,locationB,20" //
				+ "\r\n11111,11111,3,Test Item 3,1,each,locationC,30" //
				+ "\r\n11111,11111,5,Test Item 5,1,each,locationD,500" //
				+ "\r\n22222,22222,2,Test Item 2,1,each,locationB,20" //
				+ "\r\n22222,22222,3,Test Item 3,1,each,locationC,30" //
				+ "\r\n44444,44444,5,Test Item 5,1,each,locationD,500" //
				+ "\r\n55555,55555,2,Test Item 2,1,each,locationA,20";
		importOrdersData(facility, csvOrders);
		commitTransaction();

		long t3 = System.currentTimeMillis();
		if (t3 - t2 > 1000)
			LOGGER.info("___t3 is {}ms", t3 - t2);

		beginTransaction();
		facility = facility.reload();
		SftpConfiguration config = setupSftpOutConfiguration();
		SftpWiGateway sftpWIs = configureSftpService(facility, config, SftpWiGateway.class);
		sftpWIs.setActive(true);
		Assert.assertTrue(sftpWIs.isLinked());
		commitTransaction();

		long t4 = System.currentTimeMillis();
		if (t4 - t3 > 1000)
			LOGGER.info("___t4 is {}ms", t4 - t3);

		this.startSiteController();
		// Start setting up cart etc
		PickSimulator picker = createPickSim(cheGuid1);
		PickSimulator picker2 = createPickSim(cheGuid2);
		picker.loginAndSetup("Picker #1");

		LOGGER.info("3a: Set up order 11111 at position 1");
		picker.setupOrderIdAsContainer("11111", "1");

		LOGGER.info("3b: Set up order 22222 at position 2");
		picker.setupOrderIdAsContainer("22222", "2");

		LOGGER.info("3c: Set up order 44444 at position 3");
		picker.setupOrderIdAsContainer("44444", "3");

		LOGGER.info("4: Start, getting the first pick");
		picker.scanCommand("START");
		picker.waitForCheState(CheStateEnum.SETUP_SUMMARY, 3000);
		LOGGER.info(picker.getLastCheDisplay());
		picker.scanCommand("START");
		picker.waitForCheState(CheStateEnum.DO_PICK, 3000);
		// Look in console for line like this. No easy way to get it for unit test
		// 0073^ORDERSTATUS         ^0000000000^44444               ^CHE1   ^  ^OPEN            

		// Did not set up order 55555. Therefore, 3 orders and 7 jobs
		List<WorkInstruction> wiList = picker.getAllPicksList();
		logWiList(wiList);
		Assert.assertEquals(7, wiList.size());

		LOGGER.info("5: Do the first 4 picks. That is three for 11111 and one for 11111");
		picker.pickItemAuto();
		picker.pickItemAuto();
		picker.pickItemAuto();
		picker.pickItemAuto();

		LOGGER.info("5b: Verify 3 jobs left. 7 in all picks list but 4 complete");
		wiList = picker.getAllPicksList();
		logWiList(wiList);

		long t5 = System.currentTimeMillis();
		if (t5 - t4 > 1000)
			LOGGER.info("___t5 is {}ms", t5 - t4);

		LOGGER.info("6a: Log out from this CHE");
		picker.logout();

		LOGGER.info("6b: Same person log in to other CHE");
		picker2.loginAndSetup("Picker #1");

		LOGGER.info("6c: Set up orders onto second cart");
		picker2.setupOrderIdAsContainer("11111", "1");
		picker2.setupOrderIdAsContainer("22222", "2");
		// do not set up 44444 on second cart. Mainly so we have only one order-on-cart message to check contents of.

		LOGGER.info("6d: Start, getting the first pick");
		picker2.scanCommand("START");
		picker2.waitForCheState(CheStateEnum.SETUP_SUMMARY, 3000);
		LOGGER.info(picker2.getLastCheDisplay());
		picker2.scanCommand("START");
		picker2.waitForCheState(CheStateEnum.DO_PICK, 3000);
		wiList = picker2.getAllPicksList();
		logWiList(wiList);
		Assert.assertEquals(2, wiList.size());

		LOGGER.info("7a: Complete this first pick, for 22222. That should complete the order. 2 picks, both attributed to CHE2");
		LOGGER.info(picker2.getLastCheDisplay());
		picker2.pickItemAuto();

		LOGGER.info("7: Complete this pick for 11111. That should complete the order. 4 picks, all attributed to CHE2");
		LOGGER.info(picker2.getLastCheDisplay());
		picker2.pickItemAuto();
		picker2.waitForCheState(CheStateEnum.SETUP_SUMMARY, 3000);

		long t6 = System.currentTimeMillis();
		if (t6 - t5 > 1000)
			LOGGER.info("___t6 is {}ms", t6 - t5);		
		/*
		 * See [ERROR] Received processStateSetup when map is not empty. How?
		 * See DEV-1196. waitUntillQueueIsEmpty() takes a while. When site controller logs in to server, the server waits a few seconds, then sends out the CHE init messages. Most units run too fast to see this. But this one does not.
		 * In production, we would not expect to see this error as it should be impossible get carts set up in those few seconds before the init is done.
		 */

		beginTransaction();
		EdiExportService exportProvider = workService.getExportProvider();
		IFacilityEdiExporter exporter = exportProvider.getEdiExporter(facility);
		exporter.waitUntillQueueIsEmpty(20000);

		long t7 = System.currentTimeMillis();
		if (t7 - t6 > 1000)
			LOGGER.info("___t7 is {}ms", t7 - t6);

		LOGGER.info("8: Verify sent messages");
		List<ExportMessage> messages = ExportMessage.staticGetDao().getAll();
		Assert.assertEquals(7, messages.size());
		for (ExportMessage message : messages) {
			String orderId = message.getOrderId();
			String expectedContents = null;
			if ("11111".equals(orderId) && message.getType() == ExportMessageType.ORDER_ON_CART_FINISHED) {
				expectedContents = "0073^ORDERSTATUS         ^0000000000^11111               ^CHE                 ^CHE2                ^CLOSED         \r\n"
						+ "0090^PICKMISSIONSTATUS   ^0000000000^11111               ^locationA           ^000000000000001^000000000000001^1                        \r\n"
						+ "0090^PICKMISSIONSTATUS   ^0000000000^11111               ^locationB           ^000000000000001^000000000000001^2                        \r\n"
						+ "0090^PICKMISSIONSTATUS   ^0000000000^11111               ^locationC           ^000000000000001^000000000000001^3                        \r\n"
						+ "0090^PICKMISSIONSTATUS   ^0000000000^11111               ^locationD           ^000000000000001^000000000000001^5                        \r\n"
						+ "0057^ENDORDER            ^0000000000^11111";
			} else if ("22222".equals(orderId) && message.getType() == ExportMessageType.ORDER_ON_CART_FINISHED) {

				expectedContents = "0073^ORDERSTATUS         ^0000000000^22222               ^CHE                 ^CHE2                ^CLOSED         \r\n"
						+ "0090^PICKMISSIONSTATUS   ^0000000000^22222               ^locationB           ^000000000000001^000000000000001^2                        \r\n"
						+ "0090^PICKMISSIONSTATUS   ^0000000000^22222               ^locationC           ^000000000000001^000000000000001^3                        \r\n"
						+ "0057^ENDORDER            ^0000000000^22222";
			} else if ("44444".equals(orderId)) {
				expectedContents = "0073^ORDERSTATUS         ^0000000000^44444               ^CHE1   ^  ^OPEN";
			} else if ("22222".equals(orderId) && message.getType() == ExportMessageType.ORDER_ON_CART_ADDED) {
				expectedContents = ""; // don't check. There are two ORDER_ON_CART_ADDED
			} else if ("11111".equals(orderId) && message.getType() == ExportMessageType.ORDER_ON_CART_ADDED) {
				expectedContents = ""; // don't check. There are two ORDER_ON_CART_ADDED
			} else {
				Assert.fail("Unexpected message: order = " + orderId + ", contents = " + message.getContents());
			}
			if (!expectedContents.isEmpty())
				Assert.assertEquals(expectedContents, message.getContents().trim());
		}
		waitForExporterThreadToEmpty(facility);
		commitTransaction();

		long t8 = System.currentTimeMillis();
		if (t8 - t7 > 1000)
			LOGGER.info("___t8 is {}ms", t8 - t7);

	}

	/**
	 * Exploring other cart change scenarios
	 */
	@Ignore
	@Test
	public void testShortAndCompleteOtherCart() throws Exception {

		LOGGER.info("1: Set up facility. Add the export extensions");
		// somewhat cloned from FacilityAccumulatingExportTest
		Facility facility = setUpSimpleNoSlotFacility();

		beginTransaction();
		facility = facility.reload();
		propertyService.turnOffHK(facility);
		DomainObjectProperty theProperty = PropertyService.getInstance().getProperty(facility, DomainObjectProperty.WORKSEQR);
		if (theProperty != null) {
			theProperty.setValue("WorkSequence");
			PropertyDao.getInstance().store(theProperty);
		}
		addPfswebExtensions(facility);
		commitTransaction();

		LOGGER.info("2: Load orders. No inventory, so uses locationA, etc. as the location-based pick");
		beginTransaction();
		facility = facility.reload();

		String csvOrders = "preAssignedContainerId,orderId,itemId,description,quantity,uom,locationId,workSequence"
				+ "\r\n11111,11111,1,Test Item 1,1,each,locationA,1" //
				+ "\r\n11111,11111,2,Test Item 2,3,each,locationB,20" //
				+ "\r\n11111,11111,3,Test Item 3,1,each,locationC,30" //
				+ "\r\n11111,11111,5,Test Item 5,1,each,locationD,500" //
				+ "\r\n22222,22222,2,Test Item 2,1,each,locationB,20" //
				+ "\r\n22222,22222,3,Test Item 3,1,each,locationC,30" //
				+ "\r\n44444,44444,5,Test Item 5,1,each,locationD,500" //
				+ "\r\n55555,55555,2,Test Item 2,1,each,locationA,20";
		importOrdersData(facility, csvOrders);
		commitTransaction();

		beginTransaction();
		facility = facility.reload();
		SftpConfiguration config = setupSftpOutConfiguration();
		SftpWiGateway sftpWIs = configureSftpService(facility, config, SftpWiGateway.class);
		sftpWIs.setActive(true);
		Assert.assertTrue(sftpWIs.isLinked());
		commitTransaction();

		this.startSiteController();
		// Start setting up cart etc
		PickSimulator picker = createPickSim(cheGuid1);
		PickSimulator picker2 = createPickSim(cheGuid2);
		picker.loginAndSetup("Picker #1");

		LOGGER.info("3a: Set up order 11111 at position 1");
		picker.setupOrderIdAsContainer("11111", "1");

		LOGGER.info("3b: Set up order 22222 at position 2");
		picker.setupOrderIdAsContainer("22222", "2");

		LOGGER.info("3c: Set up order 44444 at position 3");
		picker.setupOrderIdAsContainer("44444", "3");

		LOGGER.info("4: Start, getting the first pick");
		picker.scanCommand("START");
		picker.waitForCheState(CheStateEnum.SETUP_SUMMARY, 3000);
		LOGGER.info(picker.getLastCheDisplay());
		picker.scanCommand("START");
		picker.waitForCheState(CheStateEnum.DO_PICK, 3000);

		// Did not set up order 55555. Therefore, 3 orders and 7 jobs
		List<WorkInstruction> wiList = picker.getAllPicksList();
		logWiList(wiList);
		Assert.assertEquals(7, wiList.size());

		LOGGER.info("5: Do the first pick, then short the second. This yields incomplete pick of 11111 and autoshort of 22222.");
		picker.pickItemAuto();
		WorkInstruction wi = picker.getActivePick();
		int button = picker.buttonFor(wi);
		int quantity = wi.getPlanQuantity();
		Assert.assertEquals(3, quantity); // just making sure this test case stays stable
		picker.scanCommand("SHORT");
		picker.waitForCheState(CheStateEnum.SHORT_PICK, 3000);
		picker.pick(button, 1);
		picker.waitForCheState(CheStateEnum.SHORT_PICK_CONFIRM, 3000);
		picker.scanCommand("YES");
		picker.waitForCheState(CheStateEnum.DO_PICK, 3000);

		LOGGER.info("6: Scan START and START, resulting in recomputing jobs. Get new jobs for the short");
		picker.scanCommand("START");
		picker.waitForCheState(CheStateEnum.SETUP_SUMMARY, 3000);
		picker.scanCommand("START");
		picker.waitForCheState(CheStateEnum.DO_PICK, 3000);

		wiList = picker.getAllPicksList();
		logWiList(wiList);
		Assert.assertEquals(6, wiList.size()); // we picked the first job above. Comes back completed? No. Not so great cart feedback for this case.

		LOGGER.info("6b: Short 1111 again. This will short ahead 22222 again.");
		wi = picker.getActivePick();
		button = picker.buttonFor(wi);
		quantity = wi.getPlanQuantity();
		Assert.assertEquals(2, quantity); // just making sure this test case stays stable
		picker.scanCommand("SHORT");
		picker.waitForCheState(CheStateEnum.SHORT_PICK, 3000);
		picker.pick(button, 0);
		picker.waitForCheState(CheStateEnum.SHORT_PICK_CONFIRM, 3000);
		picker.scanCommand("YES");
		picker.waitForCheState(CheStateEnum.DO_PICK, 3000);

		LOGGER.info("7: Log out");
		picker.logout();

		LOGGER.info("7b: Same person log in to other CHE");
		picker2.loginAndSetup("Picker #1");

		LOGGER.info("7c: Set up orders onto second cart");
		picker2.setupOrderIdAsContainer("11111", "1");
		picker2.setupOrderIdAsContainer("22222", "2");
		// do not set up 44444 on second cart. Mainly so we have only one order-on-cart message to check contents of.

		LOGGER.info("7d: Start, getting the first pick");
		picker2.scanCommand("START");
		picker2.waitForCheState(CheStateEnum.SETUP_SUMMARY, 3000);
		LOGGER.info(picker2.getLastCheDisplay());
		picker2.scanCommand("START");
		picker2.waitForCheState(CheStateEnum.DO_PICK, 3000);
		wiList = picker2.getAllPicksList();
		logWiList(wiList);
		Assert.assertEquals(5, wiList.size()); // the same 6 jobs other cart had, except no 44444 job.

		LOGGER.info("8a: Complete the pick. Not shorted.");
		LOGGER.info(picker2.getLastCheDisplay());
		picker2.pickItemAuto();

		LOGGER.info("8b: Complete picking. Except short the very last one. All attributed to CHE2");
		LOGGER.info(picker2.getLastCheDisplay());
		picker2.pickItemAuto();
		picker2.pickItemAuto();
		picker2.pickItemAuto(); // this completes order 22222
		ThreadUtils.sleep(300); // let server side logging play out before continuing.

		LOGGER.info("8c: Screen state right before shorting");
		picker2.waitForCheState(CheStateEnum.DO_PICK, 3000);
		LOGGER.info(picker2.getLastCheDisplay());

		wi = picker2.getActivePick();
		button = picker2.buttonFor(wi);
		quantity = wi.getPlanQuantity();
		Assert.assertEquals(1, quantity); // just making sure this test case stays stable
		picker2.scanCommand("SHORT");
		picker2.waitForCheState(CheStateEnum.SHORT_PICK, 3000);
		picker2.pick(button, 0);
		picker2.waitForCheState(CheStateEnum.SHORT_PICK_CONFIRM, 3000);
		picker2.scanCommand("YES");
		picker2.waitForCheState(CheStateEnum.SETUP_SUMMARY, 3000);

		LOGGER.info("8d: Finished shorting the last job");

		beginTransaction();
		EdiExportService exportProvider = workService.getExportProvider();
		IFacilityEdiExporter exporter = exportProvider.getEdiExporter(facility);
		exporter.waitUntillQueueIsEmpty(20000);

		LOGGER.info("9: Verify sent messages");
		// Comoplicated. Show a 1 of 3 for item 2 on 11111, then 0 of 2 for item 2 11111 (the short)
		List<ExportMessage> messages = ExportMessage.staticGetDao().getAll();
		Assert.assertEquals(7, messages.size());
		for (ExportMessage message : messages) {
			String orderId = message.getOrderId();
			String expectedContents = null;
			if ("11111".equals(orderId) && message.getType() == ExportMessageType.ORDER_ON_CART_FINISHED) {
				expectedContents = "0073^ORDERSTATUS         ^0000000000^11111               ^CHE                 ^CHE2                ^CLOSED         \r\n"
						+ "0090^PICKMISSIONSTATUS   ^0000000000^11111               ^locationA           ^000000000000001^000000000000001^1                        \r\n"
						+ "0090^PICKMISSIONSTATUS   ^0000000000^11111               ^locationB           ^000000000000003^000000000000001^2                        \r\n"
						+ "0090^PICKMISSIONSTATUS   ^0000000000^11111               ^locationB           ^000000000000002^000000000000000^2                        \r\n"
						+ "0090^PICKMISSIONSTATUS   ^0000000000^11111               ^locationB           ^000000000000002^000000000000002^2                        \r\n"
						+ "0090^PICKMISSIONSTATUS   ^0000000000^11111               ^locationC           ^000000000000001^000000000000001^3                        \r\n"
						+ "0090^PICKMISSIONSTATUS   ^0000000000^11111               ^locationD           ^000000000000001^000000000000000^5                        \r\n"
						+ "0057^ENDORDER            ^0000000000^11111";
			} else if ("22222".equals(orderId) && message.getType() == ExportMessageType.ORDER_ON_CART_FINISHED) {

				expectedContents = "0073^ORDERSTATUS         ^0000000000^22222               ^CHE                 ^CHE2                ^CLOSED         \r\n"
						+ "0090^PICKMISSIONSTATUS   ^0000000000^22222               ^locationB           ^000000000000001^000000000000000^2                        \r\n"
						+ "0090^PICKMISSIONSTATUS   ^0000000000^22222               ^locationB           ^000000000000001^000000000000000^2                        \r\n"
						+ "0090^PICKMISSIONSTATUS   ^0000000000^22222               ^locationB           ^000000000000001^000000000000001^2                        \r\n"
						+ "0090^PICKMISSIONSTATUS   ^0000000000^22222               ^locationC           ^000000000000001^000000000000001^3                        \r\n"
						+ "0057^ENDORDER            ^0000000000^22222";
			} else if ("44444".equals(orderId)) {
				expectedContents = "0073^ORDERSTATUS         ^0000000000^44444               ^CHE1   ^  ^OPEN";
			} else if ("22222".equals(orderId) && message.getType() == ExportMessageType.ORDER_ON_CART_ADDED) {
				expectedContents = ""; // don't check. There are two ORDER_ON_CART_ADDED
			} else if ("11111".equals(orderId) && message.getType() == ExportMessageType.ORDER_ON_CART_ADDED) {
				expectedContents = ""; // don't check. There are two ORDER_ON_CART_ADDED
			} else {
				Assert.fail("Unexpected message: order = " + orderId + ", contents = " + message.getContents());
			}
			if (!expectedContents.isEmpty())
				Assert.assertEquals(expectedContents, message.getContents().trim());
		}
		waitForExporterThreadToEmpty(facility);
		commitTransaction();
	}

	/**
	 * See what happens if the short ahead finishes the order. Answer, the file is sent.
	 */
	@Test
	@Ignore
	public void testShortAheadFinishesOrder() throws Exception {

		LOGGER.info("1: Set up facility. Add the export extensions");
		// somewhat cloned from FacilityAccumulatingExportTest
		Facility facility = setUpSimpleNoSlotFacility();

		beginTransaction();
		facility = facility.reload();
		propertyService.turnOffHK(facility);
		DomainObjectProperty theProperty = PropertyService.getInstance().getProperty(facility, DomainObjectProperty.WORKSEQR);
		if (theProperty != null) {
			theProperty.setValue("WorkSequence");
			PropertyDao.getInstance().store(theProperty);
		}
		addPfswebExtensions(facility);
		commitTransaction();

		LOGGER.info("2: Load orders. No inventory, so uses locationA, etc. as the location-based pick");
		beginTransaction();
		facility = facility.reload();

		String csvOrders = "preAssignedContainerId,orderId,itemId,description,quantity,uom,locationId,workSequence"
				+ "\r\n11111,11111,1,Test Item 1,1,each,locationA,1" //
				+ "\r\n22222,22222,1,Test Item 1,1,each,locationA,1" //
				+ "\r\n44444,44444,5,Test Item 5,1,each,locationD,500" //
				+ "\r\n55555,55555,2,Test Item 2,1,each,locationA,20";
		importOrdersData(facility, csvOrders);
		commitTransaction();

		beginTransaction();
		facility = facility.reload();
		SftpConfiguration config = setupSftpOutConfiguration();
		SftpWiGateway sftpWIs = configureSftpService(facility, config, SftpWiGateway.class);
		sftpWIs.setActive(true);
		Assert.assertTrue(sftpWIs.isLinked());
		commitTransaction();

		this.startSiteController();
		// Start setting up cart etc
		PickSimulator picker = createPickSim(cheGuid1);
		picker.loginAndSetup("Picker #1");

		LOGGER.info("3a: Set up order 11111 at position 1");
		picker.setupOrderIdAsContainer("11111", "1");

		LOGGER.info("3b: Set up order 22222 at position 2");
		picker.setupOrderIdAsContainer("22222", "2");

		LOGGER.info("3c: Set up order 44444 at position 3");
		picker.setupOrderIdAsContainer("44444", "3");

		LOGGER.info("4: Start, getting the first pick");
		picker.scanCommand("START");
		picker.waitForCheState(CheStateEnum.SETUP_SUMMARY, 3000);
		LOGGER.info(picker.getLastCheDisplay());
		picker.scanCommand("START");
		picker.waitForCheState(CheStateEnum.DO_PICK, 3000);

		// Did not set up order 55555. Therefore, 3 orders and 3 jobs
		List<WorkInstruction> wiList = picker.getAllPicksList();
		logWiList(wiList);
		Assert.assertEquals(3, wiList.size());

		LOGGER.info("5: Short the first pick, which auto-shorts the second. This yields incomplete pick of 11111 and 22222.");
		WorkInstruction wi = picker.getActivePick();
		int button = picker.buttonFor(wi);
		int quantity = wi.getPlanQuantity();
		Assert.assertEquals(1, quantity); // just making sure this test case stays stable
		picker.scanCommand("SHORT");
		picker.waitForCheState(CheStateEnum.SHORT_PICK, 3000);
		picker.pick(button, 0);
		picker.waitForCheState(CheStateEnum.SHORT_PICK_CONFIRM, 3000);
		picker.scanCommand("YES");
		picker.waitForCheState(CheStateEnum.DO_PICK, 3000);

		beginTransaction();
		EdiExportService exportProvider = workService.getExportProvider();
		IFacilityEdiExporter exporter = exportProvider.getEdiExporter(facility);
		exporter.waitUntillQueueIsEmpty(20000);

		LOGGER.info("6: Verify sent messages");
		// Comoplicated. Show a 1 of 3 for item 2 on 11111, then 0 of 2 for item 2 11111 (the short)
		List<ExportMessage> messages = ExportMessage.staticGetDao().getAll();
		Assert.assertEquals(5, messages.size());
		for (ExportMessage message : messages) {
			String orderId = message.getOrderId();
			String expectedContents = null;
			if ("11111".equals(orderId) && message.getType() == ExportMessageType.ORDER_ON_CART_FINISHED) {
				expectedContents = "0073^ORDERSTATUS         ^0000000000^11111               ^CHE                 ^CHE1                ^CLOSED         \r\n"
						+ "0090^PICKMISSIONSTATUS   ^0000000000^11111               ^locationA           ^000000000000001^000000000000000^1                        \r\n"
						+ "0057^ENDORDER            ^0000000000^11111";
			} else if ("22222".equals(orderId) && message.getType() == ExportMessageType.ORDER_ON_CART_FINISHED) {

				expectedContents = "0073^ORDERSTATUS         ^0000000000^22222               ^CHE                 ^CHE1                ^CLOSED         \r\n"
						+ "0090^PICKMISSIONSTATUS   ^0000000000^22222               ^locationA           ^000000000000001^000000000000000^1                        \r\n"
						+ "0057^ENDORDER            ^0000000000^22222";
			} else if ("44444".equals(orderId)) {
				expectedContents = "0073^ORDERSTATUS         ^0000000000^44444               ^CHE1   ^  ^OPEN";
			} else if ("22222".equals(orderId) && message.getType() == ExportMessageType.ORDER_ON_CART_ADDED) {
				expectedContents = ""; // don't check. There are two ORDER_ON_CART_ADDED
			} else if ("11111".equals(orderId) && message.getType() == ExportMessageType.ORDER_ON_CART_ADDED) {
				expectedContents = ""; // don't check. There are two ORDER_ON_CART_ADDED
			} else {
				Assert.fail("Unexpected message: order = " + orderId + ", contents = " + message.getContents());
			}
			if (!expectedContents.isEmpty())
				Assert.assertEquals(expectedContents, message.getContents().trim());
		}
		waitForExporterThreadToEmpty(facility);
		commitTransaction();
	}

	@Test
	@Ignore
	public void testEDIActiveFlag() throws Exception {
		Facility facility = setUpSimpleNoSlotFacility();

		beginTransaction();
		facility = facility.reload();
		LOGGER.info("1: Set up OrderOnCartContent extension, import orders, set up active sftp exporter");
		propertyService.changePropertyValue(facility,
			DomainObjectProperty.WORKSEQR,
			WorkInstructionSequencerType.WorkSequence.toString());

		String onCartScript = "def OrderOnCartContent(bean) { def returnStr = " //
				+ "'0073' +'^'" //
				+ "+ 'ORDERSTATUS'.padRight(20) +'^'" //
				+ "+ '0'.padLeft(10,'0') +'^'" //
				+ "+ bean.orderId.padRight(20) +'^'" //
				+ "+ bean.cheId.padRight(7) +'^'" //
				+ "+ bean.customerId.padRight(2) +'^'" //
				+ "+ 'OPEN'.padRight(15);" //
				+ " return returnStr;}";
		ExtensionPoint onCartExt = new ExtensionPoint(facility, ExtensionPointType.OrderOnCartContent);
		onCartExt.setScript(onCartScript);
		onCartExt.setActive(true);
		ExtensionPoint.staticGetDao().store(onCartExt);

		String csvOrders = "preAssignedContainerId,orderId,itemId,description,quantity,uom,locationId,workSequence"
				+ "\r\n11111,11111,1,Test Item 1,1,each,loc1,0" //
				+ "\r\n22222,22222,2,Test Item 2,1,each,loc2,0" //
				+ "\r\n33333,33333,3,Test Item 3,1,each,loc3,0";
		importOrdersData(facility, csvOrders);

		SftpConfiguration config = setupSftpOutConfiguration();
		// config.setActive(true);
		//configureSftpService(facility, config, SftpWiGateway.class);
		
		SftpWiGateway sftpWIs = configureSftpService(facility, config, SftpWiGateway.class);
		sftpWIs.setActive(true);
		Assert.assertTrue(sftpWIs.isLinked());

		commitTransaction();

		LOGGER.info("2: Load first order on cart");
		startSiteController();
		PickSimulator picker = createPickSim(cheGuid1);
		picker.loginAndSetup("Picker #1");
		picker.setupOrderIdAsContainer("11111", "1");
		picker.scanCommand("START");
		picker.waitForCheState(CheStateEnum.SETUP_SUMMARY, 4000);
		Assert.assertEquals("1 job", picker.getLastCheDisplayString(2).trim());

		beginTransaction();
		LOGGER.info("3: Confirm that a correct message had been created and sent");
		facility = facility.reload();
		EdiExportService exportProvider = workService.getExportProvider();
		IFacilityEdiExporter exporter = exportProvider.getEdiExporter(facility);
		exporter.waitUntillQueueIsEmpty(20000);
		List<ExportMessage> messages = ExportMessage.staticGetDao().getAll();
		Assert.assertEquals(1, messages.size());
		ExportMessage message = messages.get(0);
		Assert.assertEquals("0073^ORDERSTATUS         ^0000000000^11111               ^CHE1   ^  ^OPEN", message.getContents()
			.trim());

		LOGGER.info("4: Inactivate sftp exporter");
		IEdiExportGateway exportGateway = facility.getEdiExportGateway();
		exportGateway.setActive(false);
		exportProvider.updateEdiExporter(facility);
		commitTransaction();

		LOGGER.info("5: Load second order on cart");
		picker.scanCommand("SETUP");
		picker.waitForCheState(CheStateEnum.CONTAINER_SELECT, 4000);
		picker.setupOrderIdAsContainer("22222", "1");
		picker.scanCommand("START");
		picker.waitForCheState(CheStateEnum.SETUP_SUMMARY, 4000);
		Assert.assertEquals("1 job", picker.getLastCheDisplayString(2).trim());

		LOGGER.info("6: Assert that Exporter service is disabled, and that no additional messages were created");
		beginTransaction();
		facility = facility.reload();
		exporter = exportProvider.getEdiExporter(facility);
		Assert.assertNull(exporter);
		messages = ExportMessage.staticGetDao().getAll();
		Assert.assertEquals(1, messages.size());

		LOGGER.info("7: Activate sftp exporter");
		exportGateway = facility.getEdiExportGateway();
		exportGateway.setActive(true);
		exportProvider.updateEdiExporterSafe(facility);
		commitTransaction();

		LOGGER.info("8: Load third order on cart");
		picker.scanCommand("SETUP");
		picker.waitForCheState(CheStateEnum.CONTAINER_SELECT, 4000);
		picker.setupOrderIdAsContainer("33333", "1");
		picker.scanCommand("START");
		picker.waitForCheState(CheStateEnum.SETUP_SUMMARY, 4000);
		Assert.assertEquals("1 job", picker.getLastCheDisplayString(2).trim());

		LOGGER.info("9: Assert than a single new message was created");
		beginTransaction();
		facility = facility.reload();
		exporter = exportProvider.getEdiExporter(facility);
		exporter.waitUntillQueueIsEmpty(20000);
		messages = ExportMessage.staticGetDao().getAll();
		Assert.assertEquals(2, messages.size());
		waitForExporterThreadToEmpty(facility);
		commitTransaction();
	}

	@Test
	public void testWIBeanPersistence() throws Exception {
		Facility facility = setUpSimpleNoSlotFacility();

		beginTransaction();

		LOGGER.info("1: Load Order, set up active sftp exporter");
		facility = facility.reload();
		propertyService.changePropertyValue(facility,
			DomainObjectProperty.WORKSEQR,
			WorkInstructionSequencerType.WorkSequence.toString());

		String csvOrders = "preAssignedContainerId,orderId,itemId,description,quantity,uom,locationId,workSequence"
				+ "\r\n11111,11111,1,Test Item 1,1,each,loc1,0" //
				+ "\r\n11111,11111,2,Test Item 2,1,each,loc2,0" //
				+ "\r\n11111,11111,3,Test Item 3,1,each,loc3,0";
		importOrdersData(facility, csvOrders);

		SftpConfiguration config = setupSftpOutConfiguration();
		SftpWiGateway sftpWIs = configureSftpService(facility, config, SftpWiGateway.class);
		sftpWIs.setActive(true);

		commitTransaction();

		LOGGER.info("2: Pick all 3 items in the order");
		startSiteController();
		PickSimulator picker = createPickSim(cheGuid1);
		picker.loginAndSetup("Picker #1");
		picker.setupOrderIdAsContainer("11111", "1");
		picker.scanCommand("START");
		picker.waitForCheState(CheStateEnum.SETUP_SUMMARY, 4000);
		Assert.assertEquals("3 jobs", picker.getLastCheDisplayString(2).trim());
		picker.scanCommand("START");
		picker.waitForCheState(CheStateEnum.DO_PICK, 4000);
		picker.pickItemAuto(); //Pick 1
		picker.pickItemAuto(); //Housekeeping
		picker.pickItemAuto(); //Pick 2
		picker.pickItemAuto(); //Housekeeping
		picker.pickItemAuto(); //Pick 3
		picker.waitForCheState(CheStateEnum.SETUP_SUMMARY, 400000);

		LOGGER.info("3: Assert that exported WI beans have been correctly saved and updated in the wi_bean table");
		ThreadUtils.sleep(2000); //Give server time to save and send all 3 WIs
		beginTransaction();
		List<WorkInstructionCsvBean> savedWIBeans = WorkInstructionCsvBean.staticGetDao().getAll();
		Assert.assertEquals(3, savedWIBeans.size());
		HashSet<String> expectedItems = new HashSet<>();
		expectedItems.add("1");
		expectedItems.add("2");
		expectedItems.add("3");
		for (WorkInstructionCsvBean savedWIBean : savedWIBeans) {
			Assert.assertFalse(savedWIBean.getActive());
			Assert.assertEquals("11111", savedWIBean.getOrderId());
			Assert.assertNotNull(expectedItems.remove(savedWIBean.getItemId()));
		}
		Assert.assertTrue(expectedItems.isEmpty());
		facility = facility.reload();
		waitForExporterThreadToEmpty(facility);
		commitTransaction();

	}

	//++++++++++  SFTP configuration +++++++++++

	// our private sftp test place. Note: we need to maintain this SFTP endpoint for our testing
	private static final String	SFTP_TEST_HOST		= "sftp.codeshelf.com";
	private static final String	SFTP_TEST_USERNAME	= "test";
	private static final String	SFTP_TEST_PASSWORD	= "m80isrq411";

	private SftpConfiguration setupSftpOutConfiguration() {
		SftpConfiguration config = new SftpConfiguration();
		config.setHost(SFTP_TEST_HOST);
		config.setUsername(SFTP_TEST_USERNAME);
		config.setPassword(SFTP_TEST_PASSWORD);
		config.setExportPath("/automated_tests/in");
		config.setImportPath("/automated_tests/out");
		config.setArchivePath("/automated_tests/out/archive");
		return config;
	}

	@SuppressWarnings("unchecked")
	private <T extends SftpGateway> T configureSftpService(Facility facility, SftpConfiguration config, Class<T> class1) {
		// ensure loads/saves configuration correctly
		SftpGateway sftpOrders = facility.findEdiGateway(class1);
		sftpOrders.setConfiguration(config);
		sftpOrders.getDao().store(sftpOrders);
		sftpOrders = (SftpGateway) sftpOrders.getDao().findByDomainId(facility, sftpOrders.getDomainId());

		Assert.assertNotNull(sftpOrders);
		config = sftpOrders.getConfiguration();
		return (T) sftpOrders;
	}

	//++++++++++   end SFTP configuration +++++++++++

	private void waitForExporterThreadToEmpty(Facility facility) throws Exception {
		EdiExportService exportProvider = workService.getExportProvider();
		IFacilityEdiExporter exporter = exportProvider.getEdiExporter(facility);
		exporter.waitUntillQueueIsEmpty(20000);
	}

}