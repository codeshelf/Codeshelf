/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: InventoryImporterTest.java,v 1.12 2013/07/22 04:30:36 jeffw Exp $
 *******************************************************************************/
package com.codeshelf.edi;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.StringReader;
import java.sql.Timestamp;
import java.util.EnumSet;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.event.EventProducer;
import com.codeshelf.event.EventSeverity;
import com.codeshelf.event.EventTag;
import com.codeshelf.model.domain.Aisle;
import com.codeshelf.model.domain.Bay;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.Location;
import com.codeshelf.model.domain.Point;
import com.codeshelf.testframework.MockDaoTest;
import com.codeshelf.validation.Errors;

/**
 * @author jeffw
 *
 */
public class LocationAliasImporterTest extends MockDaoTest {
	private static final Logger	LOGGER	= LoggerFactory.getLogger(LocationAliasImporterTest.class);

	@SuppressWarnings("unchecked")
	@Test
	public final void successEventsProduced() {
		this.getTenantPersistenceService().beginTransaction();

		String csvString = "mappedLocationId,locationAlias\r\n" //
				+ "A1, AisleA\r\n" //
				+ "A2.B2, B34\r\n"; //
	
		Facility facility = createFacility();
		Aisle aisleA1 = facility.createAisle("A1", Point.getZeroPoint(), Point.getZeroPoint());
		aisleA1.getDao().store(aisleA1);

		Aisle aisleA2 = facility.createAisle("A2", Point.getZeroPoint(), Point.getZeroPoint());
		aisleA2.getDao().store(aisleA2);

		Bay bay2 = aisleA2.createBay("B2", Point.getZeroPoint(), Point.getZeroPoint());
		bay2.getDao().store(bay2);
		
		Timestamp ediProcessTime = new Timestamp(System.currentTimeMillis());
		EventProducer producer = mock(EventProducer.class);
		ICsvLocationAliasImporter importer = new LocationAliasCsvImporter(producer);
		importer.importLocationAliasesFromCsvStream(new StringReader(csvString), facility, ediProcessTime);
		verify(producer, times(2)).produceEvent(eq(EnumSet.of(EventTag.IMPORT, EventTag.LOCATION_ALIAS)), eq(EventSeverity.INFO), any(ImportCsvBeanABC.class));
		verify(producer, Mockito.never()).produceViolationEvent(any(Set.class), any(EventSeverity.class),  any(Exception.class), any(Object.class));

		this.getTenantPersistenceService().commitTransaction();
	}

	@SuppressWarnings("unchecked")
	@Test
	public final void violationEventProducedWhenLocationInactive() {
		this.getTenantPersistenceService().beginTransaction();

		String csvString = "mappedLocationId,locationAlias\r\n" //
				+ "A1, AisleA\r\n";
		
		Facility facility = createFacility();
		Aisle aisleA1 = facility.createAisle("A1", Point.getZeroPoint(), Point.getZeroPoint());
		aisleA1.setActive(false);
		aisleA1.getDao().store(aisleA1);
				
		Timestamp ediProcessTime = new Timestamp(System.currentTimeMillis());
		EventProducer producer = mock(EventProducer.class);
		ICsvLocationAliasImporter importer = new LocationAliasCsvImporter(producer);
		importer.importLocationAliasesFromCsvStream(new StringReader(csvString), facility, ediProcessTime);
		verify(producer, times(1)).produceViolationEvent(eq(EnumSet.of(EventTag.IMPORT, EventTag.LOCATION_ALIAS)), eq(EventSeverity.WARN), any(Errors.class), any(ImportCsvBeanABC.class));
		verify(producer, Mockito.never()).produceEvent(any(Set.class), eq(EventSeverity.INFO), any(Object.class));
		
		this.getTenantPersistenceService().commitTransaction();
	}

