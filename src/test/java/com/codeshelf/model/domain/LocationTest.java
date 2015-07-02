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
		Location bay1 = aisle.findLocationById("B1");
		Location bay2 = aisle.findLocationById("B2");

		//Verify that neither location is a Wall
		Assert.assertFalse(aisle.isImmediateWallLocation());
		Assert.assertEquals("", aisle.getWallUi());
		Assert.assertFalse(bay1.isImmediateWallLocation());
		Assert.assertEquals("", bay1.getWallUi());
		Assert.assertFalse(bay2.isImmediateWallLocation());
		Assert.assertEquals("", bay2.getWallUi());
		
		//Set bay1 to PutWall and bay2 to SkuWall
		bay1.setUsage(Location.PUTWALL_USAGE);
		bay2.setUsage(Location.SKUWALL_USAGE);
		Assert.assertFalse(aisle.isWallLocation());
		Assert.assertTrue(bay1.isWallLocation());
		Assert.assertTrue(bay1.isPutWallLocation());
		Assert.assertFalse(bay1.isSkuWallLocation());
		Assert.assertEquals("Put Wall: A1.B1", bay1.getWallUi());
		Assert.assertTrue(bay2.isWallLocation());
		Assert.assertFalse(bay2.isPutWallLocation());
		Assert.assertTrue(bay2.isSkuWallLocation());
		Assert.assertEquals("Sku Wall: A1.B2", bay2.getWallUi());

		//Toggle both bays. Bay1 should become SkuWall, Bay2 should stop being a wall
		bay1.toggleWallLocation();
		bay2.toggleWallLocation();
		Assert.assertTrue(bay1.isWallLocation());
		Assert.assertFalse(bay1.isPutWallLocation());
		Assert.assertTrue(bay1.isSkuWallLocation());
		Assert.assertEquals("Sku Wall: A1.B1", bay1.getWallUi());
		Assert.assertFalse(bay2.isWallLocation());
		Assert.assertFalse(bay2.isPutWallLocation());
		Assert.assertFalse(bay2.isSkuWallLocation());
		Assert.assertEquals("", bay2.getWallUi());

		//Set aisle to PutWall. Verify that bay1 is still a Sku Wall, but bay2 is now a Put Wall
		aisle.toggleWallLocation();
		Assert.assertTrue(aisle.isWallLocation());
		Assert.assertTrue(aisle.isImmediateWallLocation());
		Assert.assertTrue(aisle.isPutWallLocation());
		Assert.assertFalse(aisle.isSkuWallLocation());
		Assert.assertEquals("Put Wall: A1", aisle.getWallUi());
		Assert.assertTrue(bay1.isWallLocation());
		Assert.assertFalse(bay1.isPutWallLocation());
		Assert.assertTrue(bay1.isSkuWallLocation());
		Assert.assertEquals("Sku Wall: A1.B1", bay1.getWallUi());
		Assert.assertTrue(bay2.isWallLocation());
		Assert.assertTrue(bay2.isPutWallLocation());
		Assert.assertFalse(bay2.isSkuWallLocation());
		Assert.assertEquals("Put Wall: A1.B2", bay2.getWallUi());


		this.getTenantPersistenceService().commitTransaction();
	}	
}
