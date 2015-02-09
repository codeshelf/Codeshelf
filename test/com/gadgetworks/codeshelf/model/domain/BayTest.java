package com.gadgetworks.codeshelf.model.domain;

import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.gadgetworks.codeshelf.model.PositionTypeEnum;
import com.google.common.collect.ImmutableList;

public class BayTest extends DomainTestABC {

	@Before
	public void init() {
	}
	
	@Test
	public void testOrderingOfTiers() {
		this.getPersistenceService().beginTenantTransaction();

		Facility facility = getDefaultFacility();
		Aisle aisle = getDefaultAisle(facility, "A1");
		Bay bay = getDefaultBay(aisle, "B1");
		Tier tier1 = getDefaultTier(bay, "TA");
		tier1.setAnchorPoint(new Point(PositionTypeEnum.METERS_FROM_PARENT, 0.0d, 0.0d, 0.0d));
		Tier tier2 = getDefaultTier(bay, "TB");
		tier2.setAnchorPoint(new Point(PositionTypeEnum.METERS_FROM_PARENT, 0.0d, 0.0d, 2.0d));
		Tier tier3 = getDefaultTier(bay, "TC");
		tier3.setAnchorPoint(new Point(PositionTypeEnum.METERS_FROM_PARENT, 0.0d, 0.0d, 4.0d));
		List<Location> locations = bay.getSubLocationsInWorkingOrder();
		Assert.assertEquals(ImmutableList.of(tier3, tier2, tier1), ImmutableList.copyOf(locations));

		this.getPersistenceService().commitTenantTransaction();
	}
}
