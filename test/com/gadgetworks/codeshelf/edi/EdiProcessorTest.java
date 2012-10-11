/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: EdiProcessorTest.java,v 1.2 2012/10/11 02:42:39 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.edi;

import java.io.InputStreamReader;

import junit.framework.Assert;

import org.junit.Test;

import com.gadgetworks.codeshelf.model.EdiServiceStateEnum;
import com.gadgetworks.codeshelf.model.dao.MockDao;
import com.gadgetworks.codeshelf.model.domain.DropboxService;
import com.gadgetworks.codeshelf.model.domain.Facility;
import com.gadgetworks.codeshelf.model.domain.IEdiService;

/**
 * @author jeffw
 *
 */
public class EdiProcessorTest {

	@Test
	public void ediProcessThreadTest() {

		MockDao<Facility> facilityDao = new MockDao<Facility>();
		IOrderImporter orderImporter = new IOrderImporter() {

			@Override
			public void importerFromCsvStream(InputStreamReader inStreamReader, Facility inFacility) {
				// TODO Auto-generated method stub
			}
		};

		IEdiProcessor ediProcessor = new EdiProcessor(orderImporter, facilityDao);
		ediProcessor.startProcessor();

		Thread foundThread = null;
		for (Thread thread : Thread.getAllStackTraces().keySet()) {
			if (thread.getName().equals(IEdiProcessor.EDIPROCESSOR_THREAD_NAME)) {
				foundThread = thread;
			}
		}

		Assert.assertNotNull(foundThread);

		ediProcessor.restartProcessor();

		foundThread = null;
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
			public boolean processed = false;
		}

		final Result linkedResult = new Result();
		final Result unlinkedResult = new Result();
		
		MockDao<Facility> facilityDao = new MockDao<Facility>();

		IEdiService ediServiceLinked = new IEdiService() {

			public EdiServiceStateEnum getServiceStateEnum() {
				return EdiServiceStateEnum.LINKED;
			}

			public void checkForOrderUpdates(IOrderImporter inOrderImporter) {
				linkedResult.processed = true;
			}
		};

		IEdiService ediServiceUnlinked = new IEdiService() {

			public EdiServiceStateEnum getServiceStateEnum() {
				return EdiServiceStateEnum.UNLINKED;
			}

			public void checkForOrderUpdates(IOrderImporter inOrderImporter) {
				unlinkedResult.processed = true;
			}
		};

		Facility facility = new Facility();
		facility.addEdiService(ediServiceLinked);
		facility.addEdiService(ediServiceUnlinked);
		facilityDao.store(facility);

		IOrderImporter orderImporter = new IOrderImporter() {
			public void importerFromCsvStream(InputStreamReader inStreamReader, Facility inFacility) {
			}
		};

		IEdiProcessor ediProcessor = new EdiProcessor(orderImporter, facilityDao);
		ediProcessor.startProcessor();
		
		try {
			// Sleep will switch us to the EdiProcessor thread.
			Thread.sleep(2000);
		} catch (InterruptedException e) {
		}

		Assert.assertTrue(linkedResult.processed);
		Assert.assertFalse(unlinkedResult.processed);

	}
}
