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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.junit.Assert;
import org.junit.Test;

import com.codeshelf.generators.WorkInstructionGenerator;
import com.codeshelf.model.domain.AbstractSftpEdiService;
import com.codeshelf.model.domain.Che;
import com.codeshelf.model.domain.EdiServiceABC.ExportReceipt;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.OrderHeader;
import com.codeshelf.model.domain.SftpOrdersEdiService;
import com.codeshelf.model.domain.SftpWIsEdiService;
import com.codeshelf.model.domain.WorkInstruction;
import com.codeshelf.testframework.HibernateTest;
import com.codeshelf.validation.BatchResult;
import com.google.common.base.Strings;
import com.jcraft.jsch.SftpException;


public class SftpTest extends HibernateTest {
	
	private static final String	SFTP_TEST_HOST	= "sftp.codeshelf.com";
	private static final String	SFTP_TEST_USERNAME	= "test";
	private static final String	SFTP_TEST_PASSWORD	= "m80isrq411";

	@Test
	public void testSftpWIs() throws InterruptedException, ExecutionException, TimeoutException, SftpException {
		beginTransaction();
		
		getFacility();//trigger creation
		Che che = getChe1();

		SftpConfiguration config = setupConfiguration();
		SftpWIsEdiService sftpWIs = configureSftpService(che.getFacility(), config, SftpWIsEdiService.class);
		List<WorkInstruction> completeWIs = setupComputedWorkInstructionsForOrder(che, 2);
		commitTransaction();

		beginTransaction();
		OrderHeader completeOrder = completeWIs.get(0).getOrder();
		for (WorkInstruction workInstruction : completeWIs) {
			sftpWIs.notifyWiComplete(workInstruction);
		}
		ExportReceipt receipt = sftpWIs.notifyOrderCompleteOnCart(completeOrder, che);
		commitTransaction();
		try {
		
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		sftpWIs.downloadFile(receipt.getPath(), out);
		assertMatches(completeOrder, completeWIs, che, new String(out.toByteArray(), StandardCharsets.ISO_8859_1));

		} finally {
			sftpWIs.delete(receipt.getPath());
			
		}
	}
	
	/**
	 * Very broad matcher
	 */
	private void assertMatches(OrderHeader completeOrder, List<WorkInstruction> wis, Che che, String message) {
		Assert.assertNotNull(Strings.emptyToNull(completeOrder.getOrderId()));
		Assert.assertTrue("Message did not contain order id: " + completeOrder.getOrderId(), message.contains(completeOrder.getOrderId()));

		Assert.assertNotNull(Strings.emptyToNull(che.getDomainId()));
		Assert.assertTrue("Message did not contain che id: " + che.getDomainId(), message.contains(che.getDomainId()));
		
		for (WorkInstruction workInstruction : wis) {
			Assert.assertTrue("Message did not contain item id: " + workInstruction.getItemId(), message.contains(workInstruction.getItemId()));
			Assert.assertTrue("Message did not contain item id: " + workInstruction.getActualQuantity(), message.contains(String.valueOf(workInstruction.getActualQuantity())));
			
		}
	}




	private List<WorkInstruction> setupComputedWorkInstructionsForOrder(Che che, int numWIs) {
		WorkInstructionGenerator generator = new WorkInstructionGenerator();
		ArrayList<WorkInstruction> wis = new ArrayList<>();
		for (int i = 0; i < numWIs; i++) {
			WorkInstruction wi = generator.generateValid(che.getFacility());
			wis.add(wi);
		}
		return wis;
	}


	@Test
	public void testSftpOrders() throws IOException, SftpException, ExecutionException, TimeoutException, InterruptedException {
		beginTransaction();
		SftpConfiguration config = setupConfiguration();
		SftpOrdersEdiService sftpOrders = configureSftpService(getFacility(), config, SftpOrdersEdiService.class);
		// create a test file on the server to be processed
		
		String filename = Long.toString(System.currentTimeMillis())+"a.DAT";
		String importFilename = config.getImportPath()+"/"+ filename;
		String archiveFilename = config.getArchivePath()+"/"+ filename;
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
		Assert.assertEquals(SFTP_TEST_USERNAME, config.getUsername());
		Assert.assertEquals(SFTP_TEST_PASSWORD, config.getPassword());
		return (T) sftpOrders;
	}

}
