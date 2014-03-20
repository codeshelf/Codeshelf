/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: FacilityTest.java,v 1.11 2013/11/11 07:46:30 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.domain;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;

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

		Facility facility = new Facility(new Point(PositionTypeEnum.GPS, 0.0, 0.0, 0.0));
		facility.setParent(organization);
		facility.setFacilityId("FTEST1.F1");
		mFacilityDao.store(facility);

		facility.createAisle("FTEST1.A1", 1.0, 1.0, 2.0, 2.0, 2.0, 2, 5, true, true, true);

		Facility foundFacility = Facility.DAO.findByDomainId(organization, "FTEST1.F1");

		Assert.assertNotNull(foundFacility);
	}

	@Test
	public final void createWorkInstructionTest() {

		List<WorkInstruction> wiList;
			/*
		Organization.DAO = new OrganizationDao(mSchemaManager);
		LocationABC.DAO = new LocationDao(mSchemaManager, mDatabase);
		Facility.DAO = new FacilityDao(mSchemaManager);
		Aisle.DAO = new AisleDao(mSchemaManager);
		Bay.DAO = new BayDao(mSchemaManager);
		Vertex.DAO = new VertexDao(mSchemaManager);
		Path.DAO = new PathDao(mSchemaManager);
		PathSegment.DAO = new PathSegmentDao(mSchemaManager);
		WorkArea.DAO = new WorkAreaDao(mSchemaManager);
			*/

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
}
