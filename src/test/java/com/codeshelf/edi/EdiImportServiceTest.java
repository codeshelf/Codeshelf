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
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.metrics.DummyMetricsService;
import com.codeshelf.metrics.IMetricsService;
import com.codeshelf.metrics.MetricsService;
import com.codeshelf.model.dao.ITypedDao;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.IDomainObject;
import com.codeshelf.security.CodeshelfSecurityManager;
import com.codeshelf.testframework.MockDaoTest;
import com.codeshelf.validation.BatchResult;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.Service;
import com.google.inject.Provider;

/**
 * @author jeffw
 *
 */
public class EdiImportServiceTest extends MockDaoTest {
	private final Logger LOGGER = LoggerFactory.getLogger(EdiImportServiceTest.class);
	private List<Service>	ephemeralServices;


	@Override
	public boolean ephemeralServicesShouldStartAutomatically() {
		return false;
	}

	@Override
	protected List<Service> generateEphemeralServices() {
		return this.ephemeralServices;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Test
	public final void ediProcessThreadTest() {
		LOGGER.info("starting ediProcessThreadTest");
		//TODO consider resolving the below warnings
		Provider failingInstanceProvider = mock(Provider.class);
		Mockito.when(failingInstanceProvider.get()).thenReturn(generateFailingImporter());
		Provider anyProvider = mock(Provider.class);

		EdiImportService ediProcessorService = new EdiImportService(failingInstanceProvider,
			anyProvider,
			anyProvider,
			anyProvider,
			anyProvider,
			anyProvider);
		BlockingQueue<String> testBlockingQueue = new ArrayBlockingQueue<>(100);
		ediProcessorService.setEdiSignalQueue(testBlockingQueue);

		getFacility();

		IMetricsService metrics = new DummyMetricsService();
		MetricsService.setInstance(metrics);	// will be restored to normal values by framework

		this.ephemeralServices = new ArrayList<Service>();
		ephemeralServices.add(ediProcessorService);
		ephemeralServices.add(metrics);
		this.initializeEphemeralServiceManager();
	}

	@Test
	public final void facilityProcessingContinuesWithError() {
		@SuppressWarnings("rawtypes")
		Provider anyProvider = mock(Provider.class);

		@SuppressWarnings("unchecked")
		EdiImportService ediProcessorService = new EdiImportService(anyProvider,
			anyProvider,
			anyProvider,
			anyProvider,
			anyProvider,
			anyProvider);
		
		IEdiGateway failingService = mock(IEdiGateway.class);
		Mockito.when(failingService.getUpdatesFromHost(
			Mockito.any(ICsvOrderImporter.class),
			Mockito.any(ICsvOrderLocationImporter.class),
			Mockito.any(ICsvInventoryImporter.class),
			Mockito.any(ICsvLocationAliasImporter.class),
			Mockito.any(ICsvCrossBatchImporter.class),
			Mockito.any(ICsvAislesFileImporter.class))).thenThrow(new RuntimeException("any"));
		IEdiGateway goodService = mock(IEdiGateway.class);
		Facility facility = mock(Facility.class);
		Mockito.when(facility.getLinkedEdiImportServices()).thenReturn(ImmutableList.of(failingService, goodService));
		ediProcessorService.doEdiForFacility(facility);
		verifyCalled(goodService);
		
	}
	
	private void verifyCalled(IEdiGateway service) {
		Mockito.verify(service, Mockito.atMost(1)).getUpdatesFromHost(
			Mockito.any(ICsvOrderImporter.class),
			Mockito.any(ICsvOrderLocationImporter.class),
			Mockito.any(ICsvInventoryImporter.class),
			Mockito.any(ICsvLocationAliasImporter.class),
			Mockito.any(ICsvCrossBatchImporter.class),
			Mockito.any(ICsvAislesFileImporter.class)
			);
		
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
	public final void ediProcessorTest() {
		LOGGER.info("starting ediProcessorTest");

		final class Result {
			public boolean	processed	= false;
		}
		final Result linkedResult = new Result();
		final Result unlinkedResult = new Result();

		getFacility();

		IEdiGateway ediServiceLinked = new IEdiGateway() {

			public boolean isLinked() {
				return true;
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

			@Override
			public Long getLastSuccessTime() {
				// TODO Auto-generated method stub
				return 0L;
			}

			@Override
			public boolean testConnection() {
				// TODO Auto-generated method stub
				return false;
			}
		};

		IEdiGateway ediServiceUnlinked = new IEdiGateway() {

			public boolean isLinked() {
				return false;
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

			@Override
			public Long getLastSuccessTime() {
				// TODO Auto-generated method stub
				return 0L;
			}

			@Override
			public boolean testConnection() {
				// TODO Auto-generated method stub
				return false;
			}
		};

		Facility facility = getFacility();
		facility.addEdiService(ediServiceUnlinked);
		facility.addEdiService(ediServiceLinked);

		Provider anyProvider = mock(Provider.class);
		EdiImportService ediProcessorService = new EdiImportService(anyProvider,
			anyProvider,
			anyProvider,
			anyProvider,
			anyProvider,
			anyProvider);

		IMetricsService metrics = new DummyMetricsService();
		MetricsService.setInstance(metrics);	// will be restored to normal values by framework

		this.ephemeralServices = new ArrayList<Service>();
		ephemeralServices.add(ediProcessorService);
		ephemeralServices.add(metrics);
		CodeshelfSecurityManager.removeContext();

		this.initializeEphemeralServiceManager();

		try {
			// Sleep will switch us to the EdiProcessor thread.
			Thread.sleep(2000);
		} catch (InterruptedException e) {
		}

		Assert.assertTrue(linkedResult.processed);
		Assert.assertFalse(unlinkedResult.processed);

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
			
			@Override
			public void persistDataReceipt(Facility facility, String username, String filename, long receivedTime, BatchResult<?> result) {
				//do nothing
			}

			@Override
			public void setTruncatedGtins(boolean value) {
				// Stub. Don't need to implement
				
			}

		};
	}
}
