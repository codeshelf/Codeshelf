/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: OrderImporterTest.java,v 1.3 2012/10/22 07:38:07 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.edi;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;

import junit.framework.Assert;

import org.junit.Test;

import com.gadgetworks.codeshelf.model.PickStrategyEnum;
import com.gadgetworks.codeshelf.model.dao.MockDao;
import com.gadgetworks.codeshelf.model.domain.Container;
import com.gadgetworks.codeshelf.model.domain.Facility;
import com.gadgetworks.codeshelf.model.domain.Item;
import com.gadgetworks.codeshelf.model.domain.ItemMaster;
import com.gadgetworks.codeshelf.model.domain.OrderDetail;
import com.gadgetworks.codeshelf.model.domain.OrderGroup;
import com.gadgetworks.codeshelf.model.domain.OrderHeader;

/**
 * @author jeffw
 *
 */
public class OrderImporterTest {

	@Test
	public void testOrderImporterFromCsvStream() {

		String csvString = "orderGroupId,orderId,orderDetailId,itemId,description,quantity,uomId,orderDate\r\n" //
				+ "1,123,1,3001,Widget,100,each,2012-09-26 11:31:01\r\n" //
				+ "1,123,2,4550,Gadget,450,case,2012-09-26 11:31:01\r\n" //
				+ "1,123,3,3007,Dealybob,300,case,2012-09-26 11:31:02\r\n" //
				+ "1,123,4,2150,Thingamajig,220,case,2012-09-26 11:31:03\r\n" //
				+ "1,456,1,3001,Widget,230,each,2012-09-26 11:31:01\r\n" //
				+ "1,456,2,4550,Gadget,70,case,2012-09-26 11:31:01\r\n" //
				+ "1,456,3,3007,Dealybob,90,case,2012-09-26 11:31:02\r\n" //
				+ "1,456,4,2150,Thingamajig,140,case,2012-09-26 11:31:03\r\n" //
				+ "1,456,5,2170,Doodad,125,each,2012-09-26 11:31:03\r\n" //
				+ "1,789,1,2150,Thingamajig,125,each,2012-09-26 11:31:03\r\n" //
				+ "1,789,2,2170,Doodad,125,each,2012-09-26 11:31:03";

		byte csvArray[] = csvString.getBytes();

		ByteArrayInputStream stream = new ByteArrayInputStream(csvArray);
		InputStreamReader reader = new InputStreamReader(stream);

		MockDao<Facility> facilityDao = new MockDao<Facility>();
		Facility facility = new Facility();

		MockDao<OrderGroup> orderGroupDao = new MockDao<OrderGroup>();
		MockDao<OrderHeader> orderHeaderDao = new MockDao<OrderHeader>();
		MockDao<OrderDetail> orderDetailDao = new MockDao<OrderDetail>();
		MockDao<Container> containerDao = new MockDao<Container>();
		MockDao<ItemMaster> itemMasterDao = new MockDao<ItemMaster>();
		MockDao<Item> itemDao = new MockDao<Item>();

		CsvImporter importer = new CsvImporter(orderGroupDao, orderHeaderDao, orderDetailDao, containerDao, itemMasterDao, itemDao);
		importer.importOrdersFromCsvStream(reader, facility);

		OrderHeader order = facility.findOrder("123");
		Assert.assertNotNull(order);

	}

