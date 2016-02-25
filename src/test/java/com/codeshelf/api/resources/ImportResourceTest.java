package com.codeshelf.api.resources;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.api.resources.subresources.ImportResource;
import com.codeshelf.edi.OutboundOrderPrefetchCsvImporter;
import com.codeshelf.edi.WorkerCsvImporter;
import com.codeshelf.event.EventProducer;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.Worker;
import com.codeshelf.testframework.ServerTest;
import com.sun.jersey.core.header.FormDataContentDisposition;

public class ImportResourceTest extends ServerTest{
	private static final Logger	LOGGER	= LoggerFactory.getLogger(ImportResourceTest.class);
	private Facility facility;
	private ImportResource importResource;
	
	@Before
	public void init(){
		beginTransaction();
		facility = getFacility();
		importResource = new ImportResource(null,
			null,
			null,
			new OutboundOrderPrefetchCsvImporter(Mockito.mock(EventProducer.class)),
			null,
			new WorkerCsvImporter(Mockito.mock(EventProducer.class)));
		importResource.setFacility(facility);
		commitTransaction();
	}
	
	/**
	 * Simple success case
	 */
	@Test
	public void importWorkersSimple() throws IOException{
		beginTransaction();
		LOGGER.info("1. Import workers");
		String workersCsv = 
				"firstName,lastName,badgeId,workGroupName,humanResourcesId\n" + 
				"First A,Last A,Badge A,Test 1,007\n" +
				"First B,Last B,Badge B,Test 1,008\n" +
				"First C,Last C,Badge C,Test 2,009\n";
		uploadWorkers(workersCsv, false);
		
		LOGGER.info("2. Verify that workers were imported correctly");
		Worker workerA = Worker.findWorker(facility, "Badge A");
		Worker workerB = Worker.findWorker(facility, "Badge B");
		Worker workerC = Worker.findWorker(facility, "Badge C");
		assertWorker(workerA, true, "First A", "Last A", "Badge A", "Test 1", "007");
		assertWorker(workerB, true, "First B", "Last B", "Badge B", "Test 1", "008");
		assertWorker(workerC, true, "First C", "Last C", "Badge C", "Test 2", "009");
		commitTransaction();
	}
	
	/**
	 * When importing workers, make sure that old workers that did not get re-imported are deactivated 
	 */
	@Test
	public void importWorkersOverwrite() throws IOException{
		beginTransaction();
		LOGGER.info("1. Initial worker import Import workers");
		String workersCsv = 
				"firstName,lastName,badgeId,workGroupName,humanResourcesId\n" + 
				"First A,Last A,Badge A,Test 1,007\n" +
				"First B,Last B,Badge B,Test 1,008\n" +
				"First C,Last C,Badge C,Test 2,009\n";
		facility = facility.reload();
		uploadWorkers(workersCsv, false);
		commitTransaction();
		
		beginTransaction();
		LOGGER.info("2. Re-import some workers");
		workersCsv = 
				"firstName,lastName,badgeId,workGroupName,humanResourcesId\n" + 
				"First A,Last A,Badge A,Test 1,007\n" +
				"First C,Last C,Badge C,Test 2,009\n";
		facility = facility.reload();
		uploadWorkers(workersCsv, false);
		
		Worker workerA = Worker.findWorker(facility, "Badge A");
		Worker workerB = Worker.findWorker(facility, "Badge B");
		Worker workerC = Worker.findWorker(facility, "Badge C");
		assertWorker(workerA, true, "First A", "Last A", "Badge A", "Test 1", "007");
		assertWorker(workerB, false, "First B", "Last B", "Badge B", "Test 1", "008");
		assertWorker(workerC, true, "First C", "Last C", "Badge C", "Test 2", "009");
		commitTransaction();
	}
	
	/**
	 * Import workers in "append" mode, not deactivating old workers 
	 */
	@Test
	public void importWorkersAppend() throws IOException{
		beginTransaction();
		LOGGER.info("1. Initial worker import Import workers");
		String workersCsv = 
				"firstName,lastName,badgeId,workGroupName,humanResourcesId\n" + 
				"First A,Last A,Badge A,Test 1,007\n" +
				"First B,Last B,Badge B,Test 1,008\n" +
				"First C,Last C,Badge C,Test 2,009\n";
		facility = facility.reload();
		uploadWorkers(workersCsv, false);
		commitTransaction();
		
		beginTransaction();
		LOGGER.info("2. Re-import some workers, but use 'append' mode");
		workersCsv = 
				"firstName,lastName,badgeId,workGroupName,humanResourcesId\n" + 
				"First A,Last A,Badge A,Test 1,007\n" +
				"First C,Last C,Badge C,Test 2,009\n";
		facility = facility.reload();
		uploadWorkers(workersCsv, true);
		
		Worker workerA = Worker.findWorker(facility, "Badge A");
		Worker workerB = Worker.findWorker(facility, "Badge B");
		Worker workerC = Worker.findWorker(facility, "Badge C");
		assertWorker(workerA, true, "First A", "Last A", "Badge A", "Test 1", "007");
		//Note that Worker B is still active
		assertWorker(workerB, true, "First B", "Last B", "Badge B", "Test 1", "008");
		assertWorker(workerC, true, "First C", "Last C", "Badge C", "Test 2", "009");
		commitTransaction();
	}


	private void uploadWorkers(String workersCsv, boolean append) {
		importResource.uploadWorkers(new ByteArrayInputStream(workersCsv.getBytes()), Mockito.mock(FormDataContentDisposition.class), append);
	}
	
	private void assertWorker(Worker worker, boolean active, String firstName, String lastName, String badgeId, String workGroupName, String humanResourcesId){
		Assert.assertNotNull(worker);
		Assert.assertEquals(active, worker.getActive());
		Assert.assertEquals(firstName, worker.getFirstName());
		Assert.assertEquals(lastName, worker.getLastName());
		Assert.assertEquals(badgeId, worker.getDomainId());
		Assert.assertEquals(workGroupName, worker.getGroupName());
		Assert.assertEquals(humanResourcesId, worker.getHrId());
	}
}
