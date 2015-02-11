/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: DropboxServiceTest.java,v 1.6 2013/04/09 07:58:20 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.domain;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Timestamp;

import org.junit.Test;
import org.mockito.Mockito;

import com.gadgetworks.codeshelf.edi.ICsvAislesFileImporter;
import com.gadgetworks.codeshelf.edi.ICsvCrossBatchImporter;
import com.gadgetworks.codeshelf.edi.ICsvInventoryImporter;
import com.gadgetworks.codeshelf.edi.ICsvLocationAliasImporter;
import com.gadgetworks.codeshelf.edi.ICsvOrderImporter;
import com.gadgetworks.codeshelf.edi.ICsvOrderLocationImporter;
import com.gadgetworks.codeshelf.model.EdiServiceStateEnum;
import com.gadgetworks.codeshelf.model.dao.MockDao;
import com.gadgetworks.codeshelf.validation.BatchResult;

public class DropboxServiceTest {

	/*
	 * It's pretty easy to test and already connected DB account, but it's impossible to test the link process.
	 * Dropbox FORCES to use a manual procedure to connect our app to an account.
	 */

	private final static String	TEST_CREDENTIALS	= "aNGT-ls9rKAAAAAAAAAADu5mGXxFh8TqnVjt7zvIdTOt1H17h2UIky_FypTb7RcW";

	@Test
	public final void dropboxCheckTest() throws IOException {

		Facility.setDao(new MockDao<Facility>());
		Aisle.setDao(new MockDao<Aisle>());
		Bay.setDao(new MockDao<Bay>());
		Vertex.setDao(new MockDao<Vertex>());
		DropboxService.setDao(new MockDao<DropboxService>());
		EdiDocumentLocator.setDao(new MockDao<EdiDocumentLocator>());

		Facility facility = new Facility();
		facility.setFacilityId("TEST1");

		facility.createDropboxService();

		DropboxService dropboxService = facility.getDropboxService();

		dropboxService.setParent(facility);
		dropboxService.setDomainId("DB");
		dropboxService.setProviderCredentials(TEST_CREDENTIALS);
		dropboxService.setServiceState(EdiServiceStateEnum.LINKED);

		ICsvOrderImporter orderImporter = mock(ICsvOrderImporter.class);
		Mockito.when(
				orderImporter.importOrdersFromCsvStream(any(InputStreamReader.class), any(Facility.class), any(Timestamp.class)))
				.thenReturn(generateFailureResult());

		ICsvInventoryImporter inventoryImporter = mock(ICsvInventoryImporter.class);
		ICsvLocationAliasImporter locationImporter = mock(ICsvLocationAliasImporter.class);
		ICsvOrderLocationImporter orderLocationImporter = mock(ICsvOrderLocationImporter.class);
		ICsvCrossBatchImporter crossBatchImporter = mock(ICsvCrossBatchImporter.class);
		ICsvAislesFileImporter aislesFileImporter = mock(ICsvAislesFileImporter.class);
		
		dropboxService.getUpdatesFromHost(orderImporter,
			orderLocationImporter,
			inventoryImporter,
			locationImporter,
			crossBatchImporter,
			aislesFileImporter);
	}

	private BatchResult<Object> generateFailureResult() {
		BatchResult<Object> result = new BatchResult<Object>();
		result.addViolation("bad", "bad", "msg");
		return result;
	}
}
