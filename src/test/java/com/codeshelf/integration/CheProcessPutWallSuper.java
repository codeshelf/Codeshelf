package com.codeshelf.integration;

import java.io.IOException;
import java.util.List;

import org.junit.Assert;

import com.codeshelf.flyweight.command.NetGuid;
import com.codeshelf.model.DeviceType;
import com.codeshelf.model.WorkInstructionSequencerType;
import com.codeshelf.model.domain.Aisle;
import com.codeshelf.model.domain.CodeshelfNetwork;
import com.codeshelf.model.domain.DomainObjectProperty;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.ItemMaster;
import com.codeshelf.model.domain.LedController;
import com.codeshelf.model.domain.Location;
import com.codeshelf.model.domain.OrderHeader;
import com.codeshelf.model.domain.OrderLocation;
import com.codeshelf.model.domain.Path;
import com.codeshelf.model.domain.PathSegment;
import com.codeshelf.testframework.ServerTest;

public class CheProcessPutWallSuper extends ServerTest {
	protected String	CONTROLLER_1_ID	= "00001881";
	protected String	CONTROLLER_2_ID	= "00001882";
	protected String	CONTROLLER_3_ID	= "00001883";
	protected String	CONTROLLER_4_ID	= "00001884";

	/**
	 * The goal is a small version of our model put wall facility. Two fast mover areas on different paths. A slow mover area on different path.
	 * And a put wall on separate path.
	 */
	protected Facility setUpFacilityWithPutWall() throws IOException {
		//Import aisles. A4 is the put wall. No LEDs
		String aislesCsvString = "binType,nominalDomainId,lengthCm,slotsInTier,ledCountInTier,tierFloorCm,controllerLED,anchorX,anchorY,orientXorY,depthCm\n"
				+ "Aisle,A1,,,,,tierB1S1Side,2.85,10,X,20\n"
				+ "Bay,B1,50,,,,,,,,\n"
				+ "Tier,T1,50,4,20,0,,,,,\n"
				+ "Bay,B2,CLONE(B1)\n" //
				+ "Aisle,A2,CLONE(A1),,,,tierB1S1Side,2.85,20,X,20\n"
				+ "Aisle,A3,CLONE(A1),,,,tierB1S1Side,2.85,60,X,20\n"
				+ "Aisle,A4,,,,,tierB1S1Side,20,20,X,20\n" + "Bay,B1,50,,,,,,,,\n"//
				+ "Tier,T1,50,4,0,0,,,,,\n"//
				+ "Bay,B2,CLONE(B1)\n"; //
		importAislesData(getFacility(), aislesCsvString);

		// Get the aisles
		Aisle aisle1 = Aisle.staticGetDao().findByDomainId(getFacility(), "A1");
		Aisle aisle2 = Aisle.staticGetDao().findByDomainId(getFacility(), "A2");
		Aisle aisle3 = Aisle.staticGetDao().findByDomainId(getFacility(), "A3");
		Aisle aisle4 = Aisle.staticGetDao().findByDomainId(getFacility(), "A4");
		Assert.assertNotNull(aisle1);

		//Make separate paths and asssign to aisle
		Path path1 = createPathForTest(getFacility());
		PathSegment segment0 = addPathSegmentForTest(path1, 0, 3d, 6d, 5d, 6d);
		String persistStr = segment0.getPersistentId().toString();
		aisle1.associatePathSegment(persistStr);

		Path path2 = createPathForTest(getFacility());
		segment0 = addPathSegmentForTest(path2, 0, 3d, 16d, 5d, 16d);
		persistStr = segment0.getPersistentId().toString();
		aisle2.associatePathSegment(persistStr);

		Path path3 = createPathForTest(getFacility());
		segment0 = addPathSegmentForTest(path3, 0, 3d, 36d, 5d, 36d);
		persistStr = segment0.getPersistentId().toString();
		aisle3.associatePathSegment(persistStr);

		Path path4 = createPathForTest(getFacility());
		segment0 = addPathSegmentForTest(path4, 0, 15d, 6d, 20d, 6d);
		persistStr = segment0.getPersistentId().toString();
		aisle4.associatePathSegment(persistStr);

		aisle4.togglePutWallLocation();
		Assert.assertTrue(aisle4.isPutWallLocation());

		//Import location aliases
		// A1 and A2 are fast mover blocks. F11-F18 and F21-F28
		// A3 is slow mover. S11-S18
		// A4 is put wall. P11-P18, but with bays having alias names WALL1 and WALL2
		String csvLocationAliases = "mappedLocationId,locationAlias\n" //
				+ "A1.B1.T1.S1,F11\n"//
				+ "A1.B1.T1.S2,F12\n"//
				+ "A1.B1.T1.S3,F13\n"// 
				+ "A1.B1.T1.S4,F14\n"//
				+ "A1.B2.T1.S1,F15\n"//
				+ "A1.B2.T1.S2,F16\n"//
				+ "A1.B2.T1.S3,F17\n"//
				+ "A1.B2.T1.S4,F18\n"//
				+ "A2.B1.T1.S1,F21\n"//
				+ "A2.B1.T1.S2,F22\n"//
				+ "A2.B1.T1.S3,F23\n"// 
				+ "A2.B1.T1.S4,F24\n"//
				+ "A2.B2.T1.S1,F25\n"//
				+ "A2.B2.T1.S2,F26\n"//
				+ "A2.B2.T1.S3,F27\n"//
				+ "A2.B2.T1.S4,F28\n"//
				+ "A3.B1.T1.S1,S11\n"//
				+ "A3.B1.T1.S2,S12\n"//
				+ "A3.B1.T1.S3,S13\n"// 
				+ "A3.B1.T1.S4,S14\n"//
				+ "A3.B2.T1.S1,S15\n"//
				+ "A3.B2.T1.S2,S16\n"//
				+ "A3.B2.T1.S3,S17\n"//
				+ "A3.B2.T1.S4,S18\n"//
				+ "A4.B1,WALL1\n"//
				+ "A4.B1.T1.S1,P11\n"//
				+ "A4.B1.T1.S2,P12\n"//
				+ "A4.B1.T1.S3,P13\n"// 
				+ "A4.B1.T1.S4,P14\n"//
				+ "A4.B2,WALL2\n"//
				+ "A4.B2.T1.S1,P15\n"//
				+ "A4.B2.T1.S2,P16\n"//
				+ "A4.B2.T1.S3,P17\n"//
				+ "A4.B2.T1.S4,P18\n";//
		importLocationAliasesData(getFacility(), csvLocationAliases);

		CodeshelfNetwork network = getNetwork();

		//Set up a PosManager
		LedController controller1 = network.findOrCreateLedController("LED1", new NetGuid(CONTROLLER_1_ID));
		controller1.updateFromUI(CONTROLLER_1_ID, "Poscons");
		Assert.assertEquals(DeviceType.Poscons, controller1.getDeviceType());

		//Assign PosCon controller and indices to slots
		Location wall1Tier = getFacility().findSubLocationById("A4.B1.T1");
		controller1.addLocation(wall1Tier);
		wall1Tier.setLedChannel((short) 1);
		wall1Tier.getDao().store(wall1Tier);
		Location wall2Tier = getFacility().findSubLocationById("A4.B2.T1");
		controller1.addLocation(wall2Tier);
		wall2Tier.setLedChannel((short) 1);
		wall2Tier.getDao().store(wall2Tier);

		String[] slotNames = { "P11", "P12", "P13", "P14", "P15", "P16", "P17", "P18" };
		int posconIndex = 1;
		for (String slotName : slotNames) {
			Location slot = getFacility().findSubLocationById(slotName);
			slot.setPosconIndex(posconIndex);
			slot.getDao().store(slot);
			posconIndex += 1;
		}

		//Set up a LED controllers for  the fast and slow movers
		LedController controller2 = network.findOrCreateLedController("LED2", new NetGuid(CONTROLLER_2_ID));
		LedController controller3 = network.findOrCreateLedController("LED2", new NetGuid(CONTROLLER_3_ID));
		LedController controller4 = network.findOrCreateLedController("LED2", new NetGuid(CONTROLLER_4_ID));
		Location aisle = getFacility().findSubLocationById("A1");
		controller2.addLocation(aisle);
		aisle.setLedChannel((short) 1);
		aisle = getFacility().findSubLocationById("A2");
		controller3.addLocation(aisle);
		aisle.setLedChannel((short) 1);
		aisle = getFacility().findSubLocationById("A3");
		controller4.addLocation(aisle);
		aisle.setLedChannel((short) 1);

		// Check our lighting configuration
		Location slot = getFacility().findSubLocationById("P12");
		Assert.assertTrue(slot.isLightablePoscon());
		Assert.assertTrue(slot.isLightable());
		Assert.assertFalse(slot.isLightableAisleController());
		slot = getFacility().findSubLocationById("P17");
		Assert.assertTrue(slot.isLightablePoscon());
		Assert.assertTrue(slot.isLightable());
		Assert.assertFalse(slot.isLightableAisleController());

		slot = getFacility().findSubLocationById("F11");
		Assert.assertFalse(slot.isLightablePoscon());
		Assert.assertTrue(slot.isLightable());
		Assert.assertTrue(slot.isLightableAisleController());
		slot = getFacility().findSubLocationById("F23");
		Assert.assertFalse(slot.isLightablePoscon());
		Assert.assertTrue(slot.isLightable());
		Assert.assertTrue(slot.isLightableAisleController());
		slot = getFacility().findSubLocationById("S17");
		Assert.assertFalse(slot.isLightablePoscon());
		Assert.assertTrue(slot.isLightable());
		Assert.assertTrue(slot.isLightableAisleController());

		propertyService.changePropertyValue(getFacility(),
			DomainObjectProperty.WORKSEQR,
			WorkInstructionSequencerType.BayDistance.toString());

		return getFacility();
	}

