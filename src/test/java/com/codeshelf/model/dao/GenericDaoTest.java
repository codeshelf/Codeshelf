package com.codeshelf.model.dao;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Restrictions;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.model.PositionTypeEnum;
import com.codeshelf.model.domain.Aisle;
import com.codeshelf.model.domain.CodeshelfNetwork;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.Point;
import com.codeshelf.testframework.ServerTest;

public class GenericDaoTest extends ServerTest {
	private static final Logger	LOGGER	= LoggerFactory.getLogger(GenericDaoTest.class);

	public GenericDaoTest() {
	}

	@Test
	public final void testFindByFilter() {
		this.tenantPersistenceService.beginTransaction();

		Facility facility = Facility.createFacility("LOADBYFILTERTEST1", "LOADBYFILTER", Point.getZeroPoint());
		Facility.staticGetDao().store(facility);

		facility = Facility.createFacility("LOADBYFILTERTEST2", "LOADBYFILTER", Point.getZeroPoint());
		Facility.staticGetDao().store(facility);

		List<Criterion> filterParams = new ArrayList<Criterion>();
		filterParams.add(Restrictions.eq("domainId", "LOADBYFILTERTEST1"));
		List<Facility> foundOrganizationList = Facility.staticGetDao().findByFilter(filterParams);

		this.tenantPersistenceService.commitTransaction();
		Assert.assertEquals(1, foundOrganizationList.size());
	}

	@Test
	public final void testInRestrictions() {
		// DEV-1370 event seemed to show Restrictions.in of an empty list led to a SQL error
		beginTransaction();

		Facility facility1 = Facility.createFacility("INTEST1", "INTEST1", Point.getZeroPoint());
		Facility.staticGetDao().store(facility1);

		Facility facility2 = Facility.createFacility("INTEST2", "INTEST2", Point.getZeroPoint());
		Facility.staticGetDao().store(facility2);
		commitTransaction();

		LOGGER.info("1: Restrictions.in for an empty list. Did not reproduce the SQL error");
		beginTransaction();
		List<Criterion> filterParams = new ArrayList<Criterion>();
		filterParams.add(Restrictions.in("domainId", new ArrayList<>()));
		List<Facility> foundOrganizationList = Facility.staticGetDao().findByFilter(filterParams);
		Assert.assertEquals(0, foundOrganizationList.size());
		commitTransaction();

		LOGGER.info("2: Restrictions.in for a proper string list");
		List<String> facilityIds = new ArrayList<>();
		facilityIds.add("INTEST2");
		beginTransaction();
		List<Criterion> filterParams2 = new ArrayList<Criterion>();
		filterParams2.add(Restrictions.in("domainId", facilityIds));
		foundOrganizationList = Facility.staticGetDao().findByFilter(filterParams2);
		for (Facility facility : foundOrganizationList) {
			LOGGER.info("Facility domainId: {}", facility.getDomainId());
		}
		for (String name : facilityIds) {
			LOGGER.info("name in string list: {}", name);
		}
		Assert.assertEquals("returned two many facilities", 1, foundOrganizationList.size()); 

		commitTransaction();

	}

	@Test
	public final void testLoadByPersistentId() {
		// store new organization
		String desc = "Test-Desc";
		this.tenantPersistenceService.beginTransaction();
		Facility facility = createFacility();
		facility.setDescription(desc);
		Facility.staticGetDao().store(facility);
		this.tenantPersistenceService.commitTransaction();

		// load stored org and check data in a sep transaction
		this.tenantPersistenceService.beginTransaction();
		Facility foundFacility = Facility.staticGetDao().findByPersistentId(facility.getPersistentId());
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
		Facility.staticGetDao().store(facility);

		persistentIdList.add(facility.getPersistentId());

		facility = createFacility();
		facility.setDomainId("LOADBYLIST-TEST2");
		facility.setDescription("LOADBYLIST-TEST2");
		Facility.staticGetDao().store(facility);

		persistentIdList.add(facility.getPersistentId());
		this.tenantPersistenceService.commitTransaction();

		this.tenantPersistenceService.beginTransaction();
		List<Facility> foundFacilityList = Facility.staticGetDao().findByPersistentIdList(persistentIdList);
		Assert.assertEquals(2, foundFacilityList.size());
		this.tenantPersistenceService.commitTransaction();
	}

