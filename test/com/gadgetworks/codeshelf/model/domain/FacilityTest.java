/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: FacilityTest.java,v 1.11 2013/11/11 07:46:30 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.domain;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.BooleanNode;
import org.codehaus.jackson.node.ObjectNode;
import org.junit.Assert;
import org.junit.Test;

import com.gadgetworks.codeshelf.model.PositionTypeEnum;
import com.google.common.collect.Lists;

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
