package com.codeshelf.platform.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.List;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.model.dao.PropertyDao;
import com.codeshelf.model.domain.CodeshelfNetwork;
import com.codeshelf.model.domain.DomainObjectProperty;
import com.codeshelf.model.domain.DomainObjectPropertyDefault;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.testframework.HibernateTest;

public class DomainObjectPropertyTest extends HibernateTest {
	
	@SuppressWarnings("unused")
	private static final Logger	LOGGER	= LoggerFactory.getLogger(DomainObjectPropertyTest.class);

	@Test
	public void testPropertyDefaultOperations() {
		PropertyDao cfgServ = PropertyDao.getInstance();

		tenantPersistenceService.beginTransaction(getDefaultTenant());

		Facility facilityx = createFacility();
		CodeshelfNetwork network = facilityx.getNetworks().get(0);
		List<DomainObjectPropertyDefault> types = cfgServ.getPropertyDefaults(getDefaultTenant(),network);
		assertNotNull(types);
		assertEquals(0, types.size());
		DomainObjectPropertyDefault type = cfgServ.getPropertyDefault(getDefaultTenant(),network,"test-prop");
		assertNull(type);
		assertEquals(0, types.size());
		tenantPersistenceService.commitTransaction(getDefaultTenant());

		// add config type
		tenantPersistenceService.beginTransaction(getDefaultTenant());
		DomainObjectPropertyDefault type1 = new DomainObjectPropertyDefault("test-prop",network.getClassName(),"Default-Value-1","Property-Description-1");
		cfgServ.store(getDefaultTenant(),type1);
		tenantPersistenceService.commitTransaction(getDefaultTenant());

		// retrieve data, check value and delete it
		tenantPersistenceService.beginTransaction(getDefaultTenant());
		types = cfgServ.getPropertyDefaults(getDefaultTenant(),network);
		assertNotNull(types);
		assertEquals(1, types.size());
		type = cfgServ.getPropertyDefault(getDefaultTenant(),network,"test-prop");
		assertNotNull(type);
		assertEquals("Property-Description-1", type.getDescription());
		cfgServ.delete(getDefaultTenant(),type);
		tenantPersistenceService.commitTransaction(getDefaultTenant());
		
		// make sure data is deleted
		tenantPersistenceService.beginTransaction(getDefaultTenant());
		types = cfgServ.getPropertyDefaults(getDefaultTenant(),network);
		assertNotNull(types);
		assertEquals(0, types.size());
		type = cfgServ.getPropertyDefault(getDefaultTenant(),network,"test-prop");
		assertNull(type);
		assertEquals(0, types.size());
		tenantPersistenceService.commitTransaction(getDefaultTenant());
	}
	
	@Test
	public void testPropertyOperations() {
		PropertyDao cfgServ = PropertyDao.getInstance();
		// create organization and set one config value
		tenantPersistenceService.beginTransaction(getDefaultTenant());

		Facility facility = createFacility();
		// create three property types in the database
		DomainObjectPropertyDefault type1 = new DomainObjectPropertyDefault("Some-Property-1",facility.getClassName(),"Default-Value-1","Property-Description-1");
		cfgServ.store(getDefaultTenant(),type1);
		DomainObjectPropertyDefault type2 = new DomainObjectPropertyDefault("Some-Property-2",facility.getClassName(),"Default-Value-2","Property-Description-2");
		cfgServ.store(getDefaultTenant(),type2);
		DomainObjectPropertyDefault type3 = new DomainObjectPropertyDefault("Some-Property-3",facility.getClassName(),"Default-Value-3","Property-Description-3");
		cfgServ.store(getDefaultTenant(),type3);
		
		DomainObjectProperty config = new DomainObjectProperty(facility,type1,"value");
		cfgServ.store(getDefaultTenant(),config);		
		tenantPersistenceService.commitTransaction(getDefaultTenant());
		
		// retrieve data, check value and delete it
		tenantPersistenceService.beginTransaction(getDefaultTenant());
		config = cfgServ.getProperty(getDefaultTenant(),facility, "Some-Property-1");
		assertNotNull(config);
		assertEquals("value", config.getValue());
		cfgServ.delete(getDefaultTenant(),config);
		tenantPersistenceService.commitTransaction(getDefaultTenant());
		
		// make sure config can't be found
		tenantPersistenceService.beginTransaction(getDefaultTenant());
		config = cfgServ.getProperty(getDefaultTenant(),facility, "Some-Property-1");
		assertNull(config);
		tenantPersistenceService.commitTransaction(getDefaultTenant());

		// add two configs
		tenantPersistenceService.beginTransaction(getDefaultTenant());
		DomainObjectProperty config2 = new DomainObjectProperty(facility,type2,"Some-Property-2");
		DomainObjectProperty config3 = new DomainObjectProperty(facility,type3,"Some-Property-3");
		cfgServ.store(getDefaultTenant(),config2);		
		cfgServ.store(getDefaultTenant(),config3);		
		tenantPersistenceService.commitTransaction(getDefaultTenant());
		
		tenantPersistenceService.beginTransaction(getDefaultTenant());
		List<DomainObjectProperty> configs = cfgServ.getProperties(getDefaultTenant(),facility);
		assertNotNull(configs);
		assertEquals(2,configs.size());
		tenantPersistenceService.commitTransaction(getDefaultTenant());
	}
	