	@SuppressWarnings("unchecked")
	@Test
	public final void violationEventProducedWhenLocationNotFound() {
		this.getTenantPersistenceService().beginTransaction();

		String csvString = "mappedLocationId,locationAlias\r\n" //
				+ "ANOTTHERE, AisleA\r\n";
		
		Facility facility = createFacility();
				
		Timestamp ediProcessTime = new Timestamp(System.currentTimeMillis());
		EventProducer producer = mock(EventProducer.class);
		ICsvLocationAliasImporter importer = new LocationAliasCsvImporter(producer);
		importer.importLocationAliasesFromCsvStream(new StringReader(csvString), facility, ediProcessTime);
		verify(producer, times(1)).produceViolationEvent(eq(EnumSet.of(EventTag.IMPORT, EventTag.LOCATION_ALIAS)), eq(EventSeverity.WARN), any(Errors.class), any(ImportCsvBeanABC.class));
		verify(producer, Mockito.never()).produceEvent(any(Set.class), eq(EventSeverity.INFO),  any(Object.class));

		this.getTenantPersistenceService().commitTransaction();
	}

	
	@Test
	public final void findLocationByIdAfterImport() {
		this.getTenantPersistenceService().beginTransaction();

		String csvString = "mappedLocationId,locationAlias\r\n" //
				+ "A1, AisleA\r\n" //
				+ "A2, AisleB\r\n" //
				+ "A1.B1, D139\r\n" //
				+ "A2.B2, B34\r\n" //
				+ "A3, AisleC\r\n";

		Facility facility = createFacility();

		Aisle aisleA1 = facility.createAisle("A1", Point.getZeroPoint(), Point.getZeroPoint());
		Aisle.DAO.store(aisleA1);

		Bay bay1 = aisleA1.createBay("B1", Point.getZeroPoint(), Point.getZeroPoint());
		Bay.DAO.store(bay1);

		Aisle aisleA2 = facility.createAisle("A2", Point.getZeroPoint(), Point.getZeroPoint());
		Aisle.DAO.store(aisleA2);

		Bay bay2 = aisleA2.createBay("B2", Point.getZeroPoint(), Point.getZeroPoint());
		Bay.DAO.store(bay2);

		Timestamp ediProcessTime = new Timestamp(System.currentTimeMillis());
		ICsvLocationAliasImporter importer = createLocationAliasImporter();
		importer.importLocationAliasesFromCsvStream(new StringReader(csvString), facility, ediProcessTime);

		this.getTenantPersistenceService().commitTransaction();
		
		this.getTenantPersistenceService().beginTransaction();

		// Make sure we can still look up an aisle by it's FQN.
		Location location = facility.findLocationById("A1");
		Assert.assertNotNull(location);
		Assert.assertEquals(location.getDomainId(), "A1");

		// Make sure we can look it up by an alias, and get the mapped location.
		location = facility.findLocationById("AisleA");
		Assert.assertNotNull(location);
		Assert.assertEquals(location.getDomainId(), "A1");

		// Make sure we can still look up a bay by it's FQN.
		location = facility.findSubLocationById("A2.B2");
		Assert.assertNotNull(location);
		Assert.assertEquals(location.getDomainId(), "B2");

		// Make sure we can look it up by an alias, and get the mapped location.
		location = facility.findLocationById("B34");
		Assert.assertNotNull(location);
		Assert.assertEquals(location.getDomainId(), "B2");

		// Make sure we cannot lookup the bad entry (there is no A3).
		location = facility.findLocationById("AisleC");
		Assert.assertNull(location);
		
		this.getTenantPersistenceService().commitTransaction();

	}
	
