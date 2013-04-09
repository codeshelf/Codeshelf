/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: OrderImporterTest.java,v 1.10 2013/04/09 07:58:20 jeffw Exp $
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
import com.gadgetworks.codeshelf.model.domain.ContainerKind;
import com.gadgetworks.codeshelf.model.domain.ContainerUse;
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

		String csvString = "orderGroupId,shipmentId,customerId,preAssignedContainerId,orderId,orderDetailId,itemId,description,quantity,uomId,orderDate,dueDate,workSequence"
				+ "\r\n1,USF314,COSTCO,123,123,1,10700589,Napa Valley Bistro - Jalapeño Stuffed Olives,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,123,123,2,10706952,Italian Homemade Style Basil Pesto,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,123,123,3,10706962,Authentic Pizza Sauces,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,123,123,4,10100250,Organic Fire-Roasted Red Bell Peppers,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,456,456,1,10711111,Napa Valley Bistro - Jalapeño Stuffed Olives,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,456,456,2,10722222,Italian Homemade Style Basil Pesto,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,456,456,3,10706962,Authentic Pizza Sauces,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,456,456,4,10100250,Organic Fire-Roasted Red Bell Peppers,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,456,456,5,10706961,Sun Ripened Dried Tomato Pesto,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,789,789,1,10100250,Organic Fire-Roasted Red Bell Peppers,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,789,789,2,10706961,Sun Ripened Dried Tomato Pesto,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0";

		byte csvArray[] = csvString.getBytes();

		ByteArrayInputStream stream = new ByteArrayInputStream(csvArray);
		InputStreamReader reader = new InputStreamReader(stream);

		ITypedDao<Organization> organizationDao = new MockDao<Organization>();
		ITypedDao<OrderGroup> orderGroupDao = new MockDao<OrderGroup>();
		ITypedDao<OrderHeader> orderHeaderDao = new MockDao<OrderHeader>();
		ITypedDao<OrderDetail> orderDetailDao = new MockDao<OrderDetail>();
		ITypedDao<Container> containerDao = new MockDao<Container>();
		ITypedDao<ContainerUse> containerUseDao = new MockDao<ContainerUse>();
		ITypedDao<ItemMaster> itemMasterDao = new MockDao<ItemMaster>();
		ITypedDao<Item> itemDao = new MockDao<Item>();
		ITypedDao<UomMaster> uomMasterDao = new MockDao<UomMaster>();

		ContainerKind.DAO = new MockDao<ContainerKind>();

		Organization organization = new Organization();
		organization.setDomainId("O1");

		Facility.DAO = new MockDao<Facility>();
		Facility facility = new Facility();
		facility.setParent(organization);
		facility.setDomainId("F1");
		facility.createDefaultContainerKind();
		
		ContainerKind kind = new ContainerKind();
		kind.setParent(facility);
		kind.setDomainId(ContainerKind.DEFAULT_CONTAINER_KIND);

		CsvImporter importer = new CsvImporter(orderGroupDao, orderHeaderDao, orderDetailDao, containerDao, containerUseDao, itemMasterDao, itemDao, uomMasterDao);
		importer.importOrdersFromCsvStream(reader, facility);

		OrderHeader order = facility.findOrder("123");
		Assert.assertNotNull(order);

	}

	@Test
	public void testOrderImporterWithPickStrategyFromCsvStream() {

		String csvString = "orderGroupId,pickStrategy,orderId,orderDetailId,itemId,description,quantity,uomId,orderDate, dueDate\r\n" //
				+ "1,,123,1,3001,Widget,100,each,2012-09-26 11:31:01,2012-09-26 11:31:01\r\n" //
				+ "1,,123,2,4550,Gadget,450,case,2012-09-26 11:31:01,2012-09-26 11:31:01\r\n" //
				+ "1,,123,3,3007,Dealybob,300,case,2012-09-26 11:31:02,2012-09-26 11:31:01\r\n" //
				+ "1,,123,4,2150,Thingamajig,220,case,2012-09-26 11:31:03,2012-09-26 11:31:01\r\n" //
				+ "1,,456,1,3001,Widget,230,each,2012-09-26 11:31:01,2012-09-26 11:31:01\r\n" //
				+ "1,,456,2,4550,Gadget,70,case,2012-09-26 11:31:01,2012-09-26 11:31:01\r\n" //
				+ "1,,456,3,3007,Dealybob,90,case,2012-09-26 11:31:02,2012-09-26 11:31:01\r\n" //
				+ "1,,456,4,2150,Thingamajig,140,case,2012-09-26 11:31:03,2012-09-26 11:31:01\r\n" //
				+ "1,,456,5,2170,Doodad,125,each,2012-09-26 11:31:03,2012-09-26 11:31:01\r\n" //
				+ "1,PARALLEL,789,1,2150,Thingamajig,125,each,2012-09-26 11:31:03,2012-09-26 11:31:01\r\n" //
				+ "1,PARALLEL,789,2,2170,Doodad,125,each,2012-09-26 11:31:03,2012-09-26 11:31:01";

		byte csvArray[] = csvString.getBytes();

		ByteArrayInputStream stream = new ByteArrayInputStream(csvArray);
		InputStreamReader reader = new InputStreamReader(stream);

		ITypedDao<Organization> organizationDao = new MockDao<Organization>();
		ITypedDao<OrderGroup> orderGroupDao = new MockDao<OrderGroup>();
		ITypedDao<OrderHeader> orderHeaderDao = new MockDao<OrderHeader>();
		ITypedDao<OrderDetail> orderDetailDao = new MockDao<OrderDetail>();
		ITypedDao<Container> containerDao = new MockDao<Container>();
		ITypedDao<ContainerUse> containerUseDao = new MockDao<ContainerUse>();
		ITypedDao<ItemMaster> itemMasterDao = new MockDao<ItemMaster>();
		ITypedDao<Item> itemDao = new MockDao<Item>();
		ITypedDao<UomMaster> uomMasterDao = new MockDao<UomMaster>();

		ContainerKind.DAO = new MockDao<ContainerKind>();

		Organization organization = new Organization();
		organization.setDomainId("O1");

		Facility.DAO = new MockDao<Facility>();
		Facility facility = new Facility();
		facility.setParent(organization);
		facility.setDomainId("F1");
		facility.createDefaultContainerKind();

		CsvImporter importer = new CsvImporter(orderGroupDao, orderHeaderDao, orderDetailDao, containerDao, containerUseDao, itemMasterDao, itemDao, uomMasterDao);
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

		String csvString = "orderGroupId,preAssignedContainerId,orderId,orderDetailId,itemId,description,quantity,uomId,orderDate, dueDate\r\n" //
				+ "1,,123,1,3001,Widget,100,each,2012-09-26 11:31:01,2012-09-26 11:31:01\r\n" //
				+ "1,,123,2,4550,Gadget,450,case,2012-09-26 11:31:01,2012-09-26 11:31:01\r\n" //
				+ "1,,123,3,3007,Dealybob,300,case,2012-09-26 11:31:02,2012-09-26 11:31:01\r\n" //
				+ "1,,123,4,2150,Thingamajig,220,case,2012-09-26 11:31:03,2012-09-26 11:31:01\r\n" //
				+ "1,,456,1,3001,Widget,230,each,2012-09-26 11:31:01,2012-09-26 11:31:01\r\n" //
				+ "1,,456,2,4550,Gadget,70,case,2012-09-26 11:31:01,2012-09-26 11:31:01\r\n" //
				+ "1,,456,3,3007,Dealybob,90,case,2012-09-26 11:31:02,2012-09-26 11:31:01\r\n" //
				+ "1,,456,4,2150,Thingamajig,140,case,2012-09-26 11:31:03,2012-09-26 11:31:01\r\n" //
				+ "1,,456,5,2170,Doodad,125,each,2012-09-26 11:31:03,2012-09-26 11:31:01\r\n" //
				+ "1,CONTAINER1,789,1,2150,Thingamajig,125,each,2012-09-26 11:31:03,2012-09-26 11:31:01\r\n" //
				+ "1,CONTAINER1,789,2,2170,Doodad,125,each,2012-09-26 11:31:03,2012-09-26 11:31:01";

		byte csvArray[] = csvString.getBytes();

		ByteArrayInputStream stream = new ByteArrayInputStream(csvArray);
		InputStreamReader reader = new InputStreamReader(stream);

		ITypedDao<Organization> organizationDao = new MockDao<Organization>();
		ITypedDao<OrderGroup> orderGroupDao = new MockDao<OrderGroup>();
		ITypedDao<OrderHeader> orderHeaderDao = new MockDao<OrderHeader>();
		ITypedDao<OrderDetail> orderDetailDao = new MockDao<OrderDetail>();
		ITypedDao<Container> containerDao = new MockDao<Container>();
		ITypedDao<ContainerUse> containerUseDao = new MockDao<ContainerUse>();
		ITypedDao<ItemMaster> itemMasterDao = new MockDao<ItemMaster>();
		ITypedDao<Item> itemDao = new MockDao<Item>();
		ITypedDao<UomMaster> uomMasterDao = new MockDao<UomMaster>();

		ContainerKind.DAO = new MockDao<ContainerKind>();

		Organization organization = new Organization();
		organization.setDomainId("O1");

		ITypedDao<Facility> facilityDao = new MockDao<Facility>();
		Facility facility = new Facility();
		facility.setParent(organization);
		facility.setDomainId("F1");
		facility.createDefaultContainerKind();

		CsvImporter importer = new CsvImporter(orderGroupDao, orderHeaderDao, orderDetailDao, containerDao, containerUseDao, itemMasterDao, itemDao, uomMasterDao);
		importer.importOrdersFromCsvStream(reader, facility);

		OrderHeader order = facility.findOrder("789");
		Assert.assertNotNull(order);

		Container container = containerDao.findByDomainId(facility, "CONTAINER1");
		Assert.assertNotNull(container);
	}

	@Test
	public void testFailOrderImporterFromCsvStream() {

		// There's no order due date on 123.1, so it should assert/fail to import.
		String csvString = "orderGroupId,shipmentId,customerId,preAssignedContainerId,orderId,orderDetailId,itemId,description,quantity,uomId,orderDate,dueDate,workSequence"
				+ "\r\n1,USF314,COSTCO,123,123,1,10700589,Napa Valley Bistro - Jalapeño Stuffed Olives,1,each,2012-09-26 11:31:01,,0"
				+ "\r\n1,USF314,COSTCO,123,123,2,10706952,Italian Homemade Style Basil Pesto,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,123,123,3,10706962,Authentic Pizza Sauces,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,123,123,4,10100250,Organic Fire-Roasted Red Bell Peppers,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,456,456,1,10711111,Napa Valley Bistro - Jalapeño Stuffed Olives,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,456,456,2,10722222,Italian Homemade Style Basil Pesto,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,456,456,3,10706962,Authentic Pizza Sauces,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,456,456,4,10100250,Organic Fire-Roasted Red Bell Peppers,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,456,456,5,10706961,Sun Ripened Dried Tomato Pesto,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,789,789,1,10100250,Organic Fire-Roasted Red Bell Peppers,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,789,789,2,10706961,Sun Ripened Dried Tomato Pesto,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0";

		byte csvArray[] = csvString.getBytes();

		ByteArrayInputStream stream = new ByteArrayInputStream(csvArray);
		InputStreamReader reader = new InputStreamReader(stream);

		ITypedDao<Organization> organizationDao = new MockDao<Organization>();
		ITypedDao<OrderGroup> orderGroupDao = new MockDao<OrderGroup>();
		ITypedDao<OrderHeader> orderHeaderDao = new MockDao<OrderHeader>();
		ITypedDao<OrderDetail> orderDetailDao = new MockDao<OrderDetail>();
		ITypedDao<Container> containerDao = new MockDao<Container>();
		ITypedDao<ContainerUse> containerUseDao = new MockDao<ContainerUse>();
		ITypedDao<ItemMaster> itemMasterDao = new MockDao<ItemMaster>();
		ITypedDao<Item> itemDao = new MockDao<Item>();
		ITypedDao<UomMaster> uomMasterDao = new MockDao<UomMaster>();

		ContainerKind.DAO = new MockDao<ContainerKind>();

		Organization organization = new Organization();
		organization.setDomainId("O1");

		Facility.DAO = new MockDao<Facility>();
		Facility facility = new Facility();
		facility.setParent(organization);
		facility.setDomainId("F1");
		facility.createDefaultContainerKind();

		CsvImporter importer = new CsvImporter(orderGroupDao, orderHeaderDao, orderDetailDao, containerDao, containerUseDao, itemMasterDao, itemDao, uomMasterDao);
		importer.importOrdersFromCsvStream(reader, facility);

		// We should find order 123
		OrderHeader order = facility.findOrder("123");
		Assert.assertNotNull(order);

		// But not order detail item 1
		OrderDetail orderDetail = order.findOrderDetail("1");
		Assert.assertNull(orderDetail);

		// But should find order detail item 2
		orderDetail = order.findOrderDetail("2");
		Assert.assertNotNull(orderDetail);

	}

}
