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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.application.JvmProperties;
import com.codeshelf.metrics.DummyMetricsService;
import com.codeshelf.metrics.MetricsService;
import com.codeshelf.model.EdiServiceStateEnum;
import com.codeshelf.model.dao.GenericDaoABC;
import com.codeshelf.model.dao.ITypedDao;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.IDomainObject;
import com.codeshelf.model.domain.IEdiService;
import com.codeshelf.platform.persistence.ITenantPersistenceService;
import com.codeshelf.platform.persistence.TenantPersistenceService;
import com.codeshelf.validation.BatchResult;
import com.google.common.util.concurrent.Service;
import com.google.common.util.concurrent.ServiceManager;

/**
 * @author jeffw
 *
 */
public class EdiProcessorTest /* extends EdiTestABC */{
	private final Logger LOGGER = LoggerFactory.getLogger(EdiProcessorTest.class);
	static {
		JvmProperties.load("test");
	}

	public final class TestFacilityDao extends GenericDaoABC<Facility> implements ITypedDao<Facility> {
		List<Facility> list = new ArrayList<Facility>(1);

		public TestFacilityDao(final Facility facility) {
			super();
			list.add(facility);
		}

		public final Class<Facility> getDaoClass() {
			return Facility.class;
		}

		public final List<Facility> getAll() {
			return list;
		}
	}

	@Test
	public final void ediProcessThreadTest() {
		LOGGER.info("starting ediProcessThreadTest");

		ICsvOrderImporter orderImporter = generateFailingImporter();
		ICsvInventoryImporter inventoryImporter = mock(ICsvInventoryImporter.class);
		ICsvLocationAliasImporter locationImporter = mock(ICsvLocationAliasImporter.class);
		ICsvOrderLocationImporter orderLocationImporter = mock(ICsvOrderLocationImporter.class);
		ICsvCrossBatchImporter crossBatchImporter = mock(ICsvCrossBatchImporter.class);
		ICsvAislesFileImporter aislesFileImporter = mock(ICsvAislesFileImporter.class);

		EdiProcessorService ediProcessorService = new EdiProcessorService(orderImporter,
			inventoryImporter,
			locationImporter,
			orderLocationImporter,
			crossBatchImporter,
			aislesFileImporter);
		BlockingQueue<String> testBlockingQueue = new ArrayBlockingQueue<>(100);
		ediProcessorService.setEdiSignalQueue(testBlockingQueue);
		
		ArrayList<Service> services = new ArrayList<Service>();
		services.add(ediProcessorService);
		ServiceManager serviceManager = new ServiceManager(services);

		try {
			serviceManager.startAsync().awaitHealthy(10,TimeUnit.SECONDS);
		} catch (TimeoutException e) {
			Assert.fail(e.getMessage());
		} 

		try {
			serviceManager.stopAsync().awaitStopped(10,TimeUnit.SECONDS);
		} catch (TimeoutException e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public final void ediProcessorTest() {
		LOGGER.info("starting ediProcessorTest");

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

		Facility facility = new Facility();
		facility.setDomainId("FTEST");

		Facility.DAO = new TestFacilityDao(facility);

		MetricsService.setInstance(new DummyMetricsService());
		ITenantPersistenceService mock = mock(ITenantPersistenceService.class);
		TenantPersistenceService.setInstance(mock);

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

		EdiProcessorService ediProcessorService = new EdiProcessorService(orderImporter,
			inventoryImporter,
			locationImporter,
			orderLocationImporter,
			crossBatchImporter,
			aislesFileImporter);

		ArrayList<Service> services = new ArrayList<Service>();
		services.add(ediProcessorService);
		if(!MetricsService.getMaybeRunningInstance().isRunning()) { // TODO: this sort of setup goes in an abstract class
			services.add(MetricsService.getMaybeRunningInstance());
		}
		ServiceManager serviceManager = new ServiceManager(services);
		try {
			serviceManager.startAsync().awaitHealthy(30, TimeUnit.SECONDS);
		} catch (TimeoutException e) {
			Assert.fail(e.getMessage());
		}

		try {
			// Sleep will switch us to the EdiProcessor thread.
			Thread.sleep(2000);
		} catch (InterruptedException e) {
		}

		Assert.assertTrue(linkedResult.processed);
		Assert.assertFalse(unlinkedResult.processed);
		
		try {
			serviceManager.stopAsync().awaitStopped(30, TimeUnit.SECONDS);
		} catch (TimeoutException e) {
			Assert.fail(e.getMessage());
		}
	}

	private ICsvOrderImporter generateFailingImporter() {
		return new ICsvOrderImporter() {
			@Override
			public int toInteger(final String inString) {
				return 0;
			}

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
