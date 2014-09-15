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
import com.gadgetworks.codeshelf.model.PositionTypeEnum;

/**
 * @author jeffw
 *
 */
public class FacilityTest extends DomainTestABC {

	@Test
	public final void testGetParentAtLevelWithInvalidSublevel() {
		Facility facility = createFacility("ORG-testGetParentAtLevelWhenSublevel");
		Tier nullParent = facility.getParentAtLevel(Tier.class);
		Assert.assertNull(nullParent);
	}
	
	@Test
	public final void testGetLocationIdWithInvalidSublevel() {
		Facility facility = createFacility("ORG-testGetParentAtLevelWhenSublevel");
		String locationId = facility.getLocationIdToParentLevel(Tier.class);
		Assert.assertEquals("", locationId);
	}

	@Test
	public void testHasCrossbatchOrders() {

		Facility facility = createFacility("FTEST3.O1");
		OrderHeader crossbatchOrder = new OrderHeader();
		crossbatchOrder.setParent(facility);
		crossbatchOrder.setDomainId("ORDER1");
		crossbatchOrder.setUpdated(new Timestamp(0));
		crossbatchOrder.setOrderTypeEnum(OrderTypeEnum.CROSS);
		crossbatchOrder.setActive(true);
		mOrderHeaderDao.store(crossbatchOrder);
		
		boolean hasCrossBatchOrders = mFacilityDao.findByPersistentId(facility.getPersistentId()).hasCrossBatchOrders();
		Assert.assertTrue(hasCrossBatchOrders);
	}
	
	/**
	 * Tests that fields that are not ebean properties are properly serialized (has annotations)
	 */
	@Test
	public void testSerializationOfExtraFields() {
		Facility facility = createFacility("FTEST2.O1");
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode objectNode= mapper.valueToTree(facility);
		Assert.assertNotNull(objectNode.findValue("hasMeaningfulOrderGroups"));
		Assert.assertNotNull(objectNode.findValue("hasCrossBatchOrders"));
	}
}
