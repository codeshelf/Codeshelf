package com.gadgetworks.codeshelf.model.dao;

import java.util.Set;
import java.util.UUID;

import lombok.Getter;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gadgetworks.codeshelf.model.domain.IDomainObject;
import com.gadgetworks.codeshelf.model.domain.Organization;

public class ObjectChangeBroadcasterTest extends DAOTestABC {

	@Test
	public void usesDifferentThreads() {
		this.getPersistenceService().beginTenantTransaction();
		ObjectChangeBroadcaster broadcaster = this.getPersistenceService().getObjectChangeBroadcaster();
		IDaoListener listener = new DaoTestListener();
		broadcaster.registerDAOListener(listener, Organization.class);
		Organization org = new Organization();
		org.setDomainId("A");
		this.getPersistenceService().getCurrentTenantSession().save(org);
		this.getPersistenceService().endTenantTransaction();		
	}
	
	@Test
	public final void testAddNotification() throws InterruptedException {
		// register event listener
		DaoTestListener l = new DaoTestListener();
		ObjectChangeBroadcaster broadcaster = this.getPersistenceService().getObjectChangeBroadcaster();
		broadcaster.registerDAOListener(l, Organization.class);
		try {
			Assert.assertEquals(0, l.getObjectsAdded());
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
		ObjectChangeBroadcaster broadcaster = this.getPersistenceService().getObjectChangeBroadcaster();
		try {
			broadcaster.registerDAOListener(l, Organization.class);
			
			Assert.assertEquals(0, l.getObjectsUpdated());
			Assert.assertNull(l.getLastObjectUpdated());
			
			String orgDesc = "org-desc";
			String updatedDesc = "updated-desc";
			// create org
			Session session = persistenceService.getCurrentTenantSession();
			Transaction t = session.beginTransaction();
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
			foundOrganization.setDescription(updatedDesc);
			mOrganizationDao.store(foundOrganization);
			t.commit();
			Thread.sleep(1000); //shame on me
			Assert.assertNotNull(foundOrganization);
			Assert.assertEquals(1, l.getObjectsUpdated());
			Assert.assertEquals(foundOrganization.getPersistentId(), l.getLastObjectUpdated());
			Assert.assertEquals(1, l.getLastObjectPropertiesUpdated().size());
			Assert.assertTrue(l.getLastObjectPropertiesUpdated().contains("description"));
			
			// now reload it again and make sure desc has changed
			session = persistenceService.getCurrentTenantSession();
			t = session.beginTransaction();
			foundOrganization = mOrganizationDao.findByPersistentId(id);
			Assert.assertNotNull(foundOrganization);
			Assert.assertEquals(updatedDesc,foundOrganization.getDescription());
			t.commit();
		} finally {
			broadcaster.unregisterDAOListener(l);
		}
	}

	@Test
	public final void testDeleteNotification() throws InterruptedException {
		DaoTestListener l = new DaoTestListener();
		ObjectChangeBroadcaster broadcaster = this.getPersistenceService().getObjectChangeBroadcaster();
		broadcaster.registerDAOListener(l, Organization.class);
		try {
			Assert.assertEquals(0, l.getObjectsDeleted());
			
			// first transaction - create org
			Session session = persistenceService.getCurrentTenantSession();
			Transaction t = session.beginTransaction();
			Organization organization = new Organization();
			organization.setDomainId("DELETE-TEST");
			organization.setDescription("DELETE-TEST");
			mOrganizationDao.store(organization);
			UUID id = organization.getPersistentId();
			t.commit();
			Thread.sleep(1000); //shame on me

			// make sure org exists and then delete it
			session = persistenceService.getCurrentTenantSession();
			t = session.beginTransaction();
			Organization foundOrganization = mOrganizationDao.findByPersistentId(id);
			Assert.assertNotNull(foundOrganization);
			mOrganizationDao.delete(foundOrganization);
			t.commit();
			Assert.assertNotNull(foundOrganization);
			Assert.assertEquals(1, l.getObjectsDeleted());
			
			// now try to reload it again
			session = persistenceService.getCurrentTenantSession();
			t = session.beginTransaction();
			foundOrganization = mOrganizationDao.findByPersistentId(id);
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
