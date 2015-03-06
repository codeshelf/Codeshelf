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
			IronMqService ironMqService = ((IronMqService)f.getEdiExportService());
			String originalCredentials = ironMqService.getProviderCredentials();
			
			IronMqService ironMqServiceByDao = IronMqService.DAO.findByPersistentId(ironMqService.getPersistentId());
			ironMqServiceByDao.storeCredentials("NEWPROJ", "NEWTOKEN");
			String updatedCredentials = ((IronMqService)f.getEdiExportService()).getProviderCredentials();
			Assert.assertNotEquals(originalCredentials, updatedCredentials);
			
		} finally {
			this.getTenantPersistenceService().commitTransaction();
		}
	}
	
}
