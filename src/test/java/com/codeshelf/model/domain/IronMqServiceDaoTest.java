package com.codeshelf.model.domain;

import org.junit.Assert;
import org.junit.Test;
import org.postgresql.util.PSQLException;

import com.codeshelf.testframework.MockDaoTest;

public class IronMqServiceDaoTest extends MockDaoTest {

	@Test
	public void updatedCredentialsAvailableToFacilityReference() throws PSQLException {
		this.getTenantPersistenceService().beginTransaction();
		try {
			Facility f = createFacility();
			IronMqService ironMqService = f.findEdiService(IronMqService.class);
			String originalCredentials = ironMqService.getProviderCredentials();
			
			IronMqService ironMqServiceByDao = IronMqService.staticGetDao().findByPersistentId(ironMqService.getPersistentId());
			ironMqServiceByDao.storeCredentials("NEWPROJ", "NEWTOKEN", "true");
			String updatedCredentials = f.findEdiService(IronMqService.class).getProviderCredentials();
			Assert.assertNotEquals(originalCredentials, updatedCredentials);
			
		} finally {
			this.getTenantPersistenceService().commitTransaction();
		}
	}
	
}
