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
		this.tenantPersistenceService.beginTransaction(getDefaultTenant());
		
		Facility facility = Facility.createFacility( getDefaultTenant(),"LOADBYFILTERTEST1", "LOADBYFILTER", Point.getZeroPoint());		
		Facility.staticGetDao().store(getDefaultTenant(),facility);
		
		facility = Facility.createFacility(getDefaultTenant(), "LOADBYFILTERTEST2", "LOADBYFILTER", Point.getZeroPoint());
		Facility.staticGetDao().store(getDefaultTenant(),facility);
		
		List<Criterion> filterParams = new ArrayList<Criterion>();
		filterParams.add(Restrictions.eq("domainId", "LOADBYFILTERTEST1"));
		List<Facility> foundOrganizationList = Facility.staticGetDao().findByFilter(getDefaultTenant(),filterParams);
		
		this.tenantPersistenceService.commitTransaction(getDefaultTenant());
		Assert.assertEquals(1, foundOrganizationList.size());
	}

	@Test
	public final void testLoadByPersistentId() {
		// store new organization
		String desc = "Test-Desc";
		this.tenantPersistenceService.beginTransaction(getDefaultTenant());
		Facility facility = createFacility();
		facility.setDescription(desc);
		Facility.staticGetDao().store(getDefaultTenant(),facility);
		this.tenantPersistenceService.commitTransaction(getDefaultTenant());

		// load stored org and check data in a sep transaction
		this.tenantPersistenceService.beginTransaction(getDefaultTenant());
		Facility foundFacility = Facility.staticGetDao().findByPersistentId(getDefaultTenant(),facility.getPersistentId());
		this.tenantPersistenceService.commitTransaction(getDefaultTenant());

		Assert.assertNotNull(foundFacility);
		Assert.assertEquals(desc, foundFacility.getDescription());
	}

	@Test
	public final void testLoadByPersistentIdList() {
		this.tenantPersistenceService.beginTransaction(getDefaultTenant());

		List<UUID> persistentIdList = new ArrayList<UUID>();
		Facility facility = createFacility();
		facility.setDomainId("LOADBYLIST-TEST1");
		facility.setDescription("LOADBYLIST-TEST1");
		Facility.staticGetDao().store(getDefaultTenant(),facility);
		
		persistentIdList.add(facility.getPersistentId());

		facility = createFacility();
		facility.setDomainId("LOADBYLIST-TEST2");
		facility.setDescription("LOADBYLIST-TEST2");
		Facility.staticGetDao().store(getDefaultTenant(),facility);

		persistentIdList.add(facility.getPersistentId());
		this.tenantPersistenceService.commitTransaction(getDefaultTenant());

		this.tenantPersistenceService.beginTransaction(getDefaultTenant());
		List<Facility> foundFacilityList = Facility.staticGetDao().findByPersistentIdList(getDefaultTenant(),persistentIdList);
		Assert.assertEquals(2, foundFacilityList.size());
		this.tenantPersistenceService.commitTransaction(getDefaultTenant());
	}

	@Test
	public final void testFindByDomainIdDontIncludeParentDomainId() {
		String FACILITY_ID = "FIND-BY-DOMAINID";

		Session session = tenantPersistenceService.getSession(getDefaultTenant());
		Transaction t = session.beginTransaction();

		Facility facility1 = createFacility();
		facility1.setDomainId(FACILITY_ID);
		facility1.setDescription(FACILITY_ID);
		Facility.staticGetDao().store(getDefaultTenant(),facility1);

		CodeshelfNetwork network = facility1.getNetworks().get(0);		
		String NETWORK_ID = network.getDomainId();
		t.commit();

		session = tenantPersistenceService.getSession(getDefaultTenant());
		t = session.beginTransaction();
		Facility foundFacility = Facility.staticGetDao().findByDomainId(getDefaultTenant(),null, FACILITY_ID);
		Assert.assertNotNull(foundFacility);

		CodeshelfNetwork foundCodeshelfNetwork = CodeshelfNetwork.staticGetDao().findByDomainId(getDefaultTenant(),null, NETWORK_ID);
		Assert.assertNotNull(foundCodeshelfNetwork);
		t.commit();
	}

	@Test
	public final void testFindByDomainIdIncludeParentDomainId() {		
		String FACILITY_ID = "FAC-FIND-BY-DOMAINID-INC";
		String FACILITY2_ID = "FAC2-FIND-BY-DOMAINID-INC";
		String AISLE_ID = "AISLE-FIND-BY-DOMAINID-INC";

		Session session = tenantPersistenceService.getSession(getDefaultTenant());
		Transaction t = session.beginTransaction();
		Facility facility = Facility.createFacility(getDefaultTenant(),FACILITY_ID, FACILITY_ID, new Point(PositionTypeEnum.GPS, 0.0, 0.0, 0.0));
		facility.setDescription(FACILITY_ID);
		Facility.staticGetDao().store(getDefaultTenant(),facility);

		Aisle aisle1 = facility.createAisle(
			AISLE_ID,
			new Point(PositionTypeEnum.GPS, 0.0, 0.0, 0.0),
			new Point(PositionTypeEnum.GPS, 0.0, 0.0, 0.0));
		aisle1.setDomainId(AISLE_ID);
		Aisle.staticGetDao().store(getDefaultTenant(),aisle1);
		t.commit();

		session = tenantPersistenceService.getSession(getDefaultTenant());
		t = session.beginTransaction();
		Aisle foundAisle = Aisle.staticGetDao().findByDomainId(getDefaultTenant(),facility, AISLE_ID);
		Assert.assertNotNull(foundAisle);

		facility = Facility.createFacility(getDefaultTenant(),FACILITY2_ID, FACILITY2_ID, new Point(PositionTypeEnum.GPS, 0.0, 0.0, 0.0));
		facility.setDescription(FACILITY2_ID);
		Facility.staticGetDao().store(getDefaultTenant(),facility);
		t.commit();

		session = tenantPersistenceService.getSession(getDefaultTenant());
		t = session.beginTransaction();
		foundAisle = Aisle.staticGetDao().findByDomainId(getDefaultTenant(),facility, AISLE_ID);
		Assert.assertNull(foundAisle);
		t.commit();
	}

	@Test
	public final void testStoreNew() {
		// store new organization
		String desc = "Test-Desc";
		Session session = tenantPersistenceService.getSession(getDefaultTenant());
		Transaction t = session.beginTransaction();
		Facility facility = createFacility();
		facility.setDomainId("LOADBY-TEST");
		facility.setDescription(desc);
		Facility.staticGetDao().store(getDefaultTenant(),facility);
		UUID id = facility.getPersistentId();
		t.commit();
		Assert.assertNotNull(id);
	}

	@Test
	public final void testStoreUpdate() {
		String orgDesc = "org-desc";
		String updatedDesc = "updated-desc";
		// create org
		Session session = tenantPersistenceService.getSession(getDefaultTenant());
		Transaction t = session.beginTransaction();

		Assert.assertNotNull(Facility.staticGetDao());
		Facility facility = createFacility();
		facility.setDomainId("DELETE-TEST");
		facility.setDescription(orgDesc);
		Facility.staticGetDao().store(getDefaultTenant(),facility);
		UUID id = facility.getPersistentId();
		t.commit();
		
		// make sure org exists and then update it
		session = tenantPersistenceService.getSession(getDefaultTenant());
		t = session.beginTransaction();
		Facility foundFacility = Facility.staticGetDao().findByPersistentId(getDefaultTenant(),id);
		Assert.assertNotNull(foundFacility);
		Assert.assertEquals(orgDesc,foundFacility.getDescription());
		Assert.assertNotNull(foundFacility.getDao());
		foundFacility.setDescription(updatedDesc);
		Facility.staticGetDao().store(getDefaultTenant(),foundFacility);
		t.commit();
		Assert.assertNotNull(foundFacility);
		
		// now reload it again and make sure desc has changed
		session = tenantPersistenceService.getSession(getDefaultTenant());
		t = session.beginTransaction();
		foundFacility = Facility.staticGetDao().findByPersistentId(getDefaultTenant(),id);
		Assert.assertNotNull(foundFacility);
		Assert.assertEquals(updatedDesc,foundFacility.getDescription());
		t.commit();
	}

	@Test
	public final void testDelete() {
		// first transaction - create org
		Session session = tenantPersistenceService.getSession(getDefaultTenant());
		Transaction t = session.beginTransaction();
		Facility facility = new Facility();
		facility.setDomainId("DELETE-TEST");
		facility.setDescription("DELETE-TEST");
		Facility.staticGetDao().store(getDefaultTenant(),facility);
		UUID id = facility.getPersistentId();
		t.commit();
		
		// make sure org exists and then delete it
		session = tenantPersistenceService.getSession(getDefaultTenant());
		t = session.beginTransaction();
		Facility foundFacility= Facility.staticGetDao().findByPersistentId(getDefaultTenant(),id);
		Assert.assertNotNull(foundFacility);
		Facility.staticGetDao().delete(getDefaultTenant(),foundFacility);
		t.commit();
		Assert.assertNotNull(foundFacility);
		
		// now try to reload it again
		session = tenantPersistenceService.getSession(getDefaultTenant());
		t = session.beginTransaction();
		foundFacility= Facility.staticGetDao().findByPersistentId(getDefaultTenant(),id);
		Assert.assertNull(foundFacility);
		t.commit();
	}

	@Test
	public final void testGetAll() {
		Session session = tenantPersistenceService.getSession(getDefaultTenant());
		Transaction t = session.beginTransaction();

		Facility facility= new Facility();
		facility.setDomainId("GETALL-TEST1");
		facility.setDescription("GETALL-TEST1");
		Facility.staticGetDao().store(getDefaultTenant(),facility);

		facility = new Facility();
		facility.setDomainId("GETALL-TEST2");
		facility.setDescription("GETALL-TEST2");
		Facility.staticGetDao().store(getDefaultTenant(),facility);
		t.commit();
		
		session = tenantPersistenceService.getSession(getDefaultTenant());
		t = session.beginTransaction();
		// This is not a great test - all these DB tests run in parallel against the same DB.
		// There's no way to know how many items getAll() will return, so we just look for the ones we put in.
		int totalFound = 0;
		for (Facility fac : Facility.staticGetDao().getAll(getDefaultTenant())) {
			if (fac.getDomainId().startsWith("GETALL-TEST")) {
				totalFound++;
			}
		}
		t.commit();
		Assert.assertEquals(2, totalFound);
	}

}
