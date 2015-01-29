/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2014, Codeshelf, All rights reserved
 *  file IntegrationTest1.java
 *******************************************************************************/
package com.gadgetworks.codeshelf.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gadgetworks.codeshelf.application.Configuration;
import com.gadgetworks.codeshelf.device.CheStateEnum;
import com.gadgetworks.codeshelf.device.LedCmdGroup;
import com.gadgetworks.codeshelf.device.LedCmdGroupSerializer;
import com.gadgetworks.codeshelf.device.PosControllerInstr;
import com.gadgetworks.codeshelf.edi.AislesFileCsvImporter;
import com.gadgetworks.codeshelf.edi.ICsvCrossBatchImporter;
import com.gadgetworks.codeshelf.edi.ICsvInventoryImporter;
import com.gadgetworks.codeshelf.edi.ICsvLocationAliasImporter;
import com.gadgetworks.codeshelf.edi.ICsvOrderImporter;
import com.gadgetworks.codeshelf.edi.ICsvOrderLocationImporter;
import com.gadgetworks.codeshelf.model.OrderStatusEnum;
import com.gadgetworks.codeshelf.model.WiSetSummary;
import com.gadgetworks.codeshelf.model.WorkInstructionTypeEnum;
import com.gadgetworks.codeshelf.model.domain.Aisle;
import com.gadgetworks.codeshelf.model.domain.Che;
import com.gadgetworks.codeshelf.model.domain.CodeshelfNetwork;
import com.gadgetworks.codeshelf.model.domain.Container;
import com.gadgetworks.codeshelf.model.domain.Facility;
import com.gadgetworks.codeshelf.model.domain.Item;
import com.gadgetworks.codeshelf.model.domain.LedController;
import com.gadgetworks.codeshelf.model.domain.Location;
import com.gadgetworks.codeshelf.model.domain.OrderDetail;
import com.gadgetworks.codeshelf.model.domain.OrderHeader;
import com.gadgetworks.codeshelf.model.domain.Organization;
import com.gadgetworks.codeshelf.model.domain.Path;
import com.gadgetworks.codeshelf.model.domain.PathSegment;
import com.gadgetworks.codeshelf.model.domain.WorkInstruction;
import com.gadgetworks.codeshelf.service.WorkService;
import com.gadgetworks.flyweight.command.ColorEnum;
import com.gadgetworks.flyweight.command.NetGuid;
import com.google.common.base.Strings;

/**
 * @author jon ranstrom
 *
 */
public class CheProcessTestPick extends EndToEndIntegrationTest {

	private static final Logger	LOGGER	= LoggerFactory.getLogger(CheProcessTestPick.class);

	static {
		Configuration.loadConfig("test");
	}

	public CheProcessTestPick() {

	}

