/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: FacilityTest.java,v 1.11 2013/11/11 07:46:30 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.domain;

import java.sql.Timestamp;

import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gadgetworks.codeshelf.model.OrderTypeEnum;

/**
 * @author jeffw
 *
 */
public class FacilityTest extends DomainTestABC {
	
	@Test
	public final void testGetParentAtLevelWithInvalidSublevel() {
		this.getPersistenceService().beginTenantTransaction();
		Facility facility = createDefaultFacility("ORG-testGetParentAtLevelWhenSublevel");
		Tier nullParent = facility.getParentAtLevel(Tier.class);
		Assert.assertNull(nullParent);
		this.getPersistenceService().commitTenantTransaction();
	}
	
	@Test
	public final void testGetLocationIdWithInvalidSublevel() {
		this.getPersistenceService().beginTenantTransaction();
		Facility facility = createDefaultFacility("ORG-testGetParentAtLevelWhenSublevel");
		String locationId = facility.getLocationIdToParentLevel(Tier.class);
		Assert.assertEquals("", locationId);
		this.getPersistenceService().commitTenantTransaction();
	}

	@Test
	public void testHasCrossbatchOrders() {
		this.getPersistenceService().beginTenantTransaction();

		Facility facility = createDefaultFacility("FTEST3.O1");
		OrderHeader crossbatchOrder = new OrderHeader();
		crossbatchOrder.setDomainId("ORDER1");
		crossbatchOrder.setUpdated(new Timestamp(0));
		crossbatchOrder.setOrderType(OrderTypeEnum.CROSS);
		crossbatchOrder.setActive(true);

		facility.addOrderHeader(crossbatchOrder);

		mOrderHeaderDao.store(crossbatchOrder);

		
		boolean hasCrossBatchOrders = facility.hasCrossBatchOrders();
		Assert.assertTrue(hasCrossBatchOrders);
		
		Facility retrievedFacility=mFacilityDao.findByPersistentId(facility.getPersistentId());
		hasCrossBatchOrders = retrievedFacility.hasCrossBatchOrders();
		
		Assert.assertTrue(hasCrossBatchOrders);
		this.getPersistenceService().commitTenantTransaction();
	}
	
	/**
	 * Tests that fields that are not ebean properties are properly serialized (has annotations)
	 */
	@Test
	public void testSerializationOfExtraFields() {
		this.getPersistenceService().beginTenantTransaction();
		Facility facility = createDefaultFacility("FTEST2.O1");
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode objectNode= mapper.valueToTree(facility);
		Assert.assertNotNull(objectNode.findValue("hasMeaningfulOrderGroups"));
		Assert.assertNotNull(objectNode.findValue("hasCrossBatchOrders"));
		this.getPersistenceService().commitTenantTransaction();
	}
}
