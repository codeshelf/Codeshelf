package com.gadgetworks.codeshelf.model.domain;

import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.gadgetworks.codeshelf.generators.FacilityGenerator;
import com.gadgetworks.codeshelf.generators.WorkInstructionGenerator;
import com.gadgetworks.codeshelf.model.EdiProviderEnum;
import com.gadgetworks.codeshelf.model.dao.DAOTestABC;
import com.gadgetworks.codeshelf.model.dao.ITypedDao;
import com.google.common.collect.ImmutableList;

// TODO: should use mock DAO 
public class IronMqServiceOnlineTest extends DomainTestABC {

	private Map<String, String> tempPropertyRestore  = new HashMap<String, String>();
	
	@Before
	@SuppressWarnings("unchecked")
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


		//IronMqService.DAO = mock(ITypedDao.class);
		//DropboxService.DAO = mock(ITypedDao.class);
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
	
	@Test //TODO Tests Connectivity. Could put into a Category that commonly excludes
	public void networkConnectionTest() throws IOException {
		this.getPersistenceService().beginTenantTransaction();
		
		WorkInstructionGenerator workInstructionGenerator = new WorkInstructionGenerator();
		FacilityGenerator facilityGenerator = new FacilityGenerator();
		Facility facility = facilityGenerator.generateValid();
		WorkInstruction wi = workInstructionGenerator.generateValid(facility);
		
		IronMqService service = new IronMqService();
		service.setDomainId("IRONMQTEST");
		service.setProvider(EdiProviderEnum.IRONMQ);
		facility.addEdiService(service);

		service.storeCredentials("540e1486364af100050000b4", "RzgIyO5FNeNAgZljs9x4um5UVqw");
		service.sendWorkInstructionsToHost(ImmutableList.of(wi));
		String[] messages = service.getMessages(IronMqService.MAX_NUM_MESSAGES, 5);
		Assert.assertTrue(messages.length > 0);
		boolean found = false;
		for (int i = 0; i < messages.length; i++) {
			String string = messages[i];
			found = string.contains(wi.getDomainId());
		}
		Assert.assertTrue("Did not find work instruction message", found);
		
		this.getPersistenceService().endTenantTransaction();
	}
	
	
	@Test //TODO Tests Connectivity. Could put into a Category that commonly excludes
	public void badTokenTest() throws IOException {
		this.getPersistenceService().beginTenantTransaction();

		WorkInstructionGenerator workInstructionGenerator = new WorkInstructionGenerator();
		FacilityGenerator facilityGenerator = new FacilityGenerator();
		Facility facility = facilityGenerator.generateValid();
		WorkInstruction wi = workInstructionGenerator.generateValid(facility);
		
		IronMqService service = new IronMqService();
		service.setDomainId("IRONMQTEST");
		service.setProvider(EdiProviderEnum.IRONMQ);
		facility.addEdiService(service);

		service.storeCredentials("540e1486364af100050000b4", "BAD");
		try {
			service.sendWorkInstructionsToHost(ImmutableList.of(wi));
			Assert.fail("Should have thrown IOException");
		} catch(IOException e) {
			e.printStackTrace();
		}
		
		this.getPersistenceService().endTenantTransaction();
	}

	@Test //TODO Tests Connectivity. Could put into a Category that commonly excludes
	public void badProjectId() throws IOException {
		this.getPersistenceService().beginTenantTransaction();

		WorkInstructionGenerator workInstructionGenerator = new WorkInstructionGenerator();
		FacilityGenerator facilityGenerator = new FacilityGenerator();
		Facility facility = facilityGenerator.generateValid();
		WorkInstruction wi = workInstructionGenerator.generateValid(facility);
		
		IronMqService service = new IronMqService();
		service.setDomainId("IRONMQTEST");
		service.setProvider(EdiProviderEnum.IRONMQ);
		facility.addEdiService(service);

		service.storeCredentials("BAD", "RzgIyO5FNeNAgZljs9x4um5UVqw");
		try {
			service.sendWorkInstructionsToHost(ImmutableList.of(wi));
			Assert.fail("Should have thrown IOException");
		} catch(IOException e) {
			e.printStackTrace();

		
		this.getPersistenceService().endTenantTransaction();
}
	}

}
