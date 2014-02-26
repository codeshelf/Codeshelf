/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: InventoryImporterTest.java,v 1.12 2013/07/22 04:30:36 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.edi;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;

import org.junit.Assert;
import org.junit.Test;

import com.gadgetworks.codeshelf.model.PositionTypeEnum;
import com.gadgetworks.codeshelf.model.domain.Facility;
import com.gadgetworks.codeshelf.model.domain.Item;
import com.gadgetworks.codeshelf.model.domain.ItemMaster;
import com.gadgetworks.codeshelf.model.domain.Organization;

/**
 * @author jeffw
 *
 */
public class InventoryImporterTest extends EdiTestABC {

	@Test
	public final void testInventoryImporterFromCsvStream() {

		String csvString = "itemId,itemDetailId,description,quantity,uom,locationId,lotId,inventoryDate\r\n" //
				+ "3001,3001,Widget,100,each,B1,111,2012-09-26 11:31:01\r\n" //
				+ "4550,4550,Gadget,450,case,B2,222,2012-09-26 11:31:01\r\n" //
				+ "3007,3007,Dealybob,300,case,B3,333,2012-09-26 11:31:02\r\n" //
				+ "2150,2150,Thingamajig,220,case,B4,444,2012-09-26 11:31:03\r\n" //
				+ "2170,2170,Doodad,125,each,B5,555,2012-09-26 11:31:03";

		byte[] csvArray = csvString.getBytes();

		ByteArrayInputStream stream = new ByteArrayInputStream(csvArray);
		InputStreamReader reader = new InputStreamReader(stream);

		Organization organization = new Organization();
		organization.setDomainId("O-INV1.1");
		mOrganizationDao.store(organization);

		organization.createFacility("F-INV1.1", "TEST", PositionTypeEnum.METERS_FROM_PARENT.getName(), 0.0, 0.0);
		Facility facility = organization.getFacility("F-INV1.1");

		ICsvInventoryImporter importer = new CsvInventoryImporter(mItemMasterDao, mItemDao, mUomMasterDao);
		importer.importSlottedInventoryFromCsvStream(reader, facility);

		Item item = facility.getItem("3001");
		Assert.assertNotNull(item);

		ItemMaster itemMaster = item.getParent();
		Assert.assertNotNull(itemMaster);

	}
}
