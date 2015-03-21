package com.codeshelf.model.domain;

import org.junit.Assert;
import org.junit.Test;
import org.postgresql.util.PSQLException;

import com.codeshelf.testframework.MockDaoTest;

public class IronMqServiceDaoTest extends MockDaoTest {

	@Test
	public void updatedCredentialsAvailableToFacilityReference() throws PSQLException {
		this.getTenantPersistenceService().beginTransaction(getDefaultTenant());
		try {
			Facility f = createFacility();
			IronMqService ironMqService = ((IronMqService)f.getEdiExportService(getDefaultTenant()));
			String originalCredentials = ironMqService.getProviderCredentials();
			
			IronMqService ironMqServiceByDao = IronMqService.staticGetDao().findByPersistentId(getDefaultTenant(),ironMqService.getPersistentId());
			ironMqServiceByDao.storeCredentials(getDefaultTenant(),"NEWPROJ", "NEWTOKEN");
			String updatedCredentials = ((IronMqService)f.getEdiExportService(getDefaultTenant())).getProviderCredentials();
			Assert.assertNotEquals(originalCredentials, updatedCredentials);
			
		} finally {
			this.getTenantPersistenceService().commitTransaction(getDefaultTenant());
		}
	}
	
}
