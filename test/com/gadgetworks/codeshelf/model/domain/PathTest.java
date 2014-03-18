/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: OrderGroupTest.java,v 1.4 2013/07/22 04:30:36 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.domain;

import static com.natpryce.makeiteasy.MakeItEasy.a;
import static com.natpryce.makeiteasy.MakeItEasy.make;

import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.gadgetworks.codeshelf.model.PositionTypeEnum;
import com.gadgetworks.codeshelf.model.TravelDirectionEnum;
import com.gadgetworks.codeshelf.model.domain.Path.PathDao;
import com.gadgetworks.codeshelf.model.domain.PathSegment.PathSegmentDao;

/**
 * @author jeffw
 *
 */
public class PathTest extends DomainTestABC {

	@Test
	public final void addRemoveOrderGroupTest() {

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

	}

	@Test
	public final void isOrderOnPath() {

		Facility facility = createFacilityWithOutboundOrders("O-PT.2");
		Path path = facility.getPath(Path.DEFAULT_FACILITY_PATH_ID);
		OrderHeader order = facility.getOrderHeader("CROSS1");

		Assert.assertTrue(path.isOrderOnPath(order));

	}
}