	private Facility setUpSimpleNoSlotFacility() {
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

	private Facility setUpZigzagSlottedFacility() {
		// This returns a facility with aisle A1 and A2, with path between, with two bays with several tiers each.
		// This is the zigzag/cross-batch portion of the MAT as of v10

		String csvString = "binType,nominalDomainId,lengthCm,slotsInTier,ledCountInTier,tierFloorCm,controllerLED,anchorX,anchorY,orientXorY,depthCm\r\n" //
				+ "Aisle,A1,,,,,zigzagB1S1Side,12.85,43.45,X,120\r\n" //
				+ "Bay,B1,116,,,,,\r\n" //
				+ "Tier,T1,,5,32,0,\r\n" //
				+ "Tier,T2,,5,32,20,\r\n" //
				+ "Tier,T3,,5,32,40,\r\n" //
				+ "Tier,T4,,5,32,60,\r\n" //
				+ "Tier,T5,,5,32,80,\r\n" //
				+ "Bay,B2,230,,,,,\r\n" //
				+ "Tier,T1,,5,32,0,\r\n" //
				+ "Tier,T2,,5,32,20,\r\n" //
				+ "Tier,T3,,5,32,40,\r\n" //
				+ "Tier,T4,,5,32,60,\r\n" //
				+ "Tier,T5,,5,32,80,\r\n" //
				+ "Aisle,A2,,,,,zigzagB1S1Side,12.85,55.45,X,120\r\n" //
				+ "Bay,B1,116,,,,,\r\n" //
				+ "Tier,T1,,5,32,0,\r\n" //
				+ "Tier,T2,,5,32,20,\r\n" //
				+ "Tier,T3,,5,32,40,\r\n" //
				+ "Tier,T4,,5,32,60,\r\n" //
				+ "Tier,T5,,5,32,80,\r\n" //
				+ "Bay,B2,230,,,,,\r\n" //
				+ "Tier,T1,,5,32,0,\r\n" //
				+ "Tier,T2,,5,32,20,\r\n" //
				+ "Tier,T3,,5,32,40,\r\n" //
				+ "Tier,T4,,5,32,60,\r\n" //
				+ "Tier,T5,,5,32,80,\r\n";//

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

		String csvString2 = "mappedLocationId,locationAlias\r\n" //
				+ "A1.B1.T1.S1,D-96\r\n" + "A1.B1.T1.S2,D-97\r\n" + "A1.B1.T1.S3,D-98\r\n"
				+ "A1.B1.T1.S4,D-99\r\n"
				+ "A1.B1.T1.S5,D-100\r\n" + "A1.B1.T2.S1,D-91\r\n" + "A1.B1.T2.S2,D-92\r\n"
				+ "A1.B1.T2.S3,D-93\r\n"
				+ "A1.B1.T2.S4,D-94\r\n" + "A1.B1.T2.S5,D-95\r\n" + "A1.B1.T3.S1,D-86\r\n"
				+ "A1.B1.T3.S2,D-87\r\n"
				+ "A1.B1.T3.S3,D-88\r\n" + "A1.B1.T3.S4,D-89\r\n" + "A1.B1.T3.S5,D-90\r\n"
				+ "A1.B1.T4.S1,D-81\r\n"
				+ "A1.B1.T4.S2,D-82\r\n" + "A1.B1.T4.S3,D-83\r\n" + "A1.B1.T4.S4,D-84\r\n"
				+ "A1.B1.T4.S5,D-85\r\n"
				+ "A1.B1.T5.S1,D-76\r\n" + "A1.B1.T5.S2,D-77\r\n" + "A1.B1.T5.S3,D-78\r\n"
				+ "A1.B1.T5.S4,D-79\r\n"
				+ "A1.B1.T5.S5,D-80\r\n" + "A1.B2.T1.S1,D-46\r\n" + "A1.B2.T1.S2,D-47\r\n"
				+ "A1.B2.T1.S3,D-48\r\n"
				+ "A1.B2.T1.S4,D-49\r\n" + "A1.B2.T1.S5,D-50\r\n" + "A1.B2.T2.S1,D-41\r\n"
				+ "A1.B2.T2.S2,D-42\r\n"
				+ "A1.B2.T2.S3,D-43\r\n" + "A1.B2.T2.S4,D-44\r\n" + "A1.B2.T2.S5,D-45\r\n"
				+ "A1.B2.T3.S1,D-36\r\n"
				+ "A1.B2.T3.S2,D-37\r\n" + "A1.B2.T3.S3,D-38\r\n" + "A1.B2.T3.S4,D-39\r\n"
				+ "A1.B2.T3.S5,D-40\r\n"
				+ "A1.B2.T4.S1,D-31\r\n" + "A1.B2.T4.S2,D-32\r\n" + "A1.B2.T4.S3,D-33\r\n"
				+ "A1.B2.T4.S4,D-34\r\n"
				+ "A1.B2.T4.S5,D-35\r\n" + "A1.B2.T5.S1,D-26\r\n" + "A1.B2.T5.S2,D-27\r\n"
				+ "A1.B2.T5.S3,D-28\r\n"
				+ "A1.B2.T5.S4,D-29\r\n" + "A1.B2.T5.S5,D-30\r\n" + "A2.B1.T1.S1,D-75\r\n"
				+ "A2.B1.T1.S2,D-74\r\n"
				+ "A2.B1.T1.S3,D-73\r\n" + "A2.B1.T1.S4,D-72\r\n" + "A2.B1.T1.S5,D-71\r\n"
				+ "A2.B1.T2.S1,D-70\r\n"
				+ "A2.B1.T2.S2,D-69\r\n" + "A2.B1.T2.S3,D-68\r\n" + "A2.B1.T2.S4,D-67\r\n"
				+ "A2.B1.T2.S5,D-66\r\n"
				+ "A2.B1.T3.S1,D-65\r\n" + "A2.B1.T3.S2,D-64\r\n" + "A2.B1.T3.S3,D-63\r\n"
				+ "A2.B1.T3.S4,D-62\r\n"
				+ "A2.B1.T3.S5,D-61\r\n" + "A2.B1.T4.S1,D-60\r\n" + "A2.B1.T4.S2,D-59\r\n"
				+ "A2.B1.T4.S3,D-58\r\n"
				+ "A2.B1.T4.S4,D-57\r\n" + "A2.B1.T4.S5,D-56\r\n" + "A2.B1.T5.S1,D-55\r\n"
				+ "A2.B1.T5.S2,D-54\r\n"
				+ "A2.B1.T5.S3,D-53\r\n" + "A2.B1.T5.S4,D-52\r\n" + "A2.B1.T5.S5,D-51\r\n"
				+ "A2.B2.T1.S1,D-25\r\n"
				+ "A2.B2.T1.S2,D-24\r\n" + "A2.B2.T1.S3,D-23\r\n" + "A2.B2.T1.S4,D-22\r\n" + "A2.B2.T1.S5,D-21\r\n"
				/*
				// Could fix these. Not needed in current test.
				+ "A2.B2.T2.S1	D-20\r\n" + "A2.B2.T2.S2	D-19\r\n" + "A2.B2.T2.S3	D-18\r\n"
				+ "A2.B2.T2.S4	D-17\r\n"
				+ "A2.B2.T2.S5	D-16\r\n" + "A2.B2.T3.S1	D-15\r\n" + "A2.B2.T3.S2	D-14\r\n"
				+ "A2.B2.T3.S3	D-13\r\n"
				+ "A2.B2.T3.S4	D-12\r\n" + "A2.B2.T3.S5	D-11\r\n" + "A2.B2.T4.S1	D-10\r\n"
				+ "A2.B2.T4.S2	D-9\r\n"
				+ "A2.B2.T4.S3	D-8\r\n" + "A2.B2.T4.S4	D-7\r\n" + "A2.B2.T4.S5	D-6\r\n"
				+ "A2.B2.T5.S1	D-5\r\n"
				+ "A2.B2.T5.S2	D-4\r\n" + "A2.B2.T5.S3	D-3\r\n" + "A2.B2.T5.S4	D-2\r\n"
				*/
				+ "A2.B2.T5.S5	D-1\r\n";

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

		Short channel1 = 1;
		Location aisle1x = getFacility().findSubLocationById("A1");
		controller1.addLocation(aisle1x);
		aisle1x.setLedChannel(channel1);
		aisle1x.getDao().store(aisle1x);

		Location aisle2x = getFacility().findSubLocationById("A2");
		controller1.addLocation(aisle2x);
		aisle2x.setLedChannel(channel1);
		aisle2x.getDao().store(aisle2x);

		return getFacility();
	}

	private void setUpSmallInventoryAndOrders(Facility inFacility) throws IOException {
		// We are going to put cases in A3 and each in A2. Also showing variation in EA/each, etc.
		// 402 and 403 are in A2, the each aisle. 502 and 503 are in A3, the case aisle, on a separate path.
		// One case item, just as part of our immediate short scenario
		String csvString = "itemId,locationId,description,quantity,uom,inventoryDate,cmFromLeft\r\n" //
				+ "1122,D302,8 oz Bowl Lids -PLA Compostable,,ea,6/25/14 12:00,80\r\n" //
				+ "1123,D301,12/16 oz Bowl Lids -PLA Compostable,,EA,6/25/14 12:00,135\r\n" //
				+ "1124,D303,8 oz Bowls -PLA Compostable,,ea,6/25/14 12:00,55\r\n" //
				+ "1493,D301,PARK RANGER Doll,,ea,6/25/14 12:00,66\r\n" //
				+ "1522,D302,Butterfly Yoyo,,ea,6/25/14 12:00,3\r\n" //
				+ "1523,D301,SJJ BPP, ,each,6/25/14 12:00,3\r\n"//
				+ "1555,D502,paper towel, ,cs,6/25/14 12:00,18\r\n";//

		byte[] csvArray = csvString.getBytes();

		ByteArrayInputStream stream = new ByteArrayInputStream(csvArray);
		InputStreamReader reader = new InputStreamReader(stream);

		Timestamp ediProcessTime = new Timestamp(System.currentTimeMillis());
		ICsvInventoryImporter importer = createInventoryImporter();
		importer.importSlottedInventoryFromCsvStream(reader, inFacility, ediProcessTime);

		// Outbound order. No group. Using 5 digit order number and preassigned container number.
		// Item 1123 exists in case and each.
		// Item 1493 exists in case only. Order for each should short.
		// Item 1522 exists in case and each.
		// Item 1555 exists in case only, so will short on each

		String csvString2 = "orderGroupId,shipmentId,customerId,preAssignedContainerId,orderId,itemId,description,quantity,uom,orderDate,dueDate,workSequence"
				+ "\r\n,USF314,COSTCO,12345,12345,1123,12/16 oz Bowl Lids -PLA Compostable,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n,USF314,COSTCO,12345,12345,1493,PARK RANGER Doll,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n,USF314,COSTCO,12345,12345,1522,Butterfly Yoyo,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n,USF314,COSTCO,11111,11111,1122,8 oz Bowl Lids -PLA Compostable,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n,USF314,COSTCO,11111,11111,1522,Butterfly Yoyo,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n,USF314,COSTCO,11111,11111,1523,SJJ BPP,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n,USF314,COSTCO,11111,11111,1124,8 oz Bowls -PLA Compostable,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n,USF314,COSTCO,11111,11111,1555,paper towel,2,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0";

		byte[] csvArray2 = csvString2.getBytes();

		ByteArrayInputStream stream2 = new ByteArrayInputStream(csvArray2);
		InputStreamReader reader2 = new InputStreamReader(stream2);

		Timestamp ediProcessTime2 = new Timestamp(System.currentTimeMillis());
		ICsvOrderImporter importer2 = createOrderImporter();
		importer2.importOrdersFromCsvStream(reader2, inFacility, ediProcessTime2);

	}

	private void setUpBatchOrdersForZigzag(Facility inFacility) throws IOException {
		// Setting up containers 2,3,7,11 to match the bug

		// Outbound orders

		String csvString2 = "orderGroupId,orderId,itemId,description,quantity,uom\r\n" //
				+ "5/26/14,1001dry,53a8a03ab38e3c0200000330,vitalvittles Organic Flax-Seed Oat Bread,2,loaf\r\n" //
				+ "5/26/14,1003dry,539f2da2622fcc0200001009,sayhayfarms Organic Sungold Cherry Tomatoes,4,pint\r\n" //
				+ "5/26/14,1006dry,5266bd1e4d5eed0200000155,firebrand Pretzel Croutons,7,bag\r\n" //
				+ "5/26/14,1007dry,5266bd1e4d5eed0200000155,firebrand Pretzel Croutons,8,bag\r\n" //
				+ "5/26/14,1016dry,50916c6dd136890200000311,blackjet Crack*a*Roons,17,cookies\r\n"; //

		byte[] csvArray2 = csvString2.getBytes();

		ByteArrayInputStream stream2 = new ByteArrayInputStream(csvArray2);
		InputStreamReader reader2 = new InputStreamReader(stream2);

		Timestamp ediProcessTime2 = new Timestamp(System.currentTimeMillis());
		ICsvOrderImporter importer2 = createOrderImporter();
		importer2.importOrdersFromCsvStream(reader2, inFacility, ediProcessTime2);

		OrderHeader order = inFacility.getOrderHeader("1001dry");
		Assert.assertNotNull(order);
		Integer detailCount = order.getOrderDetails().size();
		Assert.assertEquals((Integer) 1, detailCount);

		// Slotting
		String csvString3 = "orderId,locationId\r\n" //
				+ "1001dry,D-26\r\n" + "1001dry,D-27\r\n" + "1001dry,D-28\r\n" + "1001dry,D-29\r\n"
				+ "1001dry,D-30\r\n"
				+ "1001dry,D-31\r\n" + "1001dry,D-32\r\n" + "1001dry,D-33\r\n" + "1001dry,D-34\r\n"
				+ "1001dry,D-35\r\n"
				+ "1003dry,D-22\r\n" + "1006dry,D-100\r\n" + "1016dry,D-76\r\n" + "1007dry,D-99\r\n";

		byte[] csvArray3 = csvString3.getBytes();

		Timestamp ediProcessTime = new Timestamp(System.currentTimeMillis());
		ICsvOrderLocationImporter importer = createOrderLocationImporter();
		ByteArrayInputStream stream3 = new ByteArrayInputStream(csvArray3);
		InputStreamReader reader3 = new InputStreamReader(stream3);

		boolean result = importer.importOrderLocationsFromCsvStream(reader3, inFacility, ediProcessTime);

		// Batches file. Only containers 2,3,7,11
		String csvString4 = "itemId,orderGroupId,containerId,description,quantity,uom\r\n" //
				+ "539f2da2622fcc0200001009,5/26/14,2,sayhayfarms Organic Sungold Cherry Tomatoes,1,pint\r\n" //
				+ "53a8a03ab38e3c0200000330,5/26/14,3,vitalvittles Organic Flax-Seed Oat Bread,1,loaf\r\n" //
				+ "50916c6dd136890200000311,5/26/14,7,blackjet Crack*a*Roons,2,cookies\r\n" //
				+ "5266bd1e4d5eed0200000155,5/26/14,11,firebrand Pretzel Croutons,7,bag\r\n";//

		byte[] csvArray4 = csvString4.getBytes();

		ByteArrayInputStream stream4 = new ByteArrayInputStream(csvArray4);
		InputStreamReader reader4 = new InputStreamReader(stream4);

		Timestamp thirdEdiProcessTime = new Timestamp(System.currentTimeMillis());
		ICsvCrossBatchImporter importer4 = createCrossBatchImporter();
		importer4.importCrossBatchesFromCsvStream(reader4, inFacility, thirdEdiProcessTime);

	}

	@SuppressWarnings("unused")
	@Test
	public final void testDataSetup() throws IOException {

		this.getPersistenceService().beginTenantTransaction();
		Facility facility = setUpSimpleNoSlotFacility();
		UUID facId = facility.getPersistentId();
		setUpSmallInventoryAndOrders(facility);
		this.getPersistenceService().commitTenantTransaction();

		this.getPersistenceService().beginTenantTransaction();
		facility = Facility.DAO.reload(facility);
		Assert.assertNotNull(facility);

		List<Container> containers = facility.getContainers();
		this.getPersistenceService().commitTenantTransaction();
	}

	@SuppressWarnings({ "unused" })
	@Test
	public final void testPick() throws IOException {
		this.getPersistenceService().beginTenantTransaction();

		Facility facility = setUpSimpleNoSlotFacility();
		this.getPersistenceService().commitTenantTransaction();

		this.getPersistenceService().beginTenantTransaction();
		facility = Facility.DAO.reload(facility);

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
		ICsvInventoryImporter importer = createInventoryImporter();
		importer.importSlottedInventoryFromCsvStream(reader, facility, ediProcessTime);
		this.getPersistenceService().commitTenantTransaction();

		this.getPersistenceService().beginTenantTransaction();
		facility = Facility.DAO.reload(facility);

		Location locationD403 = facility.findSubLocationById("D403");
		Location locationD402 = facility.findSubLocationById("D402");
		Location locationD502 = facility.findSubLocationById("D502");
		Location locationD503 = facility.findSubLocationById("D503");

		Item item1123Loc402EA = locationD402.getStoredItemFromMasterIdAndUom("1123", "EA");
		Assert.assertNotNull(item1123Loc402EA);
		this.getPersistenceService().commitTenantTransaction();

		this.getPersistenceService().beginTenantTransaction();
		facility = Facility.DAO.reload(facility);

		// Outbound order. No group. Using 5 digit order number and preassigned container number.
		// Item 1123 exists in case and each.
		// Item 1493 exists in case only. Order for each should short.
		// Item 1522 exists in case and each.

		String csvString2 = "orderGroupId,shipmentId,customerId,preAssignedContainerId,orderId,itemId,description,quantity,uom,orderDate,dueDate,workSequence"
				+ "\r\n1,USF314,COSTCO,12345,12345,1123,12/16 oz Bowl Lids -PLA Compostable,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,12345,12345,1493,PARK RANGER Doll,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,12345,12345,1522,SJJ BPP,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0";

		Timestamp ediProcessTime2 = new Timestamp(System.currentTimeMillis());
		ICsvOrderImporter importer2 = createOrderImporter();
		importer2.importOrdersFromCsvStream(new StringReader(csvString2), facility, ediProcessTime2);
		this.getPersistenceService().commitTenantTransaction();

		this.getPersistenceService().beginTenantTransaction();
		facility = Facility.DAO.reload(facility);

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
		this.getPersistenceService().commitTenantTransaction();

		this.getPersistenceService().beginTenantTransaction();
		facility = Facility.DAO.reload(facility);

		// Turn off housekeeping work instructions so as to not confuse the counts
		mPropertyService.turnOffHK(facility);
		this.getPersistenceService().commitTenantTransaction();
		this.getPersistenceService().beginTenantTransaction();
		facility = Facility.DAO.reload(facility);
		// Set up a cart for order 12345, which will generate work instructions
		Che che1 = Che.DAO.findByPersistentId(this.che1PersistentId);
		mWorkService.setUpCheContainerFromString(che1, "12345");
		this.getPersistenceService().commitTenantTransaction();

		this.getPersistenceService().beginTenantTransaction();
		facility = Facility.DAO.reload(facility);
		che1 = Che.DAO.reload(che1);
		List<WorkInstruction> aList = mWorkService.getWorkInstructions(che1, "");
		int wiCount = aList.size();
		Assert.assertEquals(2, wiCount); // 3, but one should be short. Only 1123 and 1522 find each inventory

		for (WorkInstruction workInstruction : aList) {
			Assert.assertEquals(OrderStatusEnum.INPROGRESS, workInstruction.getOrderDetail().getStatus());
		}
		this.getPersistenceService().commitTenantTransaction();

		this.getPersistenceService().beginTenantTransaction();
		facility = Facility.DAO.reload(facility);
		mPropertyService.restoreHKDefaults(facility); // set it back
		this.getPersistenceService().commitTenantTransaction();

		this.getPersistenceService().beginTenantTransaction();
		facility = Facility.DAO.reload(facility);
		che1 = Che.DAO.reload(che1);
		List<WorkInstruction> wiListAfterScan = mWorkService.getWorkInstructions(che1, "D402");
		Integer wiCountAfterScan = wiListAfterScan.size();
		Double posOf402 = locationD402.getPosAlongPath();
		Double posOf403 = locationD403.getPosAlongPath();
		Assert.assertTrue(posOf402 > posOf403);

		// If DEV-477 route-wrap is in effect, both are there, but the 402 item is first. We still get the baychange between
		// If DEV-477 is not in effect, 402 item is still first, and 403 item is not in the list.
		Assert.assertEquals((Integer) 3, wiCountAfterScan);
		// See which work instruction is which
		WorkInstruction wi1 = wiListAfterScan.get(0);
		Assert.assertNotNull(wi1);
		String wiLoc = wi1.getPickInstruction(); // this is the denormalized position on the work instruction. Should have the alias, and not F1.A2.B2.T1
		Assert.assertEquals("D402", wiLoc);
		WorkInstruction wi2 = wiListAfterScan.get(1);
		Assert.assertTrue(wi2.isHousekeeping());

		this.getPersistenceService().commitTenantTransaction();

		this.getPersistenceService().beginTenantTransaction();
		che1 = Che.DAO.reload(che1);
		// New from v4. Test our work instruction summarizer
		List<WiSetSummary> summaries = new WorkService().start().workSummary(che1.getPersistentId(), facility.getPersistentId());

		// as this test, this facility only set up this one che, there should be only one wi set. But we have 3. How?
		Assert.assertEquals(1, summaries.size());

		// getAny should get the one. Call it somewhat as the UI would. Get a time, then query again with that time.
		WiSetSummary theSummary = summaries.get(0);
		// So, how many shorts, how many active? None complete yet.
		int actives = theSummary.getActiveCount();
		int shorts = theSummary.getShortCount();
		int completes = theSummary.getCompleteCount();
		Assert.assertEquals(0, completes);
		Assert.assertEquals(3, actives);
		Assert.assertEquals(1, shorts);

		this.getPersistenceService().commitTenantTransaction();
	}

	@SuppressWarnings({ "unused" })
	@Test
	public final void testPickViaChe() throws IOException {
		this.getPersistenceService().beginTenantTransaction();

		Facility facility = setUpSimpleNoSlotFacility();
		this.getPersistenceService().commitTenantTransaction();

		this.getPersistenceService().beginTenantTransaction();
		facility = Facility.DAO.reload(facility);

		// We are going to put cases in A3 and each in A2. Also showing variation in EA/each, etc.
		// 402 and 403 are in A2, the each aisle. 502 and 503 are in A3, the case aisle, on a separate path.
		String csvString = "itemId,locationId,description,quantity,uom,inventoryDate,cmFromLeft\r\n" //
				+ "1123,D402,12/16 oz Bowl Lids -PLA Compostable,6,EA,6/25/14 12:00,135\r\n" //
				+ "1123,D502,12/16 oz Bowl Lids -PLA Compostable,6,CS,6/25/14 12:00,8\r\n" //
				+ "1123,D503,12/16 oz Bowl Lids -PLA Compostable,6,CS,6/25/14 12:00,55\r\n" //
				+ "1493,D502,PARK RANGER Doll,2,case,6/25/14 12:00,66\r\n" //
				+ "1522,D503,SJJ BPP,1,case,6/25/14 12:00,3\r\n" //
				+ "1522,D403,SJJ BPP,10,each,6/25/14 12:00,3\r\n";//

		byte[] csvArray = csvString.getBytes();

		ByteArrayInputStream stream = new ByteArrayInputStream(csvArray);
		InputStreamReader reader = new InputStreamReader(stream);

		Timestamp ediProcessTime = new Timestamp(System.currentTimeMillis());
		ICsvInventoryImporter importer = createInventoryImporter();
		importer.importSlottedInventoryFromCsvStream(reader, facility, ediProcessTime);
		this.getPersistenceService().commitTenantTransaction();

		this.getPersistenceService().beginTenantTransaction();
		facility = Facility.DAO.reload(facility);

		Location locationD403 = facility.findSubLocationById("D403");
		Location locationD402 = facility.findSubLocationById("D402");
		Location locationD502 = facility.findSubLocationById("D502");
		Location locationD503 = facility.findSubLocationById("D503");

		Item item1123Loc402EA = locationD402.getStoredItemFromMasterIdAndUom("1123", "EA");
		Assert.assertNotNull(item1123Loc402EA);

		this.getPersistenceService().commitTenantTransaction();

		this.getPersistenceService().beginTenantTransaction();
		facility = Facility.DAO.reload(facility);
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
		ICsvOrderImporter importer2 = createOrderImporter();
		importer2.importOrdersFromCsvStream(reader2, facility, ediProcessTime2);
		this.getPersistenceService().commitTenantTransaction();

		this.getPersistenceService().beginTenantTransaction();
		facility = Facility.DAO.reload(facility);

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
			} else {
				LOGGER.debug(detail.getItemMasterId() + " " + detail.getUomMasterId() + " has no location");
			}
		}
		Assert.assertEquals(2, itemLocations.size());
		// Turn off housekeeping work instructions for next test so as to not confuse the counts
		this.getPersistenceService().commitTenantTransaction();