	@Test
	public void testTypedConvenienceMethods() {
		PropertyDao cfgServ = PropertyDao.getInstance();

		tenantPersistenceService.beginTransaction(getDefaultTenant());
	
		Facility facility = createFacility();
		// create three property types in the database
		DomainObjectPropertyDefault type1 = new DomainObjectPropertyDefault("string-config",facility.getClassName(),"Default-Value-1","Property-Description-1");
		cfgServ.store(getDefaultTenant(),type1);
		DomainObjectPropertyDefault type2 = new DomainObjectPropertyDefault("int-config",facility.getClassName(),"Default-Value-2","Property-Description-2");
		cfgServ.store(getDefaultTenant(),type2);
		DomainObjectPropertyDefault type3 = new DomainObjectPropertyDefault("double-config",facility.getClassName(),"Default-Value-3","Property-Description-3");
		cfgServ.store(getDefaultTenant(),type3);

		DomainObjectProperty config1 = new DomainObjectProperty(facility,type1,"value");
		DomainObjectProperty config2 = new DomainObjectProperty(facility,type2).setValue(123);
		DomainObjectProperty config3 = new DomainObjectProperty(facility,type3).setValue(123.456);

		cfgServ.store(getDefaultTenant(),config1);
		cfgServ.store(getDefaultTenant(),config2);
		cfgServ.store(getDefaultTenant(),config3);
		tenantPersistenceService.commitTransaction(getDefaultTenant());

		tenantPersistenceService.beginTransaction(getDefaultTenant());

		String stringValue = cfgServ.getProperty(getDefaultTenant(),facility, "string-config").getValue();
		assertEquals("value", stringValue);
		
		int intValue = cfgServ.getProperty(getDefaultTenant(),facility, "int-config").getIntValue();
		assertEquals(123,intValue);

		double doubleValue = cfgServ.getProperty(getDefaultTenant(),facility, "double-config").getDoubleValue();
		assertEquals(123.456,doubleValue,0.00001);
		
		tenantPersistenceService.commitTransaction(getDefaultTenant());
	}

	@Test
	public void testPropertiesWithDefaults() {
		PropertyDao cfgServ = PropertyDao.getInstance();

		tenantPersistenceService.beginTransaction(getDefaultTenant());

		Facility facilityx = createFacility();
		CodeshelfNetwork network = facilityx.getNetworks().get(0);
		List<DomainObjectPropertyDefault> types = cfgServ.getPropertyDefaults(getDefaultTenant(),network);
		assertNotNull(types);
		assertEquals(0, types.size());
		DomainObjectPropertyDefault type = cfgServ.getPropertyDefault(getDefaultTenant(),network,"test-prop");
		assertNull(type);
		assertEquals(0, types.size());
		tenantPersistenceService.commitTransaction(getDefaultTenant());

		// create three property types in the database
		tenantPersistenceService.beginTransaction(getDefaultTenant());
		DomainObjectPropertyDefault type1 = new DomainObjectPropertyDefault("Some-Property-1",network.getClassName(),"Default-Value","Default-Description");
		cfgServ.store(getDefaultTenant(),type1);
		DomainObjectPropertyDefault type2 = new DomainObjectPropertyDefault("Some-Property-2",network.getClassName(),"Default-Value","Default-Description");
		cfgServ.store(getDefaultTenant(),type2);
		DomainObjectPropertyDefault type3 = new DomainObjectPropertyDefault("Some-Property-3",network.getClassName(),"Default-Value","Default-Description");
		cfgServ.store(getDefaultTenant(),type3);
		tenantPersistenceService.commitTransaction(getDefaultTenant());
		
		// check config types
		tenantPersistenceService.beginTransaction(getDefaultTenant());
		types = cfgServ.getPropertyDefaults(getDefaultTenant(),network);
		assertNotNull(types);
		assertEquals(3,types.size());

		// get raw config properties. should be empty set.
		List<DomainObjectProperty> configs = cfgServ.getProperties(getDefaultTenant(),network);
		assertNotNull(configs);
		assertEquals(0,configs.size());

		// now get properties with default values. should be three.
		configs = cfgServ.getPropertiesWithDefaults(getDefaultTenant(),network);
		assertNotNull(configs);
		assertEquals(3,configs.size());
		for (DomainObjectProperty c : configs) {
			assertEquals("Default-Value", c.getValue());				
		}
		
		// add a property with a different value
		DomainObjectProperty config = new DomainObjectProperty(network, type2);
		config.setValue("Modified description");
		cfgServ.store(getDefaultTenant(),config);
		tenantPersistenceService.commitTransaction(getDefaultTenant());

		// check description
		tenantPersistenceService.beginTransaction(getDefaultTenant());
		configs = cfgServ.getPropertiesWithDefaults(getDefaultTenant(),network);
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
		tenantPersistenceService.commitTransaction(getDefaultTenant());
	}

