/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: EdiProcessorTest.java,v 1.9 2013/07/22 04:30:36 jeffw Exp $
 *******************************************************************************/
package com.codeshelf.edi;

import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.io.Reader;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import org.junit.Assert;
import org.junit.Test;

import com.codeshelf.model.EdiServiceStateEnum;
import com.codeshelf.model.dao.GenericDaoABC;
import com.codeshelf.model.dao.ITypedDao;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.IDomainObject;
import com.codeshelf.model.domain.IEdiService;
import com.codeshelf.model.domain.Point;
import com.codeshelf.platform.persistence.TenantPersistenceService;
import com.codeshelf.validation.BatchResult;
import com.google.inject.Inject;

/**
 * @author jeffw
 *
 */
public class EdiProcessorTest extends EdiTestABC {

	@Test
	public final void ediProcessThreadTest() {

		ICsvOrderImporter orderImporter = 	generateFailingImporter();
		ICsvInventoryImporter inventoryImporter = mock(ICsvInventoryImporter.class);
		ICsvLocationAliasImporter locationImporter = mock(ICsvLocationAliasImporter.class);
		ICsvOrderLocationImporter orderLocationImporter = mock(ICsvOrderLocationImporter.class);
		ICsvCrossBatchImporter crossBatchImporter = mock(ICsvCrossBatchImporter.class);
		ICsvAislesFileImporter aislesFileImporter = mock(ICsvAislesFileImporter.class);

		IEdiProcessor ediProcessor = new EdiProcessor(orderImporter,
			inventoryImporter,
			locationImporter,
			orderLocationImporter,
			crossBatchImporter,
			aislesFileImporter,
			Facility.DAO,
			this.getTenantPersistenceService());
		BlockingQueue<String> testBlockingQueue = new ArrayBlockingQueue<>(100);
		ediProcessor.startProcessor(testBlockingQueue);

		Thread foundThread=findEdiThread();
		
		Assert.assertFalse(foundThread == null);

		for(int t=0;t<3;t++) {
			ediProcessor.stopProcessor();
			// That thread might be sleeping.
			if (foundThread != null) {
				foundThread.interrupt();
			}
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
			} // wait a moment for EDI processor thread to stop
			if(findEdiThread()==null) {
				break;
			}
		}

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
		public TestFacilityDao(final TenantPersistenceService tenantPersistenceService, final Facility inFacility) {
			super(tenantPersistenceService);
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
		this.getTenantPersistenceService().beginTenantTransaction();

		final class Result {
			public boolean	processed	= false;
		}

		ICsvOrderImporter orderImporter = generateFailingImporter();
		ICsvInventoryImporter inventoryImporter = mock(ICsvInventoryImporter.class);
		ICsvLocationAliasImporter locationImporter = mock(ICsvLocationAliasImporter.class);
		ICsvOrderLocationImporter orderLocationImporter = mock(ICsvOrderLocationImporter.class);
		ICsvCrossBatchImporter crossBatchImporter = mock(ICsvCrossBatchImporter.class);
		ICsvAislesFileImporter aislesFileImporter = mock(ICsvAislesFileImporter.class);

		final Result linkedResult = new Result();
		final Result unlinkedResult = new Result();

		Facility facility = Facility.createFacility(getDefaultTenant(),"F-EDI.1", "TEST", Point.getZeroPoint());

		TestFacilityDao facilityDao = new TestFacilityDao(this.getTenantPersistenceService(), facility);
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
			public void sendWorkInstructionsToHost(String inWiList) {
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
			public long getVersion() {
				// TODO Auto-generated method stub
				return 0;
			}

			@Override
			public void setVersion(long inVersion) {
				// TODO Auto-generated method stub
			}

			@Override
			public <T extends IDomainObject> ITypedDao<T> getDao() {
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
			public void sendWorkInstructionsToHost(String exportMessage) {
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
			public long getVersion() {
				// TODO Auto-generated method stub
				return 0;
			}

			@Override
			public void setVersion(long inVersion) {
				// TODO Auto-generated method stub
			}

			@Override
			public <T extends IDomainObject> ITypedDao<T> getDao() {
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
			facilityDao,
			this.getTenantPersistenceService());
		BlockingQueue<String> testBlockingQueue = new ArrayBlockingQueue<>(100);
		ediProcessor.startProcessor(testBlockingQueue);

		try {
			// Sleep will switch us to the EdiProcessor thread.
			Thread.sleep(2000);
		} catch (InterruptedException e) {
		}

		Assert.assertTrue(linkedResult.processed);
		Assert.assertFalse(unlinkedResult.processed);

		this.getTenantPersistenceService().commitTenantTransaction();
	}
	
	private ICsvOrderImporter generateFailingImporter() {
		return new ICsvOrderImporter() {

			@Override
			public BatchResult<Object> importOrdersFromCsvStream(Reader inCsvStreamReader,
				Facility inFacility,
				Timestamp inProcessTime) throws IOException {
				BatchResult<Object> result = new BatchResult<Object>();
				result.addViolation("bad", "bad", "msg");
				return result;
			}
		};
	}
}
