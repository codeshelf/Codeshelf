/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: OrderGroupTest.java,v 1.4 2013/07/22 04:30:36 jeffw Exp $
 *******************************************************************************/
package com.codeshelf.model.domain;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author jeffw
 *
 */
public class LocationTest extends DomainTestABC {

	@Test
	public final void getLocationIdToParentLevel() {
		this.getTenantPersistenceService().beginTenantTransaction();

		Facility facility = createFacilityWithOutboundOrders();
		
		Location aisle = facility.findLocationById("A1");
		Location bay = aisle.findLocationById("B1");

		String aisleId = bay.getLocationIdToParentLevel(Aisle.class);
		Assert.assertEquals(aisleId, "A1.B1");

		String facilityId = bay.getLocationIdToParentLevel(Facility.class);
		Assert.assertEquals(facilityId, facility.getDomainId()+".A1.B1");

		this.getTenantPersistenceService().commitTenantTransaction();
	}
}