	@Test
	public final void testFindByDomainIdDontIncludeParentDomainId() {
		String FACILITY_ID = "FIND-BY-DOMAINID";

		this.tenantPersistenceService.beginTransaction();

		Facility facility1 = createFacility();
		facility1.setDomainId(FACILITY_ID);
		facility1.setDescription(FACILITY_ID);
		Facility.staticGetDao().store(facility1);

		CodeshelfNetwork network = facility1.getNetwork(CodeshelfNetwork.DEFAULT_NETWORK_NAME);
		String NETWORK_ID = network.getDomainId();
		this.tenantPersistenceService.commitTransaction();

		this.tenantPersistenceService.beginTransaction();
		Facility foundFacility = Facility.staticGetDao().findByDomainId(null, FACILITY_ID);
		Assert.assertNotNull(foundFacility);

		CodeshelfNetwork foundCodeshelfNetwork = CodeshelfNetwork.staticGetDao().findByDomainId(null, NETWORK_ID);
		Assert.assertNotNull(foundCodeshelfNetwork);
		this.tenantPersistenceService.commitTransaction();
	}

	@Test
	public final void testFindByDomainIdIncludeParentDomainId() {
		String FACILITY_ID = "FAC-FIND-BY-DOMAINID-INC";
		String FACILITY2_ID = "FAC2-FIND-BY-DOMAINID-INC";
		String AISLE_ID = "AISLE-FIND-BY-DOMAINID-INC";

		Session session = tenantPersistenceService.getSession();
		Transaction t = session.beginTransaction();
		Facility facility = Facility.createFacility(FACILITY_ID, FACILITY_ID, new Point(PositionTypeEnum.GPS, 0.0, 0.0, 0.0));
		facility.setDescription(FACILITY_ID);
		Facility.staticGetDao().store(facility);

		Aisle aisle1 = facility.createAisle(AISLE_ID,
			new Point(PositionTypeEnum.GPS, 0.0, 0.0, 0.0),
			new Point(PositionTypeEnum.GPS, 0.0, 0.0, 0.0));
		aisle1.setDomainId(AISLE_ID);
		Aisle.staticGetDao().store(aisle1);
		t.commit();

		session = tenantPersistenceService.getSession();
		t = session.beginTransaction();
		Aisle foundAisle = Aisle.staticGetDao().findByDomainId(facility, AISLE_ID);
		Assert.assertNotNull(foundAisle);

		facility = Facility.createFacility(FACILITY2_ID, FACILITY2_ID, new Point(PositionTypeEnum.GPS, 0.0, 0.0, 0.0));
		facility.setDescription(FACILITY2_ID);
		Facility.staticGetDao().store(facility);
		t.commit();

		session = tenantPersistenceService.getSession();
		t = session.beginTransaction();
		foundAisle = Aisle.staticGetDao().findByDomainId(facility, AISLE_ID);
		Assert.assertNull(foundAisle);
		t.commit();
	}

	@Test
	public final void testStoreNew() {
		// store new organization
		String desc = "Test-Desc";
		tenantPersistenceService.beginTransaction();
		Facility facility = createFacility();
		facility.setDomainId("LOADBY-TEST");
		facility.setDescription(desc);
		Facility.staticGetDao().store(facility);
		UUID id = facility.getPersistentId();
		tenantPersistenceService.commitTransaction();
		Assert.assertNotNull(id);
	}

	@Test
	public final void testStoreUpdate() {
		String orgDesc = "org-desc";
		String updatedDesc = "updated-desc";
		// create org
		tenantPersistenceService.beginTransaction();

		Assert.assertNotNull(Facility.staticGetDao());
		Facility facility = createFacility();
		facility.setDomainId("DELETE-TEST");
		facility.setDescription(orgDesc);
		Facility.staticGetDao().store(facility);
		tenantPersistenceService.commitTransaction();

		// make sure org exists and then update it
		tenantPersistenceService.beginTransaction();
		Facility foundFacility = facility.reload();
		Assert.assertNotNull(foundFacility);
		Assert.assertEquals(orgDesc, foundFacility.getDescription());
		Assert.assertNotNull(foundFacility.getDao());
		foundFacility.setDescription(updatedDesc);
		Facility.staticGetDao().store(foundFacility);
		Assert.assertNotNull(foundFacility);
		tenantPersistenceService.commitTransaction();

		// now reload it again and make sure desc has changed
		tenantPersistenceService.beginTransaction();
		foundFacility = foundFacility.reload();
		Assert.assertNotNull(foundFacility);
		Assert.assertEquals(updatedDesc, foundFacility.getDescription());
		tenantPersistenceService.commitTransaction();
	}

