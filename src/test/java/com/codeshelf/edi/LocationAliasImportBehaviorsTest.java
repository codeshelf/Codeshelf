/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2015, Codeshelf, All rights reserved
 *  Author Jon Ranstrom
  *******************************************************************************/
package com.codeshelf.edi;


import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.model.domain.Aisle;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.Location;
import com.codeshelf.model.domain.LocationAlias;
import com.codeshelf.testframework.ServerTest;

/**
 * @author jeffw
 *
 */
public class LocationAliasImportBehaviorsTest extends ServerTest {
	private static final Logger	LOGGER	= LoggerFactory.getLogger(LocationAliasImportBehaviorsTest.class);

	private void readA1File(Facility inFacility) {
		// Has only A1.B1.T1.S1 through .S5

		String csvString = "binType,nominalDomainId,lengthCm,slotsInTier,ledCountInTier,tierFloorCm,controllerLED,anchorX,anchorY,orientXorY,depthCm\r\n" //
				+ "Aisle,A1,,,,,zigzagB1S1Side,12.85,43.45,X,40,Y\r\n" //
				+ "Bay,B1,112,,,,,\r\n" //
				+ "Tier,T1,5,5,32,0,,\r\n"; //
		importAislesData(inFacility, csvString);
	}

	private void readA2File(Facility inFacility) {
		// Has only A2.B1.T1.S1 through .S5

		String csvString = "binType,nominalDomainId,lengthCm,slotsInTier,ledCountInTier,tierFloorCm,controllerLED,anchorX,anchorY,orientXorY,depthCm\r\n" //
				+ "Aisle,A2,,,,,zigzagB1S1Side,12.85,43.45,X,40,Y\r\n" //
				+ "Bay,B1,112,,,,,\r\n" //
				+ "Tier,T1,5,5,32,0,,\r\n"; //
		importAislesData(inFacility, csvString);
	}

	private void readA1Aliases(Facility inFacility) {
		String csvString = "mappedLocationId,locationAlias\r\n" //
				+ "A1.B1.T1.S1, D-1\r\n" //
				+ "A1.B1.T1.S2, D-2\r\n" //
				+ "A1.B1.T1.S3, D-3\r\n" //
				+ "A1.B1.T1.S4, D-4\r\n" //
				+ "A1.B1.T1.S5, D-5\r\n"; //
		importLocationAliasesData(inFacility, csvString);
	}

	private void readA1bAliases(Facility inFacility) {
		// just remove the dash from each alias
		String csvString = "mappedLocationId,locationAlias\r\n" //
				+ "A1.B1.T1.S1, D1\r\n" //
				+ "A1.B1.T1.S2, D2\r\n" //
				+ "A1.B1.T1.S3, D3\r\n" //
				+ "A1.B1.T1.S4, D4\r\n" //
				+ "A1.B1.T1.S5, D5\r\n"; //
		importLocationAliasesData(inFacility, csvString);
	}

	private void readA2Aliases(Facility inFacility) {
		String csvString = "mappedLocationId,locationAlias\r\n" //
				+ "A2.B1.T1.S1, D-6\r\n" //
				+ "A2.B1.T1.S2, D-7\r\n" //
				+ "A2.B1.T1.S3, D-8\r\n" //
				+ "A2.B1.T1.S4, D-9\r\n" //
				+ "A2.B1.T1.S5, D-10\r\n"; //
		importLocationAliasesData(inFacility, csvString);
	}
	
	private void verifyD1D2(Facility inFacility){
		beginTransaction();
		inFacility = inFacility.reload();
		Location slot1 = inFacility.findSubLocationById("A1.B1.T1.S1");
		Assert.assertNotNull(slot1);
		Assert.assertEquals("D-1", slot1.getBestUsableLocationName());

		Location slot2 = inFacility.findSubLocationById("D-2");
		Assert.assertNotNull(slot2);		
		commitTransaction();
	}

	@Test
	public final void behaviorTest1() {
		LOGGER.info("1: trivial import case");
		Facility facility = createFacility();

		beginTransaction();
		readA1File(facility);
		commitTransaction();

		beginTransaction();
		facility = facility.reload();
		readA1Aliases(facility);
		commitTransaction();

		verifyD1D2(facility);

		LOGGER.info("2: reimport same aliases");
		beginTransaction();
		facility = facility.reload();
		readA1Aliases(facility);
		commitTransaction();

		verifyD1D2(facility);

		LOGGER.info("3: reimport A1 again");
		beginTransaction();
		facility = facility.reload();
		readA1File(facility);
		commitTransaction();

		verifyD1D2(facility);
	}

