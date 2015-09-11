package com.codeshelf.edi;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;

import org.junit.Assert;
import org.junit.Test;

import com.codeshelf.generators.WorkInstructionGenerator;
import com.codeshelf.model.domain.AbstractSftpEdiService;
import com.codeshelf.model.domain.Che;
import com.codeshelf.model.domain.FileExportReceipt;
import com.codeshelf.model.domain.ExportReceipt;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.OrderDetail;
import com.codeshelf.model.domain.OrderHeader;
import com.codeshelf.model.domain.SftpOrdersEdiService;
import com.codeshelf.model.domain.SftpWIsEdiService;
import com.codeshelf.testframework.HibernateTest;
import com.codeshelf.validation.BatchResult;


public class AbstractSftpEdiServiceTest extends HibernateTest {
	
	private static final String	SFTP_TEST_HOST	= "sftp.codeshelf.com";
	private static final String	SFTP_TEST_USERNAME	= "test";
	private static final String	SFTP_TEST_PASSWORD	= "m80isrq411";
	private WorkInstructionGenerator	wiGenerator	= new WorkInstructionGenerator();
	private InventoryGenerator	inventoryGenerator = new InventoryGenerator(null);

	@Test
	public void testBadCredentialsRemainUnlinked() {
		beginTransaction();
		
		Facility facility = getFacility();//trigger creation
		SftpConfiguration config = setupConfiguration();
		SftpWIsEdiService sftpWIs = configureSftpService(facility, config, SftpWIsEdiService.class);
		Assert.assertTrue(sftpWIs.isLinked());

		config.setPassword("BAD");
		SftpWIsEdiService badSftpWIs = configureSftpService(facility, config, SftpWIsEdiService.class);
		Assert.assertTrue("Should have been unlinked", !badSftpWIs.isLinked());

		commitTransaction();
	}
	
	@Test
	public void testSftpWIs() throws EdiFileWriteException {
		beginTransaction();
		
		Facility facility = getFacility();//trigger creation
		Che che = getChe1();

		SftpConfiguration config = setupConfiguration();
		SftpWIsEdiService sftpWIs = configureSftpService(che.getFacility(), config, SftpWIsEdiService.class);
		

		OrderHeader orderHeader = generateOrder(facility, 2);
		commitTransaction();

		beginTransaction();

		String expectedContents = "ExportCompleteMessage";
		FileExportReceipt receipt = sftpWIs.transportOrderOnCartFinished(orderHeader.getDomainId(), che.getDeviceGuidStr(), "ExportCompleteMessage");
		commitTransaction();

		try {
		
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			sftpWIs.downloadFile(receipt.getPath(), out);
			String fileContents = new String(out.toByteArray(), StandardCharsets.ISO_8859_1);
			Assert.assertEquals(expectedContents, fileContents);

		} finally {
			sftpWIs.delete(receipt.getPath());
			
		}
	}
	
	@Test
	public void testBadConnectionThrowsCheckedException() {
		beginTransaction();
		SftpConfiguration config = setupConfiguration();
		config.setPort(999);
		SftpWIsEdiService subject = configureSftpService(getFacility(), config, SftpWIsEdiService.class);
		try {
			subject.uploadAsFile("CONTENTS", config.getExportPath() + "/wontgetthere");
			Assert.fail("Should have throw an EdiFileWriteException");
		} catch (EdiFileWriteException e) {
			
		}
		commitTransaction();
	}

	@Test
	public void testSftpOrders() throws IOException  {
		beginTransaction();
		SftpConfiguration config = setupConfiguration();
		SftpOrdersEdiService sftpOrders = configureSftpService(getFacility(), config, SftpOrdersEdiService.class);
		// create a test file on the server to be processed
		
		String filename = Long.toString(System.currentTimeMillis())+"a.DAT";
		String importFilename = config.getImportPath()+"/"+ filename;
		String archiveFilename = config.getArchivePath()+"/"+ filename;
		@SuppressWarnings("unused")
		ExportReceipt receipt = sftpOrders.uploadAsFile("order data 1", importFilename);
		try {
		//String filename2 = config.getImportPath()+"/"+Long.toString(System.currentTimeMillis())+"b.DAT";
		//uploadTestFile(sftpOrders,filename2,"order data 2");

		ICsvOrderImporter mockImporter = mock(ICsvOrderImporter.class);
		@SuppressWarnings("unchecked")
		BatchResult<Object> mockBatchResult = mock(BatchResult.class);
		when(mockBatchResult.isSuccessful()).thenReturn(true);
		when(mockImporter.importOrdersFromCsvStream(any(Reader.class), any(Facility.class), any(Timestamp.class))).thenReturn(mockBatchResult);

		// now connect and process that file
		sftpOrders.getUpdatesFromHost(mockImporter ,null,null,null,null,null);		
		
		// file was processed and result checked
		verify(mockBatchResult,times(1)).isSuccessful();
		} finally {
			sftpOrders.delete(archiveFilename);
		}
		commitTransaction();
	}


	private OrderHeader generateOrder(Facility facility, int numDetails) {
		OrderHeader orderHeader = this.wiGenerator.generateValidOrderHeader(facility);
		for (int i = 0; i < numDetails; i++) {
			OrderDetail od = this.wiGenerator.generateValidOrderDetail(orderHeader, this.inventoryGenerator.generateItem(facility));
			orderHeader.addOrderDetail(od);
		}
		return orderHeader;
	}


	
	private SftpConfiguration setupConfiguration() {
		SftpConfiguration config = new SftpConfiguration();
		config.setHost(SFTP_TEST_HOST);
		config.setUsername(SFTP_TEST_USERNAME);
		config.setPassword(SFTP_TEST_PASSWORD);
		config.setExportPath("/automated_tests/in");
		config.setImportPath("/automated_tests/out");
		config.setArchivePath("/automated_tests/out/archive");
		return config;
	}
	
	@SuppressWarnings("unchecked")
	private <T extends AbstractSftpEdiService> T configureSftpService(Facility facility, SftpConfiguration config, Class<T> class1) {
		
		
		
		// ensure loads/saves configuration correctly
		AbstractSftpEdiService sftpOrders = facility.findEdiService(class1); 
		sftpOrders.setConfiguration(config);
		sftpOrders.getDao().store(sftpOrders);
		sftpOrders = (AbstractSftpEdiService) sftpOrders.getDao().findByDomainId(facility, sftpOrders.getDomainId());
		
		Assert.assertNotNull(sftpOrders);
		config = sftpOrders.getConfiguration();
		return (T) sftpOrders;
	}

}