	@Test
	public void testPropertyCascadeDelete() {
		PropertyDao cfgServ = PropertyDao.getInstance();

		tenantPersistenceService.beginTransaction(getDefaultTenant());
		Facility facilityx = createFacility();
		CodeshelfNetwork network = facilityx.getNetworks().get(0);
		List<DomainObjectPropertyDefault> types = cfgServ.getPropertyDefaults(getDefaultTenant(),network);
		assertNotNull(types);
		assertEquals(0, types.size());
		DomainObjectPropertyDefault type = cfgServ.getPropertyDefault(getDefaultTenant(),network,"test-prop");
		assertNull(type);
		assertEquals(0, types.size());
		tenantPersistenceService.commitTransaction(getDefaultTenant());

		// create three property types in the database
		tenantPersistenceService.beginTransaction(getDefaultTenant());
		DomainObjectPropertyDefault type1 = new DomainObjectPropertyDefault("Some-Property-1",network.getClassName(),"Default-Value","Default-Description");
		cfgServ.store(getDefaultTenant(),type1);
		DomainObjectPropertyDefault type2 = new DomainObjectPropertyDefault("Some-Property-2",network.getClassName(),"Default-Value","Default-Description");
		cfgServ.store(getDefaultTenant(),type2);
		DomainObjectPropertyDefault type3 = new DomainObjectPropertyDefault("Some-Property-3",network.getClassName(),"Default-Value","Default-Description");
		cfgServ.store(getDefaultTenant(),type3);
		tenantPersistenceService.commitTransaction(getDefaultTenant());
		
		// check defaults
		tenantPersistenceService.beginTransaction(getDefaultTenant());
		types = cfgServ.getPropertyDefaults(getDefaultTenant(),network);
		assertNotNull(types);
		assertEquals(3,types.size());

		// get raw config properties. should be empty set.
		List<DomainObjectProperty> configs = cfgServ.getProperties(getDefaultTenant(),network);
		assertNotNull(configs);
		assertEquals(0,configs.size());

		// now get properties with default values. should be three.
		configs = cfgServ.getPropertiesWithDefaults(getDefaultTenant(),network);
		assertNotNull(configs);
		assertEquals(3,configs.size());
		for (DomainObjectProperty c : configs) {
			assertEquals("Default-Value", c.getValue());				
		}
		
		// add a property with a different value
		DomainObjectProperty config = new DomainObjectProperty(network, type2);
		config.setValue("Modified description");
		cfgServ.store(getDefaultTenant(),config);
		tenantPersistenceService.commitTransaction(getDefaultTenant());

		// should be one explicit property
		tenantPersistenceService.beginTransaction(getDefaultTenant());
		configs = cfgServ.getProperties(getDefaultTenant(),network);
		assertNotNull(configs);
		assertEquals(1,configs.size());
		type2 = cfgServ.getPropertyDefault(getDefaultTenant(),network, "Some-Property-2");
		List<DomainObjectProperty> props = type2.getProperties();
		assertEquals(1,props.size());
		tenantPersistenceService.commitTransaction(getDefaultTenant());

		// delete default
		tenantPersistenceService.beginTransaction(getDefaultTenant());
		type2 = cfgServ.getPropertyDefault(getDefaultTenant(),network,type2.getName()); // will throw on try to delete detached instance in Hibernate 4.3.8
		cfgServ.delete(getDefaultTenant(),type2);
		tenantPersistenceService.commitTransaction(getDefaultTenant());
		
		// make sure default and instance are deleted
		tenantPersistenceService.beginTransaction(getDefaultTenant());
		DomainObjectPropertyDefault deletedDefault = cfgServ.getPropertyDefault(getDefaultTenant(),network,type2.getName());
		assertNull(deletedDefault);
		DomainObjectProperty deletedProp = cfgServ.getProperty(getDefaultTenant(),network, "Some-Property-2");
		assertNull(deletedProp);
		tenantPersistenceService.commitTransaction(getDefaultTenant());
	}

}
