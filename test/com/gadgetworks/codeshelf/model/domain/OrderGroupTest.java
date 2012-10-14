/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: OrderGroupTest.java,v 1.1 2012/10/14 01:05:22 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.domain;

import junit.framework.Assert;

import org.junit.Test;

import com.gadgetworks.codeshelf.model.dao.MockDao;

/**
 * @author jeffw
 *
 */
public class OrderGroupTest {

	@Test
	public final void addRemoveOrderGroupTest() {
		
		OrderGroup.DAO = new MockDao<OrderGroup>();
		OrderHeader.DAO = new MockDao<OrderHeader>();
		
		Organization organization = new Organization();
		organization.setOrganizationId("O1");
		
		Facility facility = new Facility();
		facility.setParentOrganization(organization);
		facility.setFacilityId("F1");
		
		OrderGroup orderGroup = new OrderGroup();
		orderGroup.setParentFacility(facility);
		orderGroup.setOrderGroupId("OG1");
		orderGroup.DAO.store(orderGroup);
		
		OrderHeader order1 = new OrderHeader();
		order1.setParentFacility(facility);
		order1.setOrderId("1");
		OrderHeader.DAO.store(order1);
		
		// Check if we can add this order.
		if (!orderGroup.addOrderHeader(order1)) {
			Assert.fail();
		}
		
		// Check if we can add this order.
		if (!orderGroup.removeOrderHeader(order1)) {
			Assert.fail();
		}
		
		// Release the order group.
		orderGroup.release();
		
		OrderHeader order2 = new OrderHeader();
		order2.setParentFacility(facility);
		order2.setOrderId("2");
		OrderHeader.DAO.store(order2);
		
		// Verify that we cannot add the new group.
		if (orderGroup.addOrderHeader(order2)) {
			Assert.fail();
		}
		
		if (orderGroup.removeOrderHeader(order2)) {
			Assert.fail();
		}
		
		Assert.assertFalse(orderGroup.release());	
	}

	@Test
	public final void releaseOrderGroupTest() {
		
		Organization organization = new Organization();
		organization.setOrganizationId("O1");
		
		Facility facility = new Facility();
		facility.setParentOrganization(organization);
		facility.setFacilityId("F1");
		
		OrderGroup orderGroup = new OrderGroup();
		orderGroup.setParentFacility(facility);
		orderGroup.setOrderGroupId("OG1");
		
		OrderHeader order1 = new OrderHeader();
		order1.setParentFacility(facility);
		order1.setOrderId("1");
		
		orderGroup.addOrderHeader(order1);
		
		Assert.assertTrue(orderGroup.release());
		
		Assert.assertFalse(orderGroup.release());	
	}
}
