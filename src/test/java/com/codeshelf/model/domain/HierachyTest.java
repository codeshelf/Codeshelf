package com.codeshelf.model.domain;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class HierachyTest extends DomainTestABC {

	@Before
	public void init() {
	}
	
	@SuppressWarnings("unused")
	@Test
	public void testHierachy() {
		// create facility, aisle, bay & tier
		this.getTenantPersistenceService().beginTransaction();
		Facility facility = createFacility();		
		Aisle aisle = getDefaultAisle(facility, "A1");
		Bay bay = getDefaultBay(aisle, "B1");
		Tier tier1 = getDefaultTier(bay, "T1");
		this.getTenantPersistenceService().commitTransaction();
				
		// traverse hierarchy
		this.getTenantPersistenceService().beginTransaction();
		
		Aisle a1 = (Aisle) facility.findSubLocationById("A1");
		Assert.assertNotNull("Aisle is undefined",a1);

		Bay b1 = (Bay) facility.findSubLocationById("A1.B1");
		Assert.assertNotNull("Bay is undefined",b1);
		
		Tier t1 = (Tier) facility.findSubLocationById("A1.B1.T1");
		Assert.assertNotNull("Tier is undefined",t1);
		
		this.getTenantPersistenceService().commitTransaction();
	}
}
