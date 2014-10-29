/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: InventoryImporterTest.java,v 1.12 2013/07/22 04:30:36 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.edi;

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

import com.gadgetworks.codeshelf.event.EventProducer;
import com.gadgetworks.codeshelf.event.EventSeverity;
import com.gadgetworks.codeshelf.event.EventTag;
import com.gadgetworks.codeshelf.model.domain.Aisle;
import com.gadgetworks.codeshelf.model.domain.Bay;
import com.gadgetworks.codeshelf.model.domain.Facility;
import com.gadgetworks.codeshelf.model.domain.ILocation;
import com.gadgetworks.codeshelf.model.domain.Point;
import com.gadgetworks.codeshelf.validation.Errors;

/**
 * @author jeffw
 *
 */
public class LocationAliasImporterTest extends EdiTestABC {

	@Test
	public final void successEventsProduced() {
		this.getPersistenceService().beginTenantTransaction();

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
		ICsvLocationAliasImporter importer = new LocationAliasCsvImporter(producer, mLocationAliasDao);
		importer.importLocationAliasesFromCsvStream(new StringReader(csvString), facility, ediProcessTime);
		verify(producer, times(2)).produceEvent(eq(EnumSet.of(EventTag.IMPORT, EventTag.LOCATION_ALIAS)), eq(EventSeverity.INFO), any(ImportCsvBeanABC.class));
		verify(producer, Mockito.never()).produceViolationEvent(any(Set.class), any(EventSeverity.class),  any(Exception.class), any(Object.class));

		this.getPersistenceService().endTenantTransaction();
	}

	@Test
	public final void violationEventProducedWhenLocationInactive() {
		this.getPersistenceService().beginTenantTransaction();

		String csvString = "mappedLocationId,locationAlias\r\n" //
				+ "A1, AisleA\r\n";
		
		Facility facility = createFacility();
		Aisle aisleA1 = facility.createAisle("A1", Point.getZeroPoint(), Point.getZeroPoint());
		aisleA1.setActive(false);
		aisleA1.getDao().store(aisleA1);
				
		Timestamp ediProcessTime = new Timestamp(System.currentTimeMillis());
		EventProducer producer = mock(EventProducer.class);
		ICsvLocationAliasImporter importer = new LocationAliasCsvImporter(producer, mLocationAliasDao);
		importer.importLocationAliasesFromCsvStream(new StringReader(csvString), facility, ediProcessTime);
		verify(producer, times(1)).produceViolationEvent(eq(EnumSet.of(EventTag.IMPORT, EventTag.LOCATION_ALIAS)), eq(EventSeverity.WARN), any(Errors.class), any(ImportCsvBeanABC.class));
		verify(producer, Mockito.never()).produceEvent(any(Set.class), eq(EventSeverity.INFO), any(Object.class));
		
		this.getPersistenceService().endTenantTransaction();
	}

	@Test
	public final void violationEventProducedWhenLocationNotFound() {
		this.getPersistenceService().beginTenantTransaction();

		String csvString = "mappedLocationId,locationAlias\r\n" //
				+ "ANOTTHERE, AisleA\r\n";
		
		Facility facility = createFacility();
				
		Timestamp ediProcessTime = new Timestamp(System.currentTimeMillis());
		EventProducer producer = mock(EventProducer.class);
		ICsvLocationAliasImporter importer = new LocationAliasCsvImporter(producer, mLocationAliasDao);
		importer.importLocationAliasesFromCsvStream(new StringReader(csvString), facility, ediProcessTime);
		verify(producer, times(1)).produceViolationEvent(eq(EnumSet.of(EventTag.IMPORT, EventTag.LOCATION_ALIAS)), eq(EventSeverity.WARN), any(Errors.class), any(ImportCsvBeanABC.class));
		verify(producer, Mockito.never()).produceEvent(any(Set.class), eq(EventSeverity.INFO),  any(Object.class));

		this.getPersistenceService().endTenantTransaction();
	}

	
	@Test
	public final void findLocationByIdAfterImport() {
		this.getPersistenceService().beginTenantTransaction();

		String csvString = "mappedLocationId,locationAlias\r\n" //
				+ "A1, AisleA\r\n" //
				+ "A2, AisleB\r\n" //
				+ "A1.B1, D139\r\n" //
				+ "A2.B2, B34\r\n" //
				+ "A3, AisleC\r\n";

		Facility facility = createFacility();

		Aisle aisleA1 = facility.createAisle("A1", Point.getZeroPoint(), Point.getZeroPoint());
		mAisleDao.store(aisleA1);

		Bay bay1 = aisleA1.createBay("B1", Point.getZeroPoint(), Point.getZeroPoint());
		mBayDao.store(bay1);

		Aisle aisleA2 = facility.createAisle("A2", Point.getZeroPoint(), Point.getZeroPoint());
		mAisleDao.store(aisleA2);

		Bay bay2 = aisleA2.createBay("B2", Point.getZeroPoint(), Point.getZeroPoint());
		mBayDao.store(bay2);

		Timestamp ediProcessTime = new Timestamp(System.currentTimeMillis());
		ICsvLocationAliasImporter importer = createLocationAliasImporter();
		importer.importLocationAliasesFromCsvStream(new StringReader(csvString), facility, ediProcessTime);

		this.getPersistenceService().endTenantTransaction();
		
		this.getPersistenceService().beginTenantTransaction();

		// Make sure we can still look up an aisle by it's FQN.
		ILocation<?> location = facility.findLocationById("A1");
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
		
		this.getPersistenceService().endTenantTransaction();

	}
}
