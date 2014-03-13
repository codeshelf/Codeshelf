/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: FacilityTest.java,v 1.11 2013/11/11 07:46:30 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.domain;

import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.gadgetworks.codeshelf.application.IUtil;
import com.gadgetworks.codeshelf.model.dao.Database;
import com.gadgetworks.codeshelf.model.dao.H2SchemaManager;
import com.gadgetworks.codeshelf.model.dao.IDatabase;
import com.gadgetworks.codeshelf.model.dao.ISchemaManager;
import com.gadgetworks.codeshelf.model.dao.ITypedDao;
import com.gadgetworks.codeshelf.model.domain.Aisle.AisleDao;
import com.gadgetworks.codeshelf.model.domain.Bay.BayDao;
import com.gadgetworks.codeshelf.model.domain.Facility.FacilityDao;
import com.gadgetworks.codeshelf.model.domain.LocationABC.LocationABCDao;
import com.gadgetworks.codeshelf.model.domain.Organization.OrganizationDao;
import com.gadgetworks.codeshelf.model.domain.Path.PathDao;
import com.gadgetworks.codeshelf.model.domain.PathSegment.PathSegmentDao;
import com.gadgetworks.codeshelf.model.domain.Vertex.VertexDao;
import com.gadgetworks.codeshelf.model.domain.WorkArea.WorkAreaDao;

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

		Facility facility = new Facility(0.0, 0.0);
		facility.setParent(organization);
		facility.setFacilityId("FTEST1.F1");
		mFacilityDao.store(facility);

		facility.createAisle("FTEST1.A1", 1.0, 1.0, 2.0, 2.0, 2.0, 2, 5, true, true);

		Facility foundFacility = Facility.DAO.findByDomainId(organization, "FTEST1.F1");

		Assert.assertNotNull(foundFacility);
	}
	
	@Test
	public final void createWorkInstructionTest() {
		
		List<WorkInstruction> wiList;
		
		Organization organization = new Organization();
		organization.setOrganizationId("FTEST2.O1");
		mOrganizationDao.store(organization);

		Facility facility = new Facility(0.0, 0.0);
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
