/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: OrderImporterTest.java,v 1.1 2012/10/12 07:55:56 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.edi;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;

import junit.framework.Assert;

import org.junit.Test;

import com.gadgetworks.codeshelf.model.dao.MockDao;
import com.gadgetworks.codeshelf.model.domain.Facility;
import com.gadgetworks.codeshelf.model.domain.ItemMaster;
import com.gadgetworks.codeshelf.model.domain.OrderDetail;
import com.gadgetworks.codeshelf.model.domain.OrderGroup;
import com.gadgetworks.codeshelf.model.domain.OrderHeader;
import com.gadgetworks.codeshelf.model.domain.UomMaster;

/**
 * @author jeffw
 *
 */
public class OrderImporterTest {

	/**
	 * Test method for {@link com.gadgetworks.codeshelf.edi.OrderImporter#importerFromCsvStream(java.io.InputStreamReader, com.gadgetworks.codeshelf.model.domain.Facility)}.
	 */
	@Test
	public void testImporterFromCsvStream() {

		String csvString = "orderGroupId,orderId,orderDetailId,itemId,description,quantity,uomId,orderDate\r\n" //
				+ "1,123,1,3001,Widget,100,each,2012-09-26 11:31:01\r\n" //
				+ "1,123,2,4550,Gadget,450,case,2012-09-26 11:31:01\r\n" //
				+ "1,123,3,3007,Dealybob,300,case,2012-09-26 11:31:02\r\n" //
				+ "1,123,4,2150,Thingamajig,220,case,2012-09-26 11:31:03\r\n" //
				+ "1,456,1,3001,Widget,230,each,2012-09-26 11:31:01\r\n" //
				+ "1,456,2,4550,Gadget,70,case,2012-09-26 11:31:01\r\n" //
				+ "1,456,3,3007,Dealybob,90,case,2012-09-26 11:31:02\r\n" //
				+ "1,456,4,2150,Thingamajig,140,case,2012-09-26 11:31:03\r\n" //
				+ "1,456,5,2170,Doodad,125,each,2012-09-26 11:31:03";

		byte csvArray[] = csvString.getBytes();

		ByteArrayInputStream stream = new ByteArrayInputStream(csvArray);
		InputStreamReader reader = new InputStreamReader(stream);

		MockDao<Facility> facilityDao = new MockDao<Facility>();
		Facility facility = new Facility();

		MockDao<OrderGroup> orderGroupDao = new MockDao<OrderGroup>();
		MockDao<OrderHeader> orderHeaderDao = new MockDao<OrderHeader>();
		MockDao<OrderDetail> orderDetailDao = new MockDao<OrderDetail>();
		MockDao<ItemMaster> itemMasterDao = new MockDao<ItemMaster>();
		MockDao<UomMaster> uomMasterDao = new MockDao<UomMaster>();

		OrderImporter importer = new OrderImporter(orderGroupDao, orderHeaderDao, orderDetailDao, itemMasterDao, uomMasterDao);
		importer.importerFromCsvStream(reader, facility);

		OrderHeader order = facility.findOrder("123");
		Assert.assertNotNull(order);

	}
}
