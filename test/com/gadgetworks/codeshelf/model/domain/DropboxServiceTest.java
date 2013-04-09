/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: DropboxServiceTest.java,v 1.6 2013/04/09 07:58:20 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.domain;

import java.io.InputStreamReader;

import junit.framework.Assert;

import org.junit.Test;

import com.gadgetworks.codeshelf.edi.ICsvImporter;
import com.gadgetworks.codeshelf.model.EdiServiceStateEnum;
import com.gadgetworks.codeshelf.model.dao.MockDao;
import com.gadgetworks.codeshelf.model.dao.Result;

public class DropboxServiceTest {
	
	/*
	 * It's pretty easy to test and already connected DB account, but it's impossible to test the link process.
	 * Dropbox FORCES to use a manual procedure to connect our app to an account.
	 */

	private final static String	TEST_CREDENTIALS	= "{\"appToken\":{\"key\":\"feh3ontnajdmmin\",\"secret\":\"4jm05vbugwnq9pe\"},\"accessToken\":{\"key\":\"rfoew5gfe5fchjr\",\"secret\":\"7ur0gokvvu0dtlo\"}}";

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

		final Result checkImport = new Result();

		dropboxService.checkForCsvUpdates(new ICsvImporter() {
			public void importOrdersFromCsvStream(InputStreamReader inStreamReader, Facility inFacility) {
				checkImport.result = true;
			}
			public void importSlottedInventoryFromCsvStream(InputStreamReader inCsvStreamReader, Facility inFacility) {
			}
			public void importDdcInventoryFromCsvStream(InputStreamReader inCsvStreamReader, Facility inFacility) {
				
			}
		});
		
		Assert.assertTrue(checkImport.result);
	}
}
