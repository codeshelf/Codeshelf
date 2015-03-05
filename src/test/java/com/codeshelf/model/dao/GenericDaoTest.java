package com.codeshelf.model.dao;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;
import org.junit.Assert;
import org.junit.Test;

import com.codeshelf.model.PositionTypeEnum;
import com.codeshelf.model.domain.Aisle;
import com.codeshelf.model.domain.CodeshelfNetwork;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.Point;
import com.codeshelf.testframework.ServerTest;

public class GenericDaoTest extends ServerTest {
	
	public GenericDaoTest() {
	}

	@Test
	public final void testFindByFilter() {
		this.tenantPersistenceService.beginTransaction();
		
		Facility facility = Facility.createFacility(getDefaultTenant(), "LOADBYFILTERTEST1", "LOADBYFILTER", Point.getZeroPoint());		
		Facility.DAO.store(facility);
		
		facility = Facility.createFacility(getDefaultTenant(), "LOADBYFILTERTEST2", "LOADBYFILTER", Point.getZeroPoint());
		Facility.DAO.store(facility);
		
		List<Criterion> filterParams = new ArrayList<Criterion>();
		filterParams.add(Restrictions.eq("domainId", "LOADBYFILTERTEST1"));
		List<Facility> foundOrganizationList = Facility.DAO.findByFilter(filterParams);
		
		this.tenantPersistenceService.commitTransaction();
		Assert.assertEquals(1, foundOrganizationList.size());
	}

	@Test
	public final void testLoadByPersistentId() {
		// store new organization
		String desc = "Test-Desc";
		this.tenantPersistenceService.beginTransaction();
		Facility facility = createFacility();
		facility.setDescription(desc);
		Facility.DAO.store(facility);
		this.tenantPersistenceService.commitTransaction();

		// load stored org and check data in a sep transaction
		this.tenantPersistenceService.beginTransaction();
		Facility foundFacility = Facility.DAO.findByPersistentId(facility.getPersistentId());
		this.tenantPersistenceService.commitTransaction();

		Assert.assertNotNull(foundFacility);
		Assert.assertEquals(desc, foundFacility.getDescription());
	}

	@Test
	public final void testLoadByPersistentIdList() {
		this.tenantPersistenceService.beginTransaction();

		List<UUID> persistentIdList = new ArrayList<UUID>();
		Facility facility = createFacility();
		facility.setDomainId("LOADBYLIST-TEST1");
		facility.setDescription("LOADBYLIST-TEST1");
		Facility.DAO.store(facility);
		
		persistentIdList.add(facility.getPersistentId());

		facility = createFacility();
		facility.setDomainId("LOADBYLIST-TEST2");
		facility.setDescription("LOADBYLIST-TEST2");
		Facility.DAO.store(facility);

		persistentIdList.add(facility.getPersistentId());
		this.tenantPersistenceService.commitTransaction();

		this.tenantPersistenceService.beginTransaction();
		List<Facility> foundFacilityList = Facility.DAO.findByPersistentIdList(persistentIdList);
		Assert.assertEquals(2, foundFacilityList.size());
		this.tenantPersistenceService.commitTransaction();
	}

	@Test
	public final void testFindByDomainIdDontIncludeParentDomainId() {
		String FACILITY_ID = "FIND-BY-DOMAINID";

		Session session = tenantPersistenceService.getSession();
		Transaction t = session.beginTransaction();

		Facility facility1 = createFacility();
		facility1.setDomainId(FACILITY_ID);
		facility1.setDescription(FACILITY_ID);
		Facility.DAO.store(facility1);

		CodeshelfNetwork network = facility1.getNetworks().get(0);		
		String NETWORK_ID = network.getDomainId();
		t.commit();

		session = tenantPersistenceService.getSession();
		t = session.beginTransaction();
		Facility foundFacility = Facility.DAO.findByDomainId(null, FACILITY_ID);
		Assert.assertNotNull(foundFacility);

		CodeshelfNetwork foundCodeshelfNetwork = CodeshelfNetwork.DAO.findByDomainId(null, NETWORK_ID);
		Assert.assertNotNull(foundCodeshelfNetwork);
		t.commit();
	}

