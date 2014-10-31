package com.gadgetworks.codeshelf.edi;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Timestamp;

import org.junit.Assert;

import com.gadgetworks.codeshelf.model.PositionTypeEnum;
import com.gadgetworks.codeshelf.model.domain.Aisle;
import com.gadgetworks.codeshelf.model.domain.CodeshelfNetwork;
import com.gadgetworks.codeshelf.model.domain.Facility;
import com.gadgetworks.codeshelf.model.domain.LedController;
import com.gadgetworks.codeshelf.model.domain.Organization;
import com.gadgetworks.codeshelf.model.domain.Path;
import com.gadgetworks.codeshelf.model.domain.PathSegment;
import com.gadgetworks.codeshelf.model.domain.Point;
import com.gadgetworks.codeshelf.model.domain.SubLocationABC;
import com.gadgetworks.codeshelf.model.domain.Tier;
import com.gadgetworks.flyweight.command.NetGuid;

public class VirtualSlottedFacilityGenerator {

	private final ICsvAislesFileImporter aisleImporter;
	private final ICsvLocationAliasImporter	locationAliasImporter;
	private final ICsvOrderImporter	orderImporter;

	public VirtualSlottedFacilityGenerator(ICsvAislesFileImporter aisleFileImporter,
				ICsvLocationAliasImporter locationAliasImporter,
				ICsvOrderImporter orderImporter) {
		this.aisleImporter = aisleFileImporter;
		this.locationAliasImporter = locationAliasImporter;
		this.orderImporter = orderImporter;
	}
	
	@SuppressWarnings("rawtypes")
	public Facility generateFacilityForVirtualSlotting(String inOrganizationName) {
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
		// There is a single CHE called CHE1

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

		Organization organization = new Organization();
		String oName = "O-" + inOrganizationName;
		organization.setDomainId(oName);
		Organization.DAO.store(organization);

		String fName = "F-" + inOrganizationName;
		organization.createFacility(fName, "TEST", Point.getZeroPoint());
		Facility facility = organization.getFacility(fName);

		Timestamp ediProcessTime = new Timestamp(System.currentTimeMillis());
		aisleImporter.importAislesFileFromCsvStream(reader, facility, ediProcessTime);

		// Get the aisle
		Aisle aisle1 = Aisle.DAO.findByDomainId(facility, "A1");
		Assert.assertNotNull(aisle1);

		Path aPath = createPathForTest("F5X.1", facility);
		PathSegment segment0 = addPathSegmentForTest("F5X.1.0", aPath, 0, 22.0, 48.45, 10.85, 48.45);

		String persistStr = segment0.getPersistentId().toString();
		aisle1.associatePathSegment(persistStr);

		Aisle aisle2 = Aisle.DAO.findByDomainId(facility, "A2");
		Assert.assertNotNull(aisle2);
		aisle2.associatePathSegment(persistStr);

		Path path2 = createPathForTest("F5X.3", facility);
		PathSegment segment02 = addPathSegmentForTest("F5X.3.0", path2, 0, 22.0, 58.45, 10.85, 58.45);

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
		locationAliasImporter.importLocationAliasesFromCsvStream(reader2, facility, ediProcessTime2);

		String nName = "N-" + inOrganizationName;
		CodeshelfNetwork network = facility.createNetwork(nName);
		//Che che = 
		network.createChe("CHE1", new NetGuid("0x00000001"));

		LedController controller1 = network.findOrCreateLedController(inOrganizationName, new NetGuid("0x00000011"));
		LedController controller2 = network.findOrCreateLedController(inOrganizationName, new NetGuid("0x00000012"));
		LedController controller3 = network.findOrCreateLedController(inOrganizationName, new NetGuid("0x00000013"));
		String uuid1 = controller1.getPersistentId().toString();
		String uuid2 = controller2.getPersistentId().toString();
		String uuid3 = controller3.getPersistentId().toString();

		SubLocationABC tier = (SubLocationABC) facility.findSubLocationById("A1.B1.T1");

		((Tier) tier).setControllerChannel(uuid1, "1", "aisle"); // all A1 T1

		tier = (SubLocationABC) facility.findSubLocationById("A2.B1.T1");
		((Tier) tier).setControllerChannel(uuid2, "1", "aisle"); // all A2 T1

		tier = (SubLocationABC) facility.findSubLocationById("A3.B1.T1");
		((Tier) tier).setControllerChannel(uuid3, "1", "aisle"); // all A3 T1

		return facility;

	}
	
	public void setupOrders(Facility inFacility) throws IOException {
		String firstCsvString = "shipmentId,customerId,preAssignedContainerId,orderId,orderDetailId,itemId,description,quantity,uom,orderDate,dueDate,workSequence"
				+ "\r\nUSF314,COSTCO,123,123,123.1,10700589,Napa Valley Bistro - Jalape������o Stuffed Olives,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\nUSF314,COSTCO,123,123,123.2,10706952,Italian Homemade Style Basil Pesto,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\nUSF314,COSTCO,123,123,123.3,10706962,Authentic Pizza Sauces,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\nUSF314,COSTCO,123,123,123.4,10100250,Organic Fire-Roasted Red Bell Peppers,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\nUSF314,COSTCO,456,456,456.1,10711111,Napa Valley Bistro - Jalape������o Stuffed Olives,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\nUSF314,COSTCO,456,456,456.2,10722222,Italian Homemade Style Basil Pesto,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\nUSF314,COSTCO,456,456,456.3,10706962,Authentic Pizza Sauces,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\nUSF314,COSTCO,456,456,456.4,10100250,Organic Fire-Roasted Red Bell Peppers,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\nUSF314,COSTCO,456,456,456.5,10706961,Sun Ripened Dried Tomato Pesto,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\nUSF314,COSTCO,789,789,789.1,10100250,Organic Fire-Roasted Red Bell Peppers,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\nUSF314,COSTCO,789,789,789.2,10706961,Sun Ripened Dried Tomato Pesto,1,case,2012-09-26 11:31:01,2012-09-26 11:31:02,0";
		importCsvString(inFacility, firstCsvString);
	}
	
	private void importCsvString(Facility facility, String csvString) throws IOException {
		byte[] firstCsvArray = csvString.getBytes();

		try (ByteArrayInputStream stream = new ByteArrayInputStream(firstCsvArray);) {
			InputStreamReader reader = new InputStreamReader(stream);

			Timestamp ediProcessTime = new Timestamp(System.currentTimeMillis());
			orderImporter.importOrdersFromCsvStream(reader, facility, ediProcessTime);
		}
	}
	
	protected Path createPathForTest(String inDomainId, Facility inFacility) {
		return inFacility.createPath(inDomainId);
	}
	
	protected PathSegment addPathSegmentForTest(final String inSegmentId,
		final Path inPath,
		final Integer inSegmentOrder,
		Double inStartX,
		Double inStartY,
		Double inEndX,
		Double inEndY) {

		Point head = new Point(PositionTypeEnum.METERS_FROM_PARENT, inStartX, inStartY, 0.0);
		Point tail = new Point(PositionTypeEnum.METERS_FROM_PARENT, inEndX, inEndY, 0.0);
		PathSegment returnSeg = inPath.createPathSegment(inSegmentId, inSegmentOrder, head, tail);
		return returnSeg;
	}

}