	@Test
	public final void rereadLocationsTest() {
		// With the hibernate change, DEV-595 bug found. Alias reread violated parent-child pattern.
		this.getTenantPersistenceService().beginTransaction();

		Facility facility = createFacility();
		String facilityDomainId = facility.getDomainId();

		Aisle aisleA1 = facility.createAisle("A1", Point.getZeroPoint(), Point.getZeroPoint());
		Aisle.DAO.store(aisleA1);

		Bay bay1 = aisleA1.createBay("B1", Point.getZeroPoint(), Point.getZeroPoint());
		Bay.DAO.store(bay1);

		Aisle aisleA2 = facility.createAisle("A2", Point.getZeroPoint(), Point.getZeroPoint());
		Aisle.DAO.store(aisleA2);

		Bay bay2 = aisleA2.createBay("B2", Point.getZeroPoint(), Point.getZeroPoint());
		Bay.DAO.store(bay2);
		this.getTenantPersistenceService().commitTransaction();

		
		LOGGER.info("1: Read the locations file");
		this.getTenantPersistenceService().beginTransaction();
		String csvString = "mappedLocationId,locationAlias\r\n" //
				+ "A1, AisleA\r\n" //
				+ "A2, AisleB\r\n" //
				+ "A1.B1, D139\r\n" //
				+ "A2.B2, B34\r\n" //
				+ "A3, AisleC\r\n";
		Timestamp ediProcessTime = new Timestamp(System.currentTimeMillis());
		ICsvLocationAliasImporter importer = createLocationAliasImporter();
		importer.importLocationAliasesFromCsvStream(new StringReader(csvString), facility, ediProcessTime);
		this.getTenantPersistenceService().commitTransaction();
		
		LOGGER.info("2: Normal lookups by alias");
		this.getTenantPersistenceService().beginTransaction();
		Location aisleA2a = facility.findLocationById("AisleA");
		Assert.assertNotNull(aisleA2a);

		Location bay2a = facility.findLocationById("B34");
		Assert.assertNotNull(bay2a);
		this.getTenantPersistenceService().commitTransaction();		
		
		LOGGER.info("3: Reread the locations file, changing some aliases");
		this.getTenantPersistenceService().beginTransaction();
		String csvString2 = "mappedLocationId,locationAlias\r\n" //
				+ "A1, AisleAx\r\n" //
				+ "A2, AisleBx\r\n" //
				+ "A1.B1, D139x\r\n" //
				+ "A2.B2, B34x\r\n" //
				+ "A3, AisleC\r\n";
		Timestamp ediProcessTime2 = new Timestamp(System.currentTimeMillis());
		ICsvLocationAliasImporter importer2 = createLocationAliasImporter();
		importer2.importLocationAliasesFromCsvStream(new StringReader(csvString2), facility, ediProcessTime2);
		this.getTenantPersistenceService().commitTransaction();

		LOGGER.info("4: Normal lookups by the new alias found. Old not");
		this.getTenantPersistenceService().beginTransaction();
		Location aisleA2b = facility.findLocationById("AisleA");
		Assert.assertNull(aisleA2b);
		aisleA2b = facility.findLocationById("AisleAx");
		Assert.assertNotNull(aisleA2b);

		Location bay2b = facility.findLocationById("B34");
		Assert.assertNull(bay2b);
		
		Location bay1b = facility.findLocationById("D139x");
		Assert.assertNotNull(bay1b);
		String bay1bDomainId = bay1b.getDomainId();
		Assert.assertEquals(bay1bDomainId, "B1");

		bay2b = facility.findLocationById("B34x");
		Assert.assertNotNull(bay2b);
		Assert.assertEquals(bay2b.getDomainId(), "B2");
		this.getTenantPersistenceService().commitTransaction();		
		
		LOGGER.info("5: Read exactly same locations file again");
		this.getTenantPersistenceService().beginTransaction();
		Timestamp ediProcessTime3 = new Timestamp(System.currentTimeMillis());
		ICsvLocationAliasImporter importer3 = createLocationAliasImporter();
		importer3.importLocationAliasesFromCsvStream(new StringReader(csvString2), facility, ediProcessTime3);
		this.getTenantPersistenceService().commitTransaction();

		LOGGER.info("6: Lookups still work");
		this.getTenantPersistenceService().beginTransaction();
		Location aisleA2c = facility.findLocationById("AisleAx");
		Assert.assertNotNull(aisleA2c);

		Location bay2c = facility.findLocationById("B34x");
		Assert.assertNotNull(bay2c);
		this.getTenantPersistenceService().commitTransaction();		

		LOGGER.info("7: Reread the locations file, swapping two aliases"); 
		/* This was the actual DEV-594 situation, inverting the names for a tier.
		2015-01-14 12:16:53,054  cannot map Alias D139x to B2 because it is still mapped to B1
		java.lang.Exception
			at com.codeshelf.model.domain.Location.addAlias(Location.java:694)
		*/		
		this.getTenantPersistenceService().beginTransaction();
		String csvString4 = "mappedLocationId,locationAlias\r\n" //
				+ "A1, AisleAx\r\n" //
				+ "A2, AisleBx\r\n" //
				+ "A1.B1, B34x\r\n" //
				+ "A2.B2, D139x\r\n" //
				+ "A3, AisleC\r\n";
		Timestamp ediProcessTime4 = new Timestamp(System.currentTimeMillis());
		ICsvLocationAliasImporter importer4 = createLocationAliasImporter();
		importer4.importLocationAliasesFromCsvStream(new StringReader(csvString4), facility, ediProcessTime4);
		this.getTenantPersistenceService().commitTransaction();

		LOGGER.info("8: See that the swapped aliases resolve correctly");
		this.getTenantPersistenceService().beginTransaction();
		Location bay1d = facility.findLocationById("B34x");
		Assert.assertNotNull(bay1d);
		String bay1dDomainId = bay1d.getDomainId();
		Assert.assertEquals(bay1dDomainId, "B1"); // works, even though facility reference is stale
		
		// Let's get the facility again within this transaction.
		Facility facility2 = Facility.DAO.findByDomainId(null, facilityDomainId);
		Location bay1e = facility2.findLocationById("B34x");
		Assert.assertNotNull(bay1e);
		String bay1eDomainId = bay1e.getDomainId();
		Assert.assertEquals(bay1eDomainId, "B1");  // works with new reference

		Location bay2d = facility.findLocationById("D139x");
		Assert.assertNotNull(bay2d);
		Assert.assertEquals(bay2d.getDomainId(), "B2"); // fails!

		this.getTenantPersistenceService().commitTransaction();		

	}

}