	@Test
	public final void testDelete() {
		// first transaction - create org
		Session session = tenantPersistenceService.getSession();
		Transaction t = session.beginTransaction();
		Facility facility = new Facility();
		facility.setDomainId("DELETE-TEST");
		facility.setDescription("DELETE-TEST");
		Facility.staticGetDao().store(facility);
		UUID id = facility.getPersistentId();
		t.commit();

		// make sure org exists and then delete it
		session = tenantPersistenceService.getSession();
		t = session.beginTransaction();
		Facility foundFacility = Facility.staticGetDao().findByPersistentId(id);
		Assert.assertNotNull(foundFacility);
		Facility.staticGetDao().delete(foundFacility);
		t.commit();
		Assert.assertNotNull(foundFacility);

		// now try to reload it again
		session = tenantPersistenceService.getSession();
		t = session.beginTransaction();
		foundFacility = Facility.staticGetDao().findByPersistentId(id);
		Assert.assertNull(foundFacility);
		t.commit();
	}

	@Test
	public final void testGetAll() {
		Session session = tenantPersistenceService.getSession();
		Transaction t = session.beginTransaction();

		Facility facility = new Facility();
		facility.setDomainId("GETALL-TEST1");
		facility.setDescription("GETALL-TEST1");
		Facility.staticGetDao().store(facility);

		facility = new Facility();
		facility.setDomainId("GETALL-TEST2");
		facility.setDescription("GETALL-TEST2");
		Facility.staticGetDao().store(facility);
		t.commit();

		session = tenantPersistenceService.getSession();
		t = session.beginTransaction();
		// This is not a great test - all these DB tests run in parallel against the same DB.
		// There's no way to know how many items getAll() will return, so we just look for the ones we put in.
		int totalFound = 0;
		for (Facility fac : Facility.staticGetDao().getAll()) {
			if (fac.getDomainId().startsWith("GETALL-TEST")) {
				totalFound++;
			}
		}
		t.commit();
		Assert.assertEquals(2, totalFound);
	}

	@Test
	public final void testCriteriaQueries() {
		Session session = tenantPersistenceService.getSession();
		Transaction t = session.beginTransaction();

		Facility facility1 = new Facility();
		facility1.setDomainId("FXXA-TEST1");
		facility1.setDescription("FXXA-TEST1");
		Facility.staticGetDao().store(facility1);

		Facility facility2 = new Facility();
		facility2.setDomainId("FXXA-TEST2");
		facility2.setDescription("FXXA-TEST2");
		Facility.staticGetDao().store(facility2);

		Facility facility3 = new Facility();
		facility3.setDomainId("FXXB-TEST3");
		facility3.setDescription("FXXB-TEST3");
		Facility.staticGetDao().store(facility3);
		t.commit();

		session = tenantPersistenceService.getSession();
		t = session.beginTransaction();
		Criteria crit1 = Facility.staticGetDao().createCriteria();
		crit1.add(Restrictions.eq("domainId", "FXXA-TEST1"));
		List<Facility> facilities = Facility.staticGetDao().findByCriteriaQuery(crit1);
		Assert.assertEquals(1, facilities.size());
		Facility facility = facilities.get(0);
		Assert.assertEquals(facility, facility1);
		t.commit();

		session = tenantPersistenceService.getSession();
		t = session.beginTransaction();
		Criteria crit2 = Facility.staticGetDao().createCriteria();
		crit2.add(Restrictions.ilike("domainId", "fxxa", MatchMode.ANYWHERE));
		// crit2.add(Restrictions.like("domainId", "FXXA", MatchMode.ANYWHERE)); // also works
		facilities = Facility.staticGetDao().findByCriteriaQuery(crit2);
		Assert.assertEquals(2, facilities.size());
		t.commit();

		session = tenantPersistenceService.getSession();
		t = session.beginTransaction();
		Criteria crit3 = Facility.staticGetDao().createCriteria();
		crit3.add(Restrictions.like("domainId", "FXXA", MatchMode.ANYWHERE));
		Assert.assertEquals(2, Facility.staticGetDao().countByCriteriaQuery(crit3));
		t.commit();

		session = tenantPersistenceService.getSession();
		t = session.beginTransaction();
		Criteria crit4 = Facility.staticGetDao().createCriteria();
		crit4.add(Restrictions.like("domainId", "FXXA", MatchMode.ANYWHERE));
		List<UUID> ids = Facility.staticGetDao().getUUIDListByCriteriaQuery(crit4);
		Assert.assertEquals(2, ids.size());
		Assert.assertTrue(ids.contains(facility1.getPersistentId()));
		Assert.assertTrue(ids.contains(facility2.getPersistentId()));
		Assert.assertFalse(ids.contains(facility3.getPersistentId()));
		t.commit();

		session = tenantPersistenceService.getSession();
		t = session.beginTransaction();
		List<Facility> facilities2 = Facility.staticGetDao().findByPersistentIdList(ids);
		Assert.assertEquals(2, facilities2.size());
		Assert.assertTrue(facilities2.contains(facility1));
		Assert.assertTrue(facilities2.contains(facility2));
		Assert.assertFalse(facilities2.contains(facility3));
		t.commit();

	}

}
