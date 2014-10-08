package com.gadgetworks.codeshelf.model.dao;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;
import org.junit.Assert;
import org.junit.Test;

import com.gadgetworks.codeshelf.application.Configuration;
import com.gadgetworks.codeshelf.model.PositionTypeEnum;
import com.gadgetworks.codeshelf.model.domain.Aisle;
import com.gadgetworks.codeshelf.model.domain.Facility;
import com.gadgetworks.codeshelf.model.domain.Organization;
import com.gadgetworks.codeshelf.model.domain.Point;

public class GenericDaoTest extends DAOTestABC {
		
	static {
		Configuration.loadConfig("test");
	}
	
	public GenericDaoTest() {
	}

	@Test
	public final void testFindByFilter() {
		Session session = persistenceService.getCurrentTenantSession();
		Transaction t = session.beginTransaction();
		Organization organization = new Organization();
		organization.setDomainId("LOADBYFILTERTEST1");
		organization.setDescription("LOADBYFILTER");
		mOrganizationDao.store(organization);
		organization = new Organization();
		organization.setDomainId("LOADBYFILTERTEST2");
		organization.setDescription("LOADBYFILTER");
		mOrganizationDao.store(organization);
		
		List<Criterion> filterParams = new ArrayList<Criterion>();
		filterParams.add(Restrictions.eq("domainId", "LOADBYFILTERTEST1"));
		List<Organization> foundOrganizationList = mOrganizationDao.findByFilter(filterParams);
		
		t.commit();
		Assert.assertEquals(1, foundOrganizationList.size());
	}

	@Test
	public final void testLoadByPersistentId() {
		// store new organization
		String desc = "Test-Desc";
		Session session = persistenceService.getCurrentTenantSession();
		Transaction t = session.beginTransaction();
		Organization organization = new Organization();
		organization.setDomainId("LOADBY-TEST");
		organization.setDescription(desc);
		mOrganizationDao.store(organization);
		t.commit();

		// load stored org and check data in a sep transaction
		session = persistenceService.getCurrentTenantSession();
		t = session.beginTransaction();
		Organization foundOrganization = mOrganizationDao.findByPersistentId(organization.getPersistentId());
		t.commit();
		Assert.assertNotNull(foundOrganization);
		Assert.assertEquals(desc, foundOrganization.getDescription());
	}

	@Test
	public final void testLoadByPersistentIdList() {
		Session session = persistenceService.getCurrentTenantSession();
		Transaction t = session.beginTransaction();

		List<UUID> persistentIdList = new ArrayList<UUID>();
		Organization organization = new Organization();
		organization.setDomainId("LOADBYLIST-TEST1");
		organization.setDescription("LOADBYLIST-TEST1");
		mOrganizationDao.store(organization);
		persistentIdList.add(organization.getPersistentId());

		organization = new Organization();
		organization.setDomainId("LOADBYLIST-TEST2");
		organization.setDescription("LOADBYLIST-TEST2");
		mOrganizationDao.store(organization);
		persistentIdList.add(organization.getPersistentId());
		t.commit();
		
		session = persistenceService.getCurrentTenantSession();
		t = session.beginTransaction();
		List<Organization> foundOrganizationList = mOrganizationDao.findByPersistentIdList(persistentIdList);
		Assert.assertEquals(2, foundOrganizationList.size());
		t.commit();
	}

	@Test
	public final void testFindByDomainIdDontIncludeParentDomainId() {
		String ORGANIZATION_ID = "FIND-BY-DOMAINID";
		String FACILITY_ID = "FIND-BY-DOMAINID";

		Session session = persistenceService.getCurrentTenantSession();
		Transaction t = session.beginTransaction();

		Organization organization1 = new Organization();
		organization1.setDomainId(ORGANIZATION_ID);
		organization1.setDescription(ORGANIZATION_ID);
		mOrganizationDao.store(organization1);

		Facility facility = organization1.createFacility(FACILITY_ID, FACILITY_ID, new Point(PositionTypeEnum.GPS, 0.0, 0.0, 0.0));
		facility.setDescription(FACILITY_ID);
		mFacilityDao.store(facility);
		t.commit();

		session = persistenceService.getCurrentTenantSession();
		t = session.beginTransaction();
		Facility foundFacility = mFacilityDao.findByDomainId(organization1, FACILITY_ID);
		Assert.assertNotNull(foundFacility);

		Organization foundOrganization = mOrganizationDao.findByDomainId(null, ORGANIZATION_ID);
		Assert.assertNotNull(foundOrganization);
		t.commit();
	}

