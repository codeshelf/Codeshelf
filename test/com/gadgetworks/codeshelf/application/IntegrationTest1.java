/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2014, Codeshelf, All rights reserved
 *  file IntegrationTest1.java
 *******************************************************************************/
package com.gadgetworks.codeshelf.application;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.gadgetworks.codeshelf.edi.AislesFileCsvImporter;
import com.gadgetworks.codeshelf.edi.EdiTestABC;
import com.gadgetworks.codeshelf.edi.ICsvInventoryImporter;
import com.gadgetworks.codeshelf.edi.ICsvLocationAliasImporter;
import com.gadgetworks.codeshelf.edi.ICsvOrderImporter;
import com.gadgetworks.codeshelf.edi.InventoryCsvImporter;
import com.gadgetworks.codeshelf.edi.LocationAliasCsvImporter;
import com.gadgetworks.codeshelf.edi.OutboundOrderCsvImporter;
import com.gadgetworks.codeshelf.model.WiSetSummary;
import com.gadgetworks.codeshelf.model.domain.Aisle;
import com.gadgetworks.codeshelf.model.domain.Che;
import com.gadgetworks.codeshelf.model.domain.CodeshelfNetwork;
import com.gadgetworks.codeshelf.model.domain.Facility;
import com.gadgetworks.codeshelf.model.domain.Item;
import com.gadgetworks.codeshelf.model.domain.LedController;
import com.gadgetworks.codeshelf.model.domain.LocationABC;
import com.gadgetworks.codeshelf.model.domain.OrderDetail;
import com.gadgetworks.codeshelf.model.domain.OrderHeader;
import com.gadgetworks.codeshelf.model.domain.Organization;
import com.gadgetworks.codeshelf.model.domain.Path;
import com.gadgetworks.codeshelf.model.domain.PathSegment;
import com.gadgetworks.codeshelf.model.domain.Point;
import com.gadgetworks.codeshelf.model.domain.SubLocationABC;
import com.gadgetworks.codeshelf.model.domain.WorkInstruction;
import com.gadgetworks.codeshelf.service.WorkService;
import com.gadgetworks.codeshelf.ws.jetty.server.JettyWebSocketServer;
import com.gadgetworks.flyweight.command.NetGuid;
import com.google.common.base.Strings;

/**
 * @author jon ranstrom
 *
 */
public class IntegrationTest1 extends EdiTestABC {

	JettyWebSocketServer webSocketServer;
	
	static {
		Configuration.loadConfig("test");
	}
	
	public IntegrationTest1() {
	}
	
	@Override
	public void doBefore() throws Exception {
		webSocketServer = new JettyWebSocketServer();
		webSocketServer.start();
		
		
	}
	
