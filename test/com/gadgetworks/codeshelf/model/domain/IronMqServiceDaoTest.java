package com.gadgetworks.codeshelf.model.domain;

import org.junit.Assert;
import org.junit.Test;
import org.postgresql.util.PSQLException;

public class IronMqServiceDaoTest extends DomainTestABC {

	@Test
	public void updatedCredentialsAvailableToFacilityReference() throws PSQLException {
		Facility f = createFacility("updatedCredentialsAvailableToFacilityReference");
		IronMqService ironMqService = ((IronMqService)f.getEdiExportService());
		String originalCredentials = ironMqService.getProviderCredentials();
		
		IronMqService ironMqServiceByDao = IronMqService.DAO.findByPersistentId(ironMqService.getPersistentId());
		ironMqServiceByDao.storeCredentials("NEWPROJ", "NEWTOKEN");
		String updatedCredentials = ((IronMqService)f.getEdiExportService()).getProviderCredentials();
		Assert.assertNotEquals(originalCredentials, updatedCredentials);
	}
	
}
