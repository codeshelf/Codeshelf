/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: EdiProcessorTest.java,v 1.9 2013/07/22 04:30:36 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.edi;

import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import org.junit.Assert;
import org.junit.Test;

import com.gadgetworks.codeshelf.model.EdiServiceStateEnum;
import com.gadgetworks.codeshelf.model.dao.GenericDaoABC;
import com.gadgetworks.codeshelf.model.dao.ITypedDao;
import com.gadgetworks.codeshelf.model.domain.Facility;
import com.gadgetworks.codeshelf.model.domain.IDomainObject;
import com.gadgetworks.codeshelf.model.domain.IEdiService;
import com.gadgetworks.codeshelf.model.domain.Organization;
import com.gadgetworks.codeshelf.model.domain.Point;
import com.gadgetworks.codeshelf.model.domain.WorkInstruction;
import com.gadgetworks.codeshelf.platform.persistence.PersistenceService;
import com.google.inject.Inject;

/**
 * @author jeffw
 *
 */
public class EdiProcessorTest extends EdiTestABC {

	//PersistenceService persistenceService = new PersistenceService();
	
	@Test
	public final void ediProcessThreadTest() {

		ICsvOrderImporter orderImporter = 	generateFailingImporter();
		ICsvInventoryImporter inventoryImporter = new ICsvInventoryImporter() {

			@Override
			public boolean importSlottedInventoryFromCsvStream(InputStreamReader inCsvStreamReader,
				Facility inFacility,
				Timestamp inProcessTime) {
				return false;
			}

			@Override
			public boolean importDdcInventoryFromCsvStream(InputStreamReader inCsvStreamReader,
				Facility inFacility,
				Timestamp inProcessTime) {
				return false;
			}
		};

		ICsvLocationAliasImporter locationImporter = new ICsvLocationAliasImporter() {

			@Override
			public boolean importLocationAliasesFromCsvStream(InputStreamReader inCsvStreamReader,
				Facility inFacility,
				Timestamp inProcessTime) {
				return false;
			}
		};

		ICsvOrderLocationImporter orderLocationImporter = new ICsvOrderLocationImporter() {

			@Override
			public boolean importOrderLocationsFromCsvStream(InputStreamReader inCsvStreamReader,
				Facility inFacility,
				Timestamp inProcessTime) {
				return false;
			}
		};

		ICsvCrossBatchImporter crossBatchImporter = new ICsvCrossBatchImporter() {

			@Override
			public boolean importCrossBatchesFromCsvStream(InputStreamReader inCsvStreamReader,
				Facility inFacility,
				Timestamp inProcessTime) {
				return false;
			}
		};
		
		ICsvAislesFileImporter aislesFileImporter = new ICsvAislesFileImporter() {

			@Override
			public boolean importAislesFileFromCsvStream(InputStreamReader inCsvStreamReader,
				Facility inFacility,
				Timestamp inProcessTime) {
				return false;
			}
		};


		IEdiProcessor ediProcessor = new EdiProcessor(orderImporter,
			inventoryImporter,
			locationImporter,
			orderLocationImporter,
			crossBatchImporter,
			aislesFileImporter,
			Facility.DAO);
		BlockingQueue<String> testBlockingQueue = new ArrayBlockingQueue<>(100);
		ediProcessor.startProcessor(testBlockingQueue);

		Thread foundThread=findEdiThread();
		
		Assert.assertFalse(foundThread == null);

		ediProcessor.stopProcessor();
		// That thread might be sleeping.
		if (foundThread != null) {
			foundThread.interrupt();
		}
		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {
		} // wait a moment for EDI processor thread to stop

		Assert.assertNull(findEdiThread());
	}
	
	private final Thread findEdiThread() {
		Thread foundThread = null;
		for (Thread thread : Thread.getAllStackTraces().keySet()) {
			if (thread.getName().equals(IEdiProcessor.EDIPROCESSOR_THREAD_NAME)) {
				foundThread = thread;
			}
		}
		return foundThread;
	}

	public final class TestFacilityDao extends GenericDaoABC<Facility> implements ITypedDao<Facility> {
		private Facility	mFacility;

