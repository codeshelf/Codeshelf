/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: EdiProcessorTest.java,v 1.6 2013/03/19 01:19:59 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.edi;

import java.io.InputStreamReader;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import junit.framework.Assert;

import org.junit.Test;

import com.gadgetworks.codeshelf.model.EdiServiceStateEnum;
import com.gadgetworks.codeshelf.model.dao.MockDao;
import com.gadgetworks.codeshelf.model.domain.Facility;
import com.gadgetworks.codeshelf.model.domain.IEdiService;
import com.gadgetworks.codeshelf.model.domain.Organization;

/**
 * @author jeffw
 *
 */
public class EdiProcessorTest {

	@Test
	public void ediProcessThreadTest() {

		MockDao<Facility> facilityDao = new MockDao<Facility>();
		ICsvImporter csvImporter = new ICsvImporter() {
			public void importOrdersFromCsvStream(InputStreamReader inCsvStreamReader, Facility inFacility) {
			}

			public void importInventoryFromCsvStream(InputStreamReader inCsvStreamReader, Facility inFacility) {
			}
		};

		IEdiProcessor ediProcessor = new EdiProcessor(csvImporter, facilityDao);
		BlockingQueue<String> testBlockingQueue = new ArrayBlockingQueue<>(100);
		ediProcessor.startProcessor(testBlockingQueue);

		Thread foundThread = null;
		for (Thread thread : Thread.getAllStackTraces().keySet()) {
			if (thread.getName().equals(IEdiProcessor.EDIPROCESSOR_THREAD_NAME)) {
				foundThread = thread;
			}
		}

		Assert.assertNotNull(foundThread);

		ediProcessor.stopProcessor();

		// That thread might be sleeping.
		if (foundThread != null) {
			foundThread.interrupt();
		}

		foundThread = null;
		for (Thread thread : Thread.getAllStackTraces().keySet()) {
			if (thread.getName().equals(IEdiProcessor.EDIPROCESSOR_THREAD_NAME)) {
				foundThread = thread;
			}
		}

		Assert.assertNotNull(foundThread);
	}

	@Test
	public void ediProcessorTest() {

		final class Result {
			public boolean	processed	= false;
		}

		final Result linkedResult = new Result();
		final Result unlinkedResult = new Result();

		MockDao<Facility> facilityDao = new MockDao<Facility>();

		IEdiService ediServiceLinked = new IEdiService() {

			public String getServiceName() {
				return "TEST";
			}

			public EdiServiceStateEnum getServiceStateEnum() {
				return EdiServiceStateEnum.LINKED;
			}

			public Boolean checkForCsvUpdates(ICsvImporter inCsvImporter) {
				linkedResult.processed = true;
				return true;
			}
		};

		IEdiService ediServiceUnlinked = new IEdiService() {

			public String getServiceName() {
				return "TEST";
			}

			public EdiServiceStateEnum getServiceStateEnum() {
				return EdiServiceStateEnum.UNLINKED;
			}

			public Boolean checkForCsvUpdates(ICsvImporter inCsvImporter) {
				unlinkedResult.processed = true;
				return true;
			}
		};

		Organization organization = new Organization();
		organization.setOrganizationId("O1");

		Facility facility = new Facility();
		facility.setParent(organization);
		facility.setFacilityId("F1");
		facility.addEdiService(ediServiceLinked);
		facility.addEdiService(ediServiceUnlinked);
		facilityDao.store(facility);

		ICsvImporter csvImporter = new ICsvImporter() {
			public void importOrdersFromCsvStream(InputStreamReader inCsvStreamReader, Facility inFacility) {
			}

			public void importInventoryFromCsvStream(InputStreamReader inCsvStreamReader, Facility inFacility) {
			}
		};

		IEdiProcessor ediProcessor = new EdiProcessor(csvImporter, facilityDao);
		BlockingQueue<String> testBlockingQueue = new ArrayBlockingQueue<>(100);
		ediProcessor.startProcessor(testBlockingQueue);

		try {
			// Sleep will switch us to the EdiProcessor thread.
			Thread.sleep(2000);
		} catch (InterruptedException e) {
		}

		Assert.assertTrue(linkedResult.processed);
		Assert.assertFalse(unlinkedResult.processed);

	}
}
