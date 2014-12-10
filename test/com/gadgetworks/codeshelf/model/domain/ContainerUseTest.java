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
import java.util.UUID;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gadgetworks.codeshelf.util.CompareNullChecker;
import com.google.common.collect.ImmutableMap;

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

		this.getPersistenceService().commitTenantTransaction();
		
		// Now just queries. Do in a transaction as our code normally would
		this.getPersistenceService().beginTenantTransaction();
		
		Location aisle = facility.findLocationById("A1");
		Location bay = aisle.findLocationById("B1");

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
		
		UUID cntrUse0Uuid = cntrUse0.getPersistentId();
		UUID cntrUse1Uuid = cntrUse1.getPersistentId();
		UUID cntrUse2Uuid = cntrUse2.getPersistentId();
		UUID header0Uuid = header0.getPersistentId();
		UUID header1Uuid = header1.getPersistentId();
		UUID header2Uuid = header2.getPersistentId();

		this.getPersistenceService().commitTenantTransaction();

		// Now the point. Do some crazy updating.
		// The calling use pattern is removeHeadersContainerUse() or addHeadersContainerUse() followed by both DAO stores.
		
		this.getPersistenceService().beginTenantTransaction();
		
		LOGGER.info("testUseHeaderRelationship Case 1: normal removal");
		header0.removeHeadersContainerUse(cntrUse0);
		OrderHeader.DAO.store(header0);
		ContainerUse.DAO.store(cntrUse0);
		this.getPersistenceService().commitTenantTransaction();
		
		// From here we will follow the pattern of original references are header0, cntrUse1, etc.  New fetches from the DAO are header0d, cntrUse1d, etc.
		ContainerUse cntrUse0d = null;
		ContainerUse cntrUse1d = null;
		ContainerUse cntrUse2d = null;
		OrderHeader header0d = null;
		OrderHeader header1d = null;
		OrderHeader header2d = null;

		Assert.assertNull(header0.getContainerUse());
		Assert.assertNull(cntrUse0.getOrderHeader());
		
		this.getPersistenceService().beginTenantTransaction();
		cntrUse0d = ContainerUse.DAO.findByPersistentId(cntrUse0Uuid);
		header0d = OrderHeader.DAO.findByPersistentId(header0Uuid);
		this.getPersistenceService().commitTenantTransaction();
		Assert.assertNull(header0d.getContainerUse());
		Assert.assertNull(cntrUse0d.getOrderHeader());
		
		LOGGER.info("Case 2: Add same again. No change to final condition");
		this.getPersistenceService().beginTenantTransaction();
		header2.addHeadersContainerUse(cntrUse2);
		OrderHeader.DAO.store(header2);
		ContainerUse.DAO.store(cntrUse2);
		this.getPersistenceService().commitTenantTransaction();
		Assert.assertEquals(cntrUse2, header2.getContainerUse());

		this.getPersistenceService().beginTenantTransaction();
		cntrUse2d = ContainerUse.DAO.findByPersistentId(cntrUse2Uuid);
		header2d = OrderHeader.DAO.findByPersistentId(header2Uuid);
		this.getPersistenceService().commitTenantTransaction();
		Assert.assertEquals(cntrUse2d, header2d.getContainerUse());


		LOGGER.info("Case 3: Try to add a container use already with another header to header without. Should refuse.");
		this.getPersistenceService().beginTenantTransaction();
		header0.addHeadersContainerUse(cntrUse2);
		OrderHeader.DAO.store(header0);
		ContainerUse.DAO.store(cntrUse2);
		this.getPersistenceService().commitTenantTransaction();
		Assert.assertNull(header0.getContainerUse());

		this.getPersistenceService().beginTenantTransaction();
		header0d = OrderHeader.DAO.findByPersistentId(header0Uuid);
		this.getPersistenceService().commitTenantTransaction();
		Assert.assertNull(header0.getContainerUse());


		LOGGER.info("Case 4: Try to add a container use to header that has one already. Should refuse.");
		this.getPersistenceService().beginTenantTransaction();
		header1.addHeadersContainerUse(cntrUse0);
		OrderHeader.DAO.store(header1);
		ContainerUse.DAO.store(cntrUse0);
		this.getPersistenceService().commitTenantTransaction();
		Assert.assertEquals(cntrUse1, header1.getContainerUse());
		
		this.getPersistenceService().beginTenantTransaction();
		header1d = OrderHeader.DAO.findByPersistentId(header1Uuid);
		cntrUse1d = ContainerUse.DAO.findByPersistentId(cntrUse1Uuid);
		this.getPersistenceService().commitTenantTransaction();
		Assert.assertEquals(cntrUse1d, header1d.getContainerUse());
	
		LOGGER.info("Case 5: Normal add. Should work as the this is how the data was setup in the first place.");
		this.getPersistenceService().beginTenantTransaction();
		header0.addHeadersContainerUse(cntrUse0);
		OrderHeader.DAO.store(header0);
		ContainerUse.DAO.store(cntrUse0);
		this.getPersistenceService().commitTenantTransaction();
		Assert.assertEquals(cntrUse0, header0.getContainerUse());
		Assert.assertEquals(header0, cntrUse0.getOrderHeader());

		this.getPersistenceService().beginTenantTransaction();
		header0d = OrderHeader.DAO.findByPersistentId(header0Uuid);
		cntrUse0d = ContainerUse.DAO.findByPersistentId(cntrUse0Uuid);
		this.getPersistenceService().commitTenantTransaction();
		Assert.assertEquals(cntrUse0d, header0d.getContainerUse());
		Assert.assertEquals(header0d, cntrUse0d.getOrderHeader());

		// Prep for the odd cases
		this.getPersistenceService().beginTenantTransaction();
		header0.removeHeadersContainerUse(cntrUse0);
		header1.removeHeadersContainerUse(cntrUse1);
		header2.removeHeadersContainerUse(cntrUse2);
		OrderHeader.DAO.store(header0);
		ContainerUse.DAO.store(cntrUse0);
		OrderHeader.DAO.store(header1);
		ContainerUse.DAO.store(cntrUse1);
		OrderHeader.DAO.store(header2);
		ContainerUse.DAO.store(cntrUse2);
		this.getPersistenceService().commitTenantTransaction();

		this.getPersistenceService().beginTenantTransaction();
		LOGGER.info("Case 5: Create an orphan ContainerUse. That is, orphan points to header, but header does not point back.");
		// This would only happen by upgrade on bad data, or a throw somewhere in normal transaction.
		// DO NOT CALL setOrderHeader elsewhere in the code. Doing it here to simulate throw in the middle of addHeadersContainerUse or removeHeadersContainerUse to achieve inconsistent data
		cntrUse0.setOrderHeader(header0);
		ContainerUse.DAO.store(cntrUse0);
		this.getPersistenceService().commitTenantTransaction();
		// check the orphan result
		Assert.assertNull(header0.getContainerUse());
		Assert.assertEquals(header0, cntrUse0.getOrderHeader());
		// check that the database has the orphan result
		this.getPersistenceService().beginTenantTransaction();
		header0d = OrderHeader.DAO.findByPersistentId(header0Uuid);
		cntrUse0d = ContainerUse.DAO.findByPersistentId(cntrUse0Uuid);
		this.getPersistenceService().commitTenantTransaction();
		Assert.assertNull(header0d.getContainerUse());
		Assert.assertEquals(header0d, cntrUse0d.getOrderHeader()); 
		
		// Now assume the code is doing a fairly normal thing of trying to add it to a header.
		// Really two cases: but just do one test case of adding to a different header.
		LOGGER.info("        Then have the header add another containerUse");
		this.getPersistenceService().beginTenantTransaction();
		header0.addHeadersContainerUse(cntrUse1);
		OrderHeader.DAO.store(header0);
		ContainerUse.DAO.store(cntrUse1);
		this.getPersistenceService().commitTenantTransaction();
		Assert.assertEquals(cntrUse1, header0.getContainerUse());
		Assert.assertEquals(header0, cntrUse1.getOrderHeader());

		this.getPersistenceService().beginTenantTransaction();
		header0d = OrderHeader.DAO.findByPersistentId(header0Uuid);
		cntrUse1d = ContainerUse.DAO.findByPersistentId(cntrUse1Uuid);
		this.getPersistenceService().commitTenantTransaction();
		Assert.assertEquals(cntrUse1d, header0d.getContainerUse());
		Assert.assertEquals(header0d, cntrUse1d.getOrderHeader());
		// did not clean up the fact that cntrUse0 still points to header0 also. We just wanted that fact to not interfere with otherwise valid setting.
		// Good enough. (knowing how the code works.)
	}
	
	@SuppressWarnings("unused")
	@Test
	public void testEmptyCriteriaByChe() {
		this.getPersistenceService().beginTenantTransaction();

		Facility facility = createFacilityWithOutboundOrders("O-CTR.1");

		this.getPersistenceService().commitTenantTransaction();
		this.getPersistenceService().beginTenantTransaction();
		List<ContainerUse> uses = ContainerUse.DAO.findByFilterAndClass("containerUsesByChe",
			ImmutableMap.<String, Object>of("cheId", UUID.randomUUID().toString()),
			ContainerUse.class);
		Assert.assertEquals(0, uses.size());
		this.getPersistenceService().commitTenantTransaction();
	}
}
