/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: InventoryImporterTest.java,v 1.10 2013/04/11 07:42:45 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.edi;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;

import junit.framework.Assert;

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

		String csvString = "itemId,description,quantity,uom,locationId,lotId,inventoryDate\r\n" //
				+ "3001,Widget,100,each,B1,111,2012-09-26 11:31:01\r\n" //
				+ "4550,Gadget,450,case,B2,222,2012-09-26 11:31:01\r\n" //
				+ "3007,Dealybob,300,case,B3,333,2012-09-26 11:31:02\r\n" //
				+ "2150,Thingamajig,220,case,B4,444,2012-09-26 11:31:03\r\n" //
				+ "2170,Doodad,125,each,B5,555,2012-09-26 11:31:03";

		byte csvArray[] = csvString.getBytes();

		ByteArrayInputStream stream = new ByteArrayInputStream(csvArray);
		InputStreamReader reader = new InputStreamReader(stream);

		Organization organization = new Organization();
		organization.setDomainId("O-INV1.1");
		mOrganizationDao.store(organization);

		organization.createFacility("F-INV1.1", "TEST", PositionTypeEnum.METERS_FROM_PARENT.getName(), 0.0, 0.0);
		Facility facility = organization.getFacility("F1.1");

		CsvImporter importer = new CsvImporter(mOrderGroupDao, mOrderHeaderDao, mOrderDetailDao, mContainerDao, mContainerUseDao, mItemMasterDao, mItemDao, mUomMasterDao);
		importer.importSlottedInventoryFromCsvStream(reader, facility);

		Item item = facility.getItem("3001");
		Assert.assertNotNull(item);

		ItemMaster itemMaster = item.getItemMaster();
		Assert.assertNotNull(itemMaster);

	}
}
