package com.gadgetworks.codeshelf.model.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang.NotImplementedException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.gadgetworks.codeshelf.model.PositionTypeEnum;
import com.gadgetworks.codeshelf.model.domain.Aisle;
import com.gadgetworks.codeshelf.model.domain.Facility;
import com.gadgetworks.codeshelf.model.domain.IDomainObject;
import com.gadgetworks.codeshelf.model.domain.Organization;
import com.gadgetworks.codeshelf.model.domain.PersistentProperty;
import com.gadgetworks.codeshelf.model.domain.Point;
import com.gadgetworks.codeshelf.platform.services.PersistencyService;
import com.google.inject.Inject;

public class GenericDaoTest {
	
	PersistencyService persistencyService = new PersistencyService();
	
	public GenericDaoTest() {
		PersistencyService persistencyService = new PersistencyService();
	}

	public class OrganizationDao extends GenericDaoABC<Organization> {
		@Inject
		public OrganizationDao(PersistencyService persistencyService) {
			super(persistencyService);
		}

		public final Class<Organization> getDaoClass() {
			return Organization.class;
		}
	}

	public class FacilityDao extends GenericDaoABC<Facility> {
		@Inject
		public FacilityDao(PersistencyService persistencyService) {
			super(persistencyService);
		}

		public final Class<Facility> getDaoClass() {
			return Facility.class;
		}
	}

	public class AisleDao extends GenericDaoABC<Aisle> {
		@Inject
		public AisleDao(PersistencyService persistencyService) {
			super(persistencyService);
		}
		public final Class<Aisle> getDaoClass() {
			return Aisle.class;
		}
	}

	@Before
	public final void setup() {
	}

	@Test
	public final void testPushNonPersistentUpdates() {
		OrganizationDao dao = new OrganizationDao(persistencyService);

		Organization organization = new Organization();
		organization.setDomainId("NON-PERSIST");
		organization.setDescription("NON-PERSIST");

		final Result checkUpdate = new Result();

		IDaoListener listener = new IDaoListener() {
			public void objectUpdated(IDomainObject inObject, Set<String> inChangedProperties) {
				checkUpdate.result = true;
			}

			public void objectDeleted(IDomainObject inObject) {
			}

			public void objectAdded(IDomainObject inObject) {
			}
		};

		dao.registerDAOListener(listener);
		dao.pushNonPersistentUpdates(organization);
		Assert.assertTrue(checkUpdate.result);

		dao.unregisterDAOListener(listener);
		checkUpdate.result = false;
		dao.pushNonPersistentUpdates(organization);
		Assert.assertFalse(checkUpdate.result);

	}

	@Test
	public final void testFindByFilter() {
		OrganizationDao dao = new OrganizationDao(persistencyService);

		List<Long> persistentIdList = new ArrayList<Long>();

		Organization organization = new Organization();
		organization.setDomainId("LOADBYFILTERTEST1");
		organization.setDescription("LOADBYFILTER");
		dao.store(organization);

		organization = new Organization();
		organization.setDomainId("LOADBYFILTERTEST2");
		organization.setDescription("LOADBYFILTER");
		dao.store(organization);

		Map<String, Object> filterParams = new HashMap<String, Object>();
		filterParams.put("theId", "LOADBYFILTERTEST1");

		List<Organization> foundOrganizationList = dao.findByFilter("domainId = :theId", filterParams);

		Assert.assertEquals(1, foundOrganizationList.size());
	}

	@Test
	public final void testLoadByPersistentId() {
		OrganizationDao dao = new OrganizationDao(persistencyService);

		Organization organization = new Organization();
		organization.setDomainId("LOADBY-TEST");
		organization.setDescription("LOADBY-TEST");
		dao.store(organization);

		Organization foundOrganization = dao.findByPersistentId(organization.getPersistentId());

		Assert.assertNotNull(foundOrganization);
	}

