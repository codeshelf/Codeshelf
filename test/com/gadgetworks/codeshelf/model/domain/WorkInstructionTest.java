/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: OrderGroupTest.java,v 1.4 2013/07/22 04:30:36 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.domain;

import java.sql.Timestamp;

import org.junit.Assert;
import org.junit.Test;

import com.gadgetworks.codeshelf.model.OrderStatusEnum;
import com.gadgetworks.codeshelf.model.OrderTypeEnum;
import com.gadgetworks.codeshelf.model.WorkInstructionStatusEnum;
import com.gadgetworks.codeshelf.model.WorkInstructionTypeEnum;

/**
 * @author jeffw
 *
 */
public class WorkInstructionTest extends DomainTestABC {

	@Test
	public final void addRemoveOrderGroupTest() {

		Organization organization = new Organization();
		organization.setOrganizationId("OWI.1");
		mOrganizationDao.store(organization);

		organization.createFacility("F1", "test", Point.getZeroPoint());
		Facility facility = organization.getFacility("F1");

		Aisle aisle1 = new Aisle(facility, "A1", Point.getZeroPoint(), Point.getZeroPoint());
		mAisleDao.store(aisle1);

		Bay baya1b1 = new Bay(aisle1, "B1", Point.getZeroPoint(), Point.getZeroPoint());
		mBayDao.store(baya1b1);
		Bay baya1b2 = new Bay(aisle1, "B2", Point.getZeroPoint(), Point.getZeroPoint());
		mBayDao.store(baya1b2);

		Aisle aisle2 = new Aisle(facility, "A2", Point.getZeroPoint(), Point.getZeroPoint());
		mAisleDao.store(aisle2);

		Bay baya2b1 = new Bay(aisle2, "B1", Point.getZeroPoint(), Point.getZeroPoint());
		mBayDao.store(baya2b1);
		Bay baya2b2 = new Bay(aisle2, "B2", Point.getZeroPoint(), Point.getZeroPoint());
		mBayDao.store(baya2b2);

		Container container = new Container();
		container.setDomainId("C1");
		container.setParent(facility);
		container.setKind(facility.getContainerKind(ContainerKind.DEFAULT_CONTAINER_KIND));
		container.setActive(true);
		container.setUpdated(new Timestamp(System.currentTimeMillis()));
		mContainerDao.store(container);

		UomMaster uomMaster = new UomMaster();
		uomMaster.setUomMasterId("EA");
		uomMaster.setParent(facility);
		mUomMasterDao.store(uomMaster);
		facility.addUomMaster(uomMaster);

		ItemMaster itemMaster = new ItemMaster();
		itemMaster.setItemId("AAA");
		itemMaster.setParent(facility);
		itemMaster.setStandardUom(uomMaster);
		itemMaster.setActive(true);
		itemMaster.setUpdated(new Timestamp(System.currentTimeMillis()));
		mItemMasterDao.store(itemMaster);

		OrderHeader order1 = new OrderHeader();
		order1.setParent(facility);
		order1.setOrderId("1");
		order1.setOrderTypeEnum(OrderTypeEnum.OUTBOUND);
		order1.setOrderDate(new Timestamp(System.currentTimeMillis()));
		order1.setDueDate(new Timestamp(System.currentTimeMillis()));
		order1.setActive(true);
		order1.setUpdated(new Timestamp(System.currentTimeMillis()));
		mOrderHeaderDao.store(order1);

		OrderDetail orderDetail = new OrderDetail();
		orderDetail.setDomainId(itemMaster.getItemId());
		orderDetail.setParent(order1);
		orderDetail.setItemMaster(itemMaster);
		orderDetail.setQuantity(5);
		orderDetail.setMinQuantity(5);
		orderDetail.setMaxQuantity(5);
		orderDetail.setUomMaster(uomMaster);
		orderDetail.setStatusEnum(OrderStatusEnum.CREATED);
		orderDetail.setActive(true);
		orderDetail.setUpdated(new Timestamp(System.currentTimeMillis()));
		mOrderDetailDao.store(orderDetail);
		order1.addOrderDetail(orderDetail);

		WorkInstruction wi = new WorkInstruction();
		wi.setParent(orderDetail);
		wi.setCreated(new Timestamp(System.currentTimeMillis()));

		// Update the WI
		wi.setDomainId(Long.toString(System.currentTimeMillis()));
		wi.setTypeEnum(WorkInstructionTypeEnum.PLAN);
		wi.setStatusEnum(WorkInstructionStatusEnum.NEW);

		wi.setLocation(baya1b2);
		wi.setLocationId(baya1b2.getFullDomainId());
		wi.setItemMaster(itemMaster);
		wi.setDescription(itemMaster.getDescription());
		wi.setPickInstruction("pick instruction");
		wi.setDescription("description");

		wi.setPosAlongPath(0.0);
		wi.setContainer(container);
		wi.setPlanQuantity(5);
		wi.setPlanMinQuantity(5);
		wi.setPlanMaxQuantity(5);
		wi.setActualQuantity(0);
		wi.setAssigned(new Timestamp(System.currentTimeMillis()));
		mWorkInstructionDao.store(wi);

		// Check if the work instruction is contained by the facility, aisle and bay
		Assert.assertTrue(wi.isContainedByLocation(facility));
		Assert.assertTrue(wi.isContainedByLocation(aisle1));
		Assert.assertTrue(wi.isContainedByLocation(baya1b2));

		Assert.assertFalse(wi.isContainedByLocation(aisle2));
		Assert.assertFalse(wi.isContainedByLocation(null));

	}
}
