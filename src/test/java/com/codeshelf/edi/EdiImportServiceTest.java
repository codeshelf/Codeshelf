/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: EdiImportServiceTest.java,v 1.9 2013/07/22 04:30:36 jeffw Exp $
 *******************************************************************************/
package com.codeshelf.edi;

import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.io.Reader;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.metrics.DummyMetricsService;
import com.codeshelf.metrics.IMetricsService;
import com.codeshelf.metrics.MetricsService;
import com.codeshelf.model.EdiTransportType;
import com.codeshelf.model.domain.Facility;
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

		EdiImportService ediImportService = new EdiImportService(failingInstanceProvider,
			anyProvider,
			anyProvider,
			anyProvider,
			anyProvider,
			anyProvider);

		getFacility();

		IMetricsService metrics = new DummyMetricsService();
		MetricsService.setInstance(metrics);	// will be restored to normal values by framework

		this.ephemeralServices = new ArrayList<Service>();
		ephemeralServices.add(ediImportService);
		ephemeralServices.add(metrics);
		this.initializeEphemeralServiceManager();
	}

	@Test
	public final void facilityProcessingContinuesWithError() {
		@SuppressWarnings("rawtypes")
		Provider anyProvider = mock(Provider.class);

		@SuppressWarnings("unchecked")
		EdiImportService ediImportService = new EdiImportService(anyProvider,
			anyProvider,
			anyProvider,
			anyProvider,
			anyProvider,
			anyProvider);
		
		IEdiImportGateway failingService = mock(IEdiImportGateway.class);
		Mockito.when(failingService.getUpdatesFromHost(
			Mockito.any(ICsvOrderImporter.class),
			Mockito.any(ICsvOrderLocationImporter.class),
			Mockito.any(ICsvInventoryImporter.class),
			Mockito.any(ICsvLocationAliasImporter.class),
			Mockito.any(ICsvCrossBatchImporter.class),
			Mockito.any(ICsvAislesFileImporter.class))).thenThrow(new RuntimeException("any"));
		IEdiImportGateway goodService = mock(IEdiImportGateway.class);
		Facility facility = mock(Facility.class);
		Mockito.when(facility.getLinkedEdiImportGateways()).thenReturn(ImmutableList.of(failingService, goodService));
		ediImportService.doEdiForFacility(facility);
		verifyCalled(goodService);
		
	}
	
	private void verifyCalled(IEdiImportGateway service) {
		Mockito.verify(service, Mockito.atMost(1)).getUpdatesFromHost(
			Mockito.any(ICsvOrderImporter.class),
			Mockito.any(ICsvOrderLocationImporter.class),
			Mockito.any(ICsvInventoryImporter.class),
			Mockito.any(ICsvLocationAliasImporter.class),
			Mockito.any(ICsvCrossBatchImporter.class),
			Mockito.any(ICsvAislesFileImporter.class)
			);
		
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
			public void persistDataReceipt(Facility facility, String username, String filename, long receivedTime, EdiTransportType tranportType, BatchResult<?> result) {
				//do nothing
			}

			@Override
			public void setTruncatedGtins(boolean value) {
				// Stub. Don't need to implement
				
			}

		};
	}
}