	@Test
	public final void testLoadByPersistentIdList() {
		OrganizationDao dao = new OrganizationDao(persistencyService);

		List<UUID> persistentIdList = new ArrayList<UUID>();

		Organization organization = new Organization();
		organization.setDomainId("LOADBYLIST-TEST1");
		organization.setDescription("LOADBYLIST-TEST1");
		dao.store(organization);

		persistentIdList.add(organization.getPersistentId());

		organization = new Organization();
		organization.setDomainId("LOADBYLIST-TEST2");
		organization.setDescription("LOADBYLIST-TEST2");
		dao.store(organization);

		persistentIdList.add(organization.getPersistentId());

		List<Organization> foundOrganizationList = dao.findByPersistentIdList(persistentIdList);

		Assert.assertEquals(2, foundOrganizationList.size());
	}

	@Test
	public final void testFindByDomainIdDontIncludeParentDomainId() {

		String ORGANIZATION_ID = "FIND-BY-DOMAINID";
		String FACILITY_ID = "FIND-BY-DOMAINID";

		OrganizationDao organizationDao = new OrganizationDao(persistencyService);
		FacilityDao faciltyDao = new FacilityDao(persistencyService);

		Organization organization1 = new Organization();
		organization1.setDomainId(ORGANIZATION_ID);
		organization1.setDescription(ORGANIZATION_ID);
		organizationDao.store(organization1);

		Facility facility = new Facility(organization1, FACILITY_ID, new Point(PositionTypeEnum.GPS, 0.0, 0.0, 0.0));
		facility.setDescription(FACILITY_ID);
		faciltyDao.store(facility);

		Facility foundFacility = faciltyDao.findByDomainId(organization1, FACILITY_ID);
		Assert.assertNotNull(foundFacility);

		Organization foundOrganization = organizationDao.findByDomainId(null, ORGANIZATION_ID);
		Assert.assertNotNull(foundOrganization);
	}

	@Test
	public final void testFindByDomainIdIncludeParentDomainId() {

		String ORGANIZATION_ID = "ORG-FIND-BY-DOMAINID-INC";
		String FACILITY_ID = "FAC-FIND-BY-DOMAINID-INC";
		String FACILITY2_ID = "FAC2-FIND-BY-DOMAINID-INC";
		String AISLE_ID = "AISLE-FIND-BY-DOMAINID-INC";

		OrganizationDao organizationDao = new OrganizationDao(persistencyService);
		FacilityDao faciltyDao = new FacilityDao(persistencyService);
		AisleDao aisleDao = new AisleDao(persistencyService);

		Organization organization1 = new Organization();
		organization1.setDomainId(ORGANIZATION_ID);
		organization1.setDescription(ORGANIZATION_ID);
		organizationDao.store(organization1);

		Facility facility = new Facility(organization1, FACILITY_ID, new Point(PositionTypeEnum.GPS, 0.0, 0.0, 0.0));
		facility.setDescription(FACILITY_ID);
		faciltyDao.store(facility);

		Aisle aisle1 = new Aisle(facility,
			AISLE_ID,
			new Point(PositionTypeEnum.GPS, 0.0, 0.0, 0.0),
			new Point(PositionTypeEnum.GPS, 0.0, 0.0, 0.0));
		aisle1.setDomainId(AISLE_ID);
		aisleDao.store(aisle1);

		Aisle foundAisle = aisleDao.findByDomainId(facility, AISLE_ID);
		Assert.assertNotNull(foundAisle);

		facility = new Facility(organization1, FACILITY2_ID, new Point(PositionTypeEnum.GPS, 0.0, 0.0, 0.0));
		facility.setDescription(FACILITY2_ID);
		faciltyDao.store(facility);

		foundAisle = aisleDao.findByDomainId(facility, AISLE_ID);
		Assert.assertNull(foundAisle);
	}

