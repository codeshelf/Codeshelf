/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: DropboxServiceTest.java,v 1.6 2013/04/09 07:58:20 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.domain;

import static org.mockito.Matchers.any;

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
import com.gadgetworks.codeshelf.edi.ImportResult;
import com.gadgetworks.codeshelf.model.EdiServiceStateEnum;
import com.gadgetworks.codeshelf.model.dao.MockDao;

public class DropboxServiceTest {

	/*
	 * It's pretty easy to test and already connected DB account, but it's impossible to test the link process.
	 * Dropbox FORCES to use a manual procedure to connect our app to an account.
	 */

	private final static String	TEST_CREDENTIALS	= "aNGT-ls9rKAAAAAAAAAADu5mGXxFh8TqnVjt7zvIdTOt1H17h2UIky_FypTb7RcW";

	@Test
	public final void dropboxCheckTest() throws IOException {

		Organization.setDao(new MockDao<Organization>());
		Facility.setDao(new MockDao<Facility>());
		Aisle.setDao(new MockDao<Aisle>());
		Bay.setDao(new MockDao<Bay>());
		Vertex.setDao(new MockDao<Vertex>());
		DropboxService.setDao(new MockDao<DropboxService>());
		EdiDocumentLocator.setDao(new MockDao<EdiDocumentLocator>());

		Organization organization = new Organization();
		organization.setOrganizationId("O1");
		Facility facility = new Facility();
		organization.addFacility(facility);
		facility.setFacilityId("TEST1");

		facility.createDropboxService();

		DropboxService dropboxService = facility.getDropboxService();

		dropboxService.setParent(facility);
		dropboxService.setDomainId("DB");
		dropboxService.setProviderCredentials(TEST_CREDENTIALS);
		dropboxService.setServiceStateEnum(EdiServiceStateEnum.LINKED);

		ICsvOrderImporter orderImporter = Mockito.mock(ICsvOrderImporter.class);
		Mockito.when(
				orderImporter.importOrdersFromCsvStream(any(InputStreamReader.class), any(Facility.class), any(Timestamp.class)))
				.thenReturn(generateFailureResult());

		ICsvInventoryImporter inventoryImporter = new ICsvInventoryImporter() {

			@Override
			public boolean importSlottedInventoryFromCsvStream(InputStreamReader inCsvStreamReader,
				Facility inFacility,
				Timestamp inProcessTime) {
				return false;
			}

			@Override
			public boolean importDdcInventoryFromCsvStream(InputStreamReader inCsvStreamReader,
				Facility inFacility,
				Timestamp inProcessTime) {
				return false;
			}
		};

		ICsvLocationAliasImporter locationImporter = new ICsvLocationAliasImporter() {

			@Override
			public boolean importLocationAliasesFromCsvStream(InputStreamReader inCsvStreamReader,
				Facility inFacility,
				Timestamp inProcessTime) {
				return false;
			}
		};

		ICsvOrderLocationImporter orderLocationImporter = new ICsvOrderLocationImporter() {

			@Override
			public boolean importOrderLocationsFromCsvStream(InputStreamReader inCsvStreamReader,
				Facility inFacility,
				Timestamp inProcessTime) {
				return false;
			}
		};

		ICsvCrossBatchImporter crossBatchImporter = new ICsvCrossBatchImporter() {

			@Override
			public boolean importCrossBatchesFromCsvStream(InputStreamReader inCsvStreamReader,
				Facility inFacility,
				Timestamp inProcessTime) {
				return false;
			}
		};

		ICsvAislesFileImporter aislesFileImporter = new ICsvAislesFileImporter() {

			@Override
			public boolean importAislesFileFromCsvStream(InputStreamReader inCsvStreamReader,
				Facility inFacility,
				Timestamp inProcessTime) {
				return false;
			}
		};

		dropboxService.getUpdatesFromHost(orderImporter,
			orderLocationImporter,
			inventoryImporter,
			locationImporter,
			crossBatchImporter,
			aislesFileImporter);
	}

	private ImportResult generateFailureResult() {
		ImportResult result = new ImportResult();
		result.addFailure("failed line", new Exception("fail"));
		return result;
	}
}
