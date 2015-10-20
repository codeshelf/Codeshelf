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
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import com.codeshelf.generators.WorkInstructionGenerator;
import com.codeshelf.model.domain.Che;
import com.codeshelf.model.domain.FileExportReceipt;
import com.codeshelf.model.domain.ExportReceipt;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.OrderDetail;
import com.codeshelf.model.domain.OrderHeader;
import com.codeshelf.model.domain.SftpGateway;
import com.codeshelf.model.domain.SftpOrderGateway;
import com.codeshelf.model.domain.SftpWiGateway;
import com.codeshelf.testframework.HibernateTest;
import com.codeshelf.validation.BatchResult;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpException;


public class AbstractSftpEdiGatewayTest extends HibernateTest {
	
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
		SftpWiGateway sftpWIs = configureSftpService(facility, config, SftpWiGateway.class);
		Assert.assertTrue(sftpWIs.isLinked());

		config.setPassword("BAD");
		SftpWiGateway badSftpWIs = configureSftpService(facility, config, SftpWiGateway.class);
		Assert.assertTrue("Should have been unlinked", !badSftpWIs.isLinked());

		commitTransaction();
	}
	
	@Test
	public void testSftpWIs() throws EdiFileWriteException {
		beginTransaction();
		
		Facility facility = getFacility();//trigger creation
		Che che = getChe1();

		SftpConfiguration config = setupConfiguration();
		SftpWiGateway sftpWIs = configureSftpService(che.getFacility(), config, SftpWiGateway.class);
		

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
		SftpWiGateway subject = configureSftpService(getFacility(), config, SftpWiGateway.class);
		try {
			subject.uploadAsFile("CONTENTS", config.getExportPath() + "/wontgetthere");
			Assert.fail("Should have throw an EdiFileWriteException");
		} catch (EdiFileWriteException e) {
			
		}
		commitTransaction();
	}

	@Test
	public void testSftpImportOrder() throws IOException, JSchException, SftpException  {
		beginTransaction();
		SftpConfiguration config = setupConfiguration();
		SftpOrderGateway sftpOrders = configureSftpService(getFacility(), config, SftpOrderGateway.class);
		String filename = Long.toString(System.currentTimeMillis())+"a.DAT";
		String archiveFilename = testImportFile(sftpOrders, filename);// create a test file on the server to be processed
		sftpOrders.delete(archiveFilename);
		commitTransaction();
	}

	@Test
	public void testReprocessSameFile() throws IOException, JSchException, SftpException {
		beginTransaction();
		SftpConfiguration config = setupConfiguration();
		SftpOrderGateway sftpOrders = configureSftpService(getFacility(), config, SftpOrderGateway.class);
		String filename = Long.toString(System.currentTimeMillis())+"a.DAT";
		String archiveFilename =   testImportFile(sftpOrders, filename);
		String archiveFilename2 =  testImportFile(sftpOrders, filename);
		
		try {
			Assert.assertNotEquals(archiveFilename, archiveFilename2);
		}
		finally {
			//cleanup
			sftpOrders.delete(archiveFilename);
			sftpOrders.delete(archiveFilename2);
			
		}
		
		commitTransaction();
		
	}
	
	private String testImportFile(SftpOrderGateway sftpOrders, String baseFilename) throws IOException, JSchException, SftpException {
		String importFilename = sftpOrders.getConfiguration().getImportPath() + "/" + baseFilename;
		@SuppressWarnings("unused")
		ExportReceipt receipt = sftpOrders.uploadAsFile("order data 1", importFilename);

		ICsvOrderImporter mockImporter = mock(ICsvOrderImporter.class);
		@SuppressWarnings("unchecked")
		BatchResult<Object> mockBatchResult = mock(BatchResult.class);
		when(mockBatchResult.isSuccessful()).thenReturn(true);
		when(mockImporter.importOrdersFromCsvStream(any(Reader.class), any(Facility.class), any(Timestamp.class))).thenReturn(mockBatchResult);
		
		// now connect and process that file
		Map<String, String> processedOrderPaths = sftpOrders.processOrders(mockImporter);		
		
		// file was processed and result checked
		verify(mockBatchResult,times(1)).isSuccessful();
		return processedOrderPaths.get(baseFilename);
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
	private <T extends SftpGateway> T configureSftpService(Facility facility, SftpConfiguration config, Class<T> class1) {
		
		
		
		// ensure loads/saves configuration correctly
		SftpGateway sftpOrders = facility.findEdiGateway(class1); 
		sftpOrders.setConfiguration(config);
		sftpOrders.getDao().store(sftpOrders);
		sftpOrders = (SftpGateway) sftpOrders.getDao().findByDomainId(facility, sftpOrders.getDomainId());
		
		Assert.assertNotNull(sftpOrders);
		config = sftpOrders.getConfiguration();
		return (T) sftpOrders;
	}

}
