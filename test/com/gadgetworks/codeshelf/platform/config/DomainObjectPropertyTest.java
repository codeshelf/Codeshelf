package com.gadgetworks.codeshelf.platform.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.List;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gadgetworks.codeshelf.model.dao.PropertyDao;
import com.gadgetworks.codeshelf.model.domain.DomainObjectProperty;
import com.gadgetworks.codeshelf.model.domain.DomainObjectPropertyDefault;
import com.gadgetworks.codeshelf.model.domain.DomainTestABC;
import com.gadgetworks.codeshelf.model.domain.Organization;

public class DomainObjectPropertyTest extends DomainTestABC {
	
	@SuppressWarnings("unused")
	private static final Logger	LOGGER	= LoggerFactory.getLogger(DomainObjectPropertyTest.class);

	@Test
	public void testPropertyDefaultOperations() {
		PropertyDao cfgServ = PropertyDao.getInstance();

		beginTenantTransaction();
		Organization org=new Organization();
		org.setDomainId("testOrg");
		Organization.DAO.store(org);
		List<DomainObjectPropertyDefault> types = cfgServ.getPropertyDefaults(org);
		assertNotNull(types);
		assertEquals(0, types.size());
		DomainObjectPropertyDefault type = cfgServ.getPropertyDefault(org,"test-prop");
		assertNull(type);
		assertEquals(0, types.size());
		commitTenantTransaction();

		// add config type
		beginTenantTransaction();
		DomainObjectPropertyDefault type1 = new DomainObjectPropertyDefault("test-prop",org.getClassName(),"Default-Value-1","Property-Description-1");
		cfgServ.store(type1);
		commitTenantTransaction();

		// retrieve data, check value and delete it
		beginTenantTransaction();
		types = cfgServ.getPropertyDefaults(org);
		assertNotNull(types);
		assertEquals(1, types.size());
		type = cfgServ.getPropertyDefault(org,"test-prop");
		assertNotNull(type);
		assertEquals("Property-Description-1", type.getDescription());
		cfgServ.delete(type);
		commitTenantTransaction();
		
		// make sure data is deleted
		beginTenantTransaction();
		types = cfgServ.getPropertyDefaults(org);
		assertNotNull(types);
		assertEquals(0, types.size());
		type = cfgServ.getPropertyDefault(org,"test-prop");
		assertNull(type);
		assertEquals(0, types.size());
		commitTenantTransaction();
	}
	
	@Test
	public void testPropertyOperations() {
		PropertyDao cfgServ = PropertyDao.getInstance();
		// create organization and set one config value
		beginTenantTransaction();
		Organization org=new Organization();
		org.setDomainId("testOrg");
		Organization.DAO.store(org);
		
		// create three property types in the database
		DomainObjectPropertyDefault type1 = new DomainObjectPropertyDefault("Some-Property-1",org.getClassName(),"Default-Value-1","Property-Description-1");
		cfgServ.store(type1);
		DomainObjectPropertyDefault type2 = new DomainObjectPropertyDefault("Some-Property-2",org.getClassName(),"Default-Value-2","Property-Description-2");
		cfgServ.store(type2);
		DomainObjectPropertyDefault type3 = new DomainObjectPropertyDefault("Some-Property-3",org.getClassName(),"Default-Value-3","Property-Description-3");
		cfgServ.store(type3);
		
		DomainObjectProperty config = new DomainObjectProperty(org,type1,"value");
		cfgServ.store(config);		
		commitTenantTransaction();
		
		// retrieve data, check value and delete it
		beginTenantTransaction();
		config = cfgServ.getProperty(org, "Some-Property-1");
		assertNotNull(config);
		assertEquals("value", config.getValue());
		cfgServ.delete(config);
		commitTenantTransaction();
		
		// make sure config can't be found
		beginTenantTransaction();
		config = cfgServ.getProperty(org, "Some-Property-1");
		assertNull(config);
		commitTenantTransaction();

		// add two configs
		beginTenantTransaction();
		DomainObjectProperty config2 = new DomainObjectProperty(org,type2,"Some-Property-2");
		DomainObjectProperty config3 = new DomainObjectProperty(org,type3,"Some-Property-3");
		cfgServ.store(config2);		
		cfgServ.store(config3);		
		commitTenantTransaction();
		
		beginTenantTransaction();
		List<DomainObjectProperty> configs = cfgServ.getProperties(org);
		assertNotNull(configs);
		assertEquals(2,configs.size());
		commitTenantTransaction();
	}
	
