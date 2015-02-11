package com.codeshelf.model.dao;

import java.util.Set;
import java.util.UUID;

import lombok.Getter;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.IDomainObject;

public class ObjectChangeBroadcasterTest extends DAOTestABC {

	@Test
	public void usesDifferentThreads() {
		this.getTenantPersistenceService().beginTenantTransaction();
		ObjectChangeBroadcaster broadcaster = this.getTenantPersistenceService().getEventListenerIntegrator().getChangeBroadcaster();
		IDaoListener listener = new DaoTestListener();
		broadcaster.registerDAOListener(listener, Facility.class);
		Facility facility = createFacility();
		facility.setDomainId("A");
		this.getTenantPersistenceService().getCurrentTenantSession().save(facility);
		this.getTenantPersistenceService().commitTenantTransaction();		
	}
	
	@Test
	public final void testAddNotification() throws InterruptedException {
		// register event listener
		DaoTestListener l = new DaoTestListener();
		ObjectChangeBroadcaster broadcaster = this.getTenantPersistenceService().getEventListenerIntegrator().getChangeBroadcaster();
		broadcaster.registerDAOListener(l, Facility.class);
		try {
			Assert.assertEquals(0, l.getObjectsAdded());
			// store new organization
			String desc = "Test-Desc";
			Session session = tenantPersistenceService.getCurrentTenantSession();
			Transaction t = session.beginTransaction();
			Facility facility = createFacility();
			facility.setDomainId("LOADBY-TEST");
			facility.setDescription(desc);
			mFacilityDao.store(facility);		
			UUID id = facility.getPersistentId();
			t.commit();
			Thread.sleep(1000); //shame on me

			Assert.assertNotNull(id);
			Assert.assertEquals(1, l.getObjectsAdded());
		} finally {
			broadcaster.unregisterDAOListener(l);
		}
	}

	@Test
	public final void testUpdateNotification() throws InterruptedException {
		DaoTestListener l = new DaoTestListener();
		ObjectChangeBroadcaster broadcaster = this.getTenantPersistenceService().getEventListenerIntegrator().getChangeBroadcaster();
		try {
			broadcaster.registerDAOListener(l, Facility.class);
			
			Assert.assertEquals(0, l.getObjectsUpdated());
			Assert.assertNull(l.getLastObjectUpdated());
			
			String orgDesc = "org-desc";
			String updatedDesc = "updated-desc";
			// create org
			this.getTenantPersistenceService().beginTenantTransaction();
			Facility facility = new Facility();
			facility.setDomainId("DELETE-TEST");
			facility.setDescription(orgDesc);
			mFacilityDao.store(facility);
			UUID id = facility.getPersistentId();
			this.getTenantPersistenceService().commitTenantTransaction();

			// make sure org exists and then update it
			this.getTenantPersistenceService().beginTenantTransaction();
			
			Facility foundFacility = mFacilityDao.findByPersistentId(id);
			Assert.assertNotNull(foundFacility);
			Assert.assertEquals(orgDesc,foundFacility.getDescription());
			foundFacility.setDescription(updatedDesc);
			mFacilityDao.store(foundFacility);
			this.getTenantPersistenceService().commitTenantTransaction();

			Thread.sleep(1000); //shame on me
								//lol
			Assert.assertNotNull(foundFacility);
			Assert.assertEquals(1, l.getObjectsUpdated());
			Assert.assertEquals(foundFacility.getPersistentId(), l.getLastObjectUpdated());
			Assert.assertEquals(1, l.getLastObjectPropertiesUpdated().size());
			Assert.assertTrue(l.getLastObjectPropertiesUpdated().contains("description"));

			// make sure org exists and then update it
			this.getTenantPersistenceService().beginTenantTransaction();
			foundFacility= mFacilityDao.findByPersistentId(id);
			Assert.assertNotNull(foundFacility);
			Assert.assertEquals(updatedDesc,foundFacility.getDescription());
			this.getTenantPersistenceService().commitTenantTransaction();
		} finally {
			broadcaster.unregisterDAOListener(l);
		}
	}

	@Test
	public final void testDeleteNotification() throws InterruptedException {
		DaoTestListener l = new DaoTestListener();
		ObjectChangeBroadcaster broadcaster = this.getTenantPersistenceService().getEventListenerIntegrator().getChangeBroadcaster();
		broadcaster.registerDAOListener(l, Facility.class);
		try {
			Assert.assertEquals(0, l.getObjectsDeleted());
			
			// first transaction - create org
			Session session = tenantPersistenceService.getCurrentTenantSession();
			Transaction t = session.beginTransaction();
			Facility facility = new Facility();
			facility.setDomainId("DELETE-TEST");
			facility.setDescription("DELETE-TEST");
			mFacilityDao.store(facility);
			UUID id = facility.getPersistentId();
			t.commit();
			Thread.sleep(1000); //shame on me

			// make sure org exists and then delete it
			session = tenantPersistenceService.getCurrentTenantSession();
			t = session.beginTransaction();
			Facility foundOrganization = mFacilityDao.findByPersistentId(id);
			Assert.assertNotNull(foundOrganization);
			mFacilityDao.delete(foundOrganization);
			t.commit();
			Assert.assertNotNull(foundOrganization);
			Assert.assertEquals(1, l.getObjectsDeleted());
			
			// now try to reload it again
			session = tenantPersistenceService.getCurrentTenantSession();
			t = session.beginTransaction();
			foundOrganization = mFacilityDao.findByPersistentId(id);
			Assert.assertNull(foundOrganization);
			t.commit();
		} finally {
			broadcaster.unregisterDAOListener(l);
		}
	}
	
	private static class DaoTestListener implements IDaoListener {

		private static final Logger	LOGGER = LoggerFactory.getLogger(DaoTestListener.class);

		@Getter
		int objectsAdded = 0;
		
		@Getter
		int objectsUpdated = 0;
		
		@Getter
		int objectsDeleted = 0;
		
		@Getter
		UUID lastObjectUpdated = null;
		
		@Getter
		Set<String> lastObjectPropertiesUpdated = null;
		
		@Override
		public void objectAdded(final Class<? extends IDomainObject> domainClass, final UUID domainPersistentId) {
			objectsAdded++;
			LOGGER.debug("Object added: "+domainPersistentId);
		}

		@Override
		public void objectUpdated(final Class<? extends IDomainObject> domainClass, final UUID domainPersistentId, Set<String> changedProperties) {
			objectsUpdated++;
			lastObjectUpdated = domainPersistentId;
			lastObjectPropertiesUpdated = changedProperties;
			LOGGER.debug("Object updated: "+domainPersistentId);
		}

		@Override
		public void objectDeleted(final Class<? extends IDomainObject> domainClass, final UUID domainPersistentId) {
			objectsDeleted++;
			LOGGER.debug("Object deleted: "+domainPersistentId);
		}
	}

}