		this.getPersistenceService().beginTenantTransaction();
		facility = Facility.DAO.reload(facility);
		mPropertyService.turnOffHK(facility);
		this.getPersistenceService().commitTenantTransaction();

		// Set up a cart for order 12345, which will generate work instructions
		PickSimulator picker = new PickSimulator(this, cheGuid1);
		picker.login("Picker #1");
		picker.setupContainer("12345", "1");
		picker.startAndSkipReview("D403", 8000, 5000);

		this.getPersistenceService().beginTenantTransaction();
		facility = Facility.DAO.reload(facility);
		mPropertyService.restoreHKDefaults(facility);
		this.getPersistenceService().commitTenantTransaction();

		Assert.assertEquals(picker.getLastSentPositionControllerDisplayValue((byte) 1).byteValue(), 1);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayDutyCycle((byte) 1), PosControllerInstr.BRIGHT_DUTYCYCLE);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayFreq((byte) 1), PosControllerInstr.SOLID_FREQ);

		Assert.assertEquals(1, picker.countActiveJobs());
		WorkInstruction currentWI = picker.nextActiveWi();
		Assert.assertEquals("SJJ BPP", currentWI.getDescription());
		Assert.assertEquals("1522", currentWI.getItemId());

		// pick first item
		picker.pick(1, 1);
		Assert.assertEquals(1, picker.countActiveJobs());
		currentWI = picker.nextActiveWi();
		Assert.assertEquals("1123", currentWI.getItemId());

		// pick second item
		picker.pick(1, 1);
		Assert.assertEquals(0, picker.countActiveJobs());

		picker.waitForCheState(CheStateEnum.PICK_COMPLETE, 1000);
		picker.logout();
	}

	@SuppressWarnings("unused")
	@Test
	public final void testCheProcess1() throws IOException {
		// Test cases:
		// 1) If no work, immediately comes to NO_WORK after start. (Before v6, it came to all work complete.)
		// 2) A happy-day pick startup. No housekeeping jobs.
		// Case 3: A happy-day short, with one short-ahead");
		// Case 4: Short and cancel leave you on the same job");
		// Case 5: Inappropriate location scan, then normal button press works");

		// set up data for pick scenario
		this.getPersistenceService().beginTenantTransaction();
		Facility facility = setUpSimpleNoSlotFacility();
		this.getPersistenceService().commitTenantTransaction();

		this.getPersistenceService().beginTenantTransaction();
		facility = Facility.DAO.reload(facility);
		UUID facId = facility.getPersistentId();
		setUpSmallInventoryAndOrders(facility);
		this.getPersistenceService().commitTenantTransaction();

		this.getPersistenceService().beginTenantTransaction();
		facility = Facility.DAO.reload(facility);
		mPropertyService.turnOffHK(facility);
		this.getPersistenceService().commitTenantTransaction();

		// perform pick operations
		this.getPersistenceService().beginTenantTransaction();
		facility = Facility.DAO.reload(facility);
		List<Container> containers = facility.getContainers();
		Assert.assertEquals(2, containers.size());
		this.getPersistenceService().commitTenantTransaction();

		// Set up a cart for orders 12345 and 1111, which will generate work instructions
		PickSimulator picker = new PickSimulator(this, cheGuid1);
		picker.login("Picker #1");

		// This brief case covers and allows retirement of CheSimulationTest.java
		LOGGER.info("Case 1: If no work, immediately comes to NO_WORK after start. (Before v6, it came to all work complete.)");
		picker.setupContainer("9x9x9", "1"); // unknown container
		picker.scanCommand("START");
		picker.waitForCheState(CheStateEnum.NO_WORK, 5000);

		Assert.assertEquals(0, picker.countActiveJobs());

		//Make sure position display controllers show proper feedback
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayValue((byte) 1), PosControllerInstr.BITENCODED_SEGMENTS_CODE);
		Assert.assertEquals(picker.getLastSentPositionControllerMinQty((byte) 1), PosControllerInstr.BITENCODED_LED_DASH);
		Assert.assertEquals(picker.getLastSentPositionControllerMaxQty((byte) 1), PosControllerInstr.BITENCODED_LED_DASH);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayFreq((byte) 1), PosControllerInstr.SOLID_FREQ);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayDutyCycle((byte) 1), PosControllerInstr.DIM_DUTYCYCLE);
		Assert.assertFalse(picker.hasLastSentInstruction((byte) 2));

		// Back to our main test
		LOGGER.info("Case 2: A happy-day pick startup. No housekeeping jobs.");
		picker.setup();
		picker.setupContainer("12345", "1"); // This prepended to scan "C%12345" as per Codeshelf scan specification

		//Check that container show last 2 digits of container id
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayValue((byte) 1), Byte.valueOf("45"));
		Assert.assertFalse(picker.hasLastSentInstruction((byte) 2));

		picker.scanOrderId("11111");
		picker.waitForCheState(CheStateEnum.CONTAINER_POSITION, 1000);

		//Make sure we do not lose last container
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayValue((byte) 1), Byte.valueOf("45"));

		picker.scanPosition("2");
		picker.waitForCheState(CheStateEnum.CONTAINER_SELECT, 1000);

		//Check that containers show last 2 digits of container id
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayValue((byte) 1), Byte.valueOf("45"));
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayValue((byte) 2), Byte.valueOf("11"));

		//picker.startAndSkipReview("D303", 5000, 3000);
		//Check to make sure we can scan a good location after a bad location
		picker.scanCommand("START");
		picker.waitForCheState(CheStateEnum.LOCATION_SELECT_REVIEW, 4000);
		picker.scanLocation("BAD_LOCATION");
		picker.waitForCheState(CheStateEnum.NO_WORK, 4000);

		picker.scanLocation("D303");
		picker.waitForCheState(CheStateEnum.DO_PICK, 4000);

		LOGGER.info("List the work instructions as the server sees them");

		this.getPersistenceService().beginTenantTransaction();
		List<WorkInstruction> serverWiList = picker.getServerVersionAllPicksList();
		logWiList(serverWiList);

		Assert.assertEquals(7, picker.countRemainingJobs());
		Assert.assertEquals(1, picker.countActiveJobs());
		WorkInstruction wi = picker.nextActiveWi();
		Che che1 = Che.DAO.findByPersistentId(this.che1PersistentId);
		assertWIColor(wi, che1);

		int button = picker.buttonFor(wi);
		int quant = wi.getPlanQuantity();
		this.getPersistenceService().commitTenantTransaction();

		this.getPersistenceService().beginTenantTransaction();
		wi = WorkInstruction.DAO.reload(wi);
		//Pos 1 should be the same
		Assert.assertFalse(picker.hasLastSentInstruction((byte) 1));
		//After Scanning start location of D303 we should be right next to the
		//8oz bowls which is part of order 11111 in position 2 with a quantity of 1
		//That means the position controller for position 2 should have a quantity of 1:
		//Make sure I was right about position 2 (order 11111), quantity 1 of 8oz bowls which has an itemId 1123
		Assert.assertEquals(button, 2);
		Assert.assertEquals(quant, 1);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayValue((byte) button).byteValue(), (byte) 1);
		Assert.assertEquals(wi.getItemId(), "1124");
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayDutyCycle((byte) button),
			PosControllerInstr.BRIGHT_DUTYCYCLE);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayFreq((byte) button), PosControllerInstr.SOLID_FREQ);

		Assert.assertNull(picker.getLastSentPositionControllerDisplayValue((byte) 3));

		// pick first item
		picker.pick(button, quant);
		picker.waitForCheState(CheStateEnum.DO_PICK, 5000);
		Assert.assertEquals(6, picker.countRemainingJobs());

		LOGGER.info("Case 3: A happy-day short, with one short-ahead");
		wi = picker.nextActiveWi();
		button = picker.buttonFor(wi);
		quant = wi.getPlanQuantity();

		//Third job has a quantity of 1 for position 2. Make sure it matches the button and quant from the wi
		//Make sure we have the right position and quantities and itemId
		Assert.assertEquals(quant, 1);
		Assert.assertEquals(button, 2);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayValue((byte) button).byteValue(), (byte) 1);
		// the third job is for 1522, which happens to be the one item going to both orders. So it should short-ahead
		Assert.assertEquals("1522", wi.getItemId());

		picker.scanCommand("SHORT");
		picker.waitForCheState(CheStateEnum.SHORT_PICK, 5000);
		picker.pick(button, 0);
		picker.waitForCheState(CheStateEnum.SHORT_PICK_CONFIRM, 5000);
		picker.scanCommand("YES");
		picker.waitForCheState(CheStateEnum.DO_PICK, 5000);
		Assert.assertEquals(4, picker.countRemainingJobs()); // Would be 5, but with one short ahead it is 4.

		LOGGER.info("Case 4: Short and cancel leave you on the same job");
		wi = picker.nextActiveWi();
		button = picker.buttonFor(wi);

		picker.scanCommand("SHORT");
		picker.waitForCheState(CheStateEnum.SHORT_PICK, 5000);
		picker.pick(button, 0);
		picker.waitForCheState(CheStateEnum.SHORT_PICK_CONFIRM, 5000);
		picker.scanCommand("NO");
		picker.waitForCheState(CheStateEnum.DO_PICK, 5000);
		Assert.assertEquals(4, picker.countRemainingJobs()); // Still 4.
		WorkInstruction wi2 = picker.nextActiveWi();
		Assert.assertEquals(wi, wi2); // same work instruction still on

		LOGGER.info("Case 5: Inappropriate location scan, then normal button press works");
		wi = picker.nextActiveWi();
		button = picker.buttonFor(wi);
		Assert.assertNotEquals(0, button);
		quant = wi.getPlanQuantity();
		picker.scanLocation("D302");
		picker.waitForCheState(CheStateEnum.DO_PICK, 5000); // still on pick state, although with an error message

		//Next job has a quantity of 1 for position 2. Make sure it matches the button and quant from the wi
		Byte ctrlDispValueObj = picker.getLastSentPositionControllerDisplayValue((byte) button);
		Assert.assertNotNull(ctrlDispValueObj);
		int ctrlDispValue = ctrlDispValueObj.byteValue();
		int planValue = wi.getPlanQuantity().byteValue();
		Assert.assertEquals(ctrlDispValue, planValue);
		//Make sure we have the right position and quantities and itemId
		Assert.assertEquals(quant, 1);
		Assert.assertEquals(button, 2);

		picker.pick(button, quant);
		picker.waitForCheState(CheStateEnum.DO_PICK, 5000);
		Assert.assertEquals(3, picker.countRemainingJobs());

		wi = picker.nextActiveWi();
		button = picker.buttonFor(wi);

		//Last check:
		//Next job has a quantity of 1 for position 2. Make sure it matches the button and quant from the wi
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayValue((byte) button).byteValue(), wi.getPlanQuantity()
			.byteValue());
		//Make sure we have the right position and quantities and itemId
		Assert.assertEquals(quant, 1);
		Assert.assertEquals(button, 2);

		picker.simulateCommitByChangingTransaction(this.persistenceService);

		LOGGER.info("List the work instructions as the server sees them");
		List<WorkInstruction> serverWiList2 = picker.getCurrentWorkInstructionsFromList(serverWiList);
		logWiList(serverWiList2);
		// In this, we see 2nd wi is user short, and third a short ahead. Item 1555 should have got an immediate short.
		WorkInstruction userShortWi = serverWiList2.get(1);
		WorkInstruction shortAheadWi = serverWiList2.get(2);
		WorkInstruction immediateShortWi = null;

		// If you ask che1 for getCheWorkInstructions(), the list will throw during lazy load because the che reference came from a different transaction.
		// But we had to change the transaction in order to see the completed work instructions.
		Che che1b = Che.DAO.findByPersistentId(this.che1PersistentId);
		// for (WorkInstruction cheWi : che1.getCheWorkInstructions()) {
		List<WorkInstruction> cheWis2 = che1b.getCheWorkInstructions();
		Assert.assertNotNull(cheWis2);
		int cheWiTotal2 = cheWis2.size();

		for (WorkInstruction cheWi : cheWis2) {
			if (cheWi.getItemMasterId().equals("1555"))
				immediateShortWi = cheWi;
		}
		Assert.assertNotNull(userShortWi);
		Assert.assertNotNull(shortAheadWi);
		Assert.assertNotNull(immediateShortWi);
		logOneWi(immediateShortWi);
		logOneWi(userShortWi);
		logOneWi(shortAheadWi);
		// All should have the same assign time
		Assert.assertEquals(immediateShortWi.getAssigned(), userShortWi.getAssigned());
		Assert.assertEquals(immediateShortWi.getAssigned(), shortAheadWi.getAssigned());

		mPropertyService.restoreHKDefaults(facility);

		this.getPersistenceService().commitTenantTransaction();
	}

	@Test
	public final void testRouteWrap() throws IOException {
		// create test data
		this.getPersistenceService().beginTenantTransaction();
		Facility facility = setUpSimpleNoSlotFacility();
		setUpSmallInventoryAndOrders(facility);
		this.getPersistenceService().commitTenantTransaction();

		// perform pick operation
		this.getPersistenceService().beginTenantTransaction();
		// mPropertyService.turnOffHK(); // leave housekeeping on for this test, because we need to test removing the bay change just prior to the wrap point.

		// Set up a cart for orders 12345 and 1111, which will generate work instructions
		PickSimulator picker = new PickSimulator(this, cheGuid1);
		picker.login("Picker #1");

		LOGGER.info("Case 1: Scan on near the end of the route. Only 3 of 7 jobs left. (There are 3 housekeeping). So, with route-wrap, 10 jobs");

		picker.setupContainer("12345", "1");
		picker.setupContainer("11111", "2");
		// Taking more than 3 seconds for the recompute and wrap.
		picker.startAndSkipReview("D301", 5000, 3000);
		mPropertyService.restoreHKDefaults(facility);

		Assert.assertEquals(picker.getLastSentPositionControllerDisplayValue((byte) 2).byteValue(), (byte) 1);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayDutyCycle((byte) 2), PosControllerInstr.BRIGHT_DUTYCYCLE);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayFreq((byte) 2), PosControllerInstr.SOLID_FREQ);
		Assert.assertFalse(picker.hasLastSentInstruction((byte) 1));

		// WARNING: whenever getting work instructions via the picker, it is in the context that the site controller has. For example
		// the itemMaster field is null.
		Assert.assertEquals(10, picker.countRemainingJobs());
		LOGGER.info("List the work instructions as the site controller sees them");
		List<WorkInstruction> theWiList = picker.getAllPicksList();
		logWiList(theWiList);
		LOGGER.info("List the work instructions as the server sees them");
		List<WorkInstruction> serverWiList = picker.getServerVersionAllPicksList();
		logWiList(serverWiList);

		Assert.assertEquals(1, picker.countActiveJobs());
		WorkInstruction wi = picker.nextActiveWi();

		Che che1 = Che.DAO.findByPersistentId(this.che1PersistentId);
		assertWIColor(wi, che1);
		int button = picker.buttonFor(wi);
		int quant = wi.getPlanQuantity();
		Assert.assertEquals("D301", wi.getPickInstruction());

		// pick first item
		picker.pick(button, quant);
		picker.waitForCheState(CheStateEnum.DO_PICK, 1000);
		Assert.assertEquals(9, picker.countRemainingJobs());

		LOGGER.info("Case 2: Pick the 2nd and 3rd jobs");
		wi = picker.nextActiveWi();
		button = picker.buttonFor(wi);
		quant = wi.getPlanQuantity();
		picker.pick(button, quant);
		picker.waitForCheState(CheStateEnum.DO_PICK, 1000);
		Assert.assertEquals(8, picker.countRemainingJobs());
		// last job
		wi = picker.nextActiveWi();
		button = picker.buttonFor(wi);
		quant = wi.getPlanQuantity();
		picker.pick(button, quant);
		Assert.assertEquals(7, picker.countRemainingJobs());
		picker.simulateCommitByChangingTransaction(this.persistenceService);

		LOGGER.info("List the work instructions as the server now has them");
		List<WorkInstruction> serverWiList2 = picker.getCurrentWorkInstructionsFromList(serverWiList);
		logWiList(serverWiList2);

		this.persistenceService.commitTenantTransaction();
	}

	@Test
	public final void testRouteWrap2() throws IOException {
		// Reproduce bug seen during MAT for v10
		this.getPersistenceService().beginTenantTransaction();
		Facility facility = setUpZigzagSlottedFacility();
		setUpBatchOrdersForZigzag(facility);
		this.getPersistenceService().commitTenantTransaction();

		// perform pick operation
		this.getPersistenceService().beginTenantTransaction();
		// mPropertyService.turnOffHK(); // leave housekeeping on for this test, because we found the bug with it on.

		// Set up a cart for orders 12345 and 1111, which will generate work instructions
		PickSimulator picker = new PickSimulator(this, cheGuid1);
		picker.login("Picker #1");

		LOGGER.info("Case 1: Scan ");
		// The case is to set up batch containers 2,3,7,11. Start location D-26 is ok (no wrap). Start location D-76 has a wrap.
		picker.setupContainer("2", "4");
		picker.setupContainer("3", "5");
		picker.setupContainer("7", "14");
		picker.setupContainer("11", "15");
		// Taking more than 3 seconds for the recompute and wrap.
		picker.scanCommand("START");

		picker.waitForCheState(CheStateEnum.LOCATION_SELECT, 3000);
		picker.scanLocation("D-76");
		picker.waitForCheState(CheStateEnum.DO_PICK, 3000);

		LOGGER.info("List the work instructions as the server sees them");
		List<WorkInstruction> serverWiList = picker.getServerVersionAllPicksList();
		logWiList(serverWiList);

		Assert.assertEquals(1, picker.countActiveJobs());
		WorkInstruction wi = picker.nextActiveWi();

		Che che1 = Che.DAO.findByPersistentId(this.che1PersistentId);
		assertWIColor(wi, che1);
		int button = picker.buttonFor(wi);
		int quant = wi.getPlanQuantity();
		Assert.assertEquals("D-76", wi.getPickInstruction());
		// D-76 is interesting. Actually last tier on the path in that tier, so our code normalizes back the the bay posAlongPath.
		// D-76 comes up first in the list compared to the other two in that bay only because it has the top tier location and we sort top down.

		// pick first item. 7 left (3 housekeeps)
		picker.pick(button, quant);
		picker.waitForCheState(CheStateEnum.DO_PICK, 1000);
		Assert.assertEquals(7, picker.countRemainingJobs());

		LOGGER.info("Case 2: Pick the 2nd and 3rd jobs");
		wi = picker.nextActiveWi();
		button = picker.buttonFor(wi);
		quant = wi.getPlanQuantity();
		picker.pick(button, quant);
		picker.waitForCheState(CheStateEnum.DO_PICK, 1000);
		Assert.assertEquals(6, picker.countRemainingJobs());
		Assert.assertEquals("D-100", wi.getPickInstruction());

		// fourth job
		wi = picker.nextActiveWi();
		button = picker.buttonFor(wi);
		quant = wi.getPlanQuantity();
		picker.pick(button, quant);
		Assert.assertEquals(5, picker.countRemainingJobs());
		Assert.assertEquals("", wi.getPickInstruction()); // a housekeep

		// fifth job
		wi = picker.nextActiveWi();
		button = picker.buttonFor(wi);
		quant = wi.getPlanQuantity();
		picker.pick(button, quant);
		Assert.assertEquals("D-99", wi.getPickInstruction());

		picker.simulateCommitByChangingTransaction(this.persistenceService);

		this.persistenceService.commitTenantTransaction();
	}

	@Test
	public final void twoChesCrossBatch() throws IOException {
		// Reproduce DEV-592 seen during MAT for v10
		// This test case setup similarly to testRouteWrap2
		this.getPersistenceService().beginTenantTransaction();
		Facility facility = setUpZigzagSlottedFacility();
		setUpBatchOrdersForZigzag(facility);
		this.getPersistenceService().commitTenantTransaction();

		// perform pick operation
		this.getPersistenceService().beginTenantTransaction();
		// mPropertyService.turnOffHK(); // leave housekeeping on for this test, because we found the bug with it on.

		PickSimulator picker = new PickSimulator(this, cheGuid1);
		picker.login("Picker #1");

		LOGGER.info("Set up first CHE ");
		// The case is to set up batch containers 2,3,7,11. Start location D-26 is ok (no wrap). Start location D-76 has a wrap.
		picker.setupContainer("2", "4");
		picker.setupContainer("3", "5");
		picker.setupContainer("7", "14");
		picker.setupContainer("11", "15");
		// Taking more than 3 seconds for the recompute and wrap.
		picker.scanCommand("START");

		picker.waitForCheState(CheStateEnum.LOCATION_SELECT, 3000);
		picker.scanLocation("D-76");
		picker.waitForCheState(CheStateEnum.DO_PICK, 3000);

		LOGGER.info("List the work instructions as the server sees them");
		List<WorkInstruction> serverWiList = picker.getServerVersionAllPicksList();
		logWiList(serverWiList);
		Assert.assertEquals(8, serverWiList.size());

		LOGGER.info("First CHE walks away. Never doing anything. Set up same thing on second CHE ");
		// This is the DEV-592 bug. Our hibernate parent-childe patterns says we cannot add WI to one CHE without first removing from the other.

		PickSimulator picker2 = new PickSimulator(this, cheGuid2);
		picker2.login("Picker #2");

		picker2.setupContainer("2", "4");
		picker2.setupContainer("3", "5");
		picker2.setupContainer("7", "14");
		picker2.setupContainer("11", "15");
		picker2.scanCommand("START");

		picker2.waitForCheState(CheStateEnum.LOCATION_SELECT, 3000);
		picker2.scanLocation("D-76");
		picker2.waitForCheState(CheStateEnum.DO_PICK, 3000);

		LOGGER.info("List the work instructions as the server sees them");
		List<WorkInstruction> serverWiList2 = picker2.getServerVersionAllPicksList();
		logWiList(serverWiList2);
		Assert.assertEquals(8, serverWiList2.size());

		Assert.assertEquals(1, picker2.countActiveJobs());
		WorkInstruction wi = picker2.nextActiveWi();

		int button = picker2.buttonFor(wi);
		int quant = wi.getPlanQuantity();
		Assert.assertEquals("D-76", wi.getPickInstruction());

		picker2.simulateCommitByChangingTransaction(this.persistenceService);

		this.persistenceService.commitTenantTransaction();
	}

	@Test
	public final void testCartSetupFeedback() throws IOException {
		// Test cases:
		// 1. Two good plans for position 1.
		// 2. One good plan for position 2 and and immediate short.
		// 3. Unknown order number for position 3.
		// 4. Only an immediate short for position 4.
		// 5. There is only a case pick order for position 5. Currently will give a work instruction if the case is in inventory.
		// set up data for pick scenario

		// set up data for pick scenario
		this.getPersistenceService().beginTenantTransaction();

		Facility facility = setUpSimpleNoSlotFacility();
		UUID facId = facility.getPersistentId();
		// We are going to put everything in A1 and A2 since they are on the same path.
		//Item 5 is out of stock and item 6 is case only.
		String csvString = "itemId,locationId,description,quantity,uom,inventoryDate,cmFromLeft\r\n" //
				+ "1,D301,Test Item 1,6,EA,6/25/14 12:00,135\r\n" //
				+ "2,D302,Test Item 2,6,EA,6/25/14 12:00,8\r\n" //
				+ "3,D303,Test Item 3,6,EA,6/25/14 12:00,55\r\n" //
				+ "4,D401,Test Item 4,1,EA,6/25/14 12:00,66\r\n" //
				+ "6,D403,Test Item 6,1,EA,6/25/14 12:00,3\r\n";//

		byte[] csvArray = csvString.getBytes();

		ByteArrayInputStream stream = new ByteArrayInputStream(csvArray);
		InputStreamReader reader = new InputStreamReader(stream);

		Timestamp ediProcessTime = new Timestamp(System.currentTimeMillis());
		ICsvInventoryImporter importer = createInventoryImporter();
		importer.importSlottedInventoryFromCsvStream(reader, facility, ediProcessTime);
		this.getPersistenceService().commitTenantTransaction();

		this.getPersistenceService().beginTenantTransaction();
		facility = Facility.DAO.reload(facility);
		// Outbound order. No group. Using 5 digit order number and preassigned container number.
		// Order 11111 has two items in stock (Item 1 and Item 2)
		// Order 22222 has 1 item in stock (Item 1) and 1 immediate short (Item 5 which is out of stock)
		// Order 44444 has an immediate short (Item 5 which is out of stock)
		// Order 55555 has a each pick for an item that only has a case (Item 6)
		String csvString2 = "orderGroupId,shipmentId,customerId,preAssignedContainerId,orderId,itemId,description,quantity,uom,orderDate,dueDate,workSequence"
				+ "\r\n1,USF314,COSTCO,a1111,a1111,1,Test Item 1,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,a1111,a1111,2,Test Item 2,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,22222,22222,1,Test Item 1,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,22222,22222,5,Test Item 5,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,44444,44444,5,Test Item 5,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,9,5,6,Test Item 6,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,a6,a6,3,Test Item 3,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0";

		byte[] csvArray2 = csvString2.getBytes();

		ByteArrayInputStream stream2 = new ByteArrayInputStream(csvArray2);
		InputStreamReader reader2 = new InputStreamReader(stream2);

		Timestamp ediProcessTime2 = new Timestamp(System.currentTimeMillis());
		ICsvOrderImporter importer2 = createOrderImporter();
		importer2.importOrdersFromCsvStream(reader2, facility, ediProcessTime2);// Outbound order. No group. Using 5 digit order number and preassigned container number.

		mPropertyService.turnOffHK(facility);
		this.getPersistenceService().commitTenantTransaction();

		this.getPersistenceService().beginTenantTransaction();
		facility = Facility.DAO.reload(facility);
		// Start setting up cart etc
		List<Container> containers = facility.getContainers();
		//Make sure we have 4 orders/containers
		Assert.assertEquals(5, containers.size());

		PickSimulator picker = new PickSimulator(this, cheGuid1);

		picker.login("Picker #1");
		picker.setupOrderIdAsContainer("a6", "6");

		//Check that container show last 2 digits of container id
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayValue((byte) 6),
			PosControllerInstr.DEFAULT_POSITION_ASSIGNED_CODE);
		Assert.assertFalse(picker.hasLastSentInstruction((byte) 2));

		this.getPersistenceService().commitTenantTransaction();

		// note no transaction active in test thread here - transactions will be opened by server during simulation

		picker.scanCommand("START");

		//Check State Make sure we do not hit REVIEW
		picker.waitForCheState(CheStateEnum.LOCATION_SELECT, 3000);

		//Case 1: 1 good pick no flashing
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayValue((byte) 6).intValue(), 1);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayDutyCycle((byte) 6), PosControllerInstr.BRIGHT_DUTYCYCLE);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayFreq((byte) 6), PosControllerInstr.SOLID_FREQ);
		//Make sure other position is null
		Assert.assertNull(picker.getLastSentPositionControllerDisplayValue((byte) 2));

		//Make this order complete
		picker.scanLocation("D303");
		picker.waitForCheState(CheStateEnum.DO_PICK, 3000);
		picker.pick(6, 1);
		picker.waitForCheState(CheStateEnum.PICK_COMPLETE, 3000);

		//Reset Picker
		picker.logout();
		picker.login("Picker #1");

		//Continue setting up containers with bad counts
		picker.setupOrderIdAsContainer("a1111", "1");
		picker.setupOrderIdAsContainer("22222", "2");
		picker.setupOrderIdAsContainer("33333", "3"); //missing order id
		picker.setupOrderIdAsContainer("44444", "4");
		picker.setupOrderIdAsContainer("9", "5");
		picker.setupOrderIdAsContainer("a6", "6");

		//Quickly check assigment feedback
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayValue((byte) 1),
			PosControllerInstr.DEFAULT_POSITION_ASSIGNED_CODE);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayValue((byte) 2), Byte.valueOf("22"));
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayValue((byte) 3), Byte.valueOf("33"));
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayValue((byte) 4), Byte.valueOf("44"));
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayValue((byte) 6),
			PosControllerInstr.DEFAULT_POSITION_ASSIGNED_CODE);

		//Pos 5 should have "09"
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayValue((byte) 5), PosControllerInstr.BITENCODED_SEGMENTS_CODE);
		Assert.assertEquals(picker.getLastSentPositionControllerMinQty((byte) 5).byteValue(),
			PosControllerInstr.BITENCODED_DIGITS[9]);
		Assert.assertEquals(picker.getLastSentPositionControllerMaxQty((byte) 5).byteValue(),
			PosControllerInstr.BITENCODED_DIGITS[0]);

		picker.scanCommand("START");

		//Check State
		picker.waitForCheState(CheStateEnum.LOCATION_SELECT_REVIEW, 3000);

		//Check Screens
		//Case 1: 2 good picks - solid , bright
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayValue((byte) 1).intValue(), 2);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayDutyCycle((byte) 1), PosControllerInstr.BRIGHT_DUTYCYCLE);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayFreq((byte) 1), PosControllerInstr.SOLID_FREQ);

		//Case 2: 1 good pick flashing, bright due to immediate short
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayValue((byte) 2).intValue(), 1);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayDutyCycle((byte) 2), PosControllerInstr.BRIGHT_DUTYCYCLE);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayFreq((byte) 2), PosControllerInstr.BLINK_FREQ);

		//Case 3: Unknown order id so display dim, solid, --
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayValue((byte) 3), PosControllerInstr.BITENCODED_SEGMENTS_CODE);
		Assert.assertEquals(picker.getLastSentPositionControllerMinQty((byte) 3), PosControllerInstr.BITENCODED_LED_DASH);
		Assert.assertEquals(picker.getLastSentPositionControllerMaxQty((byte) 3), PosControllerInstr.BITENCODED_LED_DASH);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayDutyCycle((byte) 3), PosControllerInstr.DIM_DUTYCYCLE);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayFreq((byte) 3), PosControllerInstr.SOLID_FREQ);

		//Case 4: One immediate short so display dim, solid --
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayValue((byte) 4), PosControllerInstr.BITENCODED_SEGMENTS_CODE);
		Assert.assertEquals(picker.getLastSentPositionControllerMinQty((byte) 4), PosControllerInstr.BITENCODED_LED_DASH);
		Assert.assertEquals(picker.getLastSentPositionControllerMaxQty((byte) 4), PosControllerInstr.BITENCODED_LED_DASH);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayDutyCycle((byte) 4), PosControllerInstr.DIM_DUTYCYCLE);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayFreq((byte) 4), PosControllerInstr.SOLID_FREQ);

		//Case 5: Each pick on a case pick which is an immediate short display solid, bright 1
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayValue((byte) 5).intValue(), 1);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayDutyCycle((byte) 5), PosControllerInstr.BRIGHT_DUTYCYCLE);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayFreq((byte) 5), PosControllerInstr.SOLID_FREQ);

		//Case 6: Already complete so display dim, solid, "oc"
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayValue((byte) 6), PosControllerInstr.BITENCODED_SEGMENTS_CODE);
		Assert.assertEquals(picker.getLastSentPositionControllerMinQty((byte) 6), PosControllerInstr.BITENCODED_LED_C);
		Assert.assertEquals(picker.getLastSentPositionControllerMaxQty((byte) 6), PosControllerInstr.BITENCODED_LED_O);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayDutyCycle((byte) 6), PosControllerInstr.DIM_DUTYCYCLE);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayFreq((byte) 6), PosControllerInstr.SOLID_FREQ);

		//Scan location to make sure position controller does not show counts anymore
		picker.scanLocation("D301");

		picker.waitForCheState(CheStateEnum.DO_PICK, 3000);

		//Make sure all position controllers are cleared - except for case 3,4,6 since they are zero and 2 since that is the first task
		Assert.assertNull(picker.getLastSentPositionControllerDisplayValue((byte) 1));
		Assert.assertNull(picker.getLastSentPositionControllerDisplayValue((byte) 5));

		//Make sure position 2 shows the proper item count for picking
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayValue((byte) 2).intValue(), 1);

		//Case 3: Unknown order id so display dim, solid, --
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayValue((byte) 3), PosControllerInstr.BITENCODED_SEGMENTS_CODE);
		Assert.assertEquals(picker.getLastSentPositionControllerMinQty((byte) 3), PosControllerInstr.BITENCODED_LED_DASH);
		Assert.assertEquals(picker.getLastSentPositionControllerMaxQty((byte) 3), PosControllerInstr.BITENCODED_LED_DASH);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayDutyCycle((byte) 3), PosControllerInstr.DIM_DUTYCYCLE);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayFreq((byte) 3), PosControllerInstr.SOLID_FREQ);

		//Case 4: One immediate short so display dim, solid --
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayValue((byte) 4), PosControllerInstr.BITENCODED_SEGMENTS_CODE);
		Assert.assertEquals(picker.getLastSentPositionControllerMinQty((byte) 4), PosControllerInstr.BITENCODED_LED_DASH);
		Assert.assertEquals(picker.getLastSentPositionControllerMaxQty((byte) 4), PosControllerInstr.BITENCODED_LED_DASH);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayDutyCycle((byte) 4), PosControllerInstr.DIM_DUTYCYCLE);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayFreq((byte) 4), PosControllerInstr.SOLID_FREQ);

		//Case 6: Already complete so display dim, solid oc
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayValue((byte) 6), PosControllerInstr.BITENCODED_SEGMENTS_CODE);
		Assert.assertEquals(picker.getLastSentPositionControllerMinQty((byte) 6), PosControllerInstr.BITENCODED_LED_C);
		Assert.assertEquals(picker.getLastSentPositionControllerMaxQty((byte) 6), PosControllerInstr.BITENCODED_LED_O);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayDutyCycle((byte) 6), PosControllerInstr.DIM_DUTYCYCLE);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayFreq((byte) 6), PosControllerInstr.SOLID_FREQ);

		this.getPersistenceService().beginTenantTransaction();
		mPropertyService.restoreHKDefaults(facility);
		this.getPersistenceService().commitTenantTransaction();
	}

	//DEV-603 test case
	@Test
	public final void testCartSetupFeedbackWithPreviouslyShortedWI() throws IOException {

		// set up data for pick scenario
		this.getPersistenceService().beginTenantTransaction();

		Facility facility = setUpSimpleNoSlotFacility();
		// We are going to put everything in A1 and A2 since they are on the same path.
		//Item 5 is out of stock and item 6 is case only.
		String csvString = "itemId,locationId,description,quantity,uom,inventoryDate,cmFromLeft\r\n" //
				+ "1,D301,Test Item 1,6,EA,6/25/14 12:00,135\r\n" //
				+ "2,D302,Test Item 2,6,EA,6/25/14 12:00,8\r\n" //
				+ "3,D303,Test Item 3,6,EA,6/25/14 12:00,55\r\n" //
				+ "4,D401,Test Item 4,1,EA,6/25/14 12:00,66\r\n" //
				+ "6,D403,Test Item 6,1,EA,6/25/14 12:00,3\r\n";//

		byte[] csvArray = csvString.getBytes();

		ByteArrayInputStream stream = new ByteArrayInputStream(csvArray);
		InputStreamReader reader = new InputStreamReader(stream);

		Timestamp ediProcessTime = new Timestamp(System.currentTimeMillis());
		ICsvInventoryImporter importer = createInventoryImporter();
		importer.importSlottedInventoryFromCsvStream(reader, facility, ediProcessTime);
		this.getPersistenceService().commitTenantTransaction();

		this.getPersistenceService().beginTenantTransaction();
		facility = Facility.DAO.reload(facility);
		// Outbound order. No group. Using 5 digit order number and preassigned container number.
		// Order 1 has two items in stock (Item 1 and Item 2)
		String csvString2 = "orderGroupId,shipmentId,customerId,preAssignedContainerId,orderId,itemId,description,quantity,uom,orderDate,dueDate,workSequence"
				+ "\r\n1,USF314,COSTCO,1,1,1,Test Item 1,2,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,1,1,2,Test Item 2,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0";

		byte[] csvArray2 = csvString2.getBytes();

		ByteArrayInputStream stream2 = new ByteArrayInputStream(csvArray2);
		InputStreamReader reader2 = new InputStreamReader(stream2);

		Timestamp ediProcessTime2 = new Timestamp(System.currentTimeMillis());
		ICsvOrderImporter importer2 = createOrderImporter();
		importer2.importOrdersFromCsvStream(reader2, facility, ediProcessTime2);

		mPropertyService.turnOffHK(facility);
		this.getPersistenceService().commitTenantTransaction();

		this.getPersistenceService().beginTenantTransaction();
		facility = Facility.DAO.reload(facility);

		PickSimulator picker = new PickSimulator(this, cheGuid1);

		//SETUP
		picker.login("Picker #1");
		picker.setupOrderIdAsContainer("1", "1");
		picker.scanCommand("START");
		picker.waitForCheState(CheStateEnum.LOCATION_SELECT, 3000);
		picker.scanLocation("D301");
		picker.waitForCheState(CheStateEnum.DO_PICK, 3000);

		//SHORT FIRST ITEM
		picker.scanCommand("SHORT");
		picker.waitForCheState(CheStateEnum.SHORT_PICK, 3000);
		picker.buttonPress(1, 1);
		picker.scanCommand("YES");
		picker.waitForCheState(CheStateEnum.DO_PICK, 3000);

		//SETUP AGAIN
		picker.logout();
		picker.login("Picker #1");
		picker.setupOrderIdAsContainer("1", "1");
		picker.scanCommand("START");
		picker.waitForCheState(CheStateEnum.LOCATION_SELECT, 3000);
		picker.scanLocation("D301");
		picker.waitForCheState(CheStateEnum.DO_PICK, 3000);

		//Make sure we have a bright 1 on the poscon
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayValue((byte) 1).intValue(), 1);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayDutyCycle((byte) 1), PosControllerInstr.BRIGHT_DUTYCYCLE);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayFreq((byte) 1), PosControllerInstr.SOLID_FREQ);

		//COMPLETE FIRST ITEM
		picker.pick(1, 1);
		picker.waitForCheState(CheStateEnum.DO_PICK, 3000);

		//SETUP AGAIN
		picker.logout();
		picker.login("Picker #1");
		picker.setupOrderIdAsContainer("1", "1");
		picker.scanCommand("START");
		picker.waitForCheState(CheStateEnum.LOCATION_SELECT, 3000);
		picker.scanLocation("D301");
		picker.waitForCheState(CheStateEnum.DO_PICK, 3000);

		mPropertyService.restoreHKDefaults(facility);
		this.getPersistenceService().commitTenantTransaction();
	}

	@Test
	public final void testCartRunFeedback() throws IOException {
		// Test cases:
		// 1. One good plans for position 1.
		// 2. One good plan for position 2 and and immediate short.
		// 3. Unknown order number for position 3.
		// 4. Only an immediate short for position 4.
		// 5. One good plans for position 5.
		// set up data for pick scenario

		// set up data for pick scenario
		this.getPersistenceService().beginTenantTransaction();
		Facility facility = setUpSimpleNoSlotFacility();
		this.getPersistenceService().commitTenantTransaction();

		this.getPersistenceService().beginTenantTransaction();
		UUID facId = facility.getPersistentId();
		facility = Facility.DAO.reload(facility);
		// We are going to put everything in A1 and A2 since they are on the same path.
		//Item 5 is out of stock and item 6 is case only.
		String csvString = "itemId,locationId,description,quantity,uom,inventoryDate,cmFromLeft\r\n" //
				+ "1,D301,Test Item 1,6,EA,6/25/14 12:00,135\r\n" //
				+ "2,D302,Test Item 2,6,EA,6/25/14 12:00,8\r\n" //
				+ "3,D303,Test Item 3,6,EA,6/25/14 12:00,55\r\n" //
				+ "4,D401,Test Item 4,6,EA,6/25/14 12:00,66\r\n";

		byte[] csvArray = csvString.getBytes();

		ByteArrayInputStream stream = new ByteArrayInputStream(csvArray);
		InputStreamReader reader = new InputStreamReader(stream);

		Timestamp ediProcessTime = new Timestamp(System.currentTimeMillis());
		ICsvInventoryImporter importer = createInventoryImporter();
		importer.importSlottedInventoryFromCsvStream(reader, facility, ediProcessTime);
		this.getPersistenceService().commitTenantTransaction();

		this.getPersistenceService().beginTenantTransaction();
		facility = Facility.DAO.reload(facility);

		String csvString2 = "orderGroupId,shipmentId,customerId,preAssignedContainerId,orderId,itemId,description,quantity,uom,orderDate,dueDate,workSequence"
				+ "\r\n1,USF314,COSTCO,11111,11111,1,Test Item 1,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,22222,22222,2,Test Item 2,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,44444,44444,5,Test Item 5,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,55555,55555,2,Test Item 2,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0";

		byte[] csvArray2 = csvString2.getBytes();

		ByteArrayInputStream stream2 = new ByteArrayInputStream(csvArray2);
		InputStreamReader reader2 = new InputStreamReader(stream2);

		Timestamp ediProcessTime2 = new Timestamp(System.currentTimeMillis());
		ICsvOrderImporter importer2 = createOrderImporter();
		importer2.importOrdersFromCsvStream(reader2, facility, ediProcessTime2);
		this.getPersistenceService().commitTenantTransaction();

		this.getPersistenceService().beginTenantTransaction();
		facility = Facility.DAO.reload(facility);
		mPropertyService.turnOffHK(facility);
		this.getPersistenceService().commitTenantTransaction();

		// Start setting up cart etc
		this.getPersistenceService().beginTenantTransaction();
		facility = Facility.DAO.reload(facility);
		List<Container> containers = facility.getContainers();
		//Make sure we have 4 orders/containers
		Assert.assertEquals(4, containers.size());

		PickSimulator picker = new PickSimulator(this, cheGuid1);

		picker.login("Picker #1");
		picker.setupOrderIdAsContainer("11111", "1");
		picker.setupOrderIdAsContainer("22222", "2");
		picker.setupOrderIdAsContainer("33333", "3"); //missing order id
		picker.setupOrderIdAsContainer("44444", "4");
		picker.setupOrderIdAsContainer("55555", "5");
		picker.startAndSkipReview("D301", 3000, 3000);

		//Check Screens -- Everything should be clear except the one we are picked #1, #4 immediate short and #3 unknown order id
		Assert.assertNull(picker.getLastSentPositionControllerDisplayValue((byte) 2));
		Assert.assertNull(picker.getLastSentPositionControllerDisplayValue((byte) 5));

		//Case 3: Unknown order id so display dim, solid, --
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayValue((byte) 3), PosControllerInstr.BITENCODED_SEGMENTS_CODE);
		Assert.assertEquals(picker.getLastSentPositionControllerMinQty((byte) 3), PosControllerInstr.BITENCODED_LED_DASH);
		Assert.assertEquals(picker.getLastSentPositionControllerMaxQty((byte) 3), PosControllerInstr.BITENCODED_LED_DASH);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayDutyCycle((byte) 3), PosControllerInstr.DIM_DUTYCYCLE);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayFreq((byte) 3), PosControllerInstr.SOLID_FREQ);

		//Case 4: One immediate short so display dim, solid --
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayValue((byte) 4), PosControllerInstr.BITENCODED_SEGMENTS_CODE);
		Assert.assertEquals(picker.getLastSentPositionControllerMinQty((byte) 4), PosControllerInstr.BITENCODED_LED_DASH);
		Assert.assertEquals(picker.getLastSentPositionControllerMaxQty((byte) 4), PosControllerInstr.BITENCODED_LED_DASH);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayDutyCycle((byte) 4), PosControllerInstr.DIM_DUTYCYCLE);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayFreq((byte) 4), PosControllerInstr.SOLID_FREQ);

		//Make sure position 1 shows the proper item count for picking
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayValue((byte) 1).intValue(), 1);

		picker.pick(1, 1);

		//Check Screens -- #1 it should be done so display solid, dim "oc"
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayValue((byte) 1), PosControllerInstr.BITENCODED_SEGMENTS_CODE);
		Assert.assertEquals(picker.getLastSentPositionControllerMinQty((byte) 1), PosControllerInstr.BITENCODED_LED_C);
		Assert.assertEquals(picker.getLastSentPositionControllerMaxQty((byte) 1), PosControllerInstr.BITENCODED_LED_O);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayDutyCycle((byte) 1), PosControllerInstr.DIM_DUTYCYCLE);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayFreq((byte) 1), PosControllerInstr.SOLID_FREQ);
		//5 should stay null
		Assert.assertNull(picker.getLastSentPositionControllerDisplayValue((byte) 5));

		//Make sure position 2 shows the proper item count for picking
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayValue((byte) 2).intValue(), 1);

		//Case 3: Unknown order id so display dim, solid, --
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayValue((byte) 3), PosControllerInstr.BITENCODED_SEGMENTS_CODE);
		Assert.assertEquals(picker.getLastSentPositionControllerMinQty((byte) 3), PosControllerInstr.BITENCODED_LED_DASH);
		Assert.assertEquals(picker.getLastSentPositionControllerMaxQty((byte) 3), PosControllerInstr.BITENCODED_LED_DASH);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayDutyCycle((byte) 3), PosControllerInstr.DIM_DUTYCYCLE);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayFreq((byte) 3), PosControllerInstr.SOLID_FREQ);

		//Case 4: One immediate short so display dim, solid --
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayValue((byte) 4), PosControllerInstr.BITENCODED_SEGMENTS_CODE);
		Assert.assertEquals(picker.getLastSentPositionControllerMinQty((byte) 4), PosControllerInstr.BITENCODED_LED_DASH);
		Assert.assertEquals(picker.getLastSentPositionControllerMaxQty((byte) 4), PosControllerInstr.BITENCODED_LED_DASH);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayDutyCycle((byte) 4), PosControllerInstr.DIM_DUTYCYCLE);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayFreq((byte) 4), PosControllerInstr.SOLID_FREQ);

		/**
		 * Now we will do a short pick and cancel it and make sure we never lose feedback.
		 * Then will redo the short and confirm it and make sure we keep the feedback
		 */
		picker.scanCommand("SHORT");
		picker.waitForCheState(CheStateEnum.SHORT_PICK, 3000);

		//Check Screens -- #1 it should be done so display solid, dim "oc"
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayValue((byte) 1), PosControllerInstr.BITENCODED_SEGMENTS_CODE);
		Assert.assertEquals(picker.getLastSentPositionControllerMinQty((byte) 1), PosControllerInstr.BITENCODED_LED_C);
		Assert.assertEquals(picker.getLastSentPositionControllerMaxQty((byte) 1), PosControllerInstr.BITENCODED_LED_O);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayDutyCycle((byte) 1), PosControllerInstr.DIM_DUTYCYCLE);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayDutyCycle((byte) 1), PosControllerInstr.DIM_DUTYCYCLE);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayFreq((byte) 1), PosControllerInstr.SOLID_FREQ);

		//5 should stay empty
		Assert.assertNull(picker.getLastSentPositionControllerDisplayValue((byte) 5));

		//Case 3: Unknown order id so display dim, solid, --
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayValue((byte) 3), PosControllerInstr.BITENCODED_SEGMENTS_CODE);
		Assert.assertEquals(picker.getLastSentPositionControllerMinQty((byte) 3), PosControllerInstr.BITENCODED_LED_DASH);
		Assert.assertEquals(picker.getLastSentPositionControllerMaxQty((byte) 3), PosControllerInstr.BITENCODED_LED_DASH);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayDutyCycle((byte) 3), PosControllerInstr.DIM_DUTYCYCLE);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayFreq((byte) 3), PosControllerInstr.SOLID_FREQ);

		//Case 4: One immediate short so display dim, solid --
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayValue((byte) 4), PosControllerInstr.BITENCODED_SEGMENTS_CODE);
		Assert.assertEquals(picker.getLastSentPositionControllerMinQty((byte) 4), PosControllerInstr.BITENCODED_LED_DASH);
		Assert.assertEquals(picker.getLastSentPositionControllerMaxQty((byte) 4), PosControllerInstr.BITENCODED_LED_DASH);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayDutyCycle((byte) 4), PosControllerInstr.DIM_DUTYCYCLE);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayFreq((byte) 4), PosControllerInstr.SOLID_FREQ);

		picker.buttonPress(2, 0);
		picker.waitForCheState(CheStateEnum.SHORT_PICK_CONFIRM, 3000);
		picker.scanCommand("NO");

		//Check Screens -- #1 it should be done so display solid, dim "oc"
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayValue((byte) 1), PosControllerInstr.BITENCODED_SEGMENTS_CODE);
		Assert.assertEquals(picker.getLastSentPositionControllerMinQty((byte) 1), PosControllerInstr.BITENCODED_LED_C);
		Assert.assertEquals(picker.getLastSentPositionControllerMaxQty((byte) 1), PosControllerInstr.BITENCODED_LED_O);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayDutyCycle((byte) 1), PosControllerInstr.DIM_DUTYCYCLE);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayDutyCycle((byte) 1), PosControllerInstr.DIM_DUTYCYCLE);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayFreq((byte) 1), PosControllerInstr.SOLID_FREQ);

		//5 should stay empty
		Assert.assertNull(picker.getLastSentPositionControllerDisplayValue((byte) 5));

		//Make sure position 2 shows the proper item count for picking
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayValue((byte) 2).intValue(), 1);

		//Case 3: Unknown order id so display dim, solid, --
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayValue((byte) 3), PosControllerInstr.BITENCODED_SEGMENTS_CODE);
		Assert.assertEquals(picker.getLastSentPositionControllerMinQty((byte) 3), PosControllerInstr.BITENCODED_LED_DASH);
		Assert.assertEquals(picker.getLastSentPositionControllerMaxQty((byte) 3), PosControllerInstr.BITENCODED_LED_DASH);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayDutyCycle((byte) 3), PosControllerInstr.DIM_DUTYCYCLE);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayFreq((byte) 3), PosControllerInstr.SOLID_FREQ);

		//Case 4: One immediate short so display dim, solid --
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayValue((byte) 4), PosControllerInstr.BITENCODED_SEGMENTS_CODE);
		Assert.assertEquals(picker.getLastSentPositionControllerMinQty((byte) 4), PosControllerInstr.BITENCODED_LED_DASH);
		Assert.assertEquals(picker.getLastSentPositionControllerMaxQty((byte) 4), PosControllerInstr.BITENCODED_LED_DASH);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayDutyCycle((byte) 4), PosControllerInstr.DIM_DUTYCYCLE);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayFreq((byte) 4), PosControllerInstr.SOLID_FREQ);

		picker.scanCommand("SHORT");
		picker.waitForCheState(CheStateEnum.SHORT_PICK, 3000);
		picker.buttonPress(2, 0);
		picker.waitForCheState(CheStateEnum.SHORT_PICK_CONFIRM, 3000);
		picker.scanCommand("YES");
		picker.waitForCheState(CheStateEnum.PICK_COMPLETE, 3000);

		//Check Screens -- #1 it should be done so display solid, dim "oc"
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayValue((byte) 1), PosControllerInstr.BITENCODED_SEGMENTS_CODE);
		Assert.assertEquals(picker.getLastSentPositionControllerMinQty((byte) 1), PosControllerInstr.BITENCODED_LED_C);
		Assert.assertEquals(picker.getLastSentPositionControllerMaxQty((byte) 1), PosControllerInstr.BITENCODED_LED_O);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayDutyCycle((byte) 1), PosControllerInstr.DIM_DUTYCYCLE);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayDutyCycle((byte) 1), PosControllerInstr.DIM_DUTYCYCLE);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayFreq((byte) 1), PosControllerInstr.SOLID_FREQ);

		//5 should now also be shorted ahead so display dim, solid, --
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayValue((byte) 2), PosControllerInstr.BITENCODED_SEGMENTS_CODE);
		Assert.assertEquals(picker.getLastSentPositionControllerMinQty((byte) 2), PosControllerInstr.BITENCODED_LED_DASH);
		Assert.assertEquals(picker.getLastSentPositionControllerMaxQty((byte) 2), PosControllerInstr.BITENCODED_LED_DASH);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayDutyCycle((byte) 2), PosControllerInstr.DIM_DUTYCYCLE);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayFreq((byte) 2), PosControllerInstr.SOLID_FREQ);

		//2 is now shorted so display dim, solid, --
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayValue((byte) 2), PosControllerInstr.BITENCODED_SEGMENTS_CODE);
		Assert.assertEquals(picker.getLastSentPositionControllerMinQty((byte) 2), PosControllerInstr.BITENCODED_LED_DASH);
		Assert.assertEquals(picker.getLastSentPositionControllerMaxQty((byte) 2), PosControllerInstr.BITENCODED_LED_DASH);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayDutyCycle((byte) 2), PosControllerInstr.DIM_DUTYCYCLE);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayFreq((byte) 2), PosControllerInstr.SOLID_FREQ);

		//Case 3: Unknown order id so display dim, solid, --
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayValue((byte) 3), PosControllerInstr.BITENCODED_SEGMENTS_CODE);
		Assert.assertEquals(picker.getLastSentPositionControllerMinQty((byte) 3), PosControllerInstr.BITENCODED_LED_DASH);
		Assert.assertEquals(picker.getLastSentPositionControllerMaxQty((byte) 3), PosControllerInstr.BITENCODED_LED_DASH);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayDutyCycle((byte) 3), PosControllerInstr.DIM_DUTYCYCLE);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayFreq((byte) 3), PosControllerInstr.SOLID_FREQ);

		//Case 4: One immediate short so display dim, solid --
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayValue((byte) 4), PosControllerInstr.BITENCODED_SEGMENTS_CODE);
		Assert.assertEquals(picker.getLastSentPositionControllerMinQty((byte) 4), PosControllerInstr.BITENCODED_LED_DASH);
		Assert.assertEquals(picker.getLastSentPositionControllerMaxQty((byte) 4), PosControllerInstr.BITENCODED_LED_DASH);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayDutyCycle((byte) 4), PosControllerInstr.DIM_DUTYCYCLE);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayFreq((byte) 4), PosControllerInstr.SOLID_FREQ);

		mPropertyService.restoreHKDefaults(facility);

		this.getPersistenceService().commitTenantTransaction();
	}

	@Test
	public void testContainerReassignmentDuringCHESetup() throws IOException {
		this.getPersistenceService().beginTenantTransaction();

		Facility facility = setUpSimpleNoSlotFacility();
		this.getPersistenceService().commitTenantTransaction();

		this.getPersistenceService().beginTenantTransaction();
		facility = Facility.DAO.reload(facility);
		// We are going to put everything in A1 and A2 since they are on the same path.
		//Item 5 is out of stock and item 6 is case only.
		String csvString = "itemId,locationId,description,quantity,uom,inventoryDate,cmFromLeft\r\n" //
				+ "1,D301,Test Item 1,6,EA,6/25/14 12:00,135\r\n" //
				+ "2,D302,Test Item 2,6,EA,6/25/14 12:00,8\r\n" //
				+ "3,D303,Test Item 3,6,EA,6/25/14 12:00,55\r\n" //
				+ "4,D401,Test Item 4,6,EA,6/25/14 12:00,66\r\n";

		byte[] csvArray = csvString.getBytes();

		ByteArrayInputStream stream = new ByteArrayInputStream(csvArray);
		InputStreamReader reader = new InputStreamReader(stream);

		Timestamp ediProcessTime = new Timestamp(System.currentTimeMillis());
		ICsvInventoryImporter importer = createInventoryImporter();
		importer.importSlottedInventoryFromCsvStream(reader, facility, ediProcessTime);
		this.getPersistenceService().commitTenantTransaction();

		this.getPersistenceService().beginTenantTransaction();
		facility = Facility.DAO.reload(facility);

		String csvString2 = "orderGroupId,shipmentId,customerId,preAssignedContainerId,orderId,itemId,description,quantity,uom,orderDate,dueDate,workSequence"
				+ "\r\n1,USF314,COSTCO,11111,11111,1,Test Item 1,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,22222,22222,2,Test Item 2,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,44444,44444,5,Test Item 5,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,55555,55555,2,Test Item 2,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0";

		byte[] csvArray2 = csvString2.getBytes();

		ByteArrayInputStream stream2 = new ByteArrayInputStream(csvArray2);
		InputStreamReader reader2 = new InputStreamReader(stream2);

		Timestamp ediProcessTime2 = new Timestamp(System.currentTimeMillis());
		ICsvOrderImporter importer2 = createOrderImporter();
		importer2.importOrdersFromCsvStream(reader2, facility, ediProcessTime2);
		this.getPersistenceService().commitTenantTransaction();

		this.getPersistenceService().beginTenantTransaction();
		facility = Facility.DAO.reload(facility);
		mPropertyService.turnOffHK(facility);
		this.getPersistenceService().commitTenantTransaction();

		// Start setting up cart etc
		PickSimulator picker = new PickSimulator(this, cheGuid1);

		picker.login("Picker #1");
		picker.setupOrderIdAsContainer("11111", "1");
		picker.waitForCheState(CheStateEnum.CONTAINER_SELECT, 3000);

		//Check Screens
		Assert.assertTrue(picker.getLastSentPositionControllerDisplayValue((byte) 1) == (byte) 11);
		Assert.assertNull(picker.getLastSentPositionControllerDisplayValue((byte) 2));

		picker.setupOrderIdAsContainer("11111", "2");
		picker.waitForCheState(CheStateEnum.CONTAINER_SELECT, 3000);

		//Check Screens
		Assert.assertNull(picker.getLastSentPositionControllerDisplayValue((byte) 1));
		Assert.assertTrue(picker.getLastSentPositionControllerDisplayValue((byte) 2) == (byte) 11);

		this.getPersistenceService().beginTenantTransaction();
		facility = Facility.DAO.reload(facility);
		mPropertyService.restoreHKDefaults(facility);
		this.getPersistenceService().commitTenantTransaction();

	}

	@Test
	public void testCheSetupErrors() throws IOException {
		this.getPersistenceService().beginTenantTransaction();

		Facility facility = setUpSimpleNoSlotFacility();
		this.getPersistenceService().commitTenantTransaction();

		this.getPersistenceService().beginTenantTransaction();
		facility = Facility.DAO.reload(facility);
		// We are going to put everything in A1 and A2 since they are on the same path.
		//Item 5 is out of stock and item 6 is case only.
		String csvString = "itemId,locationId,description,quantity,uom,inventoryDate,cmFromLeft\r\n" //
				+ "1,D301,Test Item 1,6,EA,6/25/14 12:00,135\r\n" //
				+ "2,D302,Test Item 2,6,EA,6/25/14 12:00,8\r\n" //
				+ "3,D303,Test Item 3,6,EA,6/25/14 12:00,55\r\n" //
				+ "4,D401,Test Item 4,6,EA,6/25/14 12:00,66\r\n";

		byte[] csvArray = csvString.getBytes();

		ByteArrayInputStream stream = new ByteArrayInputStream(csvArray);
		InputStreamReader reader = new InputStreamReader(stream);

		Timestamp ediProcessTime = new Timestamp(System.currentTimeMillis());
		ICsvInventoryImporter importer = createInventoryImporter();
		importer.importSlottedInventoryFromCsvStream(reader, facility, ediProcessTime);
		this.getPersistenceService().commitTenantTransaction();

		this.getPersistenceService().beginTenantTransaction();
		facility = Facility.DAO.reload(facility);

		String csvString2 = "orderGroupId,shipmentId,customerId,preAssignedContainerId,orderId,itemId,description,quantity,uom,orderDate,dueDate,workSequence"
				+ "\r\n1,USF314,COSTCO,11111,11111,1,Test Item 1,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,22222,22222,2,Test Item 2,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,44444,44444,5,Test Item 5,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,55555,55555,2,Test Item 2,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0";

		byte[] csvArray2 = csvString2.getBytes();

		ByteArrayInputStream stream2 = new ByteArrayInputStream(csvArray2);
		InputStreamReader reader2 = new InputStreamReader(stream2);

		Timestamp ediProcessTime2 = new Timestamp(System.currentTimeMillis());
		ICsvOrderImporter importer2 = createOrderImporter();
		importer2.importOrdersFromCsvStream(reader2, facility, ediProcessTime2);
		this.getPersistenceService().commitTenantTransaction();

		this.getPersistenceService().beginTenantTransaction();
		facility = Facility.DAO.reload(facility);

		mPropertyService.turnOffHK(facility);
		this.getPersistenceService().commitTenantTransaction();

		// Start setting up cart etc
		this.getPersistenceService().beginTenantTransaction();

		PickSimulator picker = new PickSimulator(this, cheGuid1);

		picker.login("Picker #1");

		//CASE 1: Scan 2 containers in a row
		picker.setupOrderIdAsContainer("11111", "1");
		picker.scanOrderId("22222");
		picker.waitForCheState(CheStateEnum.CONTAINER_POSITION, 1000);
		picker.scanOrderId("44444");
		picker.waitForCheState(CheStateEnum.CONTAINER_POSITION_INVALID, 1000);

		//Make sure we got an error
		Assert.assertTrue(picker.getLastSentPositionControllerDisplayValue((byte) 1) == PosControllerInstr.ERROR_CODE_QTY);
		Assert.assertTrue(picker.getLastSentPositionControllerDisplayValue((byte) 2) == PosControllerInstr.ERROR_CODE_QTY);
		Assert.assertTrue(picker.getLastSentPositionControllerDisplayValue((byte) 3) == PosControllerInstr.ERROR_CODE_QTY);

		//Make sure CLEAR_ERROR gets us out
		picker.scanCommand("CLEAR");
		picker.waitForCheState(CheStateEnum.CONTAINER_SELECT, 3000);
		Assert.assertTrue(picker.getLastSentPositionControllerDisplayValue((byte) 1) == Byte.valueOf("11"));
		Assert.assertNull(picker.getLastSentPositionControllerDisplayValue((byte) 2));
		Assert.assertNull(picker.getLastSentPositionControllerDisplayValue((byte) 3));

		//Reset
		picker.logout();
		picker.login("Picker #1");

		//CASE 2: Scan 2 positions in a row
		picker.setupOrderIdAsContainer("11111", "1");
		picker.scanOrderId("22222");
		picker.waitForCheState(CheStateEnum.CONTAINER_POSITION, 3000);
		picker.scanPosition("2");
		picker.waitForCheState(CheStateEnum.CONTAINER_SELECT, 3000);
		picker.scanPosition("3");
		picker.waitForCheState(CheStateEnum.CONTAINER_SELECTION_INVALID, 3000);

		//Make sure we got an error
		Assert.assertTrue(picker.getLastSentPositionControllerDisplayValue((byte) 1) == PosControllerInstr.ERROR_CODE_QTY);
		Assert.assertTrue(picker.getLastSentPositionControllerDisplayValue((byte) 2) == PosControllerInstr.ERROR_CODE_QTY);
		Assert.assertTrue(picker.getLastSentPositionControllerDisplayValue((byte) 3) == PosControllerInstr.ERROR_CODE_QTY);

		//Make sure CLEAR_ERROR gets us out
		picker.scanCommand("CLEAR");
		picker.waitForCheState(CheStateEnum.CONTAINER_SELECT, 3000);
		Assert.assertTrue(picker.getLastSentPositionControllerDisplayValue((byte) 1) == Byte.valueOf("11"));
		Assert.assertTrue(picker.getLastSentPositionControllerDisplayValue((byte) 2) == Byte.valueOf("22"));
		Assert.assertNull(picker.getLastSentPositionControllerDisplayValue((byte) 3));

		//CASE 3: SCAN A TAKEN POSITION
		picker.setupOrderIdAsContainer("11111", "1");
		picker.scanOrderId("22222");
		picker.waitForCheState(CheStateEnum.CONTAINER_POSITION, 3000);
		picker.scanPosition("1");
		picker.waitForCheState(CheStateEnum.CONTAINER_POSITION_IN_USE, 3000);

		//Make sure we got an error
		Assert.assertTrue(picker.getLastSentPositionControllerDisplayValue((byte) 1) == PosControllerInstr.ERROR_CODE_QTY);
		Assert.assertTrue(picker.getLastSentPositionControllerDisplayValue((byte) 2) == PosControllerInstr.ERROR_CODE_QTY);
		Assert.assertTrue(picker.getLastSentPositionControllerDisplayValue((byte) 3) == PosControllerInstr.ERROR_CODE_QTY);

		//Make sure CLEAR_ERROR gets us out
		picker.scanCommand("CLEAR");
		picker.waitForCheState(CheStateEnum.CONTAINER_SELECT, 3000);
		Assert.assertTrue(picker.getLastSentPositionControllerDisplayValue((byte) 1) == Byte.valueOf("11"));
		Assert.assertNull(picker.getLastSentPositionControllerDisplayValue((byte) 2));
		Assert.assertNull(picker.getLastSentPositionControllerDisplayValue((byte) 3));

		//CASE 4: SCAN START WORK AFTER CONTAINER W/ NO POSITION
		picker.setupOrderIdAsContainer("11111", "1");
		picker.scanOrderId("22222");
		picker.waitForCheState(CheStateEnum.CONTAINER_POSITION, 3000);
		picker.scanCommand("START");
		picker.waitForCheState(CheStateEnum.CONTAINER_POSITION_INVALID, 3000);

		//Make sure we got an error
		Assert.assertTrue(picker.getLastSentPositionControllerDisplayValue((byte) 1) == PosControllerInstr.ERROR_CODE_QTY);
		Assert.assertTrue(picker.getLastSentPositionControllerDisplayValue((byte) 2) == PosControllerInstr.ERROR_CODE_QTY);
		Assert.assertTrue(picker.getLastSentPositionControllerDisplayValue((byte) 3) == PosControllerInstr.ERROR_CODE_QTY);

		//Make sure scanning something random doesn;t change the state
		picker.scanCommand("START");
		picker.waitForCheState(CheStateEnum.CONTAINER_POSITION_INVALID, 3000);

		//Make sure we still got an error codes
		Assert.assertTrue(picker.getLastSentPositionControllerDisplayValue((byte) 1) == PosControllerInstr.ERROR_CODE_QTY);
		Assert.assertTrue(picker.getLastSentPositionControllerDisplayValue((byte) 2) == PosControllerInstr.ERROR_CODE_QTY);
		Assert.assertTrue(picker.getLastSentPositionControllerDisplayValue((byte) 3) == PosControllerInstr.ERROR_CODE_QTY);

		//Make sure CLEAR_ERROR gets us out
		picker.scanCommand("CLEAR");
		picker.waitForCheState(CheStateEnum.CONTAINER_SELECT, 3000);
		Assert.assertTrue(picker.getLastSentPositionControllerDisplayValue((byte) 1) == Byte.valueOf("11"));
		Assert.assertNull(picker.getLastSentPositionControllerDisplayValue((byte) 2));
		Assert.assertNull(picker.getLastSentPositionControllerDisplayValue((byte) 3));

		//Make sure CLEAR_ERROR again does nothing
		picker.scanCommand("CLEAR");
		picker.waitForCheState(CheStateEnum.CONTAINER_SELECT, 3000);
		Assert.assertTrue(picker.getLastSentPositionControllerDisplayValue((byte) 1) == Byte.valueOf("11"));
		Assert.assertNull(picker.getLastSentPositionControllerDisplayValue((byte) 2));
		Assert.assertNull(picker.getLastSentPositionControllerDisplayValue((byte) 3));

		mPropertyService.restoreHKDefaults(facility);

		this.getPersistenceService().commitTenantTransaction();
	}

	@Test
	public final void testScanNewLocation() throws IOException {
		// set up data for pick scenario
		this.getPersistenceService().beginTenantTransaction();

		Facility facility = setUpSimpleNoSlotFacility();
		this.getPersistenceService().commitTenantTransaction();

		this.getPersistenceService().beginTenantTransaction();
		facility = Facility.DAO.reload(facility);
		String csvString = "itemId,locationId,description,quantity,uom,inventoryDate,cmFromLeft\r\n" //
				+ "1,D301,Test Item 1,6,EA,6/25/14 12:00,135\r\n" //
				+ "2,D302,Test Item 2,6,EA,6/25/14 12:00,8\r\n" //
				+ "3,D303,Test Item 3,6,EA,6/25/14 12:00,66\r\n";

		byte[] csvArray = csvString.getBytes();

		ByteArrayInputStream stream = new ByteArrayInputStream(csvArray);
		InputStreamReader reader = new InputStreamReader(stream);

		Timestamp ediProcessTime = new Timestamp(System.currentTimeMillis());
		ICsvInventoryImporter importer = createInventoryImporter();
		importer.importSlottedInventoryFromCsvStream(reader, facility, ediProcessTime);
		this.getPersistenceService().commitTenantTransaction();

		this.getPersistenceService().beginTenantTransaction();
		facility = Facility.DAO.reload(facility);

		String csvString2 = "orderGroupId,shipmentId,customerId,preAssignedContainerId,orderId,itemId,description,quantity,uom,orderDate,dueDate,workSequence"
				+ "\r\n1,USF314,COSTCO,1,1,1,Test Item 1,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,2,2,2,Test Item 2,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,3,3,3,Test Item 3,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0";

		byte[] csvArray2 = csvString2.getBytes();

		ByteArrayInputStream stream2 = new ByteArrayInputStream(csvArray2);
		InputStreamReader reader2 = new InputStreamReader(stream2);

		Timestamp ediProcessTime2 = new Timestamp(System.currentTimeMillis());
		ICsvOrderImporter importer2 = createOrderImporter();
		importer2.importOrdersFromCsvStream(reader2, facility, ediProcessTime2);

		this.getPersistenceService().commitTenantTransaction();

		// Start setting up cart etc
		this.getPersistenceService().beginTenantTransaction();
		PickSimulator picker = new PickSimulator(this, cheGuid1);

		picker.login("Picker #1");
		picker.setupOrderIdAsContainer("1", "1");
		picker.setupOrderIdAsContainer("2", "2");
		picker.setupOrderIdAsContainer("3", "3");
		picker.scanCommand("START");
		picker.waitForCheState(CheStateEnum.LOCATION_SELECT, 3000);

		//Start at item 2
		picker.scanLocation("D302");
		picker.waitForCheState(CheStateEnum.DO_PICK, 3000);

		List<WorkInstruction> wiList = picker.getAllPicksList();

		//Check Total WI size
		assertTrue(wiList.size() == 5);
		//Check each WI
		assertEquals(wiList.get(0).getItemId(), "2");
		assertEquals(wiList.get(1).getType(), WorkInstructionTypeEnum.HK_BAYCOMPLETE);
		assertEquals(wiList.get(2).getItemId(), "1");
		assertEquals(wiList.get(3).getType(), WorkInstructionTypeEnum.HK_BAYCOMPLETE);
		assertEquals(wiList.get(4).getItemId(), "3");

		//Scan at item 1
		picker.scanLocation("D301");
		picker.waitForCheState(CheStateEnum.DO_PICK, 3000);

		wiList = picker.getAllPicksList();

		//Check Total WI size
		assertTrue(wiList.size() == 5);

		//Check each WI
		assertEquals(wiList.get(0).getItemId(), "1");
		assertEquals(wiList.get(1).getType(), WorkInstructionTypeEnum.HK_BAYCOMPLETE);
		assertEquals(wiList.get(2).getItemId(), "2");
		assertEquals(wiList.get(3).getType(), WorkInstructionTypeEnum.HK_BAYCOMPLETE);
		assertEquals(wiList.get(4).getItemId(), "3");

		//Repeat the test except now we do a pick
		picker.pick(1, 1);
		picker.waitForCheState(CheStateEnum.DO_PICK, 5000);

		//Rescan location
		//Scan at item 2
		picker.scanLocation("D302");
		picker.waitForCheState(CheStateEnum.DO_PICK, 3000);

		wiList = picker.getAllPicksList();

		//Check Total WI size -- should only have 3 as we just manually did the BC HK Instruction
		assertTrue(wiList.size() == 3);

		//Check each WI
		assertEquals(wiList.get(0).getItemId(), "2");
		assertEquals(wiList.get(1).getType(), WorkInstructionTypeEnum.HK_BAYCOMPLETE);
		assertEquals(wiList.get(2).getItemId(), "3");

		this.persistenceService.commitTenantTransaction();
	}

	private void assertWIColor(WorkInstruction wi, Che che) {
		List<LedCmdGroup> cmdGroups = LedCmdGroupSerializer.deserializeLedCmdString(wi.getLedCmdStream());
		Assert.assertEquals(1, cmdGroups.size());
		ColorEnum wiColor = cmdGroups.get(0).getLedSampleList().get(0).getColor();
		ColorEnum cheColor = che.getColor();
		Assert.assertEquals(cheColor, wiColor);

	}

}
