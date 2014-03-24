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
import com.gadgetworks.codeshelf.model.PositionTypeEnum;

/**
 * @author jeffw
 *
 */
public class OrderGroupTest extends DomainTestABC {

	@Test
	public final void addRemoveOrderGroupTest() {
		
		Organization organization = new Organization();
		organization.setOrganizationId("OG.1");
		mOrganizationDao.store(organization);
		
		organization.createFacility("F1", "test", Point.getZeroPoint());
		Facility facility = organization.getFacility("F1");
		
		OrderGroup orderGroup = new OrderGroup();
		orderGroup.setParent(facility);
		orderGroup.setOrderGroupId("OG.2");
		orderGroup.setActive(true);
		orderGroup.setUpdated(new Timestamp(System.currentTimeMillis()));
		mOrderGroupDao.store(orderGroup);
		
		OrderHeader order1 = new OrderHeader();
		order1.setParent(facility);
		order1.setOrderId("1");
		order1.setOrderTypeEnum(OrderTypeEnum.OUTBOUND);
		order1.setOrderDate(new Timestamp(System.currentTimeMillis()));
		order1.setDueDate(new Timestamp(System.currentTimeMillis()));
		order1.setActive(true);
		order1.setUpdated(new Timestamp(System.currentTimeMillis()));
		mOrderHeaderDao.store(order1);
		
		// Check if we can add this order.
		orderGroup.addOrderHeader(order1);
		Assert.assertNotNull(orderGroup.getOrderHeader(order1.getOrderId()));
		
		// Check if we can add this order.
		orderGroup.removeOrderHeader(order1.getOrderId());
		Assert.assertNull(orderGroup.getOrderHeader(order1.getOrderId()));
		
		// Release the order group.
		orderGroup.release();
		
		OrderHeader order2 = new OrderHeader();
		order2.setParent(facility);
		order2.setOrderId("2");
		order2.setOrderTypeEnum(OrderTypeEnum.OUTBOUND);
		order2.setOrderDate(new Timestamp(System.currentTimeMillis()));
		order2.setDueDate(new Timestamp(System.currentTimeMillis()));
		order2.setActive(true);
		order2.setUpdated(new Timestamp(System.currentTimeMillis()));
		mOrderHeaderDao.store(order2);
		
		// Check if we can add this order.
		orderGroup.addOrderHeader(order2);
		Assert.assertNotNull(orderGroup.getOrderHeader(order2.getOrderId()));
		
		// Check if we can add this order.
		orderGroup.removeOrderHeader(order2.getOrderId());
		Assert.assertNull(orderGroup.getOrderHeader(order2.getOrderId()));
				
		Assert.assertFalse(orderGroup.release());	
	}

	@Test
	public final void releaseOrderGroupTest() {
		
		Organization organization = new Organization();
		organization.setOrganizationId("OG.2");
		mOrganizationDao.store(organization);
		
		organization.createFacility("F1", "test", Point.getZeroPoint());
		Facility facility = organization.getFacility("F1");

		OrderGroup orderGroup = new OrderGroup();
		orderGroup.setParent(facility);
		orderGroup.setOrderGroupId("OG.2");
		orderGroup.setActive(true);
		orderGroup.setUpdated(new Timestamp(System.currentTimeMillis()));
		mOrderGroupDao.store(orderGroup);
		
		OrderHeader order1 = new OrderHeader();
		order1.setParent(facility);
		order1.setOrderId("1");
		order1.setOrderTypeEnum(OrderTypeEnum.OUTBOUND);
		order1.setOrderTypeEnum(OrderTypeEnum.OUTBOUND);
		order1.setOrderDate(new Timestamp(System.currentTimeMillis()));
		order1.setDueDate(new Timestamp(System.currentTimeMillis()));
		order1.setActive(true);
		order1.setUpdated(new Timestamp(System.currentTimeMillis()));
		mOrderHeaderDao.store(order1);
		
		orderGroup.addOrderHeader(order1);
		
		Assert.assertTrue(orderGroup.release());
		
		Assert.assertFalse(orderGroup.release());	
	}
}
