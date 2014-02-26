/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: EdiProcessorTest.java,v 1.9 2013/07/22 04:30:36 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.edi;

import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import org.junit.Assert;
import org.junit.Test;

import com.gadgetworks.codeshelf.model.EdiServiceStateEnum;
import com.gadgetworks.codeshelf.model.PositionTypeEnum;
import com.gadgetworks.codeshelf.model.dao.GenericDaoABC;
import com.gadgetworks.codeshelf.model.dao.ISchemaManager;
import com.gadgetworks.codeshelf.model.dao.ITypedDao;
import com.gadgetworks.codeshelf.model.domain.Facility;
import com.gadgetworks.codeshelf.model.domain.IEdiService;
import com.gadgetworks.codeshelf.model.domain.Organization;
import com.google.inject.Inject;

/**
 * @author jeffw
 *
 */
public class EdiProcessorTest extends EdiTestABC {

	@Test
	public final void ediProcessThreadTest() {

		ICsvOrderImporter orderImporter = new ICsvOrderImporter() {

			@Override
			public void importOrdersFromCsvStream(InputStreamReader inCsvStreamReader, Facility inFacility) {
			}
		};

		ICsvInventoryImporter inventoryImporter = new ICsvInventoryImporter() {

			@Override
			public void importSlottedInventoryFromCsvStream(InputStreamReader inCsvStreamReader, Facility inFacility) {
			}

			@Override
			public void importDdcInventoryFromCsvStream(InputStreamReader inCsvStreamReader, Facility inFacility) {
			}
		};

		ICsvLocationImporter locationImporter = new ICsvLocationImporter() {

			@Override
			public void importLocationAliasesFromCsvStream(InputStreamReader inCsvStreamReader, Facility inFacility) {
			}
		};

		IEdiProcessor ediProcessor = new EdiProcessor(orderImporter, inventoryImporter, locationImporter, Facility.DAO);
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

	public final class TestFacilityDao extends GenericDaoABC<Facility> implements ITypedDao<Facility> {
		private Facility	mFacility;

		@Inject
		public TestFacilityDao(final ISchemaManager inSchemaManager, final Facility inFacility) {
			super(inSchemaManager);
			mFacility = inFacility;
		}

		public final Class<Facility> getDaoClass() {
			return Facility.class;
		}

		public final List<Facility> getAll() {
			List<Facility> list = new ArrayList<Facility>();
			list.add(mFacility);
			return list;
		}
	}

	@Test
	public final void ediProcessorTest() {

		final class Result {
			public boolean	processed	= false;
		}

		ICsvOrderImporter orderImporter = new ICsvOrderImporter() {

			@Override
			public void importOrdersFromCsvStream(InputStreamReader inCsvStreamReader, Facility inFacility) {
			}
		};

		ICsvInventoryImporter inventoryImporter = new ICsvInventoryImporter() {

			@Override
			public void importSlottedInventoryFromCsvStream(InputStreamReader inCsvStreamReader, Facility inFacility) {
			}

			@Override
			public void importDdcInventoryFromCsvStream(InputStreamReader inCsvStreamReader, Facility inFacility) {
			}
		};

		ICsvLocationImporter locationImporter = new ICsvLocationImporter() {

			@Override
			public void importLocationAliasesFromCsvStream(InputStreamReader inCsvStreamReader, Facility inFacility) {
			}
		};

		final Result linkedResult = new Result();
		final Result unlinkedResult = new Result();

		Organization organization = new Organization();
		organization.setDomainId("O-EDI.1");
		mOrganizationDao.store(organization);

		organization.createFacility("F-EDI.1", "TEST", PositionTypeEnum.METERS_FROM_PARENT.getName(), 0.0, 0.0);
		Facility facility = organization.getFacility("F-EDI.1");
		facility.setParent(organization);

		TestFacilityDao facilityDao = new TestFacilityDao(mSchemaManager, facility);
		facilityDao.store(facility);

		IEdiService ediServiceLinked = new IEdiService() {

			public EdiServiceStateEnum getServiceStateEnum() {
				return EdiServiceStateEnum.LINKED;
			}

			public String getServiceName() {
				return "LINKED";
			}

			public Boolean checkForCsvUpdates(ICsvOrderImporter inCsvOrdersImporter,
				ICsvInventoryImporter inCsvInventoryImporter,
				ICsvLocationImporter inCsvLocationsImporter) {
				linkedResult.processed = true;
				return true;
			}
		};

		IEdiService ediServiceUnlinked = new IEdiService() {

			public EdiServiceStateEnum getServiceStateEnum() {
				return EdiServiceStateEnum.UNLINKED;
			}

			public String getServiceName() {
				return "UNLINKED";
			}

			public Boolean checkForCsvUpdates(ICsvOrderImporter inCsvOrdersImporter,
				ICsvInventoryImporter inCsvInventoryImporter,
				ICsvLocationImporter inCsvLocationsImporter) {
				unlinkedResult.processed = true;
				return true;
			}
		};

		facility.addEdiService(ediServiceUnlinked);
		facility.addEdiService(ediServiceLinked);

		IEdiProcessor ediProcessor = new EdiProcessor(orderImporter, inventoryImporter, locationImporter, facilityDao);
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
