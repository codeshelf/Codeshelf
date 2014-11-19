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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gadgetworks.codeshelf.model.WorkInstructionSequencerABC;
import com.gadgetworks.codeshelf.util.ASCIIAlphanumericComparator;
import com.gadgetworks.codeshelf.util.CompareNullChecker;

/**
 * @author ranstrom
 *
 */
public class ContainerUseTest extends DomainTestABC {

	private static final Logger	LOGGER	= LoggerFactory.getLogger(ContainerUseTest.class);

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
		//The active containerUses have OrderHeaders associated already.
		OrderHeader header0 = cntrUse0.getOrderHeader();
		OrderHeader header1 = cntrUse1.getOrderHeader();
		OrderHeader header2 = cntrUse2.getOrderHeader();
		Assert.assertEquals(cntrUse0, header0.getContainerUse());
		Assert.assertEquals(cntrUse1, header1.getContainerUse());
		Assert.assertEquals(cntrUse2, header2.getContainerUse());

		this.getPersistenceService().endTenantTransaction();

		// Now the point. Do some crazy updating.
		// The calling use pattern is removeHeadersContainerUse() or addHeadersContainerUse() followed by both DAO stores.
		
		this.getPersistenceService().beginTenantTransaction();
		
		LOGGER.info("testUseHeaderRelationship Case 1: normal removal");
		header0.removeHeadersContainerUse(cntrUse0);
		OrderHeader.DAO.store(header0);
		ContainerUse.DAO.store(cntrUse0);
		this.getPersistenceService().endTenantTransaction();
		Assert.assertNull(header0.getContainerUse());
		Assert.assertNull(cntrUse0.getOrderHeader());
		
		this.getPersistenceService().beginTenantTransaction();
		LOGGER.info("Case 2: Add same again. No change to final condition");
		header2.addHeadersContainerUse(cntrUse2);
		OrderHeader.DAO.store(header2);
		ContainerUse.DAO.store(cntrUse2);
		this.getPersistenceService().endTenantTransaction();
		Assert.assertEquals(cntrUse2, header2.getContainerUse());

		if (true) return;

		// this.getPersistenceService().beginTenantTransaction();
		LOGGER.info("Case 3: Try to add a container use already with another header to header without. Should refuse.");
		header0.addHeadersContainerUse(cntrUse2);
		OrderHeader.DAO.store(header0);
		ContainerUse.DAO.store(cntrUse2);
		this.getPersistenceService().endTenantTransaction();
		Assert.assertNull(header0.getContainerUse());

		this.getPersistenceService().beginTenantTransaction();
		LOGGER.info("Case 4: Try to add a container use to header that has one already. Should refuse.");
		header1.addHeadersContainerUse(cntrUse0);
		OrderHeader.DAO.store(header1);
		ContainerUse.DAO.store(cntrUse0);
		this.getPersistenceService().endTenantTransaction();
		Assert.assertEquals(cntrUse1, header1.getContainerUse());
		
		// Prep for the odd cases
		this.getPersistenceService().beginTenantTransaction();
		header1.removeHeadersContainerUse(cntrUse1);
		header2.removeHeadersContainerUse(cntrUse2);
		OrderHeader.DAO.store(header1);
		ContainerUse.DAO.store(cntrUse1);
		OrderHeader.DAO.store(header2);
		ContainerUse.DAO.store(cntrUse2);
		this.getPersistenceService().endTenantTransaction();

		this.getPersistenceService().beginTenantTransaction();
		LOGGER.info("Case 5: Create an orphan ContainerUse. That is, orphan points to header, but header does not point back.");
		// This would only happen by upgrade on bad data, or a throw somewhere in normal transaction.
		LOGGER.info("        Then have the header add another containerUse");
		// DO NOT CALL setOrderHeader elsewhere in the code. Doing it here to simulate throw in the middle of addHeadersContainerUse or removeHeadersContainerUse to achieve inconsistent data
		cntrUse0.setOrderHeader(header0);
		this.getPersistenceService().endTenantTransaction();
		// check the orphan result
		Assert.assertNull(header0.getContainerUse());
		Assert.assertEquals(header0, cntrUse0.getOrderHeader());
		// Now assume the code is doing a fairly normal thing of trying to add it to a header.
		// Really two cases: but just do one test case of adding to a different header.
	

		
		
		this.getPersistenceService().beginTenantTransaction();
		this.getPersistenceService().endTenantTransaction();

	}
}
