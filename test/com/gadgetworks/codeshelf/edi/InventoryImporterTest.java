/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: InventoryImporterTest.java,v 1.5 2012/10/31 09:23:59 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.edi;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;

import junit.framework.Assert;

import org.junit.Test;

import com.gadgetworks.codeshelf.model.dao.ITypedDao;
import com.gadgetworks.codeshelf.model.dao.MockDao;
import com.gadgetworks.codeshelf.model.domain.Container;
import com.gadgetworks.codeshelf.model.domain.Facility;
import com.gadgetworks.codeshelf.model.domain.Item;
import com.gadgetworks.codeshelf.model.domain.ItemMaster;
import com.gadgetworks.codeshelf.model.domain.OrderDetail;
import com.gadgetworks.codeshelf.model.domain.OrderGroup;
import com.gadgetworks.codeshelf.model.domain.OrderHeader;
import com.gadgetworks.codeshelf.model.domain.Organization;
import com.gadgetworks.codeshelf.model.domain.UomMaster;

/**
 * @author jeffw
 *
 */
public class InventoryImporterTest {

	@Test
	public void testInventoryImporterFromCsvStream() {

		String csvString = "itemId,description,quantity,uomId,locationId,lotId,inventoryDate\r\n" //
				+ "3001,Widget,100,each,B1,111,2012-09-26 11:31:01\r\n" //
				+ "4550,Gadget,450,case,B2,222,2012-09-26 11:31:01\r\n" //
				+ "3007,Dealybob,300,case,B3,333,2012-09-26 11:31:02\r\n" //
				+ "2150,Thingamajig,220,case,B4,444,2012-09-26 11:31:03\r\n" //
				+ "2170,Doodad,125,each,B5,555,2012-09-26 11:31:03";

		byte csvArray[] = csvString.getBytes();

		ByteArrayInputStream stream = new ByteArrayInputStream(csvArray);
		InputStreamReader reader = new InputStreamReader(stream);

		MockDao<Organization> organizationDao = new MockDao<Organization>();
		Organization organization = new Organization();
		organization.setDomainId("O1");

		Facility.DAO = new MockDao<Facility>();
		Facility facility = new Facility();
		facility.setParent(organization);
		facility.setDomainId("F1");

//		LocationABC.DAO = new MockDao<LocationABC>();

		MockDao<OrderGroup> orderGroupDao = new MockDao<OrderGroup>();
		MockDao<OrderHeader> orderHeaderDao = new MockDao<OrderHeader>();
		MockDao<OrderDetail> orderDetailDao = new MockDao<OrderDetail>();
		MockDao<Container> containerDao = new MockDao<Container>();
		MockDao<ItemMaster> itemMasterDao = new MockDao<ItemMaster>();
		MockDao<Item> itemDao = new MockDao<Item>();
		ITypedDao<UomMaster> uomMasterDao = new MockDao<UomMaster>();

		CsvImporter importer = new CsvImporter(orderGroupDao, orderHeaderDao, orderDetailDao, containerDao, itemMasterDao, itemDao, uomMasterDao);
		importer.importInventoryFromCsvStream(reader, facility);

		Item item = facility.getItem("3001");
		Assert.assertNotNull(item);

		ItemMaster itemMaster = item.getItemMaster();
		Assert.assertNotNull(itemMaster);

	}
}
