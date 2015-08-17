package com.codeshelf.edi;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.Charset;
import java.sql.Timestamp;

import org.junit.Assert;
import org.junit.Test;

import com.codeshelf.model.domain.AbstractSftpEdiService;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.SftpOrdersEdiService;
import com.codeshelf.testframework.HibernateTest;
import com.codeshelf.validation.BatchResult;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.SftpException;

public class SftpTest extends HibernateTest {
	
	private static final String	SFTP_TEST_HOST	= "sftp.codeshelf.com";
	private static final String	SFTP_TEST_USERNAME	= "test";
	private static final String	SFTP_TEST_PASSWORD	= "m80isrq411";

	@Test
	public void testSftpOrders() throws IOException {
		SftpConfiguration config = new SftpConfiguration();
		config.setHost(SFTP_TEST_HOST);
		config.setUsername(SFTP_TEST_USERNAME);
		config.setPassword(SFTP_TEST_PASSWORD);
		config.setImportPath("/out");
		config.setArchivePath("/out/archive");
		
		AbstractSftpEdiService sftpOrders = new SftpOrdersEdiService();
		sftpOrders.setParent(getFacility());
		String domainid = sftpOrders.getServiceName();
		sftpOrders.setConfiguration(config);
		sftpOrders.setDomainId(domainid);
		
		// ensure loads/saves configuration correctly
		beginTransaction();
		SftpOrdersEdiService.staticGetDao().store(sftpOrders);
		commitTransaction();
		
		beginTransaction();
		sftpOrders = SftpOrdersEdiService.staticGetDao().findByDomainId(getFacility(), domainid);
		commitTransaction();
		
		Assert.assertNotNull(sftpOrders);
		config = sftpOrders.getConfiguration();
		Assert.assertEquals(SFTP_TEST_USERNAME, config.getUsername());
		Assert.assertEquals(SFTP_TEST_PASSWORD, config.getPassword());
		
		// create a test file on the server to be processed
		String filename1 = config.getImportPath()+"/"+Long.toString(System.currentTimeMillis())+"a.DAT";
		uploadTestFile(sftpOrders,filename1,"order data 1");
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
	}

	private void uploadTestFile(AbstractSftpEdiService sftpService, String filename, String contents) {
		ChannelSftp sftp = sftpService.connect();
		try {
			byte[] bytes = contents.getBytes(Charset.forName("ISO-8859-1"));
			sftp.put(new ByteArrayInputStream(bytes), filename, ChannelSftp.OVERWRITE);
		} catch (SftpException e) {
			Assert.fail(e.getMessage());
		} finally {
			sftpService.disconnect();
		}
	}

}
