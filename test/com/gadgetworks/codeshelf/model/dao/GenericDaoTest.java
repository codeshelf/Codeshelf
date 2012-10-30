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

import junit.framework.Assert;

import org.junit.BeforeClass;
import org.junit.Test;

import com.gadgetworks.codeshelf.application.IUtil;
import com.gadgetworks.codeshelf.model.domain.Aisle;
import com.gadgetworks.codeshelf.model.domain.Facility;
import com.gadgetworks.codeshelf.model.domain.IDomainObject;
import com.gadgetworks.codeshelf.model.domain.Organization;
import com.gadgetworks.codeshelf.model.domain.PersistentProperty;

public class GenericDaoTest {

	private static final String	DB_INIT_URL	= "jdbc:h2:mem:database;DB_CLOSE_DELAY=-1";
	private static final String	DB_URL		= "jdbc:h2:mem:database;SCHEMA=CODESHELF;DB_CLOSE_DELAY=-1";

	public class OrganizationDao extends GenericDaoABC<Organization> {
		public final Class<Organization> getDaoClass() {
			return Organization.class;
		}
	}

	public class FacilityDao extends GenericDaoABC<Facility> {
		public final Class<Facility> getDaoClass() {
			return Facility.class;
		}
	}

	public class AisleDao extends GenericDaoABC<Aisle> {
		public final Class<Aisle> getDaoClass() {
			return Aisle.class;
		}
	}
	
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

				public String getApplicationInitDatabaseURL() {
					return DB_INIT_URL;
				}

				public String getApplicationDatabaseURL() {
					return DB_URL;
				}

				public String getApplicationDataDirPath() {
					return ".";
				}

				public void exitSystem() {
					System.exit(-1);
				}
			};

			Class.forName("org.h2.Driver");
			mSchemaManager = new H2SchemaManager(mUtil);
			mDatabase = new Database(mSchemaManager, mUtil);

			mDatabase.start();
		} catch (ClassNotFoundException e) {
		}
	}

	@Test
	public void testPushNonPersistentUpdates() {
		OrganizationDao dao = new OrganizationDao();

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
	public void testFindByFilter() {
		OrganizationDao dao = new OrganizationDao();

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
	public void testLoadByPersistentId() {
		OrganizationDao dao = new OrganizationDao();

		Organization organization = new Organization();
		organization.setDomainId("LOADBY-TEST");
		organization.setDescription("LOADBY-TEST");
		dao.store(organization);

		Organization foundOrganization = dao.findByPersistentId(organization.getPersistentId());

		Assert.assertNotNull(foundOrganization);
	}

	@Test
	public void testLoadByPersistentIdList() {
		OrganizationDao dao = new OrganizationDao();

		List<Long> persistentIdList = new ArrayList<Long>();

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
	public void testFindByDomainIdDontIncludeParentDomainId() {

		String ORGANIZATION_ID = "FIND-BY-DOMAINID";
		String FACILITY_ID = "FIND-BY-DOMAINID";

		OrganizationDao organizationDao = new OrganizationDao();
		FacilityDao faciltyDao = new FacilityDao();

		Organization organization1 = new Organization();
		organization1.setDomainId(ORGANIZATION_ID);
		organization1.setDescription(ORGANIZATION_ID);
		organizationDao.store(organization1);

		Facility facility = new Facility(0.0, 0.0);
		facility.setParentOrganization(organization1);
		facility.setDomainId(FACILITY_ID);
		facility.setDescription(FACILITY_ID);
		faciltyDao.store(facility);

		Facility foundFacility = faciltyDao.findByDomainId(organization1, FACILITY_ID);
		Assert.assertNotNull(foundFacility);

		Organization foundOrganization = organizationDao.findByDomainId(null, ORGANIZATION_ID);
		Assert.assertNotNull(foundOrganization);
	}

	@Test
	public void testFindByDomainIdIncludeParentDomainId() {

		String ORGANIZATION_ID = "FIND-BY-DOMAINID-INC";
		String FACILITY_ID = "FIND-BY-DOMAINID-INC";
		String AISLE_ID = "FIND-BY-DOMAINID-INC";

		OrganizationDao organizationDao = new OrganizationDao();
		FacilityDao faciltyDao = new FacilityDao();
		AisleDao aisleDao = new AisleDao();

		Organization organization1 = new Organization();
		organization1.setDomainId(ORGANIZATION_ID);
		organization1.setDescription(ORGANIZATION_ID);
		organizationDao.store(organization1);

		Facility facility = new Facility(0.0, 0.0);
		facility.setParentOrganization(organization1);
		facility.setDomainId(FACILITY_ID);
		facility.setDescription(FACILITY_ID);
		faciltyDao.store(facility);

		Aisle aisle1 = new Aisle(facility, AISLE_ID, 0.0, 0.0);
		aisle1.setDomainId(AISLE_ID);
		aisleDao.store(aisle1);

		Aisle foundAisle = aisleDao.findByDomainId(facility, AISLE_ID);
		Assert.assertNotNull(foundAisle);

		foundAisle = aisleDao.findByDomainId(null, AISLE_ID);
		Assert.assertNull(foundAisle);
	}

	@Test
	public void testStoreNew() {
		OrganizationDao dao = new OrganizationDao();

		Organization organization1 = new Organization();
		organization1.setDomainId("STORE-TEST-NEW");
		organization1.setDescription("STORE-TEST-NEW");
		dao.store(organization1);

		try {
			Class.forName("org.h2.Driver");
			Connection connection = DriverManager.getConnection(DB_URL, "codeshelf", "codeshelf");

			// Try to switch to the proper schema.
			Statement stmt = connection.createStatement();
			ResultSet resultSet = stmt.executeQuery("SELECT * FROM " + ISchemaManager.DATABASE_SCHEMA_NAME + ".ORGANIZATION WHERE DOMAINID = 'STORE-TEST-NEW'");

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
	}

	@Test
	public void testStoreUpdate() {
		OrganizationDao dao = new OrganizationDao();

		Organization organization2 = new Organization();
		organization2.setDomainId("STORE-TEST-UPDATE");
		organization2.setDescription("STORE-TEST-UPDATE");
		dao.store(organization2);
		organization2.setDescription("STORE-TEST-UPDATED");
		dao.store(organization2);

		try {
			Class.forName("org.h2.Driver");
			Connection connection = DriverManager.getConnection(DB_URL, "codeshelf", "codeshelf");

			// Try to switch to the proper schema.
			Statement stmt = connection.createStatement();
			ResultSet resultSet = stmt.executeQuery("SELECT * FROM " + ISchemaManager.DATABASE_SCHEMA_NAME + ".ORGANIZATION WHERE DOMAINID = 'STORE-TEST-UPDATE'");

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

	}

	@Test
	public void testDelete() {
		OrganizationDao dao = new OrganizationDao();

		Organization organization2 = new Organization();
		organization2.setDomainId("DELETE-TEST");
		organization2.setDescription("DELETE-TEST");
		dao.store(organization2);

		dao.delete(organization2);

		try {
			Class.forName("org.h2.Driver");
			Connection connection = DriverManager.getConnection(DB_URL, "codeshelf", "codeshelf");

			// Try to switch to the proper schema.
			Statement stmt = connection.createStatement();
			ResultSet resultSet = stmt.executeQuery("SELECT * FROM " + ISchemaManager.DATABASE_SCHEMA_NAME + ".ORGANIZATION WHERE DOMAINID = 'DELETE-TEST'");

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

	}

	@Test
	public void testGetAll() {
		OrganizationDao dao = new OrganizationDao();

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