	protected void setUpOrders1(Facility inFacility) throws IOException {
		// Outbound orders. No group. Using 5 digit order number and .N detail ID. The preassigned container number matches the order.
		// With preferredLocation. No inventory. All preferredLocations resolve. There is GTIN/UPC for each item.
		// Order 12345 has two items in fast, and one is slow
		// Order 11111 has four items in other fast area, and one is slow
		// Some extra singleton orders just to get to completion states.

		String orderCsvString = "orderGroupId,shipmentId,customerId,orderId,orderDetailId,preAssignedContainerId,itemId,description,quantity,uom, locationId, gtin"
				+ "\r\n,USF314,COSTCO,12345,12345.1,12345,1123,Sku1123,1,each,F11,gtin1123"
				+ "\r\n,USF314,COSTCO,12345,12345.2,12345,1493,Sku1493,1,each,F12,gtin1493"
				+ "\r\n,USF314,COSTCO,12345,12345.3,12345,1522,Sku1522,3,each,S11,gtin1522"
				+ "\r\n,USF314,COSTCO,11111,11111.1,11111,1122,Sku1122,2,each,F24,gtin1122"
				+ "\r\n,USF314,COSTCO,11111,11111.2,11111,1522,Sku1522,1,each,S11,gtin1522"
				+ "\r\n,USF314,COSTCO,11111,11111.3,11111,1523,Sku1523,1,each,F21,gtin1523"
				+ "\r\n,USF314,COSTCO,11111,11111.4,11111,1124,Sku1124,1,each,F22,gtin1124"
				+ "\r\n,USF314,COSTCO,11111,11111.5,11111,1555,Sku1555,2,each,F23,gtin1555"
				+ "\r\n,USF314,COSTCO,11112,11112.1,11112,1555,Sku1555,2,each,F23,gtin1555"
				+ "\r\n,USF314,COSTCO,11113,11113.1,11113,1555,Sku1555,2,each,F23,gtin1555"
				+ "\r\n,USF314,COSTCO,11114,11114.1,11114,1514,Sku1514,3,each,S12,gtin1514"
				+ "\r\n,USF314,COSTCO,11115,11115.1,11115,1515,Sku1515,4,each,S13,gtin1515"
				+ "\r\n,USF314,COSTCO,11116,11116.1,11116,1515,Sku1515,5,each,S13,gtin1515"
				+ "\r\n,USF314,COSTCO,11117,11117.1,11117,1515,Sku1515,5,each,F14,gtin1515"
				+ "\r\n,USF314,COSTCO,11118,11118.1,11118,1515,Sku1515,3,each,S11,gtin1515"
				+ "\r\n,USF314,COSTCO,11118,11118.2,11118,1521,Sku1521,3,each,S11,gtin1521";


		Facility facility = getFacility();
		importOrdersData(facility, orderCsvString);
		ItemMaster theMaster = ItemMaster.staticGetDao().findByDomainId(facility, "1515");
		Assert.assertNotNull("ItemMaster should be created", theMaster);
	}

