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
import com.codeshelf.model.domain.DomainTestABC;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.platform.persistence.PersistenceService;

public class DomainObjectPropertyTest extends DomainTestABC {
	
	@SuppressWarnings("unused")
	private static final Logger	LOGGER	= LoggerFactory.getLogger(DomainObjectPropertyTest.class);

	@Test
	public void testPropertyDefaultOperations() {
		PropertyDao cfgServ = PropertyDao.getInstance();

		beginTransaction();

		Facility facilityx = createFacility();
		CodeshelfNetwork network = facilityx.getNetworks().get(0);
		List<DomainObjectPropertyDefault> types = cfgServ.getPropertyDefaults(network);
		assertNotNull(types);
		assertEquals(0, types.size());
		DomainObjectPropertyDefault type = cfgServ.getPropertyDefault(network,"test-prop");
		assertNull(type);
		assertEquals(0, types.size());
		commitTransaction();

		// add config type
		beginTransaction();
		DomainObjectPropertyDefault type1 = new DomainObjectPropertyDefault("test-prop",network.getClassName(),"Default-Value-1","Property-Description-1");
		cfgServ.store(type1);
		commitTransaction();

		// retrieve data, check value and delete it
		beginTransaction();
		types = cfgServ.getPropertyDefaults(network);
		assertNotNull(types);
		assertEquals(1, types.size());
		type = cfgServ.getPropertyDefault(network,"test-prop");
		assertNotNull(type);
		assertEquals("Property-Description-1", type.getDescription());
		cfgServ.delete(type);
		commitTransaction();
		
		// make sure data is deleted
		beginTransaction();
		types = cfgServ.getPropertyDefaults(network);
		assertNotNull(types);
		assertEquals(0, types.size());
		type = cfgServ.getPropertyDefault(network,"test-prop");
		assertNull(type);
		assertEquals(0, types.size());
		commitTransaction();
	}
	
	@Test
	public void testPropertyOperations() {
		PropertyDao cfgServ = PropertyDao.getInstance();
		// create organization and set one config value
		beginTransaction();

		Facility facility = createFacility();
		// create three property types in the database
		DomainObjectPropertyDefault type1 = new DomainObjectPropertyDefault("Some-Property-1",facility.getClassName(),"Default-Value-1","Property-Description-1");
		cfgServ.store(type1);
		DomainObjectPropertyDefault type2 = new DomainObjectPropertyDefault("Some-Property-2",facility.getClassName(),"Default-Value-2","Property-Description-2");
		cfgServ.store(type2);
		DomainObjectPropertyDefault type3 = new DomainObjectPropertyDefault("Some-Property-3",facility.getClassName(),"Default-Value-3","Property-Description-3");
		cfgServ.store(type3);
		
		DomainObjectProperty config = new DomainObjectProperty(facility,type1,"value");
		cfgServ.store(config);		
		commitTransaction();
		
		// retrieve data, check value and delete it
		beginTransaction();
		config = cfgServ.getProperty(facility, "Some-Property-1");
		assertNotNull(config);
		assertEquals("value", config.getValue());
		cfgServ.delete(config);
		commitTransaction();
		
		// make sure config can't be found
		beginTransaction();
		config = cfgServ.getProperty(facility, "Some-Property-1");
		assertNull(config);
		commitTransaction();

		// add two configs
		beginTransaction();
		DomainObjectProperty config2 = new DomainObjectProperty(facility,type2,"Some-Property-2");
		DomainObjectProperty config3 = new DomainObjectProperty(facility,type3,"Some-Property-3");
		cfgServ.store(config2);		
		cfgServ.store(config3);		
		commitTransaction();
		
		beginTransaction();
		List<DomainObjectProperty> configs = cfgServ.getProperties(facility);
		assertNotNull(configs);
		assertEquals(2,configs.size());
		commitTransaction();
	}
	
	@Test
	public void testTypedConvenienceMethods() {
		PropertyDao cfgServ = PropertyDao.getInstance();

		beginTransaction();
	
		Facility facility = createFacility();
		// create three property types in the database
		DomainObjectPropertyDefault type1 = new DomainObjectPropertyDefault("string-config",facility.getClassName(),"Default-Value-1","Property-Description-1");
		cfgServ.store(type1);
		DomainObjectPropertyDefault type2 = new DomainObjectPropertyDefault("int-config",facility.getClassName(),"Default-Value-2","Property-Description-2");
		cfgServ.store(type2);
		DomainObjectPropertyDefault type3 = new DomainObjectPropertyDefault("double-config",facility.getClassName(),"Default-Value-3","Property-Description-3");
		cfgServ.store(type3);

		DomainObjectProperty config1 = new DomainObjectProperty(facility,type1,"value");
		DomainObjectProperty config2 = new DomainObjectProperty(facility,type2).setValue(123);
		DomainObjectProperty config3 = new DomainObjectProperty(facility,type3).setValue(123.456);

		cfgServ.store(config1);
		cfgServ.store(config2);
		cfgServ.store(config3);
		commitTransaction();

		beginTransaction();

		String stringValue = cfgServ.getProperty(facility, "string-config").getValue();
		assertEquals("value", stringValue);
		
		int intValue = cfgServ.getProperty(facility, "int-config").getIntValue();
		assertEquals(123,intValue);

		double doubleValue = cfgServ.getProperty(facility, "double-config").getDoubleValue();
		assertEquals(123.456,doubleValue,0.00001);
		
		commitTransaction();
	}

