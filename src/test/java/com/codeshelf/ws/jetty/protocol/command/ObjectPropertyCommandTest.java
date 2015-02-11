package com.codeshelf.ws.jetty.protocol.command;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.model.dao.PropertyDao;
import com.codeshelf.model.domain.CodeshelfNetwork;
import com.codeshelf.model.domain.DomainObjectPropertyDefault;
import com.codeshelf.model.domain.DomainTestABC;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.ws.jetty.protocol.request.ObjectPropertiesRequest;
import com.codeshelf.ws.jetty.protocol.response.ObjectPropertiesResponse;
import com.codeshelf.ws.jetty.protocol.response.ResponseStatus;

public class ObjectPropertyCommandTest extends DomainTestABC {
	
	@SuppressWarnings("unused")
	private static final Logger	LOGGER	= LoggerFactory.getLogger(ObjectPropertyCommandTest.class);

	@Test
	public void testObjectPropertyCommandUsingDefault() {
		PropertyDao cfgServ = PropertyDao.getInstance();

		beginTransaction();
		Facility facilityx=createFacility();
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

		// retrieve property via command
		beginTransaction();
		ObjectPropertiesRequest req =  new ObjectPropertiesRequest();
		req.setClassName(network.getClassName());
		req.setPersistentId(network.getPersistentId().toString());
		
		ObjectPropertiesCommand command = new ObjectPropertiesCommand(null, req);
		ObjectPropertiesResponse resp = (ObjectPropertiesResponse) command.exec();
		assertNotNull(resp);
		assertEquals(ResponseStatus.Success, resp.getStatus());
		
		assertEquals(resp.getClassName(),network.getClassName());
		assertEquals(resp.getPersistentId(),network.getPersistentId().toString());
		// The response now has results, and not the raw persistable properties. Do not look for properties.		
		// test the response results
		List<Map<String, Object>> results = resp.getResults();
		assertNotNull(results);
		assertEquals(results.size(), 1);
		Map<String, Object> oneResult = results.get(0);
		assertEquals(oneResult.size(), 7); // see setDefaultPropertyNames().
		assertEquals(oneResult.get("name"), "test-prop");
		assertEquals(oneResult.get("description"), "Property-Description-1");

		
		commitTransaction();
	}	
}