	@Override
	public void doAfter() {
		try {
			webSocketServer.stop();
		} catch (IOException e) {
			throw new RuntimeException("could not stop websocketserver", e);
		} catch (InterruptedException e) {
			throw new RuntimeException("could not stop websocketserver", e);
		}
		
		
	}
	
	
	@SuppressWarnings({ "unused", "rawtypes" })
	private Facility setUpSimpleNoSlotFacility(String inOrganizationName) {
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

		Organization organization = new Organization();
		String oName = "O-" + inOrganizationName;
		organization.setDomainId(oName);
		mOrganizationDao.store(organization);

		String fName = "F-" + inOrganizationName;
		organization.createFacility(fName, "TEST", Point.getZeroPoint());
		Facility facility = organization.getFacility(fName);

		String csvString = "binType,nominalDomainId,lengthCm,slotsInTier,ledCountInTier,tierFloorCm,controllerLED,anchorX,anchorY,orientXorY,depthCm\r\n" //
				+ "Aisle,A1,,,,,tierB1S1Side,12.85,43.45,X,120,Y\r\n" //
				+ "Bay,B1,230,,,,,\r\n" //
				+ "Tier,T1,,0,80,0,,\r\n" //
				+ "Bay,B2,230,,,,,\r\n" //
				+ "Tier,T1,,0,80,0,,\r\n" //
				+ "Bay,B3,230,,,,,\r\n" //
				+ "Tier,T1,,4,80,0,,\r\n" //
				+ "Aisle,A2,,,,,tierNotB1S1Side,12.85,55.45,X,120,Y\r\n" //
				+ "Bay,B1,230,,,,,\r\n" //
				+ "Tier,T1,,0,80,0,,\r\n"//
				+ "Bay,B2,230,,,,,\r\n" //
				+ "Tier,T1,,0,80,0,,\r\n" //
				+ "Aisle,A3,,,,,tierNotB1S1Side,12.85,65.45,X,120,Y\r\n" //
				+ "Bay,B1,230,,,,,\r\n" //
				+ "Tier,T1,,0,80,0,,\r\n"//
				+ "Bay,B2,230,,,,,\r\n" //
				+ "Tier,T1,,0,80,0,,\r\n"; //

		byte[] csvArray = csvString.getBytes();

		ByteArrayInputStream stream = new ByteArrayInputStream(csvArray);
		InputStreamReader reader = new InputStreamReader(stream);

		Timestamp ediProcessTime = new Timestamp(System.currentTimeMillis());
		AislesFileCsvImporter importer = new AislesFileCsvImporter(mAisleDao, mBayDao, mTierDao, mSlotDao);
		importer.importAislesFileFromCsvStream(reader, facility, ediProcessTime);

		// Get the aisle
		Aisle aisle1 = Aisle.DAO.findByDomainId(facility, "A1");
		Assert.assertNotNull(aisle1);

		Path aPath = createPathForTest("F5X.1", facility);
		PathSegment segment0 = addPathSegmentForTest("F5X.1.0", aPath, 0, 22.0, 48.45, 12.85, 48.45);

		String persistStr = segment0.getPersistentId().toString();
		aisle1.associatePathSegment(persistStr);

		Aisle aisle2 = Aisle.DAO.findByDomainId(facility, "A2");
		Assert.assertNotNull(aisle2);
		aisle2.associatePathSegment(persistStr);

		Path path2 = createPathForTest("F5X.3", facility);
		PathSegment segment02 = addPathSegmentForTest("F5X.3.0", path2, 0, 22.0, 58.45, 12.85, 58.45);

		Aisle aisle3 = Aisle.DAO.findByDomainId(facility, "A3");
		Assert.assertNotNull(aisle3);
		String persistStr2 = segment02.getPersistentId().toString();
		aisle3.associatePathSegment(persistStr2);

		String csvString2 = "mappedLocationId,locationAlias\r\n" //
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

		byte[] csvArray2 = csvString2.getBytes();

		ByteArrayInputStream stream2 = new ByteArrayInputStream(csvArray2);
		InputStreamReader reader2 = new InputStreamReader(stream2);

		Timestamp ediProcessTime2 = new Timestamp(System.currentTimeMillis());
		ICsvLocationAliasImporter importer2 = new LocationAliasCsvImporter(mLocationAliasDao);
		importer2.importLocationAliasesFromCsvStream(reader2, facility, ediProcessTime2);

		String nName = "N-" + inOrganizationName;
		CodeshelfNetwork network = facility.createNetwork(nName);
		Che che1 = network.createChe("CHE1", new NetGuid("0x00000001"));
		Che che2 = network.createChe("CHE2", new NetGuid("0x00000002"));

		LedController controller1 = network.findOrCreateLedController(inOrganizationName, new NetGuid("0x00000011"));
		LedController controller2 = network.findOrCreateLedController(inOrganizationName, new NetGuid("0x00000012"));
		LedController controller3 = network.findOrCreateLedController(inOrganizationName, new NetGuid("0x00000013"));
		SubLocationABC tier = (SubLocationABC) facility.findSubLocationById("A1.B1.T1");
		tier.setLedController(controller1);
		tier = (SubLocationABC) facility.findSubLocationById("A1.B2.T1");
		tier.setLedController(controller1);
		tier = (SubLocationABC) facility.findSubLocationById("A2.B1.T1");
		tier.setLedController(controller2);
		tier = (SubLocationABC) facility.findSubLocationById("A2.B2.T1");
		tier.setLedController(controller2);
		tier = (SubLocationABC) facility.findSubLocationById("A3.B1.T1");
		tier.setLedController(controller3);
		tier = (SubLocationABC) facility.findSubLocationById("A3.B2.T1");
		tier.setLedController(controller3);

		return facility;

	}

