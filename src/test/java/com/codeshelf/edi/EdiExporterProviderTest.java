package com.codeshelf.edi;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.Assert;
import org.junit.Test;

import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.SftpWIsEdiService;
import com.codeshelf.testframework.HibernateTest;

public class EdiExporterProviderTest extends HibernateTest {
	
	//@Test
	public void testGetsEdiExporterWhenFacilityHasEdiService() throws Exception {
		EdiExporterProvider subject = startEdiExporter();
		beginTransaction();

		try {
			//Facility facility = mock(Facility.class);
			Facility facility = getFacility();
			//facility.getedi
			//SftpWIsEdiService ediService = new SftpWIsEdiService("SFTPWIS");
			//ediService.setParent(facility);
			//SftpWIsEdiService.staticGetDao().store(ediService);
			//when(facility.getEdiExportTransport()).thenReturn(mock(EdiExportTransport.class));
			
			Assert.assertNotNull(subject.getEdiExporter(facility));
			Assert.assertTrue(subject.getEdiExporter(facility).isRunning());
			
		}
		finally{
			subject.stopAsync().awaitTerminated(5, TimeUnit.SECONDS);
		}
		commitTransaction();
	}

	private EdiExporterProvider startEdiExporter() throws TimeoutException {
		// TODO Auto-generated method stub
		EdiExporterProvider service = new EdiExporterProvider();
		service.startAsync().awaitRunning(5, TimeUnit.SECONDS);
		return service;
	}

}
