/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: FacilityTest.java,v 1.10 2013/11/05 06:14:55 jeffw Exp $
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
import com.gadgetworks.codeshelf.model.domain.LocationABC.LocationDao;
import com.gadgetworks.codeshelf.model.domain.Organization.OrganizationDao;
import com.gadgetworks.codeshelf.model.domain.Path.PathDao;
import com.gadgetworks.codeshelf.model.domain.PathSegment.PathSegmentDao;
import com.gadgetworks.codeshelf.model.domain.Vertex.VertexDao;
import com.gadgetworks.codeshelf.model.domain.WorkArea.WorkAreaDao;

/**
 * @author jeffw
 *
 */
public class FacilityTest {

	private IUtil			mUtil;
	private ISchemaManager	mSchemaManager;
	private IDatabase		mDatabase;

	@Before
	public final void setup() {

		try {
			mUtil = new IUtil() {

				public void setLoggingLevelsFromPrefs(Organization inOrganization, ITypedDao<PersistentProperty> inPersistentPropertyDao) {
				}

				public String getVersionString() {
					return "";
				}

				public String getApplicationLogDirPath() {
					return ".";
				}

				public String getApplicationDataDirPath() {
					return ".";
				}

				public void exitSystem() {
					System.exit(-1);
				}
			};

			Class.forName("org.h2.Driver");
			mSchemaManager = new H2SchemaManager(mUtil, "codeshelf", "codeshelf", "codeshelf", "codeshelf", "localhost", "");
			mDatabase = new Database(mSchemaManager, mUtil);

			mDatabase.start();
		} catch (ClassNotFoundException e) {
		}
	}

	@Test
	public final void createAisleTest() {

		Organization.DAO = new OrganizationDao(mSchemaManager);
		LocationABC.DAO = new LocationDao(mSchemaManager, mDatabase);
		Facility.DAO = new FacilityDao(mSchemaManager);
		Aisle.DAO = new AisleDao(mSchemaManager);
		Bay.DAO = new BayDao(mSchemaManager);
		Vertex.DAO = new VertexDao(mSchemaManager);
		Path.DAO = new PathDao(mSchemaManager);
		PathSegment.DAO = new PathSegmentDao(mSchemaManager);
		WorkArea.DAO = new WorkAreaDao(mSchemaManager);

		Organization organization = new Organization();
		organization.setOrganizationId("FTEST.O1");
		Organization.DAO.store(organization);

		Facility facility = new Facility(0.0, 0.0);
		facility.setParent(organization);
		facility.setFacilityId("FTEST.F1");
		Facility.DAO.store(facility);

		facility.createAisle("FTEST.A1", 1.0, 1.0, 2.0, 2.0, 2.0, 2, 5, true, true);

		Facility foundFacility = Facility.DAO.findByDomainId(organization, "FTEST.F1");

		Assert.assertNotNull(foundFacility);
	}
	
	@Test
	public final void createWorkInstructionTest() {
		
		List<WorkInstruction> wiList;
		
		Organization.DAO = new OrganizationDao(mSchemaManager);
		LocationABC.DAO = new LocationDao(mSchemaManager, mDatabase);
		Facility.DAO = new FacilityDao(mSchemaManager);
		Aisle.DAO = new AisleDao(mSchemaManager);
		Bay.DAO = new BayDao(mSchemaManager);
		Vertex.DAO = new VertexDao(mSchemaManager);
		Path.DAO = new PathDao(mSchemaManager);
		PathSegment.DAO = new PathSegmentDao(mSchemaManager);
		WorkArea.DAO = new WorkAreaDao(mSchemaManager);

		Organization organization = new Organization();
		organization.setOrganizationId("FTEST.O1");
		Organization.DAO.store(organization);

		Facility facility = new Facility(0.0, 0.0);
		facility.setParent(organization);
		facility.setFacilityId("FTEST.F2");
		Facility.DAO.store(facility);
		
//		wiList = facility.getWorkInstructions(inChe, inLocationId, inContainerIdList);
//		
//		Assert.assertNotNull(wiList);
//		for (WorkInstruction wi : wiList) {
//			Assert.assertNotNull(wi);
//			
//		}
	}
}