	@Test
	public void testOrderImporterWithPickStrategyFromCsvStream() {

		String csvString = "orderGroupId,pickStrategy,orderId,orderDetailId,itemId,description,quantity,uomId,orderDate\r\n" //
				+ "1,,123,1,3001,Widget,100,each,2012-09-26 11:31:01\r\n" //
				+ "1,,123,2,4550,Gadget,450,case,2012-09-26 11:31:01\r\n" //
				+ "1,,123,3,3007,Dealybob,300,case,2012-09-26 11:31:02\r\n" //
				+ "1,,123,4,2150,Thingamajig,220,case,2012-09-26 11:31:03\r\n" //
				+ "1,,456,1,3001,Widget,230,each,2012-09-26 11:31:01\r\n" //
				+ "1,,456,2,4550,Gadget,70,case,2012-09-26 11:31:01\r\n" //
				+ "1,,456,3,3007,Dealybob,90,case,2012-09-26 11:31:02\r\n" //
				+ "1,,456,4,2150,Thingamajig,140,case,2012-09-26 11:31:03\r\n" //
				+ "1,,456,5,2170,Doodad,125,each,2012-09-26 11:31:03\r\n" //
				+ "1,PARALLEL,789,1,2150,Thingamajig,125,each,2012-09-26 11:31:03\r\n" //
				+ "1,PARALLEL,789,2,2170,Doodad,125,each,2012-09-26 11:31:03";

		byte csvArray[] = csvString.getBytes();

		ByteArrayInputStream stream = new ByteArrayInputStream(csvArray);
		InputStreamReader reader = new InputStreamReader(stream);

		MockDao<Facility> facilityDao = new MockDao<Facility>();
		Facility facility = new Facility();

		MockDao<OrderGroup> orderGroupDao = new MockDao<OrderGroup>();
		MockDao<OrderHeader> orderHeaderDao = new MockDao<OrderHeader>();
		MockDao<OrderDetail> orderDetailDao = new MockDao<OrderDetail>();
		MockDao<Container> containerDao = new MockDao<Container>();
		MockDao<ItemMaster> itemMasterDao = new MockDao<ItemMaster>();
		MockDao<Item> itemDao = new MockDao<Item>();

		CsvImporter importer = new CsvImporter(orderGroupDao, orderHeaderDao, orderDetailDao, containerDao, itemMasterDao, itemDao);
		importer.importOrdersFromCsvStream(reader, facility);

		OrderHeader order = facility.findOrder("123");
		Assert.assertNotNull(order);
		Assert.assertEquals(order.getPickStrategyEnum(), PickStrategyEnum.SERIAL);

		order = facility.findOrder("789");
		Assert.assertNotNull(order);
		Assert.assertEquals(order.getPickStrategyEnum(), PickStrategyEnum.PARALLEL);

	}

	@Test
	public void testOrderImporterWithPreassignedContainerIdFromCsvStream() {

		String csvString = "orderGroupId,preAssignedContainerId,orderId,orderDetailId,itemId,description,quantity,uomId,orderDate\r\n" //
				+ "1,,123,1,3001,Widget,100,each,2012-09-26 11:31:01\r\n" //
				+ "1,,123,2,4550,Gadget,450,case,2012-09-26 11:31:01\r\n" //
				+ "1,,123,3,3007,Dealybob,300,case,2012-09-26 11:31:02\r\n" //
				+ "1,,123,4,2150,Thingamajig,220,case,2012-09-26 11:31:03\r\n" //
				+ "1,,456,1,3001,Widget,230,each,2012-09-26 11:31:01\r\n" //
				+ "1,,456,2,4550,Gadget,70,case,2012-09-26 11:31:01\r\n" //
				+ "1,,456,3,3007,Dealybob,90,case,2012-09-26 11:31:02\r\n" //
				+ "1,,456,4,2150,Thingamajig,140,case,2012-09-26 11:31:03\r\n" //
				+ "1,,456,5,2170,Doodad,125,each,2012-09-26 11:31:03\r\n" //
				+ "1,CONTAINER1,789,1,2150,Thingamajig,125,each,2012-09-26 11:31:03\r\n" //
				+ "1,CONTAINER1,789,2,2170,Doodad,125,each,2012-09-26 11:31:03";

		byte csvArray[] = csvString.getBytes();

		ByteArrayInputStream stream = new ByteArrayInputStream(csvArray);
		InputStreamReader reader = new InputStreamReader(stream);

		MockDao<Facility> facilityDao = new MockDao<Facility>();
		Facility facility = new Facility();

		MockDao<OrderGroup> orderGroupDao = new MockDao<OrderGroup>();
		MockDao<OrderHeader> orderHeaderDao = new MockDao<OrderHeader>();
		MockDao<OrderDetail> orderDetailDao = new MockDao<OrderDetail>();
		MockDao<Container> containerDao = new MockDao<Container>();
		MockDao<ItemMaster> itemMasterDao = new MockDao<ItemMaster>();
		MockDao<Item> itemDao = new MockDao<Item>();

		CsvImporter importer = new CsvImporter(orderGroupDao, orderHeaderDao, orderDetailDao, containerDao, itemMasterDao, itemDao);
		importer.importOrdersFromCsvStream(reader, facility);

		OrderHeader order = facility.findOrder("789");
		Assert.assertNotNull(order);

		Container container = containerDao.findByDomainId(facility, "CONTAINER1");
		Assert.assertNotNull(container);
	}
}
