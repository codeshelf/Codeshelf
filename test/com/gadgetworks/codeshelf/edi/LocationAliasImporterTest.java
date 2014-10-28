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

/**
 * @author jeffw
 *
 */
public class LocationAliasImporterTest extends EdiTestABC {

	@Test
	public final void successEventsProduced() {
		String csvString = "mappedLocationId,locationAlias\r\n" //
				+ "A1, AisleA\r\n" //
				+ "A2.B2, B34\r\n"; //
	
		Facility facility = createFacility();
		Aisle aisleA1 = new Aisle(facility, "A1", Point.getZeroPoint(), Point.getZeroPoint());
		aisleA1.getDao().store(aisleA1);

		Aisle aisleA2 = new Aisle(facility, "A2", Point.getZeroPoint(), Point.getZeroPoint());
		aisleA2.getDao().store(aisleA2);

		Bay bay2 = new Bay(aisleA2, "B2", Point.getZeroPoint(), Point.getZeroPoint());
		bay2.getDao().store(bay2);
		
		Timestamp ediProcessTime = new Timestamp(System.currentTimeMillis());
		EventProducer producer = mock(EventProducer.class);
		ICsvLocationAliasImporter importer = new LocationAliasCsvImporter(producer, mLocationAliasDao);
		importer.importLocationAliasesFromCsvStream(new StringReader(csvString), facility, ediProcessTime);
		verify(producer, times(2)).produceSuccessEvent(eq(EnumSet.of(EventTag.IMPORT, EventTag.LOCATION_ALIAS)), any(ImportCsvBeanABC.class));
		verify(producer, Mockito.never()).produceViolationEvent(any(Set.class), any(EventSeverity.class),  any(Exception.class), any(Object.class));
	}

	@Test
	public final void violationEventProducedWhenLocationInactive() {
		String csvString = "mappedLocationId,locationAlias\r\n" //
				+ "A1, AisleA\r\n";
		
		Facility facility = createFacility();
		Aisle aisleA1 = new Aisle(facility, "A1", Point.getZeroPoint(), Point.getZeroPoint());
		aisleA1.setActive(false);
		aisleA1.getDao().store(aisleA1);
				
		Timestamp ediProcessTime = new Timestamp(System.currentTimeMillis());
		EventProducer producer = mock(EventProducer.class);
		ICsvLocationAliasImporter importer = new LocationAliasCsvImporter(producer, mLocationAliasDao);
		importer.importLocationAliasesFromCsvStream(new StringReader(csvString), facility, ediProcessTime);
		verify(producer, times(1)).produceViolationEvent(eq(EnumSet.of(EventTag.IMPORT, EventTag.LOCATION_ALIAS)), eq(EventSeverity.WARN), any(Exception.class), any(ImportCsvBeanABC.class));
		verify(producer, Mockito.never()).produceSuccessEvent(any(Set.class), any(Object.class));
	}

	@Test
	public final void violationEventProducedWhenLocationNotFound() {
		String csvString = "mappedLocationId,locationAlias\r\n" //
				+ "ANOTTHERE, AisleA\r\n";
		
		Facility facility = createFacility();
				
		Timestamp ediProcessTime = new Timestamp(System.currentTimeMillis());
		EventProducer producer = mock(EventProducer.class);
		ICsvLocationAliasImporter importer = new LocationAliasCsvImporter(producer, mLocationAliasDao);
		importer.importLocationAliasesFromCsvStream(new StringReader(csvString), facility, ediProcessTime);
		verify(producer, times(1)).produceViolationEvent(eq(EnumSet.of(EventTag.IMPORT, EventTag.LOCATION_ALIAS)), eq(EventSeverity.WARN), any(Exception.class), any(ImportCsvBeanABC.class));
		verify(producer, Mockito.never()).produceSuccessEvent(any(Set.class), any(Object.class));
	}

	
	@Test
	public final void findLocationByIdAfterImport() {

		String csvString = "mappedLocationId,locationAlias\r\n" //
				+ "A1, AisleA\r\n" //
				+ "A2, AisleB\r\n" //
				+ "A1.B1, D139\r\n" //
				+ "A2.B2, B34\r\n" //
				+ "A3, AisleC\r\n";

		Facility facility = createFacility();

		Aisle aisleA1 = new Aisle(facility, "A1", Point.getZeroPoint(), Point.getZeroPoint());
		mSubLocationDao.store(aisleA1);

		Bay bay1 = new Bay(aisleA1, "B1", Point.getZeroPoint(), Point.getZeroPoint());
		mSubLocationDao.store(bay1);

		Aisle aisleA2 = new Aisle(facility, "A2", Point.getZeroPoint(), Point.getZeroPoint());
		mSubLocationDao.store(aisleA2);

		Bay bay2 = new Bay(aisleA2, "B2", Point.getZeroPoint(), Point.getZeroPoint());
		mSubLocationDao.store(bay2);

		Timestamp ediProcessTime = new Timestamp(System.currentTimeMillis());
		ICsvLocationAliasImporter importer = createLocationAliasImporter();
		importer.importLocationAliasesFromCsvStream(new StringReader(csvString), facility, ediProcessTime);

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

	}
}