	@Test
	public final void testStoreNew() {
		OrganizationDao dao = new OrganizationDao(persistencyService);

		Organization organization1 = new Organization();
		organization1.setDomainId("STORE-TEST-NEW");
		organization1.setDescription("STORE-TEST-NEW");
		dao.store(organization1);
		
		throw new NotImplementedException();

		/*
		try {
			Class.forName("org.h2.Driver");
			Connection connection = DriverManager.getConnection(mSchemaManager.getApplicationDatabaseURL(),
				"codeshelf",
				"codeshelf");

			// Try to switch to the proper schema.
			Statement stmt = connection.createStatement();
			ResultSet resultSet = stmt.executeQuery("SELECT * FROM " + mSchemaManager.getDbSchemaName()
					+ ".ORGANIZATION WHERE DOMAINID = 'STORE-TEST-NEW'");

			if (!resultSet.next()) {
				Assert.fail();
			} else {
				Assert.assertEquals("STORE-TEST-NEW", resultSet.getString("DOMAINID"));
				Assert.assertEquals("STORE-TEST-NEW", resultSet.getString("DESCRIPTION"));
			}
			stmt.close();
			connection.close();
		} catch (ClassNotFoundException e) {
		} catch (SQLException e) {
		}
		*/
	}

	@Test
	public final void testStoreUpdate() {
		OrganizationDao dao = new OrganizationDao(persistencyService);

		Organization organization2 = new Organization();
		organization2.setDomainId("STORE-TEST-UPDATE");
		organization2.setDescription("STORE-TEST-UPDATE");
		dao.store(organization2);
		organization2.setDescription("STORE-TEST-UPDATED");
		dao.store(organization2);

		throw new NotImplementedException();
		
		/*
		try {
			Class.forName("org.h2.Driver");
			Connection connection = DriverManager.getConnection(mSchemaManager.getApplicationDatabaseURL(),
				"codeshelf",
				"codeshelf");

			// Try to switch to the proper schema.
			Statement stmt = connection.createStatement();
			ResultSet resultSet = stmt.executeQuery("SELECT * FROM " + mSchemaManager.getDbSchemaName()
					+ ".ORGANIZATION WHERE DOMAINID = 'STORE-TEST-UPDATE'");

			if (!resultSet.next()) {
				Assert.fail();
			} else {
				Assert.assertEquals("STORE-TEST-UPDATE", resultSet.getString("DOMAINID"));
				Assert.assertEquals("STORE-TEST-UPDATED", resultSet.getString("DESCRIPTION"));
			}
			stmt.close();
			connection.close();
		} catch (ClassNotFoundException e) {
		} catch (SQLException e) {
		}
		*/

	}

	@Test
	public final void testDelete() {
		OrganizationDao dao = new OrganizationDao(persistencyService);

		Organization organization2 = new Organization();
		organization2.setDomainId("DELETE-TEST");
		organization2.setDescription("DELETE-TEST");
		dao.store(organization2);

		dao.delete(organization2);

		throw new NotImplementedException();
		
		/*
		try {
			Class.forName("org.h2.Driver");
			Connection connection = DriverManager.getConnection(mSchemaManager.getApplicationDatabaseURL(),
				"codeshelf",
				"codeshelf");

			// Try to switch to the proper schema.
			Statement stmt = connection.createStatement();
			ResultSet resultSet = stmt.executeQuery("SELECT * FROM " + mSchemaManager.getDbSchemaName()
					+ ".ORGANIZATION WHERE DOMAINID = 'DELETE-TEST'");

			if (!resultSet.next()) {
				// This is the pass condition.
			} else {
				Assert.fail();
			}
			stmt.close();
			connection.close();
		} catch (ClassNotFoundException e) {
		} catch (SQLException e) {
		}
		*/

	}

	@Test
	public final void testGetAll() {
		OrganizationDao dao = new OrganizationDao(persistencyService);

		Organization organization = new Organization();
		organization.setDomainId("GETALL-TEST1");
		organization.setDescription("GETALL-TEST1");
		dao.store(organization);

		organization = new Organization();
		organization.setDomainId("GETALL-TEST2");
		organization.setDescription("GETALL-TEST2");
		dao.store(organization);

		// This is not a great test - all these DB tests run in parallel against the same DB.
		// There's no way to know how many items getAll() will return, so we just look for the ones we put in.
		int totalFound = 0;
		for (Organization organiation : dao.getAll()) {
			if (organiation.getDomainId().startsWith("GETALL-TEST")) {
				totalFound++;
			}
		}

		Assert.assertEquals(2, totalFound);
	}

}
