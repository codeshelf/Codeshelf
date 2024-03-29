package com.codeshelf.model.domain;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.codeshelf.generators.FacilityGenerator;
import com.codeshelf.model.EdiTransportType;
import com.codeshelf.testframework.MockDaoTest;

// TODO: should use mock DAO 
public class IronMqGatewayOnlineTest extends MockDaoTest {

	private Map<String, String> tempPropertyRestore  = new HashMap<String, String>();
	
	@Before
	public void doBefore() {
		super.doBefore();
		//USE THE DEFAULT KEYSTORE COMMUNICATING TO IRON MQ
		String[] keys = new String[]{
				"javax.net.ssl.keyStore",
				"javax.net.ssl.keyStorePassword",
				"javax.net.ssl.trustStore",
				"javax.net.ssl.trustStorePassword",
				"javax.net.ssl.trustStoreType",
				
				
		};
		for (String key : keys) {
			tempPropertyRestore.put(key, System.clearProperty(key));
		}


		//IronMqGateway.DAO = mock(ITypedDao.class);
		//DropboxGateway.DAO = mock(ITypedDao.class);
		//Facility.DAO = mock(ITypedDao.class);
		//Organization.DAO = mock(ITypedDao.class);
	}

	@After
	public void doAfter() {
		super.doAfter();
		for (Entry<String, String> entry : tempPropertyRestore.entrySet()) {
			if (entry.getValue() != null) {
				try {
					System.setProperty(entry.getKey(), entry.getValue());
				} catch(Exception e) {
					throw new RuntimeException("Could not reset property: " + entry, e);
				}
			}
		}
	}
	
	@Ignore
	@Test //TODO Tests Connectivity. Could put into a Category that commonly excludes
	public void networkConnectionTest() throws IOException {
		this.getTenantPersistenceService().beginTransaction();
		
		FacilityGenerator facilityGenerator = new FacilityGenerator();
		Facility facility = facilityGenerator.generateValid();
		
		IronMqGateway gateway = new IronMqGateway();
		gateway.setDomainId("IRONMQTEST");
		gateway.setTransportType(EdiTransportType.IRONMQ);
		facility.addEdiGateway(gateway);

		gateway.storeCredentials("540e1486364af100050000b4", "RzgIyO5FNeNAgZljs9x4um5UVqw", "true");
		String message = "TESTMESSAGE" + System.currentTimeMillis();
		gateway.transportWiFinished("AMESSAGE", "AMESSAGE", message);
		String[] messages = new String[0];
		boolean found = false;
		do {
			messages = gateway.consumeMessages(IronMqGateway.MAX_NUM_MESSAGES, 5);
			for (int i = 0; i < messages.length; i++) {
				String string = messages[i];
				found = string.contains(message);
				if (found) break;
			}
		}
		while(messages.length > 0);
		Assert.assertTrue("Did not find work instruction message: " + message, found);
		
		this.getTenantPersistenceService().commitTransaction();
	}
	
	@Test //TODO Tests Connectivity. Could put into a Category that commonly excludes
	public void badTokenTest() throws IOException {
		this.getTenantPersistenceService().beginTransaction();

		FacilityGenerator facilityGenerator = new FacilityGenerator();
		Facility facility = facilityGenerator.generateValid();
		
		IronMqGateway gateway = new IronMqGateway();
		gateway.setDomainId("IRONMQTEST");
		facility.addEdiGateway(gateway);

		gateway.storeCredentials("540e1486364af100050000b4", "BAD", "true");
		try {
			gateway.transportWiFinished("AMESSAGE", "AMESSAGE", "AMESSAGE");
			Assert.fail("Should have thrown IOException");
		} catch(IOException e) {
			e.printStackTrace();
		}
		
		this.getTenantPersistenceService().commitTransaction();
	}

	@Test //TODO Tests Connectivity. Could put into a Category that commonly excludes
	public void badProjectId() throws IOException {
		this.getTenantPersistenceService().beginTransaction();

		FacilityGenerator facilityGenerator = new FacilityGenerator();
		Facility facility = facilityGenerator.generateValid();
		
		IronMqGateway gateway = new IronMqGateway();
		gateway.setDomainId("IRONMQTEST");
		facility.addEdiGateway(gateway);

		gateway.storeCredentials("BAD", "RzgIyO5FNeNAgZljs9x4um5UVqw", "true");
		try {
			gateway.transportWiFinished("AMESSAGE", "AMESSAGE", "AMESSAGE");
			Assert.fail("Should have thrown IOException");
		} catch(IOException e) {
			e.printStackTrace();

		
		this.getTenantPersistenceService().commitTransaction();
}
	}

}
