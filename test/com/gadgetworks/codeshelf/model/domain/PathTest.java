/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: OrderGroupTest.java,v 1.4 2013/07/22 04:30:36 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.domain;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author jeffw
 *
 */
public class PathTest extends DomainTestABC {

	@Test
	public final void addRemoveOrderGroupTest() {
		this.getPersistenceService().beginTenantTransaction();

		Facility facility = createFacilityWithOutboundOrders("O-PT.1");
		Path path = facility.getPath(Path.DEFAULT_FACILITY_PATH_ID);

		// Check if we can find all four aisles.
		List<Aisle> aisleList = path.<Aisle> getLocationsByClass(Aisle.class);
		Assert.assertEquals(4, aisleList.size());

		// Check if we can find all eight bays.
		List<Bay> bayList = path.<Bay> getLocationsByClass(Bay.class);
		Assert.assertEquals(8, bayList.size());

		// Make sure we don't find other random locations.
		List<Tier> tierList = path.<Tier> getLocationsByClass(Tier.class);
		Assert.assertEquals(0, tierList.size());

		this.getPersistenceService().endTenantTransaction();
	}

	@Test
	public final void isOrderOnPath() {
		this.getPersistenceService().beginTenantTransaction();

		Facility facility = createFacilityWithOutboundOrders("O-PT.2");
		Path path = facility.getPath(Path.DEFAULT_FACILITY_PATH_ID);
		OrderHeader order = facility.getOrderHeader("CROSS1");

		Assert.assertTrue(path.isOrderOnPath(order));

		this.getPersistenceService().endTenantTransaction();
	}
	
	@Test
	public final void computePosALongPath() {
		this.getPersistenceService().beginTenantTransaction();

		Facility facility = createFacilityWithOutboundOrders("O-PT.3");
		Path path = facility.getPath(Path.DEFAULT_FACILITY_PATH_ID);
		for (PathSegment segment : path.getSegments()) {
			segment.computePathDistance();
		}
		
		PathSegment segment1 = path.getPathSegment(0);
		Assert.assertEquals(segment1.getStartPosAlongPath().doubleValue(), 0.0, 0.0);
		
		PathSegment segment2 = path.getPathSegment(1);
		Assert.assertEquals(segment2.getStartPosAlongPath().doubleValue(), 5.0, 0.0);
		
		this.getPersistenceService().endTenantTransaction();
	}
}