	@Test
	public final void behaviorTest2() {
		LOGGER.info("1: Import A1 and its aliases");
		Facility facility = createFacility();

		beginTransaction();
		readA1File(facility);
		commitTransaction();

		beginTransaction();
		facility = facility.reload();
		readA1Aliases(facility);
		commitTransaction();

		verifyD1D2(facility);

		LOGGER.info("2: Import A2 and its aliases");
		beginTransaction();
		facility = facility.reload();
		readA2File(facility);
		commitTransaction();

		beginTransaction();
		facility = facility.reload();
		readA2Aliases(facility);
		commitTransaction();

		LOGGER.info("2b: Check one A1 alias"); // This failed before v21
		beginTransaction();
		facility = facility.reload();
		LocationAlias aliasD1 = LocationAlias.staticGetDao().findByDomainId(facility, "D-1");
		Assert.assertNotNull(aliasD1);
		Assert.assertTrue(aliasD1.getActive());
		commitTransaction();
	

		LOGGER.info("2c: Are the A1 aliases still intact?"); // This failed before v21
		verifyD1D2(facility);

	}

	@Test
	public final void behaviorTest3() {
		LOGGER.info("1: Import A1 and its aliases");
		Facility facility = createFacility();

		beginTransaction();
		readA1File(facility);
		commitTransaction();

		beginTransaction();
		facility = facility.reload();
		readA1Aliases(facility);
		commitTransaction();

		LOGGER.info("2: Import A2 and its aliases");
		beginTransaction();
		facility = facility.reload();
		readA2File(facility);
		commitTransaction();

		beginTransaction();
		facility = facility.reload();
		readA2Aliases(facility);
		commitTransaction();

		verifyD1D2(facility);
		
		LOGGER.info("3: Delete A1 as the UI would. Actually, not a delete, just it and its children inactive");
		beginTransaction();
		facility = facility.reload();
		Aisle aisle1 = (Aisle) facility.findSubLocationById("A1");
		Assert.assertTrue(aisle1.getActive());
		aisle1.makeInactiveAndAllChildren();
		commitTransaction();

		LOGGER.info("3b:This leave the aliases active. Not great, but ok. Shown as <D-1>");
		beginTransaction();
		facility = facility.reload();
		Location slot1a = facility.findSubLocationById("A1.B1.T1.S1");
		Assert.assertNotNull(slot1a);
		Assert.assertEquals("<D-1>", slot1a.getBestUsableLocationName());

		Location slot1b = facility.findSubLocationById("D-1");
		Assert.assertNotNull(slot1b);
		Assert.assertEquals("<D-1>", slot1b.getBestUsableLocationName());
		
		LocationAlias aliasD1 = LocationAlias.staticGetDao().findByDomainId(facility, "D-1");
		Assert.assertNotNull(aliasD1);
		Assert.assertTrue(aliasD1.getActive());

		commitTransaction();
	
		LOGGER.info("4: Import A2 aliases again. Since A1 was deleted, this will archive the A1 aliases");
		beginTransaction();
		facility = facility.reload();
		readA2Aliases(facility);
		commitTransaction();

		LOGGER.info("4b: Check the A1 locations");
		beginTransaction();
		facility = facility.reload();
		slot1a = facility.findSubLocationById("A1.B1.T1.S1");
		Assert.assertNotNull(slot1a);
		Assert.assertFalse(slot1a.getActive());
		Assert.assertEquals("<A1.B1.T1.S1>", slot1a.getBestUsableLocationName()); // does not find the old alias

		slot1b = facility.findSubLocationById("D-1"); // exists, but inactive alias will not resolve by this function. As intended.
		Assert.assertNull(slot1b);
		
		// Seems like this should find it
		aliasD1 = LocationAlias.staticGetDao().findByDomainId(facility, "D-1");
		Assert.assertNull(aliasD1);
		// Assert.assertFalse(aliasD1.getActive());

		commitTransaction();

	}

	@Test
	public final void behaviorTest4() {
		LOGGER.info("1: Import A1 and its aliases");
		Facility facility = createFacility();

		beginTransaction();
		readA1File(facility);
		commitTransaction();

		beginTransaction();
		facility = facility.reload();
		readA1Aliases(facility);
		commitTransaction();

		verifyD1D2(facility);

		LOGGER.info("2: Change the aliases for A1");

		beginTransaction();
		facility = facility.reload();
		readA1bAliases(facility);
		commitTransaction();
		
		LOGGER.info("3: Check the aliases");
		beginTransaction();
		facility = facility.reload();

		LocationAlias aliasD1 = LocationAlias.staticGetDao().findByDomainId(facility, "D-1");
		// shouldn't this be found?
		Assert.assertNull(aliasD1);
		// Assert.assertFalse(aliasD1.getActive());

		LocationAlias aliasD1b = LocationAlias.staticGetDao().findByDomainId(facility, "D1");
		Assert.assertNotNull(aliasD1b);
		Assert.assertTrue(aliasD1b.getActive());
		
		LOGGER.info("3: Check the location names and find location by name");		
		Location slot1a = facility.findSubLocationById("A1.B1.T1.S1");
		Assert.assertNotNull(slot1a);
		Assert.assertEquals("D1", slot1a.getBestUsableLocationName());
		
		// Will not be found
		Location slot1b = facility.findSubLocationById("D-1");
		Assert.assertNull(slot1b);
		
		Location slot1c = facility.findSubLocationById("D1");
		Assert.assertNotNull(slot1c);
		Assert.assertEquals("D1", slot1c.getBestUsableLocationName());

		commitTransaction();
	}

}
