package com.gadgetworks.codeshelf.edi;


import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Timestamp;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.gadgetworks.codeshelf.model.InputValidationException;
import com.gadgetworks.codeshelf.model.domain.Aisle;
import com.gadgetworks.codeshelf.model.domain.DomainTestABC;
import com.gadgetworks.codeshelf.model.domain.Facility;
import com.gadgetworks.codeshelf.model.domain.Item;
import com.gadgetworks.codeshelf.model.domain.ItemMaster;
import com.gadgetworks.codeshelf.model.domain.Organization;
import com.gadgetworks.codeshelf.model.domain.Path;
import com.gadgetworks.codeshelf.model.domain.PathSegment;
import com.gadgetworks.codeshelf.model.domain.Point;
import com.gadgetworks.codeshelf.model.domain.Tier;
import com.gadgetworks.codeshelf.model.domain.UomMaster;

/**
 * @author jeffw
 *
 */
public class InventoryServiceTest extends DomainTestABC {


	private ICsvOrderImporter	importer;

	@Before
	public void initTest() {
		importer = new OutboundOrderCsvImporter(mOrderGroupDao,
			mOrderHeaderDao,
			mOrderDetailDao,
			mContainerDao,
			mContainerUseDao,
			mItemMasterDao,
			mUomMasterDao);
	}
	
	@Test
	public void testUpsertItemNullLocationAlias() throws IOException {
		Facility facility = setupData("testUpsertItemNullLocationAlias");
		Tier tier = (Tier) facility.findSubLocationById("A1.B1.T1");
		UomMaster uomMaster = facility.getUomMaster("each");
		ItemMaster itemMaster = facility.getItemMaster("10700589");
		String locationAlias = tier.getAliases().get(0).getAlias();
		
		try {
			facility.upsertItem(itemMaster.getItemId(), null, "1", "1", uomMaster.getUomMasterId());
			Assert.fail("Should have thrown exception");
		}
		catch (InputValidationException e) {
			Assert.assertTrue(e.hasViolationForProperty("storedLocation"));
		}
	}

	@Test
	public void testUpsertItemEmptyLocationAlias() throws IOException {
		Facility facility = setupData("testUpsertItemEmptyLocationAlias");
		Tier tier = (Tier) facility.findSubLocationById("A1.B1.T1");
		UomMaster uomMaster = facility.getUomMaster("each");
		ItemMaster itemMaster = facility.getItemMaster("10700589");
		String locationAlias = tier.getAliases().get(0).getAlias();
		try {
			facility.upsertItem(itemMaster.getItemId(), "", "1", "1", uomMaster.getUomMasterId());
			Assert.fail("Should have thrown exception");
		}
		catch (InputValidationException e) {
			Assert.assertTrue(e.hasViolationForProperty("storedLocation"));
		}
	}

	@Test
	public void testUpsertItemUsingAlphaCount() throws IOException {
		Facility facility = setupData("testUpsertItemUsingAlphaCount");
		Tier tier = (Tier) facility.findSubLocationById("A1.B1.T1");
		UomMaster uomMaster = facility.getUomMaster("each");
		ItemMaster itemMaster = facility.getItemMaster("10700589");
		String locationAlias = tier.getAliases().get(0).getAlias();

		try {
			Item item = facility.upsertItem(itemMaster.getItemId(), locationAlias, "1", "A", uomMaster.getUomMasterId());
			Assert.fail("Should have thrown exception");
		}
		catch (InputValidationException e) {
			Assert.assertTrue(e.hasViolationForProperty("quantity"));
		}
	}

	@Test
	public void testUpsertItemUsingNegativeCount() throws IOException {
		Facility facility = setupData("testUpsertItemUsingNegativeCount");
		Tier tier = (Tier) facility.findSubLocationById("A1.B1.T1");
		UomMaster uomMaster = facility.getUomMaster("each");
		ItemMaster itemMaster = facility.getItemMaster("10700589");
		String locationAlias = tier.getAliases().get(0).getAlias();

		try {
			Item item = facility.upsertItem(itemMaster.getItemId(), locationAlias, "1", "-1", uomMaster.getUomMasterId());
			Assert.fail("Should have thrown exception");
		}
		catch (InputValidationException e) {
			Assert.assertTrue(e.hasViolationForProperty("quantity"));
		}
	}

