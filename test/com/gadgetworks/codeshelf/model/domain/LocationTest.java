/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: OrderGroupTest.java,v 1.4 2013/07/22 04:30:36 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.domain;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author jeffw
 *
 */
public class LocationTest extends DomainTestABC {

	@Test
	public final void getLocationIdToParentLevel() {
		this.getPersistenceService().beginTenantTransaction();

		Facility facility = createFacilityWithOutboundOrders("O-LOC.1");
		
		ISubLocation<?> aisle = facility.findLocationById("A1");
		ISubLocation<?> bay = aisle.findLocationById("B1");

		String aisleId = bay.getLocationIdToParentLevel(Aisle.class);
		Assert.assertEquals(aisleId, "A1.B1");

		String facilityId = bay.getLocationIdToParentLevel(Facility.class);
		Assert.assertEquals(facilityId, "F1.A1.B1");

		this.getPersistenceService().endTenantTransaction();
	}
}
