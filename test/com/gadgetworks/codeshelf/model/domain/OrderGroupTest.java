/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: OrderGroupTest.java,v 1.4 2013/07/22 04:30:36 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.domain;

import java.sql.Timestamp;

import org.junit.Assert;
import org.junit.Test;

import com.gadgetworks.codeshelf.model.OrderTypeEnum;
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
		facility.setParent(organization);
		facility.setFacilityId("F1");
		
		OrderGroup orderGroup = new OrderGroup();
		orderGroup.setParent(facility);
		orderGroup.setOrderGroupId("OG1");
		orderGroup.setActive(true);
		orderGroup.setUpdated(new Timestamp(System.currentTimeMillis()));
		orderGroup.DAO.store(orderGroup);
		
		OrderHeader order1 = new OrderHeader();
		order1.setParent(facility);
		order1.setOrderId("1");
		order1.setOrderTypeEnum(OrderTypeEnum.PICK);
		order1.setOrderDate(new Timestamp(System.currentTimeMillis()));
		order1.setDueDate(new Timestamp(System.currentTimeMillis()));
		order1.setActive(true);
		order1.setUpdated(new Timestamp(System.currentTimeMillis()));
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
		order2.setParent(facility);
		order2.setOrderId("2");
		order2.setOrderTypeEnum(OrderTypeEnum.PICK);
		order2.setOrderDate(new Timestamp(System.currentTimeMillis()));
		order2.setDueDate(new Timestamp(System.currentTimeMillis()));
		order2.setActive(true);
		order2.setUpdated(new Timestamp(System.currentTimeMillis()));
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
		facility.setParent(organization);
		facility.setFacilityId("F1");
		
		OrderGroup orderGroup = new OrderGroup();
		orderGroup.setParent(facility);
		orderGroup.setOrderGroupId("OG1");
		orderGroup.setActive(true);
		orderGroup.setUpdated(new Timestamp(System.currentTimeMillis()));
		orderGroup.DAO.store(orderGroup);
		
		OrderHeader order1 = new OrderHeader();
		order1.setParent(facility);
		order1.setOrderId("1");
		order1.setOrderTypeEnum(OrderTypeEnum.PICK);
		order1.setOrderTypeEnum(OrderTypeEnum.PICK);
		order1.setOrderDate(new Timestamp(System.currentTimeMillis()));
		order1.setDueDate(new Timestamp(System.currentTimeMillis()));
		order1.setActive(true);
		order1.setUpdated(new Timestamp(System.currentTimeMillis()));
		OrderHeader.DAO.store(order1);
		
		orderGroup.addOrderHeader(order1);
		
		Assert.assertTrue(orderGroup.release());
		
		Assert.assertFalse(orderGroup.release());	
	}
}
