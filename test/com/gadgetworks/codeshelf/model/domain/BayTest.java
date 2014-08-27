package com.gadgetworks.codeshelf.model.domain;

import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.gadgetworks.codeshelf.model.PositionTypeEnum;
import com.google.common.collect.ImmutableList;

public class BayTest extends DomainTestABC {

	private Facility facility;
	
	@Before
	public void init() {
		facility = createFacility(this.getClass().toString() + System.currentTimeMillis());
		
	}
	
	@Test
	public void testOrderingOfTiers() {
		Aisle aisle = getAisle(facility, "A1");
		Bay bay = getBay(aisle, "B1");
		Tier tier1 = getTier(bay, "TA");
		tier1.setAnchorPoint(new Point(PositionTypeEnum.METERS_FROM_PARENT, 0.0d, 0.0d, 0.0d));
		Tier tier2 = getTier(bay, "TB");
		tier2.setAnchorPoint(new Point(PositionTypeEnum.METERS_FROM_PARENT, 0.0d, 0.0d, 2.0d));
		Tier tier3 = getTier(bay, "TC");
		tier3.setAnchorPoint(new Point(PositionTypeEnum.METERS_FROM_PARENT, 0.0d, 0.0d, 4.0d));
		List<ILocation<?>> locations = bay.getSubLocationsInWorkingOrder();
		Assert.assertEquals(ImmutableList.of(tier3, tier2, tier1), ImmutableList.copyOf(locations));
	}
}