	@Test
	public final void testFindByDomainIdIncludeParentDomainId() {		
		Session session = persistenceService.getCurrentTenantSession();
		Transaction t = session.beginTransaction();

		String ORGANIZATION_ID = "ORG-FIND-BY-DOMAINID-INC";
		String FACILITY_ID = "FAC-FIND-BY-DOMAINID-INC";
		String FACILITY2_ID = "FAC2-FIND-BY-DOMAINID-INC";
		String AISLE_ID = "AISLE-FIND-BY-DOMAINID-INC";

		Organization organization1 = new Organization();
		organization1.setDomainId(ORGANIZATION_ID);
		organization1.setDescription(ORGANIZATION_ID);
		mOrganizationDao.store(organization1);
		t.commit();

		session = persistenceService.getCurrentTenantSession();
		t = session.beginTransaction();
		Facility facility = organization1.createFacility(FACILITY_ID, FACILITY_ID, new Point(PositionTypeEnum.GPS, 0.0, 0.0, 0.0));
		facility.setDescription(FACILITY_ID);
		organization1.addFacility(facility);
		mFacilityDao.store(facility);

		Aisle aisle1 = facility.createAisle(
			AISLE_ID,
			new Point(PositionTypeEnum.GPS, 0.0, 0.0, 0.0),
			new Point(PositionTypeEnum.GPS, 0.0, 0.0, 0.0));
		aisle1.setDomainId(AISLE_ID);
		mAisleDao.store(aisle1);
		t.commit();

		session = persistenceService.getCurrentTenantSession();
		t = session.beginTransaction();
		Aisle foundAisle = mAisleDao.findByDomainId(facility, AISLE_ID);
		Assert.assertNotNull(foundAisle);

		facility = organization1.createFacility(FACILITY2_ID, FACILITY2_ID, new Point(PositionTypeEnum.GPS, 0.0, 0.0, 0.0));
		facility.setDescription(FACILITY2_ID);
		mFacilityDao.store(facility);
		t.commit();

		session = persistenceService.getCurrentTenantSession();
		t = session.beginTransaction();
		foundAisle = mAisleDao.findByDomainId(facility, AISLE_ID);
		Assert.assertNull(foundAisle);
		t.commit();
	}

	@Test
	public final void testStoreNew() {
		// store new organization
		String desc = "Test-Desc";
		Session session = persistenceService.getCurrentTenantSession();
		Transaction t = session.beginTransaction();
		Organization organization = new Organization();
		organization.setDomainId("LOADBY-TEST");
		organization.setDescription(desc);
		mOrganizationDao.store(organization);
		UUID id = organization.getPersistentId();
		t.commit();
		Assert.assertNotNull(id);
	}

	@Test
	public final void testStoreUpdate() {
		String orgDesc = "org-desc";
		String updatedDesc = "updated-desc";
		// create org
		Session session = persistenceService.getCurrentTenantSession();
		Transaction t = session.beginTransaction();
		Assert.assertNotNull(mOrganizationDao);
		Organization organization = new Organization();
		organization.setDomainId("DELETE-TEST");
		organization.setDescription(orgDesc);
		mOrganizationDao.store(organization);
		UUID id = organization.getPersistentId();
		t.commit();
		
		// make sure org exists and then update it
		session = persistenceService.getCurrentTenantSession();
		t = session.beginTransaction();
		Organization foundOrganization = mOrganizationDao.findByPersistentId(id);
		Assert.assertNotNull(foundOrganization);
		Assert.assertEquals(orgDesc,foundOrganization.getDescription());
		Assert.assertNotNull(foundOrganization.getDao());
		foundOrganization.setDescription(updatedDesc);
		mOrganizationDao.store(foundOrganization);
		t.commit();
		Assert.assertNotNull(foundOrganization);
		
		// now reload it again and make sure desc has changed
		session = persistenceService.getCurrentTenantSession();
		t = session.beginTransaction();
		foundOrganization = mOrganizationDao.findByPersistentId(id);
		Assert.assertNotNull(foundOrganization);
		Assert.assertEquals(updatedDesc,foundOrganization.getDescription());
		t.commit();
	}

	@Test
	public final void testDelete() {
		// first transaction - create org
		Session session = persistenceService.getCurrentTenantSession();
		Transaction t = session.beginTransaction();
		Organization organization = new Organization();
		organization.setDomainId("DELETE-TEST");
		organization.setDescription("DELETE-TEST");
		mOrganizationDao.store(organization);
		UUID id = organization.getPersistentId();
		t.commit();
		
		// make sure org exists and then delete it
		session = persistenceService.getCurrentTenantSession();
		t = session.beginTransaction();
		Organization foundOrganization = mOrganizationDao.findByPersistentId(id);
		Assert.assertNotNull(foundOrganization);
		mOrganizationDao.delete(foundOrganization);
		t.commit();
		Assert.assertNotNull(foundOrganization);
		
		// now try to reload it again
		session = persistenceService.getCurrentTenantSession();
		t = session.beginTransaction();
		foundOrganization = mOrganizationDao.findByPersistentId(id);
		Assert.assertNull(foundOrganization);
		t.commit();
	}

	@Test
	public final void testGetAll() {
		Session session = persistenceService.getCurrentTenantSession();
		Transaction t = session.beginTransaction();

		Organization organization = new Organization();
		organization.setDomainId("GETALL-TEST1");
		organization.setDescription("GETALL-TEST1");
		mOrganizationDao.store(organization);

		organization = new Organization();
		organization.setDomainId("GETALL-TEST2");
		organization.setDescription("GETALL-TEST2");
		mOrganizationDao.store(organization);
		t.commit();
		
		session = persistenceService.getCurrentTenantSession();
		t = session.beginTransaction();
		// This is not a great test - all these DB tests run in parallel against the same DB.
		// There's no way to know how many items getAll() will return, so we just look for the ones we put in.
		int totalFound = 0;
		for (Organization organiation : mOrganizationDao.getAll()) {
			if (organiation.getDomainId().startsWith("GETALL-TEST")) {
				totalFound++;
			}
		}
		t.commit();
		Assert.assertEquals(2, totalFound);
	}

}
