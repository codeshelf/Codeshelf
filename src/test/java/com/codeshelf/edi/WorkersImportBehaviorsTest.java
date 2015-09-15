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

		beginTransaction();
		readA1File(facility);
		commitTransaction();

		beginTransaction();
		facility = facility.reload();
		readA1Aliases(facility);
		commitTransaction();

		beginTransaction();
		facility = facility.reload();
		String csvString = "badgeId\r\n" //
				/*
				+ "Badge_01\r\n" //
				+ "Badge_02\r\n" //
				+ "Badge_03\r\n" //
				+ "Badge_04\r\n" //
				*/
				+ "Badge_05\r\n"; //
		importWorkersData(facility, csvString);
		commitTransaction();

		beginTransaction();
		// Worker not a child of facility, so reload facility does nothing
		Worker worker5 = Worker.findTenantWorker("Badge_05");
		Assert.assertNotNull(worker5);
		commitTransaction();
	}

	@Test
	public final void workerTest2() {
		LOGGER.info("1: Import A1 and its aliases");
		Facility facility = createFacility();

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

	}

}
