package com.codeshelf.testframework;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.sql.Timestamp;
import java.util.List;

import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.edi.AislesFileCsvImporter;
import com.codeshelf.edi.CrossBatchCsvImporter;
import com.codeshelf.edi.ICsvCrossBatchImporter;
import com.codeshelf.edi.ICsvInventoryImporter;
import com.codeshelf.edi.ICsvLocationAliasImporter;
import com.codeshelf.edi.ICsvOrderImporter;
import com.codeshelf.edi.ICsvOrderLocationImporter;
import com.codeshelf.edi.InventoryCsvImporter;
import com.codeshelf.edi.LocationAliasCsvImporter;
import com.codeshelf.edi.OrderLocationCsvImporter;
import com.codeshelf.edi.OutboundOrderCsvImporter;
import com.codeshelf.flyweight.command.NetGuid;
import com.codeshelf.model.PositionTypeEnum;
import com.codeshelf.model.domain.Aisle;
import com.codeshelf.model.domain.CodeshelfNetwork;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.LedController;
import com.codeshelf.model.domain.Location;
import com.codeshelf.model.domain.Path;
import com.codeshelf.model.domain.PathSegment;
import com.codeshelf.model.domain.Point;
import com.codeshelf.model.domain.WorkInstruction;

public abstract class ServerTest extends FrameworkTest {
	private final Logger LOGGER = LoggerFactory.getLogger(ServerTest.class);

	@Override
	Type getFrameworkType() {
		return Type.SERVER;
	}

	// various server utilities
	protected static Path createPathForTest(Facility facility) {
		return facility.createPath("");
	}
	protected static PathSegment addPathSegmentForTest(final Path inPath,
		final Integer inSegmentOrder,
		Double inStartX,
		Double inStartY,
		Double inEndX,
		Double inEndY) {
	
		Point head = new Point(PositionTypeEnum.METERS_FROM_PARENT, inStartX, inStartY, 0.0);
		Point tail = new Point(PositionTypeEnum.METERS_FROM_PARENT, inEndX, inEndY, 0.0);
		PathSegment returnSeg = inPath.createPathSegment(inSegmentOrder, head, tail);
		return returnSeg;
	}
	
	protected AislesFileCsvImporter createAisleFileImporter() {
		return new AislesFileCsvImporter(this.eventProducer);
	}
	protected ICsvLocationAliasImporter createLocationAliasImporter() {
		return new LocationAliasCsvImporter(this.eventProducer);
	}
	protected ICsvOrderImporter createOrderImporter() {
		return new OutboundOrderCsvImporter(this.eventProducer);
	}
	protected CodeshelfNetwork getNetwork() {
		return CodeshelfNetwork.DAO.findByPersistentId(this.networkPersistentId);
	}
	private static String padRight(String s, int n) {
		return String.format("%1$-" + n + "s", s);
	}
	public void logWiList(List<WorkInstruction> inList) {
		for (WorkInstruction wi : inList) {
			// If this is called from a list of WIs from the site controller, the WI may not have all its normal fields populated.
			String statusStr = padRight(wi.getStatusString(), 8);
			
			LOGGER.info(statusStr + " WiSort: " + wi.getGroupAndSortCode() + " cntr: " + wi.getContainerId() + " loc: "
					+ wi.getPickInstruction() + "(" + wi.getNominalLocationId() + ")" + " count: " + wi.getPlanQuantity()
					+ " SKU: " + wi.getItemId() + " order: " + wi.getOrderId() + " desc.: " + wi.getDescription());
		}
	}
	public void logOneWi(WorkInstruction inWi) {
		// If this is called from a list of WIs from the site controller, the WI may not have all its normal fields populated.
		String statusStr = padRight(inWi.getStatusString(), 8);
		
		LOGGER.info(statusStr + " " + inWi.getGroupAndSortCode() + " " + inWi.getContainerId() + " loc: "
				+ inWi.getPickInstruction() + "(" + inWi.getNominalLocationId() + ")" + " count: " + inWi.getPlanQuantity() + " actual: " + inWi.getActualQuantity()
				+ " SKU: " + inWi.getItemMasterId() + " order: " + inWi.getOrderId() + " desc.: " + inWi.getDescription());
	}

	protected ICsvCrossBatchImporter createCrossBatchImporter() {
		ICsvCrossBatchImporter importer = new CrossBatchCsvImporter(eventProducer,
			workService);
		return importer;
	}
	protected ICsvOrderLocationImporter createOrderLocationImporter() {
		ICsvOrderLocationImporter importer = new OrderLocationCsvImporter(eventProducer);
		return importer;
	}
	protected ICsvInventoryImporter createInventoryImporter() {
		return new InventoryCsvImporter(eventProducer);
	}

	protected void importInventoryData(Facility facility, String csvString) {
		Timestamp ediProcessTime = new Timestamp(System.currentTimeMillis());
		ICsvInventoryImporter importer = createInventoryImporter();
		importer.importSlottedInventoryFromCsvStream(new StringReader(csvString), facility, ediProcessTime);
	}

	protected void importOrdersData(Facility facility, String csvString) throws IOException {
		Timestamp ediProcessTime = new Timestamp(System.currentTimeMillis());
		ICsvOrderImporter importer = createOrderImporter();
		importer.importOrdersFromCsvStream(new StringReader(csvString), facility, ediProcessTime);
	}
	protected Facility setUpSimpleNoSlotFacility() {
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

		LedController controller1 = network.findOrCreateLedController("1", new NetGuid("0x00000011"));
		LedController controller2 = network.findOrCreateLedController("2", new NetGuid("0x00000012"));
		LedController controller3 = network.findOrCreateLedController("3", new NetGuid("0x00000013"));

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


}