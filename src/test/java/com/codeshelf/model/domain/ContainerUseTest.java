/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: OrderGroupTest.java,v 1.4 2013/07/22 04:30:36 jeffw Exp $
 *******************************************************************************/
package com.codeshelf.model.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.testframework.HibernateTest;
import com.codeshelf.util.CompareNullChecker;
import com.google.common.collect.ImmutableMap;

/**
 * @author ranstrom
 *
 */
public class ContainerUseTest extends HibernateTest {

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
		this.getTenantPersistenceService().beginTransaction();

		Facility facility = createFacilityWithOutboundOrders();

		this.getTenantPersistenceService().commitTransaction();
		
		// Now just queries. Do in a transaction as our code normally would
		this.getTenantPersistenceService().beginTransaction();
		
		Location aisle = facility.findLocationById("A1");
		Location bay = aisle.findLocationById("B1");

		String aisleId = bay.getLocationIdToParentLevel(Aisle.class);
		Assert.assertEquals(aisleId, "A1.B1");

		String facilityId = bay.getLocationIdToParentLevel(Facility.class);
		Assert.assertEquals(facilityId, facility.getDomainId()+".A1.B1");

		List<OrderHeader> headerList = OrderHeader.staticGetDao().findByParent(facility);
		int headerCount = headerList.size();
		Assert.assertEquals(13, headerCount);

		List<Container> containerList = Container.staticGetDao().findByParent(facility);
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

		this.getTenantPersistenceService().commitTransaction();

		// Now the point. Do some crazy updating.
		// The calling use pattern is removeHeadersContainerUse() or addHeadersContainerUse() followed by both DAO stores.
		
		this.getTenantPersistenceService().beginTransaction();
		
		LOGGER.info("testUseHeaderRelationship Case 1: normal removal");
		header0.removeHeadersContainerUse(cntrUse0);
		OrderHeader.staticGetDao().store(header0);
		ContainerUse.staticGetDao().store(cntrUse0);
		this.getTenantPersistenceService().commitTransaction();
		
		// From here we will follow the pattern of original references are header0, cntrUse1, etc.  New fetches from the DAO are header0d, cntrUse1d, etc.
		ContainerUse cntrUse0d = null;
		ContainerUse cntrUse1d = null;
		ContainerUse cntrUse2d = null;
		OrderHeader header0d = null;
		OrderHeader header1d = null;
		OrderHeader header2d = null;

		Assert.assertNull(header0.getContainerUse());
		Assert.assertNull(cntrUse0.getOrderHeader());
		
		this.getTenantPersistenceService().beginTransaction();
		cntrUse0d = ContainerUse.staticGetDao().findByPersistentId(cntrUse0Uuid);
		header0d = OrderHeader.staticGetDao().findByPersistentId(header0Uuid);
		this.getTenantPersistenceService().commitTransaction();
		Assert.assertNull(header0d.getContainerUse());
		Assert.assertNull(cntrUse0d.getOrderHeader());
		
		LOGGER.info("Case 2: Add same again. No change to final condition");
		this.getTenantPersistenceService().beginTransaction();
		header2.addHeadersContainerUse(cntrUse2);
		OrderHeader.staticGetDao().store(header2);
		ContainerUse.staticGetDao().store(cntrUse2);
		this.getTenantPersistenceService().commitTransaction();
		Assert.assertEquals(cntrUse2, header2.getContainerUse());

		this.getTenantPersistenceService().beginTransaction();
		cntrUse2d = ContainerUse.staticGetDao().findByPersistentId(cntrUse2Uuid);
		header2d = OrderHeader.staticGetDao().findByPersistentId(header2Uuid);
		this.getTenantPersistenceService().commitTransaction();
		Assert.assertEquals(cntrUse2d, header2d.getContainerUse());


		LOGGER.info("Case 3: Try to add a container use already with another header to header without. Should refuse.");
		this.getTenantPersistenceService().beginTransaction();
		header0.addHeadersContainerUse(cntrUse2);
		OrderHeader.staticGetDao().store(header0);
		ContainerUse.staticGetDao().store(cntrUse2);
		this.getTenantPersistenceService().commitTransaction();
		Assert.assertNull(header0.getContainerUse());

		this.getTenantPersistenceService().beginTransaction();
		header0d = OrderHeader.staticGetDao().findByPersistentId(header0Uuid);
		this.getTenantPersistenceService().commitTransaction();
		Assert.assertNull(header0.getContainerUse());

		
		LOGGER.info("Case 4: Try to add a container use to header that has one already. Should refuse.");
		this.getTenantPersistenceService().beginTransaction();
		header1.addHeadersContainerUse(cntrUse0);
		OrderHeader.staticGetDao().store(header1);
		ContainerUse.staticGetDao().store(cntrUse0);
		this.getTenantPersistenceService().commitTransaction();
		Assert.assertEquals(cntrUse1, header1.getContainerUse());
		
