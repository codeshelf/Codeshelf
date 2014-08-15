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
	public final void createAisleTest() {

		Organization organization = new Organization();
		organization.setOrganizationId("FTEST1.O1");
		mOrganizationDao.store(organization);

		Facility facility = new Facility(Point.getZeroPoint());
		facility.setParent(organization);
		facility.setFacilityId("FTEST1.F1");
		mFacilityDao.store(facility);

		Point anchorPoint = new Point(PositionTypeEnum.METERS_FROM_PARENT, 1.0, 1.0, 1.0);
		Point protoBayPoint = new Point(PositionTypeEnum.METERS_FROM_PARENT, 2.0, 2.0, 2.0);
		facility.createAisle("FTEST1.A1", anchorPoint, protoBayPoint, 2, 5, "0x00000002", true, true);

		Facility foundFacility = Facility.DAO.findByDomainId(organization, "FTEST1.F1");

		Assert.assertNotNull(foundFacility);
	}

	@Test
	public final void createWorkInstructionTest() {
		Organization organization = new Organization();
		organization.setOrganizationId("FTEST2.O1");
		mOrganizationDao.store(organization);

		Facility facility = new Facility(new Point(PositionTypeEnum.GPS, 0.0, 0.0, 0.0));
		facility.setParent(organization);
		facility.setFacilityId("FTEST2.F2");
		mFacilityDao.store(facility);

		//		wiList = facility.getWorkInstructions(inChe, inLocationId, inContainerIdList);
		//		
		//		Assert.assertNotNull(wiList);
		//		for (WorkInstruction wi : wiList) {
		//			Assert.assertNotNull(wi);
		//			
		//		}
	}
	
	@Test
	public void testHasCrossbatchOrders() {
		Organization organization = new Organization();
		organization.setOrganizationId("FTEST3.O1");
		mOrganizationDao.store(organization);

		Facility facility = new Facility(new Point(PositionTypeEnum.GPS, 0.0, 0.0, 0.0));
		facility.setParent(organization);
		facility.setFacilityId("FTEST3.F2");
		mFacilityDao.store(facility);
		
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
		Organization organization = new Organization();
		organization.setOrganizationId("FTEST2.O1");

		Facility facility = new Facility(new Point(PositionTypeEnum.GPS, 0.0, 0.0, 0.0));
		facility.setParent(organization);
		facility.setFacilityId("FTEST2.F2");
		
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode objectNode= mapper.valueToTree(facility);
		Assert.assertNotNull(objectNode.findValue("hasMeaningfulOrderGroups"));
		Assert.assertNotNull(objectNode.findValue("hasCrossBatchOrders"));
	}
}