	@Test
	public void testPropertiesWithDefaults() {
		PropertyDao cfgServ = PropertyDao.getInstance();

		beginTransaction();

		Facility facilityx = createFacility();
		CodeshelfNetwork network = facilityx.getNetworks().get(0);
		List<DomainObjectPropertyDefault> types = cfgServ.getPropertyDefaults(network);
		assertNotNull(types);
		assertEquals(0, types.size());
		DomainObjectPropertyDefault type = cfgServ.getPropertyDefault(network,"test-prop");
		assertNull(type);
		assertEquals(0, types.size());
		commitTransaction();

		// create three property types in the database
		beginTransaction();
		DomainObjectPropertyDefault type1 = new DomainObjectPropertyDefault("Some-Property-1",network.getClassName(),"Default-Value","Default-Description");
		cfgServ.store(type1);
		DomainObjectPropertyDefault type2 = new DomainObjectPropertyDefault("Some-Property-2",network.getClassName(),"Default-Value","Default-Description");
		cfgServ.store(type2);
		DomainObjectPropertyDefault type3 = new DomainObjectPropertyDefault("Some-Property-3",network.getClassName(),"Default-Value","Default-Description");
		cfgServ.store(type3);
		commitTransaction();
		
		// check config types
		beginTransaction();
		types = cfgServ.getPropertyDefaults(network);
		assertNotNull(types);
		assertEquals(3,types.size());

		// get raw config properties. should be empty set.
		List<DomainObjectProperty> configs = cfgServ.getProperties(network);
		assertNotNull(configs);
		assertEquals(0,configs.size());

		// now get properties with default values. should be three.
		configs = cfgServ.getPropertiesWithDefaults(network);
		assertNotNull(configs);
		assertEquals(3,configs.size());
		for (DomainObjectProperty c : configs) {
			assertEquals("Default-Value", c.getValue());				
		}
		
		// add a property with a different value
		DomainObjectProperty config = new DomainObjectProperty(network, type2);
		config.setValue("Modified description");
		cfgServ.store(config);
		commitTransaction();

		// check description
		beginTransaction();
		configs = cfgServ.getPropertiesWithDefaults(network);
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
		commitTransaction();
	}

	@Test
	public void testPropertyCascadeDelete() {
		PropertyDao cfgServ = PropertyDao.getInstance();

		beginTransaction();
		Facility facilityx = createFacility();
		CodeshelfNetwork network = facilityx.getNetworks().get(0);
		List<DomainObjectPropertyDefault> types = cfgServ.getPropertyDefaults(network);
		assertNotNull(types);
		assertEquals(0, types.size());
		DomainObjectPropertyDefault type = cfgServ.getPropertyDefault(network,"test-prop");
		assertNull(type);
		assertEquals(0, types.size());
		commitTransaction();

		// create three property types in the database
		beginTransaction();
		DomainObjectPropertyDefault type1 = new DomainObjectPropertyDefault("Some-Property-1",network.getClassName(),"Default-Value","Default-Description");
		cfgServ.store(type1);
		DomainObjectPropertyDefault type2 = new DomainObjectPropertyDefault("Some-Property-2",network.getClassName(),"Default-Value","Default-Description");
		cfgServ.store(type2);
		DomainObjectPropertyDefault type3 = new DomainObjectPropertyDefault("Some-Property-3",network.getClassName(),"Default-Value","Default-Description");
		cfgServ.store(type3);
		commitTransaction();
		
		// check defaults
		beginTransaction();
		types = cfgServ.getPropertyDefaults(network);
		assertNotNull(types);
		assertEquals(3,types.size());

		// get raw config properties. should be empty set.
		List<DomainObjectProperty> configs = cfgServ.getProperties(network);
		assertNotNull(configs);
		assertEquals(0,configs.size());

		// now get properties with default values. should be three.
		configs = cfgServ.getPropertiesWithDefaults(network);
		assertNotNull(configs);
		assertEquals(3,configs.size());
		for (DomainObjectProperty c : configs) {
			assertEquals("Default-Value", c.getValue());				
		}
		
		// add a property with a different value
		DomainObjectProperty config = new DomainObjectProperty(network, type2);
		config.setValue("Modified description");
		cfgServ.store(config);
		commitTransaction();

		// should be one explicit property
		beginTransaction();
		configs = cfgServ.getProperties(network);
		assertNotNull(configs);
		assertEquals(1,configs.size());
		type2 = cfgServ.getPropertyDefault(network, "Some-Property-2");
		List<DomainObjectProperty> props = type2.getProperties();
		assertEquals(1,props.size());
		commitTransaction();

		// delete default
		beginTransaction();
		type2 = cfgServ.getPropertyDefault(network,type2.getName()); // will throw on try to delete detached instance in Hibernate 4.3.8
		cfgServ.delete(type2);
		commitTransaction();
		
		// make sure default and instance are deleted
		beginTransaction();
		DomainObjectPropertyDefault deletedDefault = cfgServ.getPropertyDefault(network,type2.getName());
		assertNull(deletedDefault);
		DomainObjectProperty deletedProp = cfgServ.getProperty(network, "Some-Property-2");
		assertNull(deletedProp);
		commitTransaction();
	}

}
