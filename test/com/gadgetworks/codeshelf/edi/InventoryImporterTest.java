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
import com.gadgetworks.codeshelf.model.domain.Aisle;
import com.gadgetworks.codeshelf.model.domain.Bay;
import com.gadgetworks.codeshelf.model.domain.Facility;
import com.gadgetworks.codeshelf.model.domain.Item;
import com.gadgetworks.codeshelf.model.domain.ItemMaster;
import com.gadgetworks.codeshelf.model.domain.Organization;
import com.gadgetworks.codeshelf.model.domain.Point;

/**
 * @author jeffw
 *
 */
public class InventoryImporterTest extends EdiTestABC {

	@Test
	public final void testInventoryImporterFromCsvStream() {

		String csvString = "itemId,itemDetailId,description,quantity,uom,locationId,lotId,inventoryDate\r\n" //
				+ "3001,3001,Widget,100,each,A1.B1,111,2012-09-26 11:31:01\r\n" //
				+ "4550,4550,Gadget,450,case,A1.B2,222,2012-09-26 11:31:01\r\n" //
				+ "3007,3007,Dealybob,300,case,A1.B3,333,2012-09-26 11:31:02\r\n" //
				+ "2150,2150,Thingamajig,220,case,A1.B4,444,2012-09-26 11:31:03\r\n" //
				+ "2170,2170,Doodad,125,each,A1.B5,555,2012-09-26 11:31:03";

		byte[] csvArray = csvString.getBytes();

		ByteArrayInputStream stream = new ByteArrayInputStream(csvArray);
		InputStreamReader reader = new InputStreamReader(stream);

		Organization organization = new Organization();
		organization.setDomainId("O-INV1.1");
		mOrganizationDao.store(organization);

		organization.createFacility("F-INV1.1", "TEST", PositionTypeEnum.METERS_FROM_PARENT.getName(), 0.0, 0.0);
		Facility facility = organization.getFacility("F-INV1.1");

		ICsvInventoryImporter importer = new InventoryCsvImporter(mItemMasterDao, mItemDao, mUomMasterDao);
		importer.importSlottedInventoryFromCsvStream(reader, facility);

		Item item = facility.getStoredItem("3001");
		Assert.assertNotNull(item);

		ItemMaster itemMaster = item.getParent();
		Assert.assertNotNull(itemMaster);

	}

	// --------------------------------------------------------------------------
	/**
	 * Created when we discovered that multiple inventory items in the same facility failed to import due to key collisions.
	 */
	@Test
	public final void testMultipleItemInstancesInventoryImporterFromCsvStream() {

		String csvString = "itemId,itemDetailId,description,quantity,uom,locationId,lotId,inventoryDate\r\n" //
				+ "3001,3001,Widget,100,each,A1.B1,111,2012-09-26 11:31:01\r\n" //
				+ "3001,3001,Widget,100,each,A1.B2,111,2012-09-26 11:31:01\r\n";

		byte[] csvArray = csvString.getBytes();

		ByteArrayInputStream stream = new ByteArrayInputStream(csvArray);
		InputStreamReader reader = new InputStreamReader(stream);

		Organization organization = new Organization();
		organization.setDomainId("O-INV2.1");
		mOrganizationDao.store(organization);

		organization.createFacility("F-INV2.1", "TEST", PositionTypeEnum.METERS_FROM_PARENT.getName(), 0.0, 0.0);
		Facility facility = organization.getFacility("F-INV2.1");
		mFacilityDao.store(facility);

		Aisle aisleA1 = new Aisle(facility, "A1", new Point(PositionTypeEnum.GPS, 0.0, 0.0, 0.0), new Point(PositionTypeEnum.GPS,
			0.0,
			0.0,
			0.0));
		mSubLocationDao.store(aisleA1);

		Bay bay1 = new Bay(aisleA1, "B1", new Point(PositionTypeEnum.GPS, 0.0, 0.0, 0.0), new Point(PositionTypeEnum.GPS,
			0.0,
			0.0,
			0.0));
		mSubLocationDao.store(bay1);

		Bay bay2 = new Bay(aisleA1, "B2", new Point(PositionTypeEnum.GPS, 0.0, 0.0, 0.0), new Point(PositionTypeEnum.GPS,
			0.0,
			0.0,
			0.0));
		mSubLocationDao.store(bay2);

		ICsvInventoryImporter importer = new InventoryCsvImporter(mItemMasterDao, mItemDao, mUomMasterDao);
		importer.importSlottedInventoryFromCsvStream(reader, facility);

		bay1 = (Bay) facility.findSubLocationById("A1.B1");
		bay2 = (Bay) facility.findSubLocationById("A1.B2");

		Item item = bay1.getStoredItem("3001");
		Assert.assertNotNull(item);
		Assert.assertEquals(100.0, item.getQuantity().doubleValue(), 0.0);

		item = bay2.getStoredItem("3001");
		Assert.assertNotNull(item);
		Assert.assertEquals(100.0, item.getQuantity().doubleValue(), 0.0);

		ItemMaster itemMaster = item.getParent();
		Assert.assertNotNull(itemMaster);

		// Run the import again - it should not trip up on the same items at the same place(s) - it should instead update them.

		csvString = "itemId,itemDetailId,description,quantity,uom,locationId,lotId,inventoryDate\r\n" //
				+ "3001,3001,Widget,200,each,A1.B1,111,2012-09-26 11:31:01\r\n" //
				+ "3001,3001,Widget,200,each,A1.B2,111,2012-09-26 11:31:01\r\n";

		csvArray = csvString.getBytes();

		stream = new ByteArrayInputStream(csvArray);
		reader = new InputStreamReader(stream);
		importer = new InventoryCsvImporter(mItemMasterDao, mItemDao, mUomMasterDao);
		importer.importSlottedInventoryFromCsvStream(reader, facility);

		bay1 = (Bay) facility.findSubLocationById("A1.B1");
		bay2 = (Bay) facility.findSubLocationById("A1.B2");

		item = bay1.getStoredItem("3001");
		Assert.assertNotNull(item);
		Assert.assertEquals(200.0, item.getQuantity().doubleValue(), 0.0);

		itemMaster = item.getParent();
		Assert.assertNotNull(itemMaster);

	}
}