	@SuppressWarnings({ "unused", "rawtypes" })
	@Test
	public final void testPick() throws IOException {

		Facility facility = setUpSimpleNoSlotFacility("IT01");

		// We are going to put cases in A3 and each in A2. Also showing variation in EA/each, etc.
		// 402 and 403 are in A2, the each aisle. 502 and 503 are in A3, the case aisle, on a separate path.
		String csvString = "itemId,locationId,description,quantity,uom,inventoryDate,cmFromLeft\r\n" //
				+ "1123,D402,12/16 oz Bowl Lids -PLA Compostable,6,EA,6/25/14 12:00,135\r\n" //
				+ "1123,D502,12/16 oz Bowl Lids -PLA Compostable,6,CS,6/25/14 12:00,8\r\n" //
				+ "1123,D503,12/16 oz Bowl Lids -PLA Compostable,6,CS,6/25/14 12:00,55\r\n" //
				+ "1493,D502,PARK RANGER Doll,2,case,6/25/14 12:00,66\r\n" //
				+ "1522,D503,SJJ BPP,1,Case,6/25/14 12:00,3\r\n" //
				+ "1522,D403,SJJ BPP,10,each,6/25/14 12:00,3\r\n";//

		byte[] csvArray = csvString.getBytes();

		ByteArrayInputStream stream = new ByteArrayInputStream(csvArray);
		InputStreamReader reader = new InputStreamReader(stream);

		Timestamp ediProcessTime = new Timestamp(System.currentTimeMillis());
		ICsvInventoryImporter importer = new InventoryCsvImporter(mItemMasterDao, mItemDao, mUomMasterDao);
		importer.importSlottedInventoryFromCsvStream(reader, facility, ediProcessTime);

		LocationABC locationD403 = (LocationABC) facility.findSubLocationById("D403");
		LocationABC locationD402 = (LocationABC) facility.findSubLocationById("D402");
		LocationABC locationD502 = (LocationABC) facility.findSubLocationById("D502");
		LocationABC locationD503 = (LocationABC) facility.findSubLocationById("D503");

		Item item1123Loc402EA = locationD402.getStoredItemFromMasterIdAndUom("1123", "EA");
		Assert.assertNotNull(item1123Loc402EA);

		// Outbound order. No group. Using 5 digit order number and preassigned container number.
		// Item 1123 exists in case and each.
		// Item 1493 exists in case only. Order for each should short.
		// Item 1522 exists in case and each.

		String csvString2 = "orderGroupId,shipmentId,customerId,preAssignedContainerId,orderId,itemId,description,quantity,uom,orderDate,dueDate,workSequence"
				+ "\r\n1,USF314,COSTCO,12345,12345,1123,12/16 oz Bowl Lids -PLA Compostable,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,12345,12345,1493,PARK RANGER Doll,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,12345,12345,1522,SJJ BPP,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0";

		byte[] csvArray2 = csvString2.getBytes();

		ByteArrayInputStream stream2 = new ByteArrayInputStream(csvArray2);
		InputStreamReader reader2 = new InputStreamReader(stream2);

		Timestamp ediProcessTime2 = new Timestamp(System.currentTimeMillis());
		ICsvOrderImporter importer2 = new OutboundOrderCsvImporter(mOrderGroupDao,
			mOrderHeaderDao,
			mOrderDetailDao,
			mContainerDao,
			mContainerUseDao,
			mItemMasterDao,
			mUomMasterDao);
		importer2.importOrdersFromCsvStream(reader2, facility, ediProcessTime2);

		// We should have one order with 3 details. Only 2 of which are fulfillable.
		OrderHeader order = facility.getOrderHeader("12345");
		Assert.assertNotNull(order);
		Integer detailCount = order.getOrderDetails().size();
		Assert.assertEquals((Integer) 3, detailCount);

		List<String> itemLocations = new ArrayList<String>();
		for (OrderDetail detail : order.getOrderDetails()) {
			String itemLocationString = detail.getItemLocations();
			if (!Strings.isNullOrEmpty(itemLocationString)) {
				itemLocations.add(itemLocationString);
			}
		}
		Assert.assertEquals(2, itemLocations.size());

		// Let's find our CHE
		CodeshelfNetwork theNetwork = facility.getNetworks().get(0);
		Assert.assertNotNull(theNetwork);
		Che theChe = theNetwork.getChe("CHE1");
		Assert.assertNotNull(theChe);

		// Set up a cart for order 12345, which will generate work instructions
		facility.setUpCheContainerFromString(theChe, "12345");

		List<WorkInstruction> aList = theChe.getCheWorkInstructions();
		Integer wiCount = aList.size();
		Assert.assertEquals((Integer) 3, wiCount); // 3, but one should be short. Only 1123 and 1522 find each inventory

		List<WorkInstruction> wiListAfterScan = facility.getWorkInstructions(theChe, "D402");
		Integer wiCountAfterScan = wiListAfterScan.size();
		Double posOf402 = locationD402.getPosAlongPath();
		Double posOf403 = locationD403.getPosAlongPath();
		Assert.assertTrue(posOf402 > posOf403);
		
		Assert.assertEquals((Integer) 1, wiCountAfterScan); // only the one each item in 402 should be there. The item in 403 is earlier on the path.
		// See which work instruction is which
		WorkInstruction wi1 = wiListAfterScan.get(0);
		Assert.assertNotNull(wi1);
		String wi1Item = wi1.getItemMasterId();
		Double wi1Pos = wi1.getPosAlongPath();

		// New from v4. Test our work instruction summarizer
		List<WiSetSummary> summaries = new WorkService().workSummary(theChe.getPersistentId().toString(),
			facility.getPersistentId().toString());

		// as this test, this facility only set up this one che, there should be only one wi set. But we have 3. How?
		Assert.assertEquals(1, summaries.size());

		// getAny should get the one. Call it somewhat as the UI would. Get a time, then query again with that time.
		WiSetSummary theSummary = summaries.get(0);
		// So, how many shorts, how many active? None complete yet.
		int actives = theSummary.getActiveCount();
		int shorts = theSummary.getShortCount();
		int completes = theSummary.getCompleteCount();
		Assert.assertEquals(0, completes);
		Assert.assertEquals(2, actives);
		Assert.assertEquals(1, shorts);

	}

}
