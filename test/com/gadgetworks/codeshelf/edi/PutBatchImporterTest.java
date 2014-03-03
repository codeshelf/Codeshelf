/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: OrderImporterTest.java,v 1.11 2013/04/11 07:42:45 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.edi;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;

import org.junit.Assert;
import org.junit.Test;

import com.gadgetworks.codeshelf.model.PositionTypeEnum;
import com.gadgetworks.codeshelf.model.domain.Facility;
import com.gadgetworks.codeshelf.model.domain.OrderHeader;
import com.gadgetworks.codeshelf.model.domain.Organization;

/**
 * @author jeffw
 * 
 */
public class PutBatchImporterTest extends EdiTestABC {

	@Test
	public final void testPutBatchImporterFromCsvStream() {

		String csvString = "orderGroupId,containerId,itemId,description,quantity,uom\r\n"
				+ ",C123,I123.1,Item 123.1 Desc,100,ea\r\n" //
				+ ",C123,I123.2,Item 123.2 Desc,200,ea\r\n" //
				+ ",C123,I123.3,Item 123.3 Desc,300,ea\r\n" //
				+ ",C123,I123.4,Item 123.4 Desc,400,ea\r\n";

		byte csvArray[] = csvString.getBytes();

		ByteArrayInputStream stream = new ByteArrayInputStream(csvArray);
		InputStreamReader reader = new InputStreamReader(stream);

		Organization organization = new Organization();
		organization.setDomainId("O-PUT1.1");
		mOrganizationDao.store(organization);

		organization.createFacility("F-PUT1.1", "TEST", PositionTypeEnum.METERS_FROM_PARENT.getName(), 0.0, 0.0);
		Facility facility = organization.getFacility("F-PUT1.1");

		ICsvPutBatchImporter importer = new PutBatchCsvImporter(mOrderGroupDao, mOrderHeaderDao, mOrderDetailDao, mContainerDao, mContainerUseDao, mItemMasterDao, mUomMasterDao);
		importer.importPutBatchesFromCsvStream(reader, facility);

		OrderHeader order = facility.findOrder("C123");
		Assert.assertNotNull(order);

	}
}
