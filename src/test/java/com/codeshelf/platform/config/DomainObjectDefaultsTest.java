package com.gadgetworks.codeshelf.platform.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gadgetworks.codeshelf.model.dao.PropertyDao;
import com.gadgetworks.codeshelf.model.domain.DomainObjectProperty;
import com.gadgetworks.codeshelf.model.domain.DomainObjectPropertyDefault;
import com.gadgetworks.codeshelf.model.domain.DomainTestABC;
import com.gadgetworks.codeshelf.model.domain.Facility;

public class DomainObjectDefaultsTest extends DomainTestABC {
	
	@SuppressWarnings("unused")
	private static final Logger	LOGGER	= LoggerFactory.getLogger(DomainObjectDefaultsTest.class);

	@Test
	public void ensureRequiredDefaultsExist() {
		PropertyDao cfgServ = PropertyDao.getInstance();

		beginTenantTransaction();
		
		// check LOCAPICK default
		DomainObjectPropertyDefault value = cfgServ.getPropertyDefault(Facility.class.getSimpleName(), DomainObjectProperty.LOCAPICK);
		assertNotNull(value);
		assertEquals(false,value.getBooleanValue());
		
		// check EACHMULT default
		value = cfgServ.getPropertyDefault(Facility.class.getSimpleName(), DomainObjectProperty.EACHMULT);
		assertNotNull(value);
		assertEquals(false,value.getBooleanValue());
		
		commitTenantTransaction();
	}
	
}
