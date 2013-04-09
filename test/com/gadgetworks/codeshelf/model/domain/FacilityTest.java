/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: FacilityTest.java,v 1.8 2013/04/09 07:58:20 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.domain;

import junit.framework.Assert;

import org.junit.BeforeClass;
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

	private static IUtil			mUtil;
	private static ISchemaManager	mSchemaManager;
	private static IDatabase		mDatabase;

	@BeforeClass
	public final static void setup() {

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

		Organization.DAO = new OrganizationDao();
		LocationABC.DAO = new LocationDao();
		Facility.DAO = new FacilityDao();
		Aisle.DAO = new AisleDao();
		Bay.DAO = new BayDao();
		Vertex.DAO = new VertexDao();
		Path.DAO = new PathDao();
		PathSegment.DAO = new PathSegmentDao();
		WorkArea.DAO = new WorkAreaDao();

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

}
