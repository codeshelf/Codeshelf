/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: DropboxServiceTest.java,v 1.6 2013/04/09 07:58:20 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.domain;

import java.io.InputStreamReader;
import java.sql.Timestamp;

import org.junit.Assert;
import org.junit.Test;

import com.gadgetworks.codeshelf.edi.ICsvCrossBatchImporter;
import com.gadgetworks.codeshelf.edi.ICsvInventoryImporter;
import com.gadgetworks.codeshelf.edi.ICsvLocationAliasImporter;
import com.gadgetworks.codeshelf.edi.ICsvOrderImporter;
import com.gadgetworks.codeshelf.edi.ICsvOrderLocationImporter;
import com.gadgetworks.codeshelf.model.EdiServiceStateEnum;
import com.gadgetworks.codeshelf.model.dao.MockDao;
import com.gadgetworks.codeshelf.model.dao.Result;

public class DropboxServiceTest {

	/*
	 * It's pretty easy to test and already connected DB account, but it's impossible to test the link process.
	 * Dropbox FORCES to use a manual procedure to connect our app to an account.
	 */

	private final static String	TEST_CREDENTIALS	= "_j2uNuj7o0AAAAAAAAAAAQSaC0B6__GxvCqr-GMHyr7V97ci8Qqb80KThe-qdvOB";

	@Test
	public final void dropboxCheckTest() {

		Organization.DAO = new MockDao<Organization>();
		Facility.DAO = new MockDao<Facility>();
		Aisle.DAO = new MockDao<Aisle>();
		Bay.DAO = new MockDao<Bay>();
		Vertex.DAO = new MockDao<Vertex>();
		DropboxService.DAO = new MockDao<DropboxService>();
		EdiDocumentLocator.DAO = new MockDao<EdiDocumentLocator>();

		Organization organization = new Organization();
		organization.setOrganizationId("O1");
		Facility facility = new Facility();
		facility.setParent(organization);
		facility.setFacilityId("TEST1");

		facility.createDropboxService();

		DropboxService dropboxService = facility.getDropboxService();

		dropboxService.setParent(facility);
		dropboxService.setDomainId("DB");
		dropboxService.setProviderCredentials(TEST_CREDENTIALS);
		dropboxService.setServiceStateEnum(EdiServiceStateEnum.LINKED);

		final Result checkImportOrders = new Result();

		ICsvOrderImporter orderImporter = new ICsvOrderImporter() {

			@Override
			public void importOrdersFromCsvStream(InputStreamReader inCsvStreamReader, Facility inFacility, Timestamp inProcessTime) {
				checkImportOrders.result = true;
			}
		};

		ICsvInventoryImporter inventoryImporter = new ICsvInventoryImporter() {

			@Override
			public void importSlottedInventoryFromCsvStream(InputStreamReader inCsvStreamReader, Facility inFacility, Timestamp inProcessTime) {
			}

			@Override
			public void importDdcInventoryFromCsvStream(InputStreamReader inCsvStreamReader, Facility inFacility, Timestamp inProcessTime) {
			}
		};

		ICsvLocationAliasImporter locationImporter = new ICsvLocationAliasImporter() {

			@Override
			public void importLocationAliasesFromCsvStream(InputStreamReader inCsvStreamReader, Facility inFacility, Timestamp inProcessTime) {
			}
		};

		ICsvOrderLocationImporter orderLocationImporter = new ICsvOrderLocationImporter() {

			@Override
			public void importOrderLocationsFromCsvStream(InputStreamReader inCsvStreamReader, Facility inFacility, Timestamp inProcessTime) {
			}
		};

		ICsvCrossBatchImporter crossBatchImporter = new ICsvCrossBatchImporter() {

			@Override
			public void importCrossBatchesFromCsvStream(InputStreamReader inCsvStreamReader, Facility inFacility, Timestamp inProcessTime) {
			}
		};

		dropboxService.getUpdatesFromHost(orderImporter,
			orderLocationImporter,
			inventoryImporter,
			locationImporter,
			crossBatchImporter);

		Assert.assertTrue(checkImportOrders.result);
	}
}
