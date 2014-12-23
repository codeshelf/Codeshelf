package com.gadgetworks.codeshelf.ws.jetty.protocol.command;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gadgetworks.codeshelf.model.dao.PropertyDao;
import com.gadgetworks.codeshelf.model.domain.DomainObjectProperty;
import com.gadgetworks.codeshelf.model.domain.DomainObjectPropertyDefault;
import com.gadgetworks.codeshelf.model.domain.DomainTestABC;
import com.gadgetworks.codeshelf.model.domain.Organization;
import com.gadgetworks.codeshelf.ws.jetty.protocol.request.ObjectPropertiesRequest;
import com.gadgetworks.codeshelf.ws.jetty.protocol.response.ObjectPropertiesResponse;
import com.gadgetworks.codeshelf.ws.jetty.protocol.response.ResponseStatus;

public class ObjectPropertyCommandTest extends DomainTestABC {
	
	@SuppressWarnings("unused")
	private static final Logger	LOGGER	= LoggerFactory.getLogger(ObjectPropertyCommandTest.class);

	@Test
	public void testObjectPropertyCommandUsingDefault() {
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

		// retrieve property via command
		beginTenantTransaction();
		ObjectPropertiesRequest req =  new ObjectPropertiesRequest();
		req.setClassName(org.getClassName());
		req.setPersistentId(org.getPersistentId().toString());
		
		ObjectPropertiesCommand command = new ObjectPropertiesCommand(null, req);
		ObjectPropertiesResponse resp = (ObjectPropertiesResponse) command.exec();
		assertNotNull(resp);
		assertEquals(ResponseStatus.Success, resp.getStatus());
		
		assertEquals(resp.getClassName(),org.getClassName());
		assertEquals(resp.getPersistentId(),org.getPersistentId().toString());
		// The response now has results, and not the raw persistable properties. Do not look for properties.		
		// test the response results
		List<Map<String, Object>> results = resp.getResults();
		assertNotNull(results);
		assertEquals(results.size(), 1);
		Map<String, Object> oneResult = results.get(0);
		assertEquals(oneResult.size(), 7); // see setDefaultPropertyNames().
		assertEquals(oneResult.get("name"), "test-prop");
		assertEquals(oneResult.get("description"), "Property-Description-1");

		
		commitTenantTransaction();
	}	
}