		@Inject
		public TestFacilityDao(final PersistenceService persistenceService, final Facility inFacility) {
			super(persistenceService);
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
		this.getPersistenceService().beginTenantTransaction();

		final class Result {
			public boolean	processed	= false;
		}

		ICsvOrderImporter orderImporter = generateFailingImporter();
		
		ICsvInventoryImporter inventoryImporter = new ICsvInventoryImporter() {

			@Override
			public boolean importSlottedInventoryFromCsvStream(InputStreamReader inCsvStreamReader,
				Facility inFacility,
				Timestamp inProcessTime) {
				return false;
			}

			@Override
			public boolean importDdcInventoryFromCsvStream(InputStreamReader inCsvStreamReader,
				Facility inFacility,
				Timestamp inProcessTime) {
				return false;
			}
		};

		ICsvLocationAliasImporter locationImporter = new ICsvLocationAliasImporter() {

			@Override
			public boolean importLocationAliasesFromCsvStream(InputStreamReader inCsvStreamReader,
				Facility inFacility,
				Timestamp inProcessTime) {
				return false;
			}
		};

		ICsvOrderLocationImporter orderLocationImporter = new ICsvOrderLocationImporter() {

			@Override
			public boolean importOrderLocationsFromCsvStream(InputStreamReader inCsvStreamReader,
				Facility inFacility,
				Timestamp inProcessTime) {
				return false;
			}
		};

		ICsvCrossBatchImporter crossBatchImporter = new ICsvCrossBatchImporter() {

			@Override
			public boolean importCrossBatchesFromCsvStream(InputStreamReader inCsvStreamReader,
				Facility inFacility,
				Timestamp inProcessTime) {
				return false;
			}
		};

		ICsvAislesFileImporter aislesFileImporter = new ICsvAislesFileImporter() {

			@Override
			public boolean importAislesFileFromCsvStream(InputStreamReader inCsvStreamReader,
				Facility inFacility,
				Timestamp inProcessTime) {
				return false;
			}
		};

		final Result linkedResult = new Result();
		final Result unlinkedResult = new Result();

		Organization organization = new Organization();
		organization.setDomainId("O-EDI.1");
		mOrganizationDao.store(organization);

		organization.createFacility("F-EDI.1", "TEST", Point.getZeroPoint());
		Facility facility = organization.getFacility("F-EDI.1");
		organization.addFacility(facility);

		TestFacilityDao facilityDao = new TestFacilityDao(persistenceService, facility);
		facilityDao.store(facility);

		IEdiService ediServiceLinked = new IEdiService() {

			public EdiServiceStateEnum getServiceState() {
				return EdiServiceStateEnum.LINKED;
			}

			public String getServiceName() {
				return "LINKED";
			}

			public boolean getUpdatesFromHost(ICsvOrderImporter inCsvOrdersImporter,
				ICsvOrderLocationImporter inCsvOrderLocationImporter,
				ICsvInventoryImporter inCsvInventoryImporter,
				ICsvLocationAliasImporter inCsvLocationsImporter,
				ICsvCrossBatchImporter inCsvCrossBatchImporter,
				ICsvAislesFileImporter inCsvAislesFileImporter) {
				linkedResult.processed = true;
				return true;
			}

			@Override
			public void sendWorkInstructionsToHost(List<WorkInstruction> inWiList) {
				// TODO Auto-generated method stub

			}

			@Override
			public Facility getParent() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public void setParent(Facility inParent) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public UUID getParentPersistentId() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public String getParentFullDomainId() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public String getFullDomainId() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public String getDefaultDomainIdPrefix() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public String getDomainId() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public void setDomainId(String inId) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public String getClassName() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public UUID getPersistentId() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public void setPersistentId(UUID inPersistentId) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public Timestamp getVersion() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public void setVersion(Timestamp inVersion) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public <T extends IDomainObject> ITypedDao<T> getDao() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public Object getFieldValueByName(String inFieldName) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public void setFieldValueByName(String inFieldName, Object inFieldValue) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public Organization getOrganization() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public Facility getFacility() {
				// TODO Auto-generated method stub
				return null;
			}
		};

		IEdiService ediServiceUnlinked = new IEdiService() {

			public EdiServiceStateEnum getServiceState() {
				return EdiServiceStateEnum.UNLINKED;
			}

			public String getServiceName() {
				return "UNLINKED";
			}

			public boolean getUpdatesFromHost(ICsvOrderImporter inCsvOrdersImporter,
				ICsvOrderLocationImporter iCsvOrderLocationImporter,
				ICsvInventoryImporter inCsvInventoryImporter,
				ICsvLocationAliasImporter inCsvLocationsImporter,
				ICsvCrossBatchImporter inCsvCrossBatchImporter,
				ICsvAislesFileImporter inCsvAislesFileImporter) {
				unlinkedResult.processed = true;
				return true;
			}

			@Override
			public void sendWorkInstructionsToHost(List<WorkInstruction> inWiList) {
				// TODO Auto-generated method stub

			}

			@Override
			public Facility getParent() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public void setParent(Facility inParent) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public UUID getParentPersistentId() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public String getParentFullDomainId() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public String getFullDomainId() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public String getDefaultDomainIdPrefix() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public String getDomainId() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public void setDomainId(String inId) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public String getClassName() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public UUID getPersistentId() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public void setPersistentId(UUID inPersistentId) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public Timestamp getVersion() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public void setVersion(Timestamp inVersion) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public <T extends IDomainObject> ITypedDao<T> getDao() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public Object getFieldValueByName(String inFieldName) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public void setFieldValueByName(String inFieldName, Object inFieldValue) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public Organization getOrganization() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public Facility getFacility() {
				// TODO Auto-generated method stub
				return null;
			}
		};

		facility.addEdiService(ediServiceUnlinked);
		facility.addEdiService(ediServiceLinked);

		IEdiProcessor ediProcessor = new EdiProcessor(orderImporter,
			inventoryImporter,
			locationImporter,
			orderLocationImporter,
			crossBatchImporter,
			aislesFileImporter,
			facilityDao);
		BlockingQueue<String> testBlockingQueue = new ArrayBlockingQueue<>(100);
		ediProcessor.startProcessor(testBlockingQueue);

		try {
			// Sleep will switch us to the EdiProcessor thread.
			Thread.sleep(2000);
		} catch (InterruptedException e) {
		}

		Assert.assertTrue(linkedResult.processed);
		Assert.assertFalse(unlinkedResult.processed);

		this.getPersistenceService().endTenantTransaction();
	}
	
	private ICsvOrderImporter generateFailingImporter() {
		return new ICsvOrderImporter() {

			@Override
			public ImportResult importOrdersFromCsvStream(InputStreamReader inCsvStreamReader,
				Facility inFacility,
				Timestamp inProcessTime) throws IOException {
				ImportResult result = new ImportResult();
				result.addFailure("failed line", new Exception("fail"));
				return result;
			}
		};
	}
}
