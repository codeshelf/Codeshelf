/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: OrderImporterTest.java,v 1.6 2012/10/30 15:21:34 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.edi;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;

import junit.framework.Assert;

import org.junit.Test;

import com.gadgetworks.codeshelf.model.PickStrategyEnum;
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

		MockDao<Organization> organizationDao = new MockDao<Organization>();
		Organization organization = new Organization();
		organization.setDomainId("O1");

		Facility.DAO = new MockDao<Facility>();
		Facility facility = new Facility();
		facility.setParentOrganization(organization);
		facility.setDomainId("F1");

		MockDao<OrderGroup> orderGroupDao = new MockDao<OrderGroup>();
		MockDao<OrderHeader> orderHeaderDao = new MockDao<OrderHeader>();
		MockDao<OrderDetail> orderDetailDao = new MockDao<OrderDetail>();
		MockDao<Container> containerDao = new MockDao<Container>();
		MockDao<ItemMaster> itemMasterDao = new MockDao<ItemMaster>();
		MockDao<Item> itemDao = new MockDao<Item>();
		ITypedDao<UomMaster> uomMasterDao = new MockDao<UomMaster>();

		CsvImporter importer = new CsvImporter(orderGroupDao, orderHeaderDao, orderDetailDao, containerDao, itemMasterDao, itemDao, uomMasterDao);
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

		MockDao<Organization> organizationDao = new MockDao<Organization>();
		Organization organization = new Organization();
		organization.setDomainId("O1");

		Facility.DAO = new MockDao<Facility>();
		Facility facility = new Facility();
		facility.setParentOrganization(organization);
		facility.setDomainId("F1");

		MockDao<OrderGroup> orderGroupDao = new MockDao<OrderGroup>();
		MockDao<OrderHeader> orderHeaderDao = new MockDao<OrderHeader>();
		MockDao<OrderDetail> orderDetailDao = new MockDao<OrderDetail>();
		MockDao<Container> containerDao = new MockDao<Container>();
		MockDao<ItemMaster> itemMasterDao = new MockDao<ItemMaster>();
		MockDao<Item> itemDao = new MockDao<Item>();
		ITypedDao<UomMaster> uomMasterDao = new MockDao<UomMaster>();

		CsvImporter importer = new CsvImporter(orderGroupDao, orderHeaderDao, orderDetailDao, containerDao, itemMasterDao, itemDao, uomMasterDao);
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

		MockDao<Organization> organizationDao = new MockDao<Organization>();
		Organization organization = new Organization();
		organization.setDomainId("O1");

		MockDao<Facility> facilityDao = new MockDao<Facility>();
		Facility facility = new Facility();
		facility.setParentOrganization(organization);
		facility.setDomainId("F!");

		MockDao<OrderGroup> orderGroupDao = new MockDao<OrderGroup>();
		MockDao<OrderHeader> orderHeaderDao = new MockDao<OrderHeader>();
		MockDao<OrderDetail> orderDetailDao = new MockDao<OrderDetail>();
		MockDao<Container> containerDao = new MockDao<Container>();
		MockDao<ItemMaster> itemMasterDao = new MockDao<ItemMaster>();
		MockDao<Item> itemDao = new MockDao<Item>();
		ITypedDao<UomMaster> uomMasterDao = new MockDao<UomMaster>();

		CsvImporter importer = new CsvImporter(orderGroupDao, orderHeaderDao, orderDetailDao, containerDao, itemMasterDao, itemDao, uomMasterDao);
		importer.importOrdersFromCsvStream(reader, facility);

		OrderHeader order = facility.findOrder("789");
		Assert.assertNotNull(order);

		Container container = containerDao.findByDomainId(facility, "CONTAINER1");
		Assert.assertNotNull(container);
	}
}
