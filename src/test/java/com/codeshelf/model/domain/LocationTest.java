/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: OrderGroupTest.java,v 1.4 2013/07/22 04:30:36 jeffw Exp $
 *******************************************************************************/
package com.codeshelf.model.domain;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import com.codeshelf.model.DeviceType;
import com.codeshelf.model.LedRange;
import com.codeshelf.testframework.MockDaoTest;

/**
 * @author jeffw
 *
 */
public class LocationTest extends MockDaoTest {

	@Test
	public final void locationLedControllerIsLightable() {
		Location location = Mockito.mock(Location.class, Mockito.CALLS_REAL_METHODS);
		Mockito.when(location.getParent()).thenReturn(Mockito.mock(Location.class));
		LedController controller = Mockito.mock(LedController.class);
		Mockito.when(controller.getDeviceType()).thenReturn(DeviceType.Lights);
		location.setLedController(controller);
		location.setLedChannel((short)1);
		location.setFirstLedNumAlongPath((short) 1);
		location.setLastLedNumAlongPath((short) 3);
		Assert.assertTrue(location.isLightable());
	}
	
	@Test
	public final void facilityLedRangeZero() {
		try {
			this.getTenantPersistenceService().beginTransaction();
		

			Facility facility = createFacilityWithOutboundOrders();
			Assert.assertEquals(LedRange.zero(), facility.getFirstLastLedsForLocation());
		} finally {
			this.getTenantPersistenceService().commitTransaction();
			
		}
	}

	@Test
	public final void facilityUnspecificedLocationLedRangeZero() {
		try {
			this.getTenantPersistenceService().beginTransaction();
		

			Facility facility = createFacilityWithOutboundOrders();
			Location location = facility.getUnspecifiedLocation();
			Assert.assertEquals(LedRange.zero(), location.getFirstLastLedsForLocation());
		} finally {
			this.getTenantPersistenceService().commitTransaction();
			
		}
	}

	@Test
	public final void getLocationIdToParentLevel() {
		this.getTenantPersistenceService().beginTransaction();

		Facility facility = createFacilityWithOutboundOrders();
		
		Location aisle = facility.findLocationById("A1");
		Location bay = aisle.findLocationById("B1");

		String aisleId = bay.getLocationIdToParentLevel(Aisle.class);
		Assert.assertEquals(aisleId, "A1.B1");

		String facilityId = bay.getLocationIdToParentLevel(Facility.class);
		Assert.assertEquals(facilityId, facility.getDomainId()+".A1.B1");

		this.getTenantPersistenceService().commitTransaction();
	}
	
	@Test
	public final void getLocationUsage() {
		this.getTenantPersistenceService().beginTransaction();

		Facility facility = createFacilityWithOutboundOrders();
		
		Location aisle = facility.findLocationById("A1");
		Location bay = aisle.findLocationById("B1");

		//Verify that neither location is PutWall
		Assert.assertEquals(false, aisle.isPutWallLocation());
		Assert.assertEquals(false, bay.isPutWallLocation());
		
		//Set bay to PutWall, and verify that only it is such
		bay.setAsPutWallLocation(true);
		Assert.assertEquals(false, aisle.isPutWallLocation());
		Assert.assertEquals(true, bay.isPutWallLocation());
		Assert.assertEquals("", aisle.getPutWallUi());
		Assert.assertEquals("Yes", bay.getPutWallUi());


		//Revert to both off
		bay.setAsPutWallLocation(false);
		Assert.assertEquals(false, aisle.isPutWallLocation());
		Assert.assertEquals(false, bay.isPutWallLocation());

		//Set aisle to PutWall, and veryfy that both locations are not PutWall
		aisle.togglePutWallLocation();
		Assert.assertEquals(true, aisle.isPutWallLocation());
		Assert.assertEquals(true, bay.isPutWallLocation());
		Assert.assertEquals("Yes", aisle.getPutWallUi());
		Assert.assertEquals("(Yes)", bay.getPutWallUi());

		this.getTenantPersistenceService().commitTransaction();
	}

}
