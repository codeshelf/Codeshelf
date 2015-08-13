package com.codeshelf.edi;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import com.codeshelf.model.domain.SftpOrdersEdiService;
import com.codeshelf.testframework.HibernateTest;

public class SftpTest extends HibernateTest {
	
	
	private static final String	SFTP_TEST_HOST	= "sftp.codeshelf.com";
	private static final String	SFTP_TEST_USERNAME	= "test";
	private static final String	SFTP_TEST_PASSWORD	= "m80isrq411";

	@Test
	public void testSetupAndConnect() {
		SftpConfiguration config = new SftpConfiguration();
		config.setHost(SFTP_TEST_HOST);
		config.setUsername(SFTP_TEST_USERNAME);
		config.setPassword(SFTP_TEST_PASSWORD);
		config.setImportPath("/out");
		config.setArchivePath("/out/archive");
		
		SftpOrdersEdiService sftpOrders = new SftpOrdersEdiService();
		sftpOrders.setParent(getFacility());
		String domainid = sftpOrders.getServiceName();
		sftpOrders.setConfiguration(config);
		sftpOrders.setDomainId(domainid);
		
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
		
		ICsvOrderImporter mockImporter = Mockito.mock(ICsvOrderImporter.class);
		
		// try to connect
		sftpOrders.getUpdatesFromHost(mockImporter ,null,null,null,null, null);
		
		// TODO: finish this test to ensure ability to connect to remote SFTP and get file list
	}

}