	@Test
	public final void testFindByDomainIdIncludeParentDomainId() {		
		String FACILITY_ID = "FAC-FIND-BY-DOMAINID-INC";
		String FACILITY2_ID = "FAC2-FIND-BY-DOMAINID-INC";
		String AISLE_ID = "AISLE-FIND-BY-DOMAINID-INC";

		Session session = tenantPersistenceService.getSession();
		Transaction t = session.beginTransaction();
		Facility facility = Facility.createFacility(getDefaultTenant(),FACILITY_ID, FACILITY_ID, new Point(PositionTypeEnum.GPS, 0.0, 0.0, 0.0));
		facility.setDescription(FACILITY_ID);
		Facility.DAO.store(facility);

		Aisle aisle1 = facility.createAisle(
			AISLE_ID,
			new Point(PositionTypeEnum.GPS, 0.0, 0.0, 0.0),
			new Point(PositionTypeEnum.GPS, 0.0, 0.0, 0.0));
		aisle1.setDomainId(AISLE_ID);
		Aisle.DAO.store(aisle1);
		t.commit();

		session = tenantPersistenceService.getSession();
		t = session.beginTransaction();
		Aisle foundAisle = Aisle.DAO.findByDomainId(facility, AISLE_ID);
		Assert.assertNotNull(foundAisle);

		facility = Facility.createFacility(getDefaultTenant(),FACILITY2_ID, FACILITY2_ID, new Point(PositionTypeEnum.GPS, 0.0, 0.0, 0.0));
		facility.setDescription(FACILITY2_ID);
		Facility.DAO.store(facility);
		t.commit();

		session = tenantPersistenceService.getSession();
		t = session.beginTransaction();
		foundAisle = Aisle.DAO.findByDomainId(facility, AISLE_ID);
		Assert.assertNull(foundAisle);
		t.commit();
	}

	@Test
	public final void testStoreNew() {
		// store new organization
		String desc = "Test-Desc";
		Session session = tenantPersistenceService.getSession();
		Transaction t = session.beginTransaction();
		Facility facility = createFacility();
		facility.setDomainId("LOADBY-TEST");
		facility.setDescription(desc);
		Facility.DAO.store(facility);
		UUID id = facility.getPersistentId();
		t.commit();
		Assert.assertNotNull(id);
	}

	@Test
	public final void testStoreUpdate() {
		String orgDesc = "org-desc";
		String updatedDesc = "updated-desc";
		// create org
		Session session = tenantPersistenceService.getSession();
		Transaction t = session.beginTransaction();

		Assert.assertNotNull(Facility.DAO);
		Facility facility = createFacility();
		facility.setDomainId("DELETE-TEST");
		facility.setDescription(orgDesc);
		Facility.DAO.store(facility);
		UUID id = facility.getPersistentId();
		t.commit();
		
		// make sure org exists and then update it
		session = tenantPersistenceService.getSession();
		t = session.beginTransaction();
		Facility foundFacility = Facility.DAO.findByPersistentId(id);
		Assert.assertNotNull(foundFacility);
		Assert.assertEquals(orgDesc,foundFacility.getDescription());
		Assert.assertNotNull(foundFacility.getDao());
		foundFacility.setDescription(updatedDesc);
		Facility.DAO.store(foundFacility);
		t.commit();
		Assert.assertNotNull(foundFacility);
		
		// now reload it again and make sure desc has changed
		session = tenantPersistenceService.getSession();
		t = session.beginTransaction();
		foundFacility = Facility.DAO.findByPersistentId(id);
		Assert.assertNotNull(foundFacility);
		Assert.assertEquals(updatedDesc,foundFacility.getDescription());
		t.commit();
	}

	@Test
	public final void testDelete() {
		// first transaction - create org
		Session session = tenantPersistenceService.getSession();
		Transaction t = session.beginTransaction();
		Facility facility = new Facility();
		facility.setDomainId("DELETE-TEST");
		facility.setDescription("DELETE-TEST");
		Facility.DAO.store(facility);
		UUID id = facility.getPersistentId();
		t.commit();
		
		// make sure org exists and then delete it
		session = tenantPersistenceService.getSession();
		t = session.beginTransaction();
		Facility foundFacility= Facility.DAO.findByPersistentId(id);
		Assert.assertNotNull(foundFacility);
		Facility.DAO.delete(foundFacility);
		t.commit();
		Assert.assertNotNull(foundFacility);
		
		// now try to reload it again
		session = tenantPersistenceService.getSession();
		t = session.beginTransaction();
		foundFacility= Facility.DAO.findByPersistentId(id);
		Assert.assertNull(foundFacility);
		t.commit();
	}

	@Test
	public final void testGetAll() {
		Session session = tenantPersistenceService.getSession();
		Transaction t = session.beginTransaction();

		Facility facility= new Facility();
		facility.setDomainId("GETALL-TEST1");
		facility.setDescription("GETALL-TEST1");
		Facility.DAO.store(facility);

		facility = new Facility();
		facility.setDomainId("GETALL-TEST2");
		facility.setDescription("GETALL-TEST2");
		Facility.DAO.store(facility);
		t.commit();
		
		session = tenantPersistenceService.getSession();
		t = session.beginTransaction();
		// This is not a great test - all these DB tests run in parallel against the same DB.
		// There's no way to know how many items getAll() will return, so we just look for the ones we put in.
		int totalFound = 0;
		for (Facility fac : Facility.DAO.getAll()) {
			if (fac.getDomainId().startsWith("GETALL-TEST")) {
				totalFound++;
			}
		}
		t.commit();
		Assert.assertEquals(2, totalFound);
	}

}