	@Test
	public void testUpsertItemUsingNegativePositionFromLeft() throws IOException {
		Facility facility = setUpSimpleNoSlotFacility("testUpsertItemUsingNegativePositionFromLeft");
		setupOrders(facility);
		Tier tier = (Tier) facility.findSubLocationById("A1.B1.T1");
		UomMaster uomMaster = facility.getUomMaster("each");
		ItemMaster itemMaster = facility.getItemMaster("10700589");
		String locationAlias = tier.getAliases().get(0).getAlias();

		try {
			Item item = facility.upsertItem(itemMaster.getItemId(), locationAlias, "-1", "1", uomMaster.getUomMasterId());
			Assert.fail("Should have thrown exception");
		}
		catch (InputValidationException e) {
			Assert.assertTrue(e.hasViolationForProperty("positionFromLeft"));
		}
	}
	
	@Test
	public void testUpsertItemUsingAlphaPositionFromLeft() throws IOException {
		Facility facility = setUpSimpleNoSlotFacility("testUpsertItemUsingAlphaPositionFromLeft");
		setupOrders(facility);
		Tier tier = (Tier) facility.findSubLocationById("A1.B1.T1");
		UomMaster uomMaster = facility.getUomMaster("each");
		ItemMaster itemMaster = facility.getItemMaster("10700589");
		String locationAlias = tier.getAliases().get(0).getAlias();

		try {
			Item item = facility.upsertItem(itemMaster.getItemId(), locationAlias, "A", "1", uomMaster.getUomMasterId());
			Assert.fail("Should have thrown exception");
		}
		catch (InputValidationException e) {
			Assert.assertTrue(e.hasViolationForProperty("positionFromLeft"));
		}
	}
	
	@Test
	public void testUpsertItemUsingEmptyUom() throws IOException {
		Facility facility = setUpSimpleNoSlotFacility("testUpsertItemUsingEmptyUom");
		setupOrders(facility);
		Tier tier = (Tier) facility.findSubLocationById("A1.B1.T1");
		UomMaster uomMaster = facility.getUomMaster("each");
		ItemMaster itemMaster = facility.getItemMaster("10700589");
		String locationAlias = tier.getAliases().get(0).getAlias();

		try {
			Item item = facility.upsertItem(itemMaster.getItemId(), locationAlias, "1", "1", "");
			Assert.fail("Should have thrown exception");
		}
		catch (InputValidationException e) {
			Assert.assertTrue(e.toString(), e.hasViolationForProperty("uomMasterId"));
		}
	}

	@Test
	public void testUpsertItemUsingNominalLocationId() throws IOException {
		Facility facility = setUpSimpleNoSlotFacility("testUpsertItemUsingLocationId");
		setupOrders(facility);
		Tier tier = (Tier) facility.findSubLocationById("A1.B1.T1");
		UomMaster uomMaster = facility.getUomMaster("each");
		ItemMaster itemMaster = facility.getItemMaster("10700589");

		
		Item item = facility.upsertItem(itemMaster.getItemId(), tier.getNominalLocationId(), "1", "1", uomMaster.getUomMasterId());
		Assert.assertEquals(tier, item.getStoredLocation());
	}

	@Test
	public void testUpsertItemUsingLocationAlias() throws IOException {
		Facility facility = setUpSimpleNoSlotFacility("testUpsertItemUsingLocationAlias");
		setupOrders(facility);
		Tier tier = (Tier) facility.findSubLocationById("A1.B1.T1");
		UomMaster uomMaster = facility.getUomMaster("each");
		ItemMaster itemMaster = facility.getItemMaster("10700589");

		String locationAlias = tier.getAliases().get(0).getAlias();
		Item item = facility.upsertItem(itemMaster.getItemId(), locationAlias, "1", "1", uomMaster.getUomMasterId());
		Assert.assertEquals(tier, item.getStoredLocation());
	}
	

	
	protected void setupOrders(Facility inFacility) throws IOException {
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
				+ "\r\nUSF314,COSTCO,789,789,789.2,10706961,Sun Ripened Dried Tomato Pesto,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0";
		importCsvString(inFacility, firstCsvString);
	}
	
