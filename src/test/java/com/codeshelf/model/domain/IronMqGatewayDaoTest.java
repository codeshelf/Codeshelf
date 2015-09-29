package com.codeshelf.model.domain;

import org.junit.Assert;
import org.junit.Test;
import org.postgresql.util.PSQLException;

import com.codeshelf.testframework.MockDaoTest;

public class IronMqGatewayDaoTest extends MockDaoTest {

	@Test
	public void updatedCredentialsAvailableToFacilityReference() throws PSQLException {
		this.getTenantPersistenceService().beginTransaction();
		try {
			Facility f = createFacility();
			IronMqGateway ironMqGateway = f.findEdiService(IronMqGateway.class);
			String originalCredentials = ironMqGateway.getProviderCredentials();
			
			IronMqGateway ironMqGatewayByDao = (IronMqGateway)EdiGateway.staticGetDao().findByPersistentId(ironMqGateway.getPersistentId());
			ironMqGatewayByDao.storeCredentials("NEWPROJ", "NEWTOKEN", "true");
			String updatedCredentials = f.findEdiService(IronMqGateway.class).getProviderCredentials();
			Assert.assertNotEquals(originalCredentials, updatedCredentials);
			
		} finally {
			this.getTenantPersistenceService().commitTransaction();
		}
	}
	
}
