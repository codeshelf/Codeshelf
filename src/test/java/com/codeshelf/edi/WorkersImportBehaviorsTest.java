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

import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.LocationAlias;
import com.codeshelf.model.domain.Worker;
import com.codeshelf.testframework.ServerTest;

/**
 * @author jeffw
 *
 */
public class WorkersImportBehaviorsTest extends ServerTest {
	private static final Logger	LOGGER	= LoggerFactory.getLogger(WorkersImportBehaviorsTest.class);

	private void readA1File(Facility inFacility) {
		// Has only A1.B1.T1.S1 through .S5

		String csvString = "binType,nominalDomainId,lengthCm,slotsInTier,ledCountInTier,tierFloorCm,controllerLED,anchorX,anchorY,orientXorY,depthCm\r\n" //
				+ "Aisle,A1,,,,,zigzagB1S1Side,12.85,43.45,X,40,Y\r\n" //
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

	@Test
	public final void workerTest1() {
		LOGGER.info("1: trivial import case. But non trivial in the sense that this file has only the badgeId field");
		Facility facility = createFacility();

		// Just doing a little stuff that we know works to get the test going.
		beginTransaction();
		readA1File(facility);
		commitTransaction();

		beginTransaction();
		facility = facility.reload();
		readA1Aliases(facility);
		commitTransaction();

		beginTransaction();
		LocationAlias aliasD1 = LocationAlias.staticGetDao().findByDomainId(facility, "D-1");
		Assert.assertNotNull(aliasD1);
		Assert.assertTrue(aliasD1.getActive());
		commitTransaction();

		// Now sart the real test case
		beginTransaction();
		facility = facility.reload();
		String csvString = "badgeId\r\n" //
				+ "Badge_01\r\n" //
				+ "Badge_02\r\n" //
				+ "Badge_03\r\n" //
				+ "Badge_04\r\n" //
				+ "Badge_05\r\n"; //
		importWorkersData(facility, csvString);
		commitTransaction();

		beginTransaction();
		// Worker not a child of facility, so reload facility does nothing
		Worker worker5a = Worker.findTenantWorker("Badge_05");
		Assert.assertNotNull(worker5a);
		
		Worker worker5b = Worker.findWorker(facility, "Badge_05");
		Assert.assertNotNull(worker5b);
		
		Assert.assertEquals(worker5a, worker5b);

		commitTransaction();
	}

	@Test
	public final void workerTest2() {
		LOGGER.info("1: Some basic setup");
		Facility facility = createFacility();

		beginTransaction();
		readA1File(facility);
		commitTransaction();

		beginTransaction();
		facility = facility.reload();
		readA1Aliases(facility);
		commitTransaction();

		LOGGER.info("2: Import 5 workers");		
		beginTransaction();
		facility = facility.reload();
		String csvString = "badgeId, firstName, lastName\r\n" //
				+ "Badge_01,Jay,Smith 01\r\n" //
				+ "Badge_02,Kay,Smith\r\n" //
				+ "Badge_03,Lori,Smith\r\n" //
				+ "Badge_04,Mary,Smith\r\n" //
				+ "Badge_05,Nancy,Smith\r\n"; //
		importWorkersData(facility, csvString);
		commitTransaction();

		LOGGER.info("3: Inactivate one worker. Show how our APIs work");		
		beginTransaction();
		// Worker not a child of facility, so reload facility does nothing
		Worker worker1 = Worker.findTenantWorker("Badge_01");
		Assert.assertNotNull(worker1);
		Assert.assertEquals("Jay", worker1.getFirstName());
		worker1.setActive(false);
		Worker.staticGetDao().store(worker1);
		commitTransaction();

		LOGGER.info("3b: Show that inactive use is found by findTenantWork, but not by findActiveWorkerInFacility");
		beginTransaction();
		// Worker not a child of facility, so reload facility does nothing
		Worker worker1a = Worker.findTenantWorker("Badge_01");
		Assert.assertNotNull(worker1a);
		Assert.assertFalse(worker1a.getActive());
		Assert.assertEquals("Smith 01",  worker1a.getLastName());

		LOGGER.info("4: Reimport the same file. Show that it makes the worker active again.");		
		beginTransaction();
		facility = facility.reload();
		String csvString2 = "badgeId, firstName, lastName\r\n" //
				+ "Badge_01,Jay,Smith\r\n" //
				+ "Badge_02,Kay,Smith\r\n" //
				+ "Badge_03,Lori,Smith\r\n" //
				+ "Badge_04,Mary,Smith\r\n" //
				+ "Badge_05,Nancy,Smith\r\n"; //
		importWorkersData(facility, csvString2);
		commitTransaction();

		LOGGER.info("4b: Prove that we reactivated the previous worker, and did not make a new one.");		
		beginTransaction();
		worker1a = Worker.staticGetDao().reload(worker1a);
		Worker worker1c = Worker.findTenantWorker("Badge_01");
		Assert.assertTrue(worker1a.getActive());
		Assert.assertEquals(worker1a, worker1c);
		commitTransaction();

		LOGGER.info("5: Import a file that does not have some of the workers and adds more. The missing workers should go inactive.");		
		beginTransaction();
		facility = facility.reload();
		String csvString3 = "badgeId, firstName, lastName\r\n" //
				+ "Badge_06,Jay,Smith\r\n" //
				+ "Badge_07,Kay,Smith\r\n" //
				+ "Badge_05,Nancy,Smith\r\n"; //
		importWorkersData(facility, csvString3);
		commitTransaction();

		beginTransaction();
		Worker worker2 = Worker.findTenantWorker("Badge_02");
		Assert.assertFalse(worker2.getActive());

		// old, reread one still active
		Worker worker5 = Worker.findTenantWorker("Badge_05");
		Assert.assertTrue(worker5.getActive());

		// new one active
		Worker worker7 = Worker.findTenantWorker("Badge_07");
		Assert.assertTrue(worker7.getActive());
		commitTransaction();

	}

}