	private void importCsvString(Facility facility, String csvString) throws IOException {
		byte[] firstCsvArray = csvString.getBytes();

		try (ByteArrayInputStream stream = new ByteArrayInputStream(firstCsvArray);) {
			InputStreamReader reader = new InputStreamReader(stream);

			Timestamp ediProcessTime = new Timestamp(System.currentTimeMillis());
			importer.importOrdersFromCsvStream(reader, facility, ediProcessTime);
		}
	}
	
	protected Facility setUpSimpleNoSlotFacility(String inOrganizationName) {
		// This returns a facility with aisle A1, with two bays with one tier each. No slots. With a path, associated to the aisle. 
		//   With location alias for first baytier only, not second. 
		// The organization will get "O-" prepended to the name. Facility F-
		// Caller must use a different organization name each time this is used
		// Valid tier names: A1.B1.T1 = D101, and A1.B2.T1
		// Also, A1.B1 has alias D100
		// Just for variance, bay3 has 4 slots
		
		String csvString = "binType,nominalDomainId,lengthCm,slotsInTier,ledCountInTier,tierFloorCm,controllerLED,anchorX,anchorY,orientXorY,depthCm\r\n" //
				+ "Aisle,A1,,,,,tierB1S1Side,12.85,43.45,X,120,Y\r\n" //
				+ "Bay,B1,230,,,,,\r\n" //
				+ "Tier,T1,,0,80,0,,\r\n" //
				+ "Bay,B2,230,,,,,\r\n" //
				+ "Tier,T1,,0,80,0,,\r\n" //
				+ "Bay,B3,230,,,,,\r\n" //
				+ "Tier,T1,,4,80,0,,\r\n"; //

		byte[] csvArray = csvString.getBytes();

		ByteArrayInputStream stream = new ByteArrayInputStream(csvArray);
		InputStreamReader reader = new InputStreamReader(stream);

		Organization organization = new Organization();
		String oName = "O-" + inOrganizationName;
		organization.setDomainId(oName);
		mOrganizationDao.store(organization);

		String fName = "F-" + inOrganizationName;
		organization.createFacility(fName, "TEST", Point.getZeroPoint());
		Facility facility = organization.getFacility(fName);

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
		
		String csvString2 = "mappedLocationId,locationAlias\r\n" //
				+ "A1.B1, D100\r\n" //
				+ "A1.B1.T1, D101\r\n" //
				+ "A1.B1.T1.S1, D301\r\n" //
				+ "A1.B1.T1.S2, D302\r\n" //
				+ "A1.B1.T1.S3, D303\r\n" //
				+ "A1.B1.T1.S4, D304\r\n"; //

		byte[] csvArray2 = csvString2.getBytes();

		ByteArrayInputStream stream2 = new ByteArrayInputStream(csvArray2);
		InputStreamReader reader2 = new InputStreamReader(stream2);
		
		Timestamp ediProcessTime2 = new Timestamp(System.currentTimeMillis());
		ICsvLocationAliasImporter importer2 = new LocationAliasCsvImporter(mLocationAliasDao);
		importer2.importLocationAliasesFromCsvStream(reader2, facility, ediProcessTime2);
		
		return facility;
	
	}
	
	private Facility setupData(String organizationId) throws IOException {
		Facility facility = setUpSimpleNoSlotFacility(organizationId);
		setupOrders(facility);
		Tier tier = (Tier) facility.findSubLocationById("A1.B1.T1");
		UomMaster uomMaster = facility.getUomMaster("each");
		ItemMaster itemMaster = facility.getItemMaster("10700589");
		String locationAlias = tier.getAliases().get(0).getAlias();
		return facility;
	}

}