	/**
	 * Will fail (null) if orderId is null or does not resolve to an order.
	 * If locationId is provided, that should be the name of the order location on the order.
	 * Always checks the rather complicated putWallUiField field, which is blank if no order location or not in put wall.
	 */
	protected void assertOrderLocation(String orderId, String locationId, String putWallUiField) {
		Facility facility = getFacility();
		OrderHeader order = OrderHeader.staticGetDao().findByDomainId(facility, orderId);
		Assert.assertNotNull(order);

		if (!locationId.isEmpty()) {
			Location location = facility.findSubLocationById(locationId);
			Assert.assertNotNull(location);
			List<OrderLocation> locations = order.getOrderLocations();
			Assert.assertEquals(1, locations.size());
			OrderLocation savedOrderLocation = locations.get(0);
			Location savedLocation = savedOrderLocation.getLocation();
			Assert.assertEquals(location, savedLocation);
		} else {
			List<OrderLocation> locations = order.getOrderLocations();
			Assert.assertEquals(0, locations.size());
		}

		// getPutWallUi is what shows in the WebApp
		Assert.assertEquals(putWallUiField, order.getPutWallUi());
	}

	protected void assertItemMaster(Facility facility, String sku) {
		ItemMaster master = ItemMaster.staticGetDao().findByDomainId(facility, sku);
		Assert.assertNotNull(master);
	}
}