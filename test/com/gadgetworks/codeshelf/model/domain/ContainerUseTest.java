/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: OrderGroupTest.java,v 1.4 2013/07/22 04:30:36 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.gadgetworks.codeshelf.util.ASCIIAlphanumericComparator;
import com.gadgetworks.codeshelf.util.CompareNullChecker;

/**
 * @author ranstrom
 *
 */
public class ContainerUseTest extends DomainTestABC {

	private class CntrUseComparator implements Comparator<ContainerUse> {
		public int compare(ContainerUse inCntrUse1, ContainerUse inCntrUse2) {
			int value = CompareNullChecker.compareNulls(inCntrUse1, inCntrUse2);
			if (value != 0)
				return value;

			String string1 = inCntrUse1.toString();
			String string2 = inCntrUse2.toString();
			return string1.compareTo(string2);
		}
	};

	@Test
	public final void testUseHeaderRelationship() {
		this.getPersistenceService().beginTenantTransaction();

		Facility facility = createFacilityWithOutboundOrders("O-CTR.1");

		ISubLocation<?> aisle = facility.findLocationById("A1");
		ISubLocation<?> bay = aisle.findLocationById("B1");

		String aisleId = bay.getLocationIdToParentLevel(Aisle.class);
		Assert.assertEquals(aisleId, "A1.B1");

		String facilityId = bay.getLocationIdToParentLevel(Facility.class);
		Assert.assertEquals(facilityId, "F1.A1.B1");

		this.getPersistenceService().endTenantTransaction();

		List<OrderHeader> headerList = facility.getOrderHeaders();
		int headerCount = headerList.size();
		Assert.assertEquals(13, headerCount);

		List<Container> containerList = facility.getContainers();
		int cntrCount = containerList.size();
		Assert.assertEquals(6, cntrCount);

		List<ContainerUse> activeContainerUseList = new ArrayList<ContainerUse>();

		for (Container cntr : containerList) {
			ContainerUse cntrUse = cntr.getCurrentContainerUse();
			if (cntrUse != null)
				activeContainerUseList.add(cntrUse);
		}
		int useCount = activeContainerUseList.size();
		Assert.assertEquals(3, useCount);

		// Sort the list to make it determinant and reproducible.
		Collections.sort(activeContainerUseList, new CntrUseComparator());
		ContainerUse cntrUse0 = activeContainerUseList.get(0);
		ContainerUse cntrUse1 = activeContainerUseList.get(1);
		ContainerUse cntrUse2 = activeContainerUseList.get(2);
		// Only a couple of these have OrderHeaders associated already.
		OrderHeader header0 = cntrUse0.getOrderHeader();
		OrderHeader header1 = cntrUse1.getOrderHeader();
		OrderHeader header2 = cntrUse2.getOrderHeader();

		// Now the point. Do some crazy updating.
		this.getPersistenceService().beginTenantTransaction();

//		header1.addHeadersContainerUse(cntrUse0);

		this.getPersistenceService().endTenantTransaction();

	}
}
