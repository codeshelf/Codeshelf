package com.gadgetworks.codeshelf.model.dao;

import java.util.UUID;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.junit.Assert;
import org.junit.Test;

import com.gadgetworks.codeshelf.application.Configuration;
import com.gadgetworks.codeshelf.model.domain.Organization;

public class EventListenerTest extends DAOTestABC {
		
	static {
		Configuration.loadConfig("test");
	}
	
	public EventListenerTest() {
	}

	@Test
	public final void testAddNotification() {
		// register event listener
		DaoTestListener l = new DaoTestListener();
		mOrganizationDao.registerDAOListener(l);
		Assert.assertEquals(0, l.getObjectsAdded());
		// store new organization
		String desc = "Test-Desc";
		Session session = persistencyService.getCurrentTenantSession();
		Transaction t = session.beginTransaction();
		Organization organization = new Organization();
		organization.setDomainId("LOADBY-TEST");
		organization.setDescription(desc);
		mOrganizationDao.store(organization);		
		UUID id = organization.getPersistentId();
		t.commit();
		Assert.assertNotNull(id);
		Assert.assertEquals(1, l.getObjectsAdded());
	}

	@Test
	public final void testUpdateNotification() {
		DaoTestListener l = new DaoTestListener();
		mOrganizationDao.registerDAOListener(l);
		Assert.assertEquals(0, l.getObjectsUpdated());
		Assert.assertNull(l.getLastObjectUpdated());
		
		String orgDesc = "org-desc";
		String updatedDesc = "updated-desc";
		// create org
		Session session = persistencyService.getCurrentTenantSession();
		Transaction t = session.beginTransaction();
		Organization organization = new Organization();
		organization.setDomainId("DELETE-TEST");
		organization.setDescription(orgDesc);
		mOrganizationDao.store(organization);
		UUID id = organization.getPersistentId();
		t.commit();
		
		// make sure org exists and then update it
		session = persistencyService.getCurrentTenantSession();
		t = session.beginTransaction();
		Organization foundOrganization = mOrganizationDao.findByPersistentId(id);
		Assert.assertNotNull(foundOrganization);
		Assert.assertEquals(orgDesc,foundOrganization.getDescription());
		foundOrganization.setDescription(updatedDesc);
		mOrganizationDao.store(foundOrganization);
		t.commit();
		Assert.assertNotNull(foundOrganization);
		Assert.assertEquals(1, l.getObjectsUpdated());
		Assert.assertEquals(foundOrganization, l.getLastObjectUpdated());
		Assert.assertEquals(1, l.getLastObjectPropertiesUpdated().size());
		Assert.assertTrue(l.getLastObjectPropertiesUpdated().contains("description"));
		
		// now reload it again and make sure desc has changed
		session = persistencyService.getCurrentTenantSession();
		t = session.beginTransaction();
		foundOrganization = mOrganizationDao.findByPersistentId(id);
		Assert.assertNotNull(foundOrganization);
		Assert.assertEquals(updatedDesc,foundOrganization.getDescription());
		t.commit();
	}

	@Test
	public final void testDeleteNotification() {
		DaoTestListener l = new DaoTestListener();
		mOrganizationDao.registerDAOListener(l);
		Assert.assertEquals(0, l.getObjectsDeleted());
		
		// first transaction - create org
		Session session = persistencyService.getCurrentTenantSession();
		Transaction t = session.beginTransaction();
		Organization organization = new Organization();
		organization.setDomainId("DELETE-TEST");
		organization.setDescription("DELETE-TEST");
		mOrganizationDao.store(organization);
		UUID id = organization.getPersistentId();
		t.commit();
		
		// make sure org exists and then delete it
		session = persistencyService.getCurrentTenantSession();
		t = session.beginTransaction();
		Organization foundOrganization = mOrganizationDao.findByPersistentId(id);
		Assert.assertNotNull(foundOrganization);
		mOrganizationDao.delete(foundOrganization);
		t.commit();
		Assert.assertNotNull(foundOrganization);
		Assert.assertEquals(1, l.getObjectsDeleted());
		
		// now try to reload it again
		session = persistencyService.getCurrentTenantSession();
		t = session.beginTransaction();
		foundOrganization = mOrganizationDao.findByPersistentId(id);
		Assert.assertNull(foundOrganization);
		t.commit();
	}
}