		this.getTenantPersistenceService().beginTransaction();
		header1d = OrderHeader.staticGetDao().findByPersistentId(header1Uuid);
		cntrUse1d = ContainerUse.staticGetDao().findByPersistentId(cntrUse1Uuid);
		Assert.assertEquals(cntrUse1d, header1d.getContainerUse());
		this.getTenantPersistenceService().commitTransaction();
	
		LOGGER.info("Case 5: Normal add. Should work as the this is how the data was setup in the first place.");
		this.getTenantPersistenceService().beginTransaction();
		header0.addHeadersContainerUse(cntrUse0);
		OrderHeader.staticGetDao().store(header0);
		ContainerUse.staticGetDao().store(cntrUse0);
		this.getTenantPersistenceService().commitTransaction();
		Assert.assertEquals(cntrUse0, header0.getContainerUse());
		Assert.assertEquals(header0, cntrUse0.getOrderHeader());

		this.getTenantPersistenceService().beginTransaction();
		header0d = OrderHeader.staticGetDao().findByPersistentId(header0Uuid);
		cntrUse0d = ContainerUse.staticGetDao().findByPersistentId(cntrUse0Uuid);
		this.getTenantPersistenceService().commitTransaction();
		Assert.assertEquals(cntrUse0d, header0d.getContainerUse());
		Assert.assertEquals(header0d, cntrUse0d.getOrderHeader());

		// Prep for the odd cases
		this.getTenantPersistenceService().beginTransaction();
		header0.removeHeadersContainerUse(cntrUse0);
		header1.removeHeadersContainerUse(cntrUse1);
		header2.removeHeadersContainerUse(cntrUse2);
		OrderHeader.staticGetDao().store(header0);
		ContainerUse.staticGetDao().store(cntrUse0);
		OrderHeader.staticGetDao().store(header1);
		ContainerUse.staticGetDao().store(cntrUse1);
		OrderHeader.staticGetDao().store(header2);
		ContainerUse.staticGetDao().store(cntrUse2);
		this.getTenantPersistenceService().commitTransaction();

		this.getTenantPersistenceService().beginTransaction();
		LOGGER.info("Case 5: Create an orphan ContainerUse. That is, orphan points to header, but header does not point back.");
		// This would only happen by upgrade on bad data, or a throw somewhere in normal transaction.
		// DO NOT CALL setOrderHeader elsewhere in the code. Doing it here to simulate throw in the middle of addHeadersContainerUse or removeHeadersContainerUse to achieve inconsistent data
		cntrUse0.setOrderHeader(header0);
		ContainerUse.staticGetDao().store(cntrUse0);
		this.getTenantPersistenceService().commitTransaction();
		// check the orphan result
		Assert.assertNull(header0.getContainerUse());
		Assert.assertEquals(header0, cntrUse0.getOrderHeader());
		// check that the database has the orphan result
		this.getTenantPersistenceService().beginTransaction();
		header0d = OrderHeader.staticGetDao().findByPersistentId(header0Uuid);
		cntrUse0d = ContainerUse.staticGetDao().findByPersistentId(cntrUse0Uuid);
		this.getTenantPersistenceService().commitTransaction();
		Assert.assertNull(header0d.getContainerUse());
		Assert.assertEquals(header0d, cntrUse0d.getOrderHeader()); 
		
		// Now assume the code is doing a fairly normal thing of trying to add it to a header.
		// Really two cases: but just do one test case of adding to a different header.
		LOGGER.info("        Then have the header add another containerUse");
		this.getTenantPersistenceService().beginTransaction();
		header0.addHeadersContainerUse(cntrUse1);
		OrderHeader.staticGetDao().store(header0);
		ContainerUse.staticGetDao().store(cntrUse1);
		this.getTenantPersistenceService().commitTransaction();
		Assert.assertEquals(cntrUse1, header0.getContainerUse());
		Assert.assertEquals(header0, cntrUse1.getOrderHeader());

		this.getTenantPersistenceService().beginTransaction();
		header0d = OrderHeader.staticGetDao().findByPersistentId(header0Uuid);
		cntrUse1d = ContainerUse.staticGetDao().findByPersistentId(cntrUse1Uuid);
		this.getTenantPersistenceService().commitTransaction();
		Assert.assertEquals(cntrUse1d, header0d.getContainerUse());
		Assert.assertEquals(header0d, cntrUse1d.getOrderHeader());
		// did not clean up the fact that cntrUse0 still points to header0 also. We just wanted that fact to not interfere with otherwise valid setting.
		// Good enough. (knowing how the code works.)
	}
	
	@SuppressWarnings("unused")
	@Test
	public void testEmptyCriteriaByChe() {
		this.getTenantPersistenceService().beginTransaction();

		Facility facility = createFacilityWithOutboundOrders();

		this.getTenantPersistenceService().commitTransaction();
		this.getTenantPersistenceService().beginTransaction();
		List<ContainerUse> uses = ContainerUse.staticGetDao().findByFilter("containerUsesByChe",
			ImmutableMap.<String, Object>of("cheId", UUID.randomUUID().toString()));
		Assert.assertEquals(0, uses.size());
		this.getTenantPersistenceService().commitTransaction();
	}
}
