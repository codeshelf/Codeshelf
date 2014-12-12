package com.gadgetworks.codeshelf.platform.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.List;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gadgetworks.codeshelf.model.domain.Configuration;
import com.gadgetworks.codeshelf.model.domain.DomainTestABC;
import com.gadgetworks.codeshelf.model.domain.Organization;

public class ConfigurationTest extends DomainTestABC {
	
	private static final Logger	LOGGER	= LoggerFactory.getLogger(ConfigurationTest.class);

	@Test
	public void testConfigOperations() {
		ConfigurationService cfgServ = ConfigurationService.getInstance();
		// create organization and set one config value
		beginTenantTransaction();
		Organization org=new Organization();
		org.setDomainId("testOrg");
		Organization.DAO.store(org);
		
		Configuration config = new Configuration(org,"name","value");
		cfgServ.store(config);		
		commitTenantTransaction();
		
		// retrieve data, check value and delete it
		beginTenantTransaction();
		config = cfgServ.getConfiguration(org, "name");
		assertNotNull(config);
		assertEquals("value", config.getValue());
		cfgServ.delete(config);
		commitTenantTransaction();
		
		// make sure config can't be found
		beginTenantTransaction();
		config = cfgServ.getConfiguration(org, "name");
		assertNull(config);
		commitTenantTransaction();

		// add two configs
		beginTenantTransaction();
		Configuration config2 = new Configuration(org,"name2","value2");
		Configuration config3 = new Configuration(org,"name3","value3");
		cfgServ.store(config2);		
		cfgServ.store(config3);		
		commitTenantTransaction();
		
		beginTenantTransaction();
		List<Configuration> configs = cfgServ.getConfigurations(org);
		assertNotNull(configs);
		assertEquals(2,configs.size());
		commitTenantTransaction();
	}
	
	@Test
	public void testTypedConvenienceMethods() {
		ConfigurationService cfgServ = ConfigurationService.getInstance();

		beginTenantTransaction();
		Organization org=new Organization();
		org.setDomainId("testOrg");
		Organization.DAO.store(org);

		Configuration config1 = new Configuration(org,"string-config","value");
		Configuration config2 = new Configuration(org,"int-config").setValue(123);
		Configuration config3 = new Configuration(org,"double-config").setValue(123.456);

		cfgServ.store(config1);
		cfgServ.store(config2);
		cfgServ.store(config3);
		commitTenantTransaction();

		beginTenantTransaction();

		String stringValue = cfgServ.getConfigAsString(org, "string-config");
		assertEquals("value", stringValue);
		
		int intValue = cfgServ.getConfigAsInt(org, "int-config");
		assertEquals(123,intValue);

		double doubleValue = cfgServ.getConfigAsDouble(org, "double-config");
		assertEquals(123.456,doubleValue,0.00001);
		
		commitTenantTransaction();
	}

	@Test
	public void testDefaultAndMissingValues() {
		ConfigurationService cfgServ = ConfigurationService.getInstance();

		beginTenantTransaction();
		Organization org=new Organization();
		org.setDomainId("testOrg");
		Organization.DAO.store(org);
		commitTenantTransaction();
		
		beginTenantTransaction();
		String stringValue = cfgServ.getConfigAsString(org, "string-config");
		assertNull(stringValue);
		Integer intValue = cfgServ.getConfigAsInt(org, "int-config");
		assertNull(intValue);
		Double doubleValue = cfgServ.getConfigAsDouble(org, "double-config");
		assertNull(doubleValue);

		stringValue = cfgServ.getConfigAsString(org, "string-config","default");
		assertEquals("default",stringValue);
		intValue = cfgServ.getConfigAsInt(org, "int-config", 678);
		assertNotNull(intValue);
		assertEquals(678,intValue.intValue());
		doubleValue = cfgServ.getConfigAsDouble(org, "double-config",473.741);
		assertNotNull(doubleValue);
		assertEquals(473.741,doubleValue,0.00001);

		commitTenantTransaction();
	}	
}
