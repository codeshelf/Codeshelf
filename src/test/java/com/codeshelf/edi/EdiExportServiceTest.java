package com.codeshelf.edi;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import com.codeshelf.model.domain.Facility;
import com.codeshelf.testframework.HibernateTest;

public class EdiExportServiceTest extends HibernateTest {
	
	@Test
	public void testGetsEdiExporterWhenFacilityHasEdiService() throws Exception {
		EdiExportService subject = startEdiExporter();
		beginTransaction();

		try {
			Facility facility = getFacility();
			Facility mockedFacility = Mockito.spy(facility);
			when(mockedFacility.getEdiExportTransport()).thenReturn(mock(IEdiExportGateway.class));
			
			Assert.assertNotNull(subject.getEdiExporter(mockedFacility));
			Assert.assertTrue(subject.getEdiExporter(mockedFacility).isRunning());
			
		}
		finally{
			subject.stopAsync().awaitTerminated(5, TimeUnit.SECONDS);
		}
		commitTransaction();
	}

	private EdiExportService startEdiExporter() throws TimeoutException {
		// TODO Auto-generated method stub
		EdiExportService service = new EdiExportService();
		service.startAsync().awaitRunning(5, TimeUnit.SECONDS);
		return service;
	}

}
