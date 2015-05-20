/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: OrderGroupTest.java,v 1.4 2013/07/22 04:30:36 jeffw Exp $
 *******************************************************************************/
package com.codeshelf.model.domain;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.codeshelf.testframework.HibernateTest;

/**
 * @author jeffw
 *
 */
public class PathTest extends HibernateTest {

	@Test
	public final void addRemoveOrderGroupTest() {
		this.getTenantPersistenceService().beginTransaction();

		Facility facility = createFacilityWithOutboundOrders();
		Assert.assertNotNull(facility);
		this.getTenantPersistenceService().commitTransaction();

		this.getTenantPersistenceService().beginTransaction();
		Facility retrievedFacility = Facility.staticGetDao().findByPersistentId(facility.getPersistentId());
		
		Path path = retrievedFacility.getPath(Path.DEFAULT_FACILITY_PATH_ID);
		Assert.assertNotNull("Path is undefined",path);

		// Check if we can find all four aisles.
		List<Aisle> aisleList = path.<Aisle> getLocationsByClass(Aisle.class);
		Assert.assertEquals(4, aisleList.size());

		// Check if we can find all eight bays.
		List<Bay> bayList = path.<Bay> getLocationsByClass(Bay.class);
		Assert.assertEquals(8, bayList.size());

		// Make sure we don't find other random locations.
		List<Tier> tierList = path.<Tier> getLocationsByClass(Tier.class);
		Assert.assertEquals(0, tierList.size());

		this.getTenantPersistenceService().commitTransaction();
	}

	@Test
	public final void isOrderOnPath() {
		this.getTenantPersistenceService().beginTransaction();

		Facility facility = createFacilityWithOutboundOrders();
		Assert.assertNotNull(facility);
		Path path = facility.getPath(Path.DEFAULT_FACILITY_PATH_ID);
		Assert.assertNotNull("Path is undefined",path);
		OrderHeader order = OrderHeader.staticGetDao().findByDomainId(facility, "CROSS1");
		Assert.assertNotNull("Order header is undefined",order);

		Assert.assertTrue(path.isOrderOnPath(order));

		this.getTenantPersistenceService().commitTransaction();
	}
	
	@Test
	public final void computePosALongPath() {
		this.getTenantPersistenceService().beginTransaction();

		Facility facility = createFacilityWithOutboundOrders();
		Assert.assertNotNull(facility);
		Path path = facility.getPath(Path.DEFAULT_FACILITY_PATH_ID);
		Assert.assertNotNull("Path is undefined",path);

		for (PathSegment segment : path.getSegments()) {
			segment.computePathDistance();
		}
		
		PathSegment segment1 = path.getPathSegment(0);
		Assert.assertEquals(segment1.getStartPosAlongPath().doubleValue(), 0.0, 0.0);
		
		PathSegment segment2 = path.getPathSegment(1);
		Assert.assertEquals(segment2.getStartPosAlongPath().doubleValue(), 5.0, 0.0);
		
		this.getTenantPersistenceService().commitTransaction();
	}
	
	@Test
	public final void testPathScript() {
		this.getTenantPersistenceService().beginTransaction();

		Facility facility = createFacilityWithOutboundOrders();
		Assert.assertNotNull(facility);
		
		Path path = createPathForTest(facility);
		addPathSegmentForTest(path, 0, 2.82, 5.40, 5.19, 5.40);
		addPathSegmentForTest(path, 1, 5.19, 5.40, 5.19, 6.40);
		addPathSegmentForTest(path, 2, 5.19, 6.40, 2.77, 6.40); 
		String script = path.getPathScript().trim();
		Assert.assertEquals("- 2.82 5.40 5.19 5.40 - 5.19 5.40 5.19 6.40 - 5.19 6.40 2.77 6.40", script);

		this.getTenantPersistenceService().commitTransaction();
	}

}