	@Test
	public void testTypedConvenienceMethods() {
		PropertyDao cfgServ = PropertyDao.getInstance();

		beginTenantTransaction();
		Organization org=new Organization();
		org.setDomainId("testOrg");
		Organization.DAO.store(org);
		
		// create three property types in the database
		DomainObjectPropertyDefault type1 = new DomainObjectPropertyDefault("string-config",org.getClassName(),"Default-Value-1","Property-Description-1");
		cfgServ.store(type1);
		DomainObjectPropertyDefault type2 = new DomainObjectPropertyDefault("int-config",org.getClassName(),"Default-Value-2","Property-Description-2");
		cfgServ.store(type2);
		DomainObjectPropertyDefault type3 = new DomainObjectPropertyDefault("double-config",org.getClassName(),"Default-Value-3","Property-Description-3");
		cfgServ.store(type3);

		DomainObjectProperty config1 = new DomainObjectProperty(org,type1,"value");
		DomainObjectProperty config2 = new DomainObjectProperty(org,type2).setValue(123);
		DomainObjectProperty config3 = new DomainObjectProperty(org,type3).setValue(123.456);

		cfgServ.store(config1);
		cfgServ.store(config2);
		cfgServ.store(config3);
		commitTenantTransaction();

		beginTenantTransaction();

		String stringValue = cfgServ.getProperty(org, "string-config").getValue();
		assertEquals("value", stringValue);
		
		int intValue = cfgServ.getProperty(org, "int-config").getIntValue();
		assertEquals(123,intValue);

		double doubleValue = cfgServ.getProperty(org, "double-config").getDoubleValue();
		assertEquals(123.456,doubleValue,0.00001);
		
		commitTenantTransaction();
	}

	@Test
	public void testPropertiesWithDefaults() {
		PropertyDao cfgServ = PropertyDao.getInstance();

		beginTenantTransaction();
		Organization org=new Organization();
		org.setDomainId("testOrg");
		Organization.DAO.store(org);
		List<DomainObjectPropertyDefault> types = cfgServ.getPropertyDefaults(org);
		assertNotNull(types);
		assertEquals(0, types.size());
		DomainObjectPropertyDefault type = cfgServ.getPropertyDefault(org,"test-prop");
		assertNull(type);
		assertEquals(0, types.size());
		commitTenantTransaction();

		// create three property types in the database
		beginTenantTransaction();
		DomainObjectPropertyDefault type1 = new DomainObjectPropertyDefault("Some-Property-1",org.getClassName(),"Default-Value","Default-Description");
		cfgServ.store(type1);
		DomainObjectPropertyDefault type2 = new DomainObjectPropertyDefault("Some-Property-2",org.getClassName(),"Default-Value","Default-Description");
		cfgServ.store(type2);
		DomainObjectPropertyDefault type3 = new DomainObjectPropertyDefault("Some-Property-3",org.getClassName(),"Default-Value","Default-Description");
		cfgServ.store(type3);
		commitTenantTransaction();
		
		// check config types
		beginTenantTransaction();
		types = cfgServ.getPropertyDefaults(org);
		assertNotNull(types);
		assertEquals(3,types.size());

		// get raw config properties. should be empty set.
		List<DomainObjectProperty> configs = cfgServ.getProperties(org);
		assertNotNull(configs);
		assertEquals(0,configs.size());

		// now get properties with default values. should be three.
		configs = cfgServ.getPropertiesWithDefaults(org);
		assertNotNull(configs);
		assertEquals(3,configs.size());
		for (DomainObjectProperty c : configs) {
			assertEquals("Default-Value", c.getValue());				
		}
		
		// add a property with a different value
		DomainObjectProperty config = new DomainObjectProperty(org, type2);
		config.setValue("Modified description");
		cfgServ.store(config);
		commitTenantTransaction();

		// check description
		beginTenantTransaction();
		configs = cfgServ.getPropertiesWithDefaults(org);
		assertNotNull(configs);
		assertEquals(3,configs.size());
		for (DomainObjectProperty c : configs) {
			if (c.getName().equals("Some-Property-2")) {
				assertEquals("Modified description", c.getValue());
			}
			else {
				assertEquals("Default-Value", c.getValue());				
			}
		}
		commitTenantTransaction();
	}
}
